package it.elledue.control

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import it.elledue.control.domain.*
import it.elledue.control.notifications.*
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
class ReminderEngineTest {
    private val app=ApplicationProvider.getApplicationContext<ElledueApplication>()
    @Test fun actualAndroidAlarmNotificationPermissionDeepLinkAndDeduplicationAreIntegrated()=runBlocking<Unit>{
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        app.propertyRepository.save(PropertyDraft(id="p",ownerId="o",name="Immobile sintetico",address="Test",ownerName="Test",maxGuests="4"))
        val date=BookingDates.today().plusDays(3)
        val b=BookingDraft(id="b",propertyId="p",firstName="Ospite",lastName="Test",checkIn=BookingDates.display(date.toEpochDay()),checkOut=BookingDates.display(date.plusDays(3).toEpochDay()),gross="1000")
        app.reservationRepository.save(b);app.reminderStore.save(ReminderSettings(enabled=true,monthly=false))
        app.reminders.reconcile(force=true)
        val job=app.reminderStore.scheduled().getValue("arrival:b")
        val alarms=Shadows.shadowOf(app.getSystemService(AlarmManager::class.java))
        assertTrue(alarms.scheduledAlarms.isNotEmpty())
        // Reboot clears AlarmManager state while the persisted schedule remains.
        val manager=app.getSystemService(AlarmManager::class.java)
        alarms.scheduledAlarms.toList().forEach{manager.cancel(requireNotNull(it.operation))}
        assertTrue(alarms.scheduledAlarms.isEmpty());app.reminders.reconcile(force=true);assertTrue(alarms.scheduledAlarms.isNotEmpty())
        app.reminders.deliver(job.key,job.version,Instant.ofEpochMilli(job.at))
        val notifications=Shadows.shadowOf(app.getSystemService(NotificationManager::class.java))
        val shown=notifications.allNotifications.single()
        assertEquals("Check-in imminente",shown.extras.getCharSequence("android.title"))
        assertEquals("booking/b",Shadows.shadowOf(shown.contentIntent).savedIntent.getStringExtra("route"))
        assertEquals(job.version,app.reminderStore.delivered(job.key))
        app.reminders.deliver(job.key,job.version,Instant.ofEpochMilli(job.at));assertEquals(1,notifications.allNotifications.size)
        app.reminders.reconcile(force=true);assertFalse(app.reminderStore.scheduled().containsKey("arrival:b"))
        app.reservationRepository.cancel("b");app.reminders.reconcile(force=true)
        assertTrue(app.reminderStore.scheduled().isEmpty());assertTrue(alarms.scheduledAlarms.isEmpty())
        app.reminderStore.save(ReminderSettings(enabled=false))
    }
    @Test fun deniedRuntimePermissionLeavesNoScheduledAlarmAndTestNotificationCannotPretendSuccess()=runBlocking<Unit>{
        Shadows.shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        app.reminderStore.save(ReminderSettings(enabled=true));app.reminders.reconcile(force=true)
        assertFalse(app.reminders.allowed());assertTrue(app.reminderStore.scheduled().isEmpty())
        try{app.reminders.test();fail()}catch(_:IllegalStateException){}
        assertTrue(Shadows.shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications.isEmpty())
    }
}
