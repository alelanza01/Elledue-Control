package it.elledue.control

import android.Manifest
import android.app.*
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import it.elledue.control.data.*
import it.elledue.control.domain.*
import it.elledue.control.notifications.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=ElledueApplication::class)
class FinalNotificationTest {
 private val app=ApplicationProvider.getApplicationContext<ElledueApplication>()
 private fun permit(){Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS);app.reminders.channel()}
 private suspend fun seed(){app.propertyRepository.save(PropertyDraft(id="p",ownerId="o",name="Casa test",address="Test",ownerName="Test"))}
 @Test fun immediateProbeReportsPermissionAndChannelFailuresRatherThanFalseSuccess() {
  Shadows.shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS);assertFalse(app.reminders.permissionGranted());assertTrue(app.reminders.blockedReason()!!.contains("Permesso"))
  try{app.reminders.test();fail()}catch(_:IllegalStateException){}
  permit();app.reminders.test();assertNotNull(app.reminderStore.status("last-posted"))
  val manager=app.getSystemService(NotificationManager::class.java);manager.deleteNotificationChannel(ReminderEngine.CHANNEL);manager.createNotificationChannel(NotificationChannel(ReminderEngine.CHANNEL,"Bloccato",NotificationManager.IMPORTANCE_NONE))
  assertFalse(app.reminders.channelEnabled());assertFalse(app.reminders.allowed());assertTrue(app.reminders.blockedReason()!!.contains("Canale"))
 }
 @Test fun scheduledProbePersistsAcceptedAlarmDeliversAndDeduplicatesWithoutAnyBusinessRecord()=runBlocking<Unit>{
  permit();app.reminderStore.save(ReminderSettings(enabled=true,monthly=false,quietEnabled=true));val job=app.reminders.scheduleTest()
  assertEquals(job,app.reminderStore.scheduled()[job.key]);assertTrue(Shadows.shadowOf(app.getSystemService(AlarmManager::class.java)).scheduledAlarms.isNotEmpty())
  app.reminders.deliver(job.key,job.version,Instant.ofEpochMilli(job.at));app.reminders.deliver(job.key,job.version,Instant.ofEpochMilli(job.at))
  assertEquals(1,Shadows.shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications.size);assertNotNull(app.reminderStore.status("test-posted"));assertNotNull(app.reminderStore.status("last-receiver"));assertTrue(app.reservationRepository.reservations.first().isEmpty())
 }
 @Test fun disabledRemindersRejectScheduledProbeAndRemoveAllAcceptedJobs()=runBlocking<Unit>{
  permit();app.reminderStore.save(ReminderSettings(enabled=true,monthly=false));app.reminders.scheduleTest();app.reminderStore.save(ReminderSettings(enabled=false));app.reminders.reconcile(force=true)
  assertTrue(app.reminderStore.scheduled().isEmpty());try{app.reminders.scheduleTest();fail()}catch(_:IllegalStateException){}
 }
 @Test fun modificationDeletionAndForceRepairUseCurrentAlarmsOnly()=runBlocking<Unit>{
  permit();seed();val due=Instant.now().plusSeconds(3600).toEpochMilli();app.reminderStore.save(ReminderSettings(enabled=true,monthly=false,taskLeadMinutes=0));app.activityRepository.save(ActivityEntity("t","p","Prima",due))
  app.reminders.reconcile(force=true);val old=app.reminderStore.scheduled().getValue("task:t")
  app.activityRepository.save(ActivityEntity("t","p","Dopo",due+60000));app.reminders.reconcile(force=true);val changed=app.reminderStore.scheduled().getValue("task:t");assertNotEquals(old.version,changed.version)
  val alarms=app.getSystemService(AlarmManager::class.java);val shadow=Shadows.shadowOf(alarms);shadow.scheduledAlarms.toList().forEach{alarms.cancel(it.operation!!)};assertTrue(shadow.scheduledAlarms.isEmpty())
  app.reminders.reconcile(force=true);assertFalse(shadow.scheduledAlarms.isEmpty());app.activityRepository.delete("t");app.reminders.reconcile(force=true);assertFalse(app.reminderStore.scheduled().containsKey("task:t"))
 }
 @Test fun actualBootReceiverRebuildsPersistedFutureTestAndRecordsTheBroadcast()=runBlocking<Unit>{
  permit();app.reminderStore.save(ReminderSettings(enabled=true,monthly=false));val job=app.reminders.scheduleTest();val alarm=app.getSystemService(AlarmManager::class.java);val shadow=Shadows.shadowOf(alarm);shadow.scheduledAlarms.toList().forEach{alarm.cancel(it.operation!!)}
  app.sendBroadcast(Intent(app,ReminderReceiver::class.java).setAction(Intent.ACTION_BOOT_COMPLETED));org.robolectric.shadows.ShadowLooper.idleMainLooper()
  withTimeout(10000){while(shadow.scheduledAlarms.isEmpty() || app.reminderStore.status("last-broadcast")!=Intent.ACTION_BOOT_COMPLETED)delay(20)}
  assertEquals(job,app.reminderStore.scheduled()[job.key])
 }
 @Test fun realAlarmBroadcastRunsReceiverAndRejectsPrematureOrStaleDelivery()=runBlocking<Unit>{
  permit();app.reminderStore.save(ReminderSettings(enabled=true,monthly=false));val job=app.reminders.scheduleTest()
  app.sendBroadcast(Intent(app,ReminderReceiver::class.java).setAction(ReminderEngine.ACTION).putExtra("key",job.key).putExtra("version",job.version));org.robolectric.shadows.ShadowLooper.idleMainLooper()
  withTimeout(10000){while(app.reminderStore.status("last-receiver")==null)delay(20)}
  assertNull(app.reminderStore.delivered(job.key));app.reminders.deliver(job.key,"obsolete",Instant.ofEpochMilli(job.at));assertNull(app.reminderStore.delivered(job.key))
 }
 @Test fun taxConventionDoesNotCreateRoutineCollectionPromptsAndQuietDstStayLocal() {
  val p=PropertyDraft(id="p",ownerId="o",name="Casa",address="Test",ownerName="Test").toRecord();val b=BookingDraft(id="b",propertyId="p",firstName="Test",lastName="Test",gross="500",checkIn="28/03/2026",checkOut="31/03/2026").toRecord(null,false).copy(automaticTaxAdvance=true,expectedTaxCents=1800)
  val settings=ReminderSettings(enabled=true,monthly=false,arrivals=false,departures=false,operations=false,quietEnabled=true)
  assertTrue(ReminderPlanner.plan(listOf(p),listOf(b),emptyList(),settings,Instant.parse("2026-03-27T00:00:00Z")).isEmpty())
  val quiet=ReminderPlanner.quiet(ZonedDateTime.of(2026,3,28,23,0,0,0,ReminderPlanner.zone),settings);assertEquals(Instant.parse("2026-03-29T06:00:00Z"),quiet.toInstant())
 }
}