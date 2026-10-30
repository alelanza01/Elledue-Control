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
    @Test fun deleteBookingWithConfirmedReportIsRejectedAndIndependentCostsAndDocumentsSurvive()=runBlocking<Unit> {
        val frozen=reportFixture(db,context)
        val service=DeletionRepository(db,context)
        reject{service.prepare("booking","synthetic-booking")}
        assertNotNull(db.reservationDao().get("synthetic-booking"));assertEquals(frozen,ReportRepository(db,context).snapshot(frozen.id))
        assertEquals(3,db.reportDao().expenses().size);assertEquals(2,db.accountingDao().movements().size)
        BackupService(db,context).prepare(BackupService(db,context).export())
    }
    @Test fun deletePropertyWithConfirmedReportIsBlockedAndArchiveKeepsAllRelations()=runBlocking<Unit> {
        val frozen=reportFixture(db,context)
        reject{DeletionRepository(db,context).prepare("property",frozen.property.id)}
        PropertyRepository(db).archive(frozen.property.id,true)
        assertEquals(frozen,ReportRepository(db,context).snapshot(frozen.id));assertEquals(3,db.reportDao().expenses().size)
        assertNotNull(db.reservationDao().get("synthetic-booking"));assertTrue(db.propertyDao().get(frozen.property.id)!!.property.archived)
        db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use{assertFalse(it.moveToFirst())}
    }
    @Test fun changedDataRequiresFreshBackupAndStrongConfirmation()=runBlocking<Unit> {
        PropertyRepository(db).save(PropertyDraft(id="p",ownerId="o",name="Casa",address="Test",ownerName="Test"))
        val service=DeletionRepository(db,context);val p=service.prepare("property","p");val receipt=service.saveVerifiedBackup(p,Uri.fromFile(File(context.cacheDir,"before-change.json")))
        reject{service.delete(p,receipt,"Casa",true)};PropertyRepository(db).archive("p",true);reject{service.delete(p,receipt,"ELIMINA",true)};assertNotNull(db.propertyDao().get("p"))
    }
    @Test fun deletionRequiresTheSafetyBackupStillToExistAndDraftCleanupMatchesIdsPrecisely()=runBlocking<Unit>{
        val props=PropertyRepository(db);props.save(PropertyDraft(id="p",ownerId="o",name="Casa",address="Test",ownerName="Test"))
        val booking=BookingDraft(id="b",propertyId="p",firstName="Nome",lastName="Test",gross="1000");ReservationRepository(db).save(booking)
        val store=DraftStore.get(context);store.save("booking/b/edit",booking);store.save("booking/unrelated-b/edit",booking.copy(id="unrelated-b"))
        store.save("property/new",PropertyDraft(name="Bozza indipendente"));store.save("expense/new/OTHER?booking=b",ExpenseDraft(propertyId="p",reservationId="b"))
        val service=DeletionRepository(db,context);val plan=service.prepare("booking","b");val file=File(context.cacheDir,"still-readable.json")
        val receipt=service.saveVerifiedBackup(plan,Uri.fromFile(file));file.delete()
        reject{service.delete(plan,receipt,"ELIMINA",true)};assertNotNull(db.reservationDao().get("b"))
        val verified=service.saveVerifiedBackup(plan,Uri.fromFile(file));service.delete(plan,verified,"ELIMINA",true)
        assertFalse(store.has("booking/b/edit"));assertFalse(store.has("expense/new/OTHER?booking=b"));assertTrue(store.has("booking/unrelated-b/edit"));assertTrue(store.has("property/new"))
    }

}
