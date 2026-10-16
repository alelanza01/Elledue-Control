package it.elledue.control

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.elledue.control.data.*
import it.elledue.control.domain.*
import it.elledue.control.reports.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=android.app.Application::class)
class StabilityDataTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db:AppDatabase
    @Before fun setup(){db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()}
    @After fun close(){db.close()}
    private suspend fun reject(block:suspend()->Unit){try{block();fail("Expected rejection")}catch(_:IllegalArgumentException){}}
    @Test fun fivePlusBookingFieldsReallyPersistAcrossDatabaseCloseReopenAndFeedProjections()=runBlocking<Unit> {
        val name="048-booking-reopen.db";context.deleteDatabase(name)
        val before=BookingDraft(id="b",propertyId="p",firstName="Primo",lastName="Nome",guests="2",checkIn="29/12/2026",checkOut="31/12/2026",gross="1000",taxableGuestCount="2")
        try {
            Room.databaseBuilder(context,AppDatabase::class.java,name).build().withClosing{fileDb ->
                val props=PropertyRepository(fileDb);props.save(PropertyDraft(id="p",ownerId="o",name="Prima",address="Test",ownerName="Test",maxGuests="6"));props.save(PropertyDraft(id="q",ownerId="qo",name="Seconda",address="Test",ownerName="Test",maxGuests="6"))
                val repo=ReservationRepository(fileDb);repo.save(before)
                val current=repo.get("b")!!
                repo.save(BookingDraft.from(current).copy(firstName="Nicolò",lastName="Aggiornato",guests="4",propertyId="q",checkIn="30/12/2026",checkOut="03/01/2027",expectedArrival="16:45",expectedDeparture="11:30",actualArrival="17:00",platform="Airbnb",gross="1200",otaPercent="15",actualOta="150",notes="Nota persistente",correctionReason="Verifica operativa"))
            }
            Room.databaseBuilder(context,AppDatabase::class.java,name).build().withClosing{fileDb ->
                val b=ReservationRepository(fileDb).get("b")!!
                assertEquals(1,fileDb.reportDao().bookings().size);assertEquals("Nicolò",b.guestFirstName);assertEquals("Aggiornato",b.guestLastName);assertEquals(4,b.guestCount);assertEquals("q",b.propertyId);assertEquals("Airbnb",b.platform)
                assertEquals("16:45",b.expectedArrivalTime);assertEquals("17:00",b.actualArrivalTime);assertEquals("11:30",b.expectedDepartureTime);assertEquals("Nota persistente",b.notes)
                assertEquals(120000L,b.grossCents);assertEquals(15000L,b.actualOtaCents);assertEquals(105000L,b.netCents);assertEquals(21000L,b.managementCents);assertEquals("2026-12",b.competenceMonth)
                assertEquals(2400L,fileDb.accountingDao().assessment("b")!!.expectedCents);assertFalse(fileDb.accountingDao().assessment("b")!!.consolidated);assertTrue(fileDb.accountingDao().movements().isEmpty())
                assertTrue(CalendarText.render("Seconda",listOf(b)).contains("Nicolò Aggiornato (4 ospiti)"))
                assertEquals(21000L,PortfolioAnalytics.calculate(YearMonth.of(2026,12),"q",listOf(b)).fees)
                val report=ReportRepository(fileDb,context);assertEquals(21000L,report.snapshot(report.generate("q","2026-12")).summary().fees)
            }
        }finally{context.deleteDatabase(name)}
    }
    @Test fun durableDraftsAreIsolatedClearOnlyAfterSuccessAndRejectStaleOverwrite()=runBlocking<Unit> {
        val store=DraftStore(context);store.routes().forEach(store::clear)
        val props=PropertyRepository(db,store);props.save(PropertyDraft(id="p",ownerId="o",name="Casa",address="Test",ownerName="Test",maxGuests="4"))
        val repo=ReservationRepository(db,store);val original=BookingDraft(id="b",propertyId="p",firstName="Nome",lastName="Test",gross="1000")
        store.save("booking/new",original);store.save("property/new",PropertyDraft(name="Bozza immobile"));store.save("activity/new#title","Solo titolo")
        val reopened=DraftStore(context);assertEquals(original,reopened.load("booking/new",BookingDraft::class.java));assertEquals("Solo titolo",reopened.load("activity/new#title",String::class.java))
        assertTrue(context.getSharedPreferences("local-form-drafts-v1",Context.MODE_PRIVATE).contains("booking/new"))
        reject{repo.save(original.copy(lastName=""))};assertTrue(store.has("booking/new"));assertTrue(db.reportDao().bookings().isEmpty())
        repo.save(original);assertFalse(store.has("booking/new"));assertTrue(store.has("property/new"))
        store.save("booking/new",original.copy(id="unrelated-draft"))
        val stale=BookingDraft.from(repo.get("b")!!);repo.save(stale.copy(notes="Aggiornata"));assertEquals("unrelated-draft",store.load("booking/new",BookingDraft::class.java)!!.id);reject{repo.save(stale.copy(notes="Vecchia bozza"))};assertEquals("Aggiornata",repo.get("b")!!.notes)
        store.routes().forEach(store::clear)
    }
    @Test fun deleteBookingRemovesItsDependenciesAndInvalidatesWholeReportChainButKeepsIndependentCosts()=runBlocking<Unit> {
        val frozen=reportFixture(db,context);val accounting=AccountingRepository(db)
        accounting.saveExpense(ExpenseDraft(id="independent",propertyId=frozen.property.id,categoryId="other",date="15/11/2026",month="2026-11",amount="7"))
        val service=DeletionRepository(db,context);val plan=service.prepare("booking","synthetic-booking")
        assertTrue(plan.reportIds.contains(frozen.id));assertEquals(2,plan.counts["Movimenti e storni"])
        val file=File(context.cacheDir,"delete-backup.json");val receipt=service.saveVerifiedBackup(plan,Uri.fromFile(file))
        reject{service.delete(plan,receipt,"ELIMINA ${plan.label}",false)}
        assertNotNull(db.reservationDao().get("synthetic-booking"))
        service.delete(plan,receipt,"ELIMINA ${plan.label}",true)
        assertNull(db.reservationDao().get("synthetic-booking"));assertNull(db.accountingDao().expense("synthetic-cleaning"));assertNotNull(db.accountingDao().expense("independent"));assertNotNull(db.accountingDao().expense("synthetic-laundry"));assertNull(db.reportDao().get(frozen.id));assertTrue(db.accountingDao().movements().isEmpty())
        BackupService(db,context).restore(BackupService(db,context).prepare(file.readBytes()),true)
        assertEquals(frozen,ReportRepository(db,context).snapshot(frozen.id));assertNotNull(db.reservationDao().get("synthetic-booking"))
    }
    @Test fun deletePropertyIsAtomicOnFailureAndSuccessfulDeletionLeavesNoOrphans()=runBlocking<Unit> {
        val frozen=reportFixture(db,context);PropertyRepository(db).save(PropertyDraft(id="other",ownerId=frozen.owner.id,name="Condivisa",address="Test",ownerName=frozen.owner.name))
        val service=DeletionRepository(db,context){error("Injected pre-commit failure")};val plan=service.prepare("property",frozen.property.id)
        val receipt=service.saveVerifiedBackup(plan,Uri.fromFile(File(context.cacheDir,"before-delete.json")))
        try{service.delete(plan,receipt,"ELIMINA ${plan.label}",true);fail()}catch(_:IllegalStateException){}
        assertEquals(frozen,ReportRepository(db,context).snapshot(frozen.id));assertEquals(3,db.reportDao().expenses().size)
        DeletionRepository(db,context).delete(plan,receipt,"ELIMINA ${plan.label}",true)
        assertNull(db.propertyDao().get(frozen.property.id));assertNotNull(db.propertyDao().get("other"));assertTrue(db.reportDao().bookings().isEmpty());assertTrue(db.reportDao().expenses().isEmpty())
        db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use{assertFalse(it.moveToFirst())}
    }
    @Test fun changedDataRequiresFreshBackupAndStrongConfirmation()=runBlocking<Unit> {
        PropertyRepository(db).save(PropertyDraft(id="p",ownerId="o",name="Casa",address="Test",ownerName="Test"))
        val service=DeletionRepository(db,context);val p=service.prepare("property","p");val receipt=service.saveVerifiedBackup(p,Uri.fromFile(File(context.cacheDir,"before-change.json")))
        reject{service.delete(p,receipt,"Casa",true)};PropertyRepository(db).archive("p",true);reject{service.delete(p,receipt,"ELIMINA Casa",true)};assertNotNull(db.propertyDao().get("p"))
    }
    @Test fun deletionRequiresTheSafetyBackupStillToExistAndDraftCleanupMatchesIdsPrecisely()=runBlocking<Unit>{
        val props=PropertyRepository(db);props.save(PropertyDraft(id="p",ownerId="o",name="Casa",address="Test",ownerName="Test"))
        val booking=BookingDraft(id="b",propertyId="p",firstName="Nome",lastName="Test",gross="1000");ReservationRepository(db).save(booking)
        val store=DraftStore.get(context);store.save("booking/b/edit",booking);store.save("booking/unrelated-b/edit",booking.copy(id="unrelated-b"))
        store.save("property/new",PropertyDraft(name="Bozza indipendente"));store.save("expense/new/OTHER?booking=b",ExpenseDraft(propertyId="p",reservationId="b"))
        val service=DeletionRepository(db,context);val plan=service.prepare("booking","b");val file=File(context.cacheDir,"still-readable.json")
        val receipt=service.saveVerifiedBackup(plan,Uri.fromFile(file));file.delete()
        reject{service.delete(plan,receipt,"ELIMINA ${plan.label}",true)};assertNotNull(db.reservationDao().get("b"))
        val verified=service.saveVerifiedBackup(plan,Uri.fromFile(file));service.delete(plan,verified,"ELIMINA ${plan.label}",true)
        assertFalse(store.has("booking/b/edit"));assertFalse(store.has("expense/new/OTHER?booking=b"));assertTrue(store.has("booking/unrelated-b/edit"));assertTrue(store.has("property/new"))
    }

}
