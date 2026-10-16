package it.elledue.control

import android.content.Context
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=android.app.Application::class)
class BusinessModelTest {
    private val context=ApplicationProvider.getApplicationContext<Context>();private lateinit var db:AppDatabase
    @Before fun setup(){db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()}
    @After fun close(){db.close()}
    private suspend fun reject(block:suspend()->Unit){try{block();fail()}catch(_:IllegalArgumentException){}}
    @Test fun ownershipDoesNotInventManagementFeesOwnersOrPartnerSharesAndArchivesAreExcludedBySql()=runBlocking<Unit>{
        val props=PropertyRepository(db);val managed=PropertyDraft(id="m",ownerId="mo",name="Gestito",address="Test",ownerName="Proprietario",maxGuests="4")
        val own=PropertyDraft(id="o",ownerId="oo",name="Proprio",address="Test",businessType="OWNED",managementPercent="",maxGuests="4")
        props.save(managed);props.save(own)
        val b=BookingDraft(id="bm",propertyId="m",firstName="Uno",lastName="Test",gross="1000",actualOta="195",checkIn="29/12/2026",checkOut="03/01/2027")
        ReservationRepository(db).save(b);ReservationRepository(db).save(b.copy(id="bo").withPropertyDefaults(props.get("o")!!).copy(actualOta="195"))
        assertEquals(16100L,db.reservationDao().get("bm")!!.managementCents);assertEquals(0L,db.reservationDao().get("bo")!!.managementCents)
        val scope=OperationalRepository(db).scope("ALL").first();assertEquals(2,scope.properties.size)
        val totals=BusinessAnalytics.calculate("2026-12",scope.properties,scope.bookings,emptyList(),emptyList(),emptyList())
        assertEquals(16100L,totals.fees);assertEquals(100000L,totals.ownGross);assertEquals(80500L,totals.ownResult)
        reject{ReportRepository(db,context).generate("o","2026-12")}
        props.archive("m",true);assertEquals(listOf("o"),db.propertyDao().observeActive("ALL").first().map{it.property.id});assertEquals(listOf("bo"),db.reservationDao().observeOperational().first().map{it.id});assertEquals(2,db.reportDao().bookings().size)
    }
    @Test fun taxProposalUsesKnownSubjectsNightsCapsAndChangesOnlyUntilConsolidated()=runBlocking<Unit>{
        val props=PropertyRepository(db);props.save(PropertyDraft(id="p",ownerId="o",name="Casa",address="Test",ownerName="Test",maxGuests="6",taxMaxNights="4"))
        val repo=ReservationRepository(db)
        repo.save(BookingDraft(id="b",propertyId="p",firstName="Uno",lastName="Test",guests="3",taxableGuestCount="2",gross="1000",checkIn="29/12/2026",checkOut="04/01/2027"))
        assertEquals(2400L,db.accountingDao().assessment("b")!!.expectedCents);assertEquals(1,db.accountingDao().assessment("b")!!.exemptGuests);assertEquals(0L,repo.get("b")!!.collectedTaxCents)
        repo.save(BookingDraft.from(repo.get("b")!!).copy(checkOut="31/12/2026"));assertEquals(1200L,db.accountingDao().assessment("b")!!.expectedCents)
        props.save(PropertyDraft.from(props.get("p")!!).copy(taxRate="4"));assertEquals(1600L,db.accountingDao().assessment("b")!!.expectedCents)
        val old=db.accountingDao().assessment("b")!!;AccountingRepository(db).saveTax("b",TaxDraft.from(old))
        repo.save(BookingDraft.from(repo.get("b")!!).copy(checkOut="01/01/2027"))
        assertEquals(1600L,db.accountingDao().assessment("b")!!.expectedCents);assertTrue(db.accountingDao().assessment("b")!!.needsReview);assertTrue(db.accountingDao().movements().isEmpty())
        repo.save(BookingDraft(id="unknown",propertyId="p",firstName="Senza",lastName="Conteggi",gross="1000",checkIn="01/01/2028",checkOut="03/01/2028"))
        assertNull(db.accountingDao().assessment("unknown"));assertNull(repo.get("unknown")!!.expectedTaxCents)
    }
    @Test fun hiddenIsDifferentFromExcludedAndConfirmedDocumentsRequireVersioning()=runBlocking<Unit>{
        val original=reportFixture(db,context);val repo=ReportRepository(db,context)
        reject{repo.setBookingOption(original.id,"synthetic-booking",true,false)}
        val id=repo.beginRevision(original.id,"Controllo inclusione")
        val before=repo.snapshot(id);val preview=repo.previewBookingOption(id,"synthetic-booking",true,false)
        assertEquals(17000L,preview.summary().balance);assertEquals(before,repo.snapshot(id))
        repo.setBookingOption(id,"synthetic-booking",true,false,ReportCodec.hash(ReportCodec.encode(preview)))
        assertEquals(listOf("synthetic-booking"),repo.snapshot(id).hiddenBookingIds)
        val excluded=repo.previewBookingOption(id,"synthetic-booking",false,true);assertEquals(1500L,excluded.summary().balance)
        repo.setBookingOption(id,"synthetic-booking",false,true,ReportCodec.hash(ReportCodec.encode(excluded)))
        assertTrue(repo.snapshot(id).bookings.isEmpty());assertEquals(3000L,db.reservationDao().get("synthetic-booking")!!.collectedTaxCents);assertEquals(2,db.accountingDao().movements().size)
        repo.setBookingOption(id,"synthetic-booking",false,false);assertEquals(17000L,repo.snapshot(id).summary().balance)
        assertEquals(original,repo.snapshot(original.id));assertEquals(1,db.reportDao().bookings().size)
    }
    @Test fun privateDraftsAreIncludedInBackupAndNoOtherDataIsFinalized()=runBlocking<Unit>{
        val drafts=DraftStore.get(context);drafts.routes().forEach(drafts::clear)
        drafts.save("booking/new",BookingDraft(firstName="Da completare"));drafts.save("activity/new#title","Bozza")
        val backup=BackupService(db,context);val bytes=backup.export();drafts.routes().forEach(drafts::clear)
        backup.restore(backup.prepare(bytes),true)
        assertEquals("Da completare",drafts.load("booking/new",BookingDraft::class.java)!!.firstName);assertEquals("Bozza",drafts.load("activity/new#title",String::class.java));assertTrue(db.reportDao().bookings().isEmpty())
        drafts.routes().forEach(drafts::clear)
    }
}
