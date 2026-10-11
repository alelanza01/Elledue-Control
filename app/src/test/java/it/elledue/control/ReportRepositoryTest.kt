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
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class ReportRepositoryTest {
    private lateinit var db: AppDatabase; private lateinit var report: ReportRepository; private lateinit var accounting: AccountingRepository
    private lateinit var reservations: ReservationRepository
    private val property = PropertyDraft(name = "Casa Sintetica", address = "Via Test", ownerName = "Proprietario Sintetico", maxGuests = "4")
    private val input = BookingDraft(propertyId = property.id, firstName = "Ospite", lastName = "Sintetico", guests = "4", checkIn = "29/11/2026", checkOut = "03/12/2026", gross = "1000", actualOta = "195")
    @Before fun setup() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>(); db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        report = ReportRepository(db, context); accounting = AccountingRepository(db); reservations = ReservationRepository(db)
        PropertyRepository(db).save(property); reservations.save(input); accounting.initialize(); accounting.saveTax(input.id, TaxDraft("3", "1", "4"))
    }
    @After fun close() { db.close() }
    private suspend fun reject(block: suspend () -> Unit) { try { block(); fail("Expected rejection") } catch (_: IllegalArgumentException) { } }
    private suspend fun confirm(id: String, acknowledge: Boolean = true) = report.confirm(id, ReportCodec.hash(ReportCodec.encode(report.snapshot(id))), acknowledge)
    private suspend fun addCosts() {
        listOf(ExpenseDraft(propertyId = property.id, date = "30/11/2026", month = "2026-11", categoryId = "cleaning", quantity = "3", unit = "8"),
            ExpenseDraft(propertyId = property.id, date = "30/11/2026", month = "2026-11", categoryId = "laundry", quantity = "2", unit = "5"),
            ExpenseDraft(propertyId = property.id, date = "30/11/2026", month = "2026-11", categoryId = "detergents", amount = "5")).forEach { accounting.saveExpense(it) }
    }
    private fun cash(kind: String, cents: Long, sourceId: String? = null, month: String = "2026-11") = MoneyMovementEntity(UUID.randomUUID().toString(), property.id, input.id, kind, cents, BookingDates.parse("03/12/2026").toEpochDay(), month, "Confermato manualmente", sourceId)
    @Test fun draftUsesActualRowsAndMandatoryFinalExampleIs170() = runBlocking<Unit> {
        addCosts(); val collection = cash("COLLECTION", 3000); accounting.post(collection)
        val id = report.generate(property.id, "2026-11"); val before = report.snapshot(id)
        assertEquals(100000L, before.gross()); assertEquals(19500L, before.ota()); assertEquals(80500L, before.net()); assertEquals(16100L, before.summary().fees)
        assertEquals(20000L, before.summary().balance) // collection alone never subtracts
        accounting.post(cash("COMPENSATION", 3000, collection.id)); report.refresh(id)
        val after = report.snapshot(id); assertEquals(17000L, after.summary().balance); assertEquals(3900L, after.summary().reimbursable)
        assertTrue(after.draft); assertEquals("DRAFT", db.reportDao().get(id)!!.status)
    }
    @Test fun monthAndYearBoundariesNeverAllocateBookingTwice() = runBlocking<Unit> {
        val nov = report.generate(property.id, "2026-11"); confirm(nov)
        val dec = report.generate(property.id, "2026-12"); assertTrue(report.snapshot(dec).bookings.isEmpty())
        val acrossYears = input.copy(id = "year-cross", checkIn = "30/12/2026", checkOut = "05/01/2027"); reservations.save(acrossYears)
        report.refresh(dec); assertEquals(1, report.snapshot(dec).bookings.size); confirm(dec)
        assertTrue(report.snapshot(report.generate(property.id, "2027-01")).bookings.isEmpty())
        assertEquals(2, db.reportDao().allocations().count { it.kind == "BOOKING" })
        assertTrue(report.unassigned().isEmpty())
    }
    @Test fun generationReusesDraftAndRefusesSecondConfirmedDocumentForSamePeriod() = runBlocking<Unit> {
        val first = report.generate(property.id, "2026-11"); assertEquals(first, report.generate(property.id, "2026-11")); confirm(first)
        reject { report.generate(property.id, "2026-11") }; reject { confirm(first) }
        assertEquals(1, report.reports.first().size)
    }
    @Test fun estimatedOtaWarningsRequireAcknowledgmentAndZeroActualPrevails() = runBlocking<Unit> {
        reservations.save(BookingDraft.from(reservations.get(input.id)!!).copy(actualOta = "", correctionReason = "Usa stima"))
        val id = report.generate(property.id, "2026-11"); assertTrue(report.snapshot(id).issues().any { it.message.contains("OTA stimata") })
        reject { confirm(id, false) }
        reservations.save(BookingDraft.from(reservations.get(input.id)!!).copy(actualOta = "0", managementPercent = "25", correctionReason = "Commissione effettiva zero"))
        report.refresh(id); val s = report.snapshot(id)
        assertEquals(0L, s.ota()); assertEquals(25000L, s.summary().fees); assertTrue(s.issues().isEmpty()); confirm(id, false)
    }
    @Test fun sourceEditsRefreshDraftAndStalePreviewCannotBeConfirmed() = runBlocking<Unit> {
        val id = report.generate(property.id, "2026-11"); val stale = ReportCodec.hash(ReportCodec.encode(report.snapshot(id)))
        reservations.save(BookingDraft.from(reservations.get(input.id)!!).copy(actualOta = "150", correctionReason = "OTA verificata"))
        reject { report.confirm(id, stale, true) }
        assertEquals("DRAFT", db.reportDao().get(id)!!.status)
        report.refresh(id, "Nota confermata"); val latest = confirm(id)
        assertEquals(17000L, latest.summary().fees); assertEquals("Nota confermata", latest.notes)
    }
    @Test fun immutableSnapshotAndRevisionKeepOneEconomicAllocation() = runBlocking<Unit> {
        addCosts(); val cash = cash("COLLECTION", 3000); accounting.post(cash); accounting.post(cash("COMPENSATION", 3000, cash.id))
        val id = report.generate(property.id, "2026-11"); val frozen = confirm(id); val json = db.reportDao().get(id)!!.snapshotJson
        reject { reservations.save(BookingDraft.from(reservations.get(input.id)!!).copy(actualOta = "150", correctionReason = "Correzione")) }
        val revision = report.beginRevision(id, "OTA effettive aggiornate")
        reservations.save(BookingDraft.from(reservations.get(input.id)!!).copy(actualOta = "150", correctionReason = "Correzione documentata"))
        report.refresh(revision); val rectified = confirm(revision)
        assertEquals(17900L, rectified.summary().balance); assertEquals(17000L, frozen.summary().balance)
        assertEquals(json, db.reportDao().get(id)!!.snapshotJson); assertFalse(report.snapshot(id).draft)
        assertEquals(2, report.reports.first().size); assertEquals(5, db.reportDao().allocations().size)
        PropertyRepository(db).save(property.copy(name = "Nome nuovo", managementPercent = "40"))
        assertEquals("Casa Sintetica", report.snapshot(id).property.name); assertEquals(17000L, report.snapshot(id).summary().balance)
    }
    @Test fun allocatedExpenseCorrectionsRequireRevisionAndCannotChangePeriod() = runBlocking<Unit> {
        addCosts(); val id = report.generate(property.id, "2026-11"); confirm(id)
        val expense = accounting.expenses.first().first()
        reject { accounting.saveExpense(ExpenseDraft.from(expense).copy(amount = "7", reason = "Rettifica")) }
        val revision = report.beginRevision(id, "Rettifica spesa")
        reject { accounting.saveExpense(ExpenseDraft.from(expense).copy(month = "2026-12", reason = "Cambio mese")) }
        accounting.saveExpense(ExpenseDraft.from(expense).copy(amount = "7", reason = "Rettifica effettiva"))
        val current = report.refresh(revision); assertTrue(current.expenses.any { it.id == expense.id && it.actualCents == 700L })
        assertEquals(3, db.reportDao().allocations().count { it.kind == "EXPENSE" })
    }
    @Test fun collectionAfterConfirmationIsAllowedButAllocatedCompensationReversalRequiresRevision() = runBlocking<Unit> {
        val partial = cash("COLLECTION", 1000); accounting.post(partial); val comp = cash("COMPENSATION", 1000, partial.id); accounting.post(comp)
        val id = report.generate(property.id, "2026-11"); confirm(id)
        accounting.post(cash("COLLECTION", 2600, month = "2026-12"))
        assertEquals(1000L, report.snapshot(id).summary().collectedTax)
        val reversal = comp.copy(id = "reverse", kind = "REVERSAL", sourceId = null, reversesId = comp.id, notes = "Storno motivato")
        reject { accounting.post(reversal) }
        val version = report.beginRevision(id, "Riconciliazione compensazione"); accounting.post(reversal); report.refresh(version)
        assertEquals(0L, report.snapshot(version).summary().compensated); assertEquals(1000L, report.snapshot(id).summary().compensated)
    }
    @Test fun cancelledBookingKeepsHistoricalCashAndForeignExpenseCompetenceRemainsSeparate() = runBlocking<Unit> {
        val cash = cash("COLLECTION", 1000); accounting.post(cash); reservations.cancel(input.id)
        accounting.saveExpense(ExpenseDraft(propertyId = property.id, date = "30/11/2026", month = "2026-12", amount = "5"))
        val nov = report.snapshot(report.generate(property.id, "2026-11")); assertEquals(0L, nov.summary().fees); assertEquals(1000L, nov.summary().collectedTax); assertEquals(0L, nov.summary().expenses)
        val dec = report.snapshot(report.generate(property.id, "2026-12")); assertEquals(500L, dec.summary().expenses)
        assertEquals(0, nov.allocations().count { it.kind == "BOOKING" })
    }
    @Test fun inconsistentEconomicRowsAndPendingTaxReviewBlockConfirmation() = runBlocking<Unit> {
        val b = reservations.get(input.id)!!; db.reservationDao().save(b.copy(netCents = 1))
        val id = report.generate(property.id, "2026-11"); val snap = report.snapshot(id)
        assertTrue(snap.issues().any { it.blocking }); reject { confirm(id) }
        db.reservationDao().save(b); db.accountingDao().markTaxForReview(input.id); report.refresh(id)
        assertTrue(report.snapshot(id).issues().any { it.blocking }); reject { confirm(id) }
    }
    @Test fun perBookingRoundingIsSummedRatherThanRoundingMonthlyNetAgain() = runBlocking<Unit> {
        reservations.cancel(input.id)
        for (i in 1..3) reservations.save(input.copy(id = "small-$i", checkIn = "0${i * 2}/11/2026", checkOut = "0${i * 2 + 1}/11/2026", gross = "0,03", actualOta = "0", managementPercent = "20", guests = "1"))
        val s = report.snapshot(report.generate(property.id, "2026-11"))
        assertEquals(3L, s.summary().fees); assertEquals(2L, Amounts.percentage(s.net(), 2000))
    }
    @Test fun recognizedOwnerPaymentProducesCreditWithoutNegativeTransfer() = runBlocking<Unit> {
        val payment = MoneyMovementEntity("owner-paid", property.id, null, "OWNER_PAYMENT", 30000, BookingDates.parse("30/11/2026").toEpochDay(), "2026-11", "Pagamento riconosciuto")
        accounting.post(payment)
        val id = report.generate(property.id, "2026-11"); val snapshot = confirm(id)
        assertEquals(-13900L, snapshot.summary().balance)
        assertEquals(Amounts.euro(13900), Amounts.euroAbsolute(snapshot.summary().balance))
        assertEquals(30000L, snapshot.summary().payments)
    }

}
