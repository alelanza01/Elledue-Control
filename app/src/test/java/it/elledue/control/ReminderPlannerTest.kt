package it.elledue.control

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.elledue.control.data.*
import it.elledue.control.domain.*
import it.elledue.control.notifications.*
import it.elledue.control.security.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=android.app.Application::class)
class ReminderPlannerTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db:AppDatabase
    @Before fun setup(){db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()}
    @After fun close(){db.close()}
    private suspend fun property():PropertyWithOwner {
        PropertyRepository(db).save(PropertyDraft(id="p",ownerId="o",name="Casa test",address="Test",ownerName="Test",maxGuests="4"))
        return db.propertyDao().get("p")!!
    }
    private fun stay()=BookingDraft(id="b",propertyId="p",firstName="Nome",lastName="Test",guests="2",checkIn="29/03/2026",checkOut="31/03/2026",gross="1000",taxableGuestCount="2").toRecord(null,false)
    @Test fun remindersHonorRomeDaylightSavingAndQuietHoursAndUnknownTaxNeverBecomesCash()=runBlocking<Unit>{
        val p=property();val b=stay();val settings=ReminderSettings(enabled=true)
        val jobs=ReminderPlanner.plan(listOf(p),listOf(b),emptyList(),settings,Instant.parse("2026-03-27T00:00:00Z"))
        assertEquals(Instant.parse("2026-03-28T17:00:00Z").toEpochMilli(),jobs.single{it.key=="arrival:b"}.at)
        assertEquals(Instant.parse("2026-03-30T16:00:00Z").toEpochMilli(),jobs.single{it.key=="departure:b"}.at)
        assertTrue(jobs.any{it.key=="tax:b"});assertEquals(0L,b.collectedTaxCents)
        val s=settings.copy(quietEnabled=true,quietStart="22:00",quietEnd="08:00")
        assertEquals("2026-03-29T08:00+02:00[Europe/Rome]",ReminderPlanner.quiet(ZonedDateTime.of(2026,3,28,23,0,0,0,ReminderPlanner.zone),s).toString())
        assertEquals(Instant.parse("2026-10-25T07:00:00Z"),ReminderPlanner.quiet(ZonedDateTime.of(2026,10,24,23,0,0,0,ReminderPlanner.zone),s).toInstant())
    }
    @Test fun cancelledArchivedCompletedAndCollectedEventsAreRemovedAndUpdatesHaveNewIdentity()=runBlocking<Unit>{
        val p=property();val b=stay();val s=ReminderSettings(enabled=true,monthly=false);val now=Instant.parse("2026-03-27T00:00:00Z")
        fun jobs(item:ReservationEntity=b,prop:PropertyWithOwner=p)=ReminderPlanner.plan(listOf(prop),listOf(item),emptyList(),s,now)
        assertTrue(jobs(b.copy(status="CANCELLED")).isEmpty());assertTrue(jobs(prop=p.copy(property=p.property.copy(archived=true))).isEmpty())
        assertFalse(jobs(b.copy(expectedTaxCents=600,collectedTaxCents=600)).any{it.key=="tax:b"})
        assertTrue(jobs(b.copy(status="COMPLETED")).all{it.key=="tax:b"})
        assertNotEquals(jobs().first{it.key=="arrival:b"}.version,jobs(b.copy(checkInEpochDay=b.checkInEpochDay+1,updatedAt=b.updatedAt+1)).first{it.key=="arrival:b"}.version)
        assertTrue(ReminderPlanner.plan(listOf(p),listOf(b),emptyList(),s.copy(enabled=false),now).isEmpty())
    }
    @Test fun actualScheduledCleaningAndDateOnlyTasksNotifyButCompletedOrUnscheduledNeverDo()=runBlocking<Unit>{
        val p=property();val now=Instant.parse("2026-03-27T00:00:00Z");val date=Instant.parse("2026-03-28T00:00:00Z").toEpochMilli()
        val task=ActivityEntity("t","p","Pulizia da eseguire",date,kind="CLEANING",hasTime=false)
        fun jobs(t:ActivityEntity)=ReminderPlanner.plan(listOf(p),emptyList(),listOf(t),ReminderSettings(enabled=true,monthly=false,allDayTime="09:00",taskLeadMinutes=60),now)
        assertEquals(Instant.parse("2026-03-28T07:00:00Z").toEpochMilli(),jobs(task).single().at)
        assertEquals("activity/t",jobs(task).single().route)
        assertTrue(jobs(task.copy(completed=true)).isEmpty());assertTrue(jobs(task.copy(dueEpochMillis=null)).isEmpty())
        assertTrue(ReminderPlanner.plan(listOf(p),emptyList(),listOf(task),ReminderSettings(enabled=true,cleaning=false,monthly=false),now).isEmpty())
    }
    @Test fun settingsAndDeliveredMarkersSurviveANewStoreAndLockCannotBypassColdStartOrTimeout(){
        val reminders=ReminderStore(context);val settings=ReminderSettings(enabled=true,quietEnabled=true);reminders.save(settings)
        val item=Reminder("arrival:b",1,"test","test","booking/b","v1");reminders.scheduled(mapOf(item.key to item));reminders.mark(item)
        val reopened=ReminderStore(context);assertEquals(settings,reopened.load());assertEquals("v1",reopened.delivered(item.key));assertEquals(item,reopened.scheduled()[item.key])
        val lock=AppLock(context);lock.configureAuthenticated(LockSettings(true,60));assertTrue(lock.unlocked.value)
        lock.stop(1000);lock.start(60999);assertTrue(lock.unlocked.value);lock.stop(61000);lock.start(121000);assertFalse(lock.unlocked.value)
        lock.grant();assertTrue(lock.unlocked.value);assertFalse(AppLock(context).unlocked.value)
        lock.configureAuthenticated(LockSettings(false));assertTrue(AppLock(context).unlocked.value)
        assertFalse(context.getSharedPreferences("app-access-v1",Context.MODE_PRIVATE).all.keys.any{it.contains("pin") || it.contains("password")})
    }
}
