package it.elledue.control

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.elledue.control.data.*
import it.elledue.control.domain.*
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
class AccountingRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var accounting: AccountingRepository
    private lateinit var bookings: ReservationRepository
    private val property = PropertyDraft(name = "Casa Contabilità", address = "Via Test", ownerName = "Proprietario Test", maxGuests = "4")
    private val other = property.copy(id = "other-property", ownerId = "other-owner", name = "Altra Casa")
    private val input = BookingDraft(propertyId = property.id, firstName = "Ospite", lastName = "Test", guests = "4", checkIn = "10/06/2026", checkOut = "14/06/2026", gross = "1000")
    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        accounting = AccountingRepository(db); bookings = ReservationRepository(db)
        PropertyRepository(db).save(property); PropertyRepository(db).save(other)
        bookings.save(input); accounting.initialize()
    }
    @After fun close() { db.close() }
    private fun expense() = ExpenseDraft(propertyId = property.id, date = "10/06/2026", month = "2026-06", categoryId = "detergents", amount = "5")
    private suspend fun tax() { accounting.saveTax(input.id, TaxDraft("3", "1", "4")) }
    private fun movement(kind: String, cents: Long, source: String? = null, reverse: String? = null) = MoneyMovementEntity(UUID.randomUUID().toString(), property.id, input.id, kind, cents, input.toRecord(null, false).checkInEpochDay, "2026-06", "Verificato", source, reverse)
    private suspend fun rejects(action: suspend () -> Unit) { try { action(); fail("Expected validation failure") } catch (_: IllegalArgumentException) { } }

    @Test fun categoriesAreEditableAndSeedDoesNotOverwriteCustomization() = runBlocking {
        accounting.saveCategory("custom", "Giardinaggio")
        accounting.saveCategory("detergents", "Prodotti pulizia")
        accounting.initialize()
        assertEquals(10, accounting.categories.first().size)
        assertEquals("Prodotti pulizia", db.accountingDao().category("detergents")!!.label)
        accounting.saveExpense(expense())
        accounting.saveCategory("detergents", "Nuovo nome", archived = true)
        assertEquals("Prodotti pulizia", accounting.expenses.first().single().category)
        rejects { accounting.saveExpense(expense()) }
    }
    @Test fun expensesSeparatePropertiesAndRejectBookingFromOtherProperty() = runBlocking {
        accounting.saveExpense(expense()); accounting.saveExpense(expense().copy(propertyId = other.id, amount = "10"))
        val entries = accounting.expenses.first()
        assertEquals(500L, operationalSummary("2026-06", property.id, emptyList(), entries, emptyList(), emptyList()).expenses)
        assertEquals(1000L, operationalSummary("2026-06", other.id, emptyList(), entries, emptyList(), emptyList()).expenses)
        rejects { accounting.saveExpense(expense().copy(propertyId = other.id, reservationId = input.id)) }
        assertEquals(2, accounting.expenses.first().size)
    }
    @Test fun cleaningLaundryAreSingleRowsWithEffectiveCostAndHistoricalUnitRate() = runBlocking {
        val cleaning = expense().copy(categoryId = "cleaning", quantity = "3", unit = "8", amount = "25", reservationId = input.id)
        val laundry = expense().copy(categoryId = "laundry", quantity = "2", unit = "5", amount = "", reservationId = input.id)
        accounting.saveExpense(cleaning); accounting.saveExpense(laundry)
        val rows = accounting.expenses.first(); assertEquals(2, rows.size)
        assertEquals(2400L, db.accountingDao().expense(cleaning.id)!!.calculatedCents)
        assertEquals(2500L, db.accountingDao().expense(cleaning.id)!!.actualCents)
        assertEquals(1000L, db.accountingDao().expense(laundry.id)!!.actualCents)
        PropertyRepository(db).save(property.copy(cleaningRate = "20", laundryRate = "12"))
        assertEquals(800L, db.accountingDao().expense(cleaning.id)!!.unitCents)
        rejects { accounting.saveExpense(laundry.copy(id = "fractional-set", quantity = "1,5")) }
    }
    @Test fun expenseCorrectionsKeepSnapshotsAndVoidingExcludesWithoutDeleting() = runBlocking {
        val draft = expense(); accounting.saveExpense(draft)
        val old = db.accountingDao().expense(draft.id)!!
        rejects { accounting.saveExpense(ExpenseDraft.from(old).copy(amount = "6")) }
        accounting.saveExpense(ExpenseDraft.from(old).copy(amount = "6", month = "2026-07", reason = "Competenza corretta"))
        rejects { accounting.saveExpense(ExpenseDraft.from(old).copy(amount = "7", reason = "Stale")) }
        assertEquals(2, accounting.expenseHistory(old.id).size)
        assertEquals(500L, org.json.JSONObject(accounting.expenseHistory(old.id).first().snapshotJson).getLong("actualCents"))
        accounting.voidExpense(old.id, "Acquisto annullato")
        assertEquals(1, accounting.expenses.first().size); assertTrue(db.accountingDao().expense(old.id)!!.voided)
        assertEquals(3, accounting.expenseHistory(old.id).size)
        assertEquals(0L, operationalSummary("2026-07", property.id, emptyList(), accounting.expenses.first(), emptyList(), emptyList()).expenses)
    }
    @Test fun reimbursementsRequireBothAdvancedAndReimbursableFlags() = runBlocking {
        accounting.saveExpense(expense()); accounting.saveExpense(expense().copy(advanced = false)); accounting.saveExpense(expense().copy(reimbursable = false))
        val summary = operationalSummary("2026-06", property.id, emptyList(), accounting.expenses.first(), emptyList(), emptyList())
        assertEquals(1500L, summary.expenses); assertEquals(1000L, summary.advanced); assertEquals(500L, summary.reimbursable)
    }
    @Test fun partialAndFullCollectionsAreManualEvenAfterCheckoutAndCancellationPreservesHistory() = runBlocking {
        tax(); val assessment = accounting.assessments.first().single()
        assertEquals(3600L, assessment.expectedCents); assertEquals(0L, bookings.get(input.id)!!.collectedTaxCents)
        bookings.save(BookingDraft.from(bookings.get(input.id)!!).copy(status = "COMPLETED"))
        assertEquals(0L, bookings.get(input.id)!!.collectedTaxCents)
        val first = movement("COLLECTION", 1000); accounting.post(first)
        assertEquals("Parzialmente incassata", Ledger.taxStatus(3600, bookings.get(input.id)!!.collectedTaxCents))
        accounting.post(movement("COLLECTION", 2600))
        assertEquals(3600L, bookings.get(input.id)!!.collectedTaxCents)
        rejects { accounting.post(movement("COLLECTION", 1)) }
        bookings.cancel(input.id)
        assertEquals(3600L, bookings.get(input.id)!!.collectedTaxCents)
        assertEquals(2, accounting.movements.first().size)
    }
    @Test fun collectionIsIdempotentAndCompensationCannotExceedSourceOrBeDuplicated() = runBlocking {
        tax(); val cash = movement("COLLECTION", 3000)
        accounting.post(cash); accounting.post(cash.copy(createdAt = cash.createdAt + 1))
        val comp = movement("COMPENSATION", 2000, cash.id); accounting.post(comp); accounting.post(comp)
        rejects { accounting.post(movement("COMPENSATION", 1001, cash.id)) }
        accounting.post(movement("COMPENSATION", 1000, cash.id))
        rejects { accounting.post(movement("COMPENSATION", 1, cash.id)) }
        assertEquals(3000L, Ledger.total(accounting.movements.first(), "COLLECTION"))
        assertEquals(3000L, Ledger.total(accounting.movements.first(), "COMPENSATION"))
        assertEquals(3, accounting.movements.first().size)
        rejects { accounting.post(cash.copy(amountCents = 2000)) }
    }
    @Test fun remittanceHasSeparateResponsibleAndDoesNotDoubleDeductBalance() = runBlocking {
        tax(); val cash = movement("COLLECTION", 3000); accounting.post(cash)
        rejects { accounting.post(movement("REMITTANCE", 3000, cash.id)) }
        accounting.saveTax(input.id, TaxDraft.from(accounting.assessments.first().single()).copy(responsible = "OWNER", remittanceNotes = "Obblighi verificati"))
        accounting.post(movement("REMITTANCE", 3000, cash.id)); accounting.post(movement("COMPENSATION", 3000, cash.id))
        val summary = operationalSummary("2026-06", property.id, listOf(bookings.get(input.id)!!), emptyList(), accounting.assessments.first(), accounting.movements.first())
        assertEquals(13100L, summary.balance)
        rejects { accounting.post(movement("REMITTANCE", 1, cash.id)) }
    }
    @Test fun reversalRequiresReasonAndDependentCompensationsMustBeReversedFirst() = runBlocking {
        tax(); val cash = movement("COLLECTION", 3000); accounting.post(cash)
        val comp = movement("COMPENSATION", 3000, cash.id); accounting.post(comp)
        rejects { accounting.post(movement("REVERSAL", 3000, reverse = cash.id)) }
        rejects { accounting.post(movement("REVERSAL", 3000, reverse = comp.id).copy(notes = "")) }
        accounting.post(movement("REVERSAL", 3000, reverse = comp.id))
        assertEquals(0L, Ledger.total(accounting.movements.first(), "COMPENSATION"))
        accounting.post(movement("REVERSAL", 3000, reverse = cash.id))
        assertEquals(0L, bookings.get(input.id)!!.collectedTaxCents)
        rejects { accounting.post(movement("REVERSAL", 3000, reverse = cash.id)) }
        assertEquals(4, accounting.movements.first().size)
    }
    @Test fun bookingChangesRequireTaxReviewAndCannotMoveCollectedTaxToAnotherProperty() = runBlocking {
        tax(); val cash = movement("COLLECTION", 1000); accounting.post(cash)
        rejects { bookings.save(BookingDraft.from(bookings.get(input.id)!!).copy(propertyId = other.id)) }
        bookings.save(BookingDraft.from(bookings.get(input.id)!!).copy(guests = "3"))
        assertTrue(accounting.assessments.first().single().needsReview)
        rejects { accounting.post(movement("COLLECTION", 100)) }
        accounting.saveTax(input.id, TaxDraft.from(accounting.assessments.first().single()).copy(taxable = "2", exempt = "1"))
        assertFalse(accounting.assessments.first().single().needsReview)
        assertEquals(2400L, bookings.get(input.id)!!.expectedTaxCents)
        assertEquals(1000L, bookings.get(input.id)!!.collectedTaxCents)
        assertEquals(2, accounting.taxHistory(input.id).size)
    }
    @Test fun ownerPaymentsAndNegativeBalancesAreExplicitAndReversible() = runBlocking {
        val pay = movement("OWNER_PAYMENT", 20000).copy(reservationId = null)
        accounting.post(pay)
        val summary = operationalSummary("2026-06", property.id, listOf(bookings.get(input.id)!!), emptyList(), emptyList(), accounting.movements.first())
        assertEquals(-3900L, summary.balance)
        accounting.post(movement("REVERSAL", 20000, reverse = pay.id).copy(reservationId = null))
        assertEquals(0L, Ledger.total(accounting.movements.first(), "OWNER_PAYMENT"))
    }
    @Test fun expensesTaxAndMovementsPersistAfterClosingAndReopeningFileDatabase() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>(); val name = "accounting-persistence.db"; context.deleteDatabase(name)
        val cost = expense()
        try {
            Room.databaseBuilder(context, AppDatabase::class.java, name).build().withClosing { file ->
                PropertyRepository(file).save(property); ReservationRepository(file).save(input)
                val repo = AccountingRepository(file); repo.initialize(); repo.saveExpense(cost); repo.saveTax(input.id, TaxDraft("3", "1", "4"))
                val cash = movement("COLLECTION", 3000); repo.post(cash); repo.post(movement("COMPENSATION", 3000, cash.id))
            }
            Room.databaseBuilder(context, AppDatabase::class.java, name).build().withClosing { file ->
                val repo = AccountingRepository(file)
                assertEquals(500L, repo.expenses.first().single().actualCents)
                assertEquals(3600L, repo.assessments.first().single().expectedCents)
                assertEquals(3000L, Ledger.total(repo.movements.first(), "COMPENSATION"))
                assertEquals(1, repo.expenseHistory(cost.id).size)
                assertEquals("2026-06", file.reservationDao().get(input.id)!!.competenceMonth)
                assertEquals(3000L, file.reservationDao().get(input.id)!!.collectedTaxCents)
            }
        } finally { context.deleteDatabase(name) }
    }
    @Test fun financialCorrectionRequiresReasonAndPreservesBothSnapshots() = runBlocking {
        val original = bookings.get(input.id)!!
        rejects { bookings.save(BookingDraft.from(original).copy(actualOta = "150")) }
        assertEquals(19500L, bookings.get(input.id)!!.otaCents)
        bookings.save(BookingDraft.from(original).copy(actualOta = "150", correctionReason = "Commissione effettiva verificata"))
        val change = db.accountingDao().observeFinancialChanges().first().single()
        assertEquals("Commissione effettiva verificata", change.reason)
        assertEquals(19500L, org.json.JSONObject(change.previousJson).getLong("otaCents"))
        assertEquals(15000L, org.json.JSONObject(change.currentJson).getLong("otaCents"))
        assertEquals(17000L, bookings.get(input.id)!!.managementCents)
    }
    @Test fun movingAnUncollectedBookingRequiresNewPropertyTaxRulesAfterReview() = runBlocking {
        tax()
        PropertyRepository(db).save(other.copy(taxRate = "5", taxMaxNights = "2"))
        bookings.save(BookingDraft.from(bookings.get(input.id)!!).copy(propertyId = other.id))
        assertTrue(accounting.assessments.first().single().needsReview)
        accounting.saveTax(input.id, TaxDraft.from(accounting.assessments.first().single()).copy(nights = "2"))
        val value = accounting.assessments.first().single()
        assertEquals(other.id, value.propertyId); assertEquals(500L, value.nightCents); assertEquals(3000L, value.expectedCents)
    }

}
