package it.elledue.control
import android.Manifest
import android.app.*
import android.content.*
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import it.elledue.control.data.*
import it.elledue.control.notifications.*
import it.elledue.control.security.LockSettings
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=ElledueApplication::class)
class ConsolidatedReminderTest {
 private val app=ApplicationProvider.getApplicationContext<ElledueApplication>()
 @Test fun scheduledPendingIntentReallyRunsReceiverAndPublishesNotificationWithoutCallingTestButton()=runBlocking<Unit>{
  Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
  app.activityRepository.save(ActivityEntity("scheduled-task",null,"Controllo sintetico",Instant.now().plusSeconds(30).toEpochMilli()))
  app.reminderStore.save(ReminderSettings(enabled=true,taskLeadMinutes=0,monthly=false))
  app.reminders.reconcile(force=true)
  val job=app.reminderStore.scheduled().getValue("task:scheduled-task")
  val alarm=Shadows.shadowOf(app.getSystemService(AlarmManager::class.java)).scheduledAlarms.first{Shadows.shadowOf(it.operation!!).savedIntent.getStringExtra("key")==job.key}
  alarm.operation!!.send()
  val until=System.nanoTime()+10_000_000_000L
  while(app.reminderStore.delivered(job.key)!=job.version && System.nanoTime()<until){Shadows.shadowOf(Looper.getMainLooper()).idle();kotlinx.coroutines.delay(20)}
  assertEquals(job.version,app.reminderStore.delivered(job.key))
  val shown=Shadows.shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications.single()
  assertEquals("activity/scheduled-task",Shadows.shadowOf(shown.contentIntent).savedIntent.getStringExtra("route"))
  app.reminders.reconcile(force=true);assertFalse(app.reminderStore.scheduled().containsKey(job.key))
 }
 @Test fun delayedScheduledJobIsRecoveredOnceAndProtectedNotificationContainsNoPrivateText()=runBlocking<Unit>{
  Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
  val now=Instant.now()
  app.activityRepository.save(ActivityEntity("private",null,"Fornitore e dati riservati",now.plusSeconds(30).toEpochMilli()))
  app.reminderStore.save(ReminderSettings(enabled=true,taskLeadMinutes=0,monthly=false))
  app.reminders.reconcile(force=true);val job=app.reminderStore.scheduled().getValue("task:private")
  app.appLock.configureAuthenticated(LockSettings(enabled=true))
  app.reminders.reconcile(now.plusSeconds(90),force=true)
  assertEquals(job.version,app.reminderStore.delivered(job.key))
  val manager=Shadows.shadowOf(app.getSystemService(NotificationManager::class.java))
  assertEquals(1,manager.allNotifications.size)
  val n=manager.allNotifications.single();assertEquals("Elledue Control",n.extras.getCharSequence("android.title"))
  assertFalse(n.extras.getCharSequence("android.text").toString().contains("Fornitore"))
  app.reminders.reconcile(now.plusSeconds(120),force=true);assertEquals(1,manager.allNotifications.size)
 }
}
