package it.elledue.control

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.elledue.control.data.*
import it.elledue.control.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=android.app.Application::class)
class OperationsRepositoryTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db:AppDatabase
    @Before fun setup()=runBlocking<Unit> {db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build();PropertyRepository(db).save(PropertyDraft(id="p",ownerId="o",name="Casa",address="Test",ownerName="Test"));ReservationRepository(db).save(BookingDraft(id="b",propertyId="p",firstName="Nome",lastName="Test",gross="1000"))}
    @After fun close(){db.close()}
    private suspend fun reject(block:suspend ()->Unit){try{block();fail("Expected validation failure")}catch(_:IllegalArgumentException){}}
    @Test fun titleOnlyTaskCrudIsIdempotentAndCannotDeleteAccounting()=runBlocking<Unit> {
        val repo=ActivityRepository(db);val task=ActivityEntity("t",null," Solo titolo ",null)
        repo.save(task);repo.save(task);assertEquals(1,repo.activities.first().size);assertNull(repo.activities.first().single().dueEpochMillis)
        repo.save(task.copy(propertyId="p",reservationId="b",notes="Nota",dueEpochMillis=1800000000000,completed=true));assertTrue(repo.activities.first().single().completed)
        repo.save(repo.activities.first().single().copy(completed=false));assertFalse(repo.activities.first().single().completed)
        AccountingRepository(db).initialize();AccountingRepository(db).saveExpense(ExpenseDraft(id="e",propertyId="p",reservationId="b",categoryId="cleaning",quantity="3",unit="8"))
        repo.delete("t");assertTrue(repo.activities.first().isEmpty());assertEquals(2400L,db.accountingDao().expense("e")!!.actualCents);assertNotNull(db.reservationDao().get("b"))
        reject {repo.save(task.copy(title=""))};reject {repo.save(task.copy(reservationId="b"))}
    }
    @Test fun messageCrudCategoriesAndScopedBackupRoundTrip()=runBlocking<Unit> {
        val repo=MessageRepository(db);repo.initialize();assertEquals(7,repo.categories.first().size)
        val global=GuestMessageEntity("g",null,"pre","Arrivo","Ciao {nome_ospite}","Hello {nome_ospite}")
        val scoped=global.copy(id="s",propertyId="p",title="Casa")
        repo.save(global);repo.save(scoped);repo.save(scoped.copy(archived=true));assertEquals(2,repo.messages.first().size)
        repo.category(MessageCategoryEntity("custom","Personalizzata"));repo.save(global.copy(categoryId="custom"));reject{repo.deleteCategory("custom")}
        SettingsRepository(db.settingsDao()).cover(3)
        ActivityRepository(db).save(ActivityEntity("task","p","Verifica",1800000000000,notes="Nota salvata",reservationId="b"))
        val service=BackupService(db,context);val bytes=service.export();val prepared=service.prepare(bytes)
        repo.delete(scoped);repo.delete(global);service.restore(prepared,true)
        assertEquals(2,repo.messages.first().size);assertTrue(repo.messages.first().first{it.id=="s"}.archived);assertEquals(3,db.settingsDao().get()!!.homeCover)
        assertEquals("Nota salvata",db.activityDao().get("task")!!.notes)
        reject{PropertyRepository(db).deleteEmpty("p")}
    }
    @Test fun v5BackupUpgradesAndDoesNotInventNewData()=runBlocking<Unit> {
        val report=ReportRepository(db,context);val service=BackupService(db,context)
        db.settingsDao().save(AppSettings(organizationName="Storica"));ActivityRepository(db).save(ActivityEntity("t",null,"Storica",null))
        val root=JSONObject(service.export().toString(Charsets.UTF_8));val payload=JSONObject(root.getString("payload"))
        payload.remove("guest_messages");payload.remove("message_categories");payload.getJSONArray("activities").getJSONObject(0).remove("notes");payload.getJSONArray("app_settings").getJSONObject(0).remove("homeCover");payload.getJSONArray("app_settings").getJSONObject(0).remove("messageCategoriesInitialized")
        root.put("databaseVersion",5).put("payload",payload.toString()).put("sha256",it.elledue.control.reports.ReportCodec.hash(payload.toString()))
        service.restore(service.prepare(root.toString().toByteArray()),true)
        assertEquals("Storica",db.settingsDao().get()!!.organizationName);assertEquals(0,db.settingsDao().get()!!.homeCover);assertEquals("",db.activityDao().get("t")!!.notes);assertTrue(db.messageDao().all().isEmpty());assertEquals(16100L,db.reservationDao().get("b")!!.managementCents)
    }
    @Test fun portalRatesAreExplicitDirectZeroActualWinsAndSnapshotsNeverRefresh()=runBlocking<Unit> {
        val p=db.propertyDao().get("p")!!
        assertEquals("19,50",BookingDraft.otaDefault(p,"Airbnb"));assertEquals("0,00",BookingDraft.otaDefault(p,"Prenotazione diretta"))
        PropertyRepository(db).save(PropertyDraft(id="p",ownerId="o",name="Casa",address="Test",ownerName="Test",platformRates="Booking.com=19,5\nAirbnb=15\nVrbo=8"))
        val configured=db.propertyDao().get("p")!!
        assertEquals("15,00",BookingDraft.otaDefault(configured,"Airbnb"));assertEquals("8,00",BookingDraft.otaDefault(configured,"Vrbo"))
        ReservationRepository(db).save(BookingDraft(id="second",propertyId="p",firstName="Altro",lastName="Ospite",gross="1000",checkIn="01/01/2028",checkOut="03/01/2028",otaPercent="",actualOta="0"))
        assertEquals(20000L,db.reservationDao().get("second")!!.managementCents);assertEquals(16100L,db.reservationDao().get("b")!!.managementCents)
    }
    @Test fun linkedTaskFollowsLegitimateBookingMoveAndBackupRemainsRestorable()=runBlocking<Unit> {
        PropertyRepository(db).save(PropertyDraft(id="q",ownerId="qo",name="Altra",address="Test",ownerName="Test"))
        ActivityRepository(db).save(ActivityEntity("task","p","Da spostare",null,reservationId="b"))
        ReservationRepository(db).save(BookingDraft.from(db.reservationDao().get("b")!!).copy(propertyId="q"))
        assertEquals("q",db.activityDao().get("task")!!.propertyId)
        BackupService(db,context).prepare(BackupService(db,context).export())
    }

    @Test fun removedDefaultMessageCategoryDoesNotReappearOnReopeningLibrary()=runBlocking<Unit> {
        val repo=MessageRepository(db);repo.initialize();repo.deleteCategory("pre");repo.initialize()
        assertEquals(6,repo.categories.first().size);assertNull(db.messageDao().category("pre"))
    }

}
