package it.elledue.control

import it.elledue.control.data.*
import it.elledue.control.domain.*
import org.junit.Assert.*
import org.junit.Test

class OperationalAccountingTest {
    private fun booking(start: String, end: String) = BookingDraft(propertyId = "p", firstName = "Ospite", lastName = "Test", checkIn = start, checkOut = end, gross = "1000").toRecord(null, false)
    @Test fun staysCrossingMonthsBelongEntirelyToCheckInMonth() {
        val stay = booking("29/11/2026", "03/12/2026")
        assertEquals("2026-11", stay.competenceMonth)
        assertEquals(16100L, operationalSummary("2026-11", "p", listOf(stay), emptyList(), emptyList(), emptyList()).fees)
        assertEquals(0L, operationalSummary("2026-12", "p", listOf(stay), emptyList(), emptyList(), emptyList()).fees)
    }
    @Test fun staysCrossingYearsBelongToDecemberOfPreviousYear() {
        val stay = booking("30/12/2026", "05/01/2027")
        assertEquals("2026-12", stay.competenceMonth)
        assertEquals(0L, operationalSummary("2027-01", "p", listOf(stay), emptyList(), emptyList(), emptyList()).fees)
    }
    @Test fun quantitiesUseExactCentsHalfUpAndRejectOverflowOrInvalidValues() {
        assertEquals(2400L, OperationalMoney.cost("3", 800))
        assertEquals(1000L, OperationalMoney.cost("2", 500))
        assertEquals(201L, OperationalMoney.cost("0,25", 802))
        listOf("0", "-1", "1.234", "NaN").forEach { assertThrows(IllegalArgumentException::class.java) { OperationalMoney.cost(it, 800) } }
        assertThrows(ArithmeticException::class.java) { OperationalMoney.cost("2", Long.MAX_VALUE) }
        assertThrows(ArithmeticException::class.java) { OperationalMoney.total(listOf(Long.MAX_VALUE, 1)) }
    }
    @Test fun taxCountsExemptionsNightsLimitAndSixteenYearCriterion() {
        val stay = booking("10/06/2026", "14/06/2026").copy(guestCount = 4)
        val property = PropertyEntity(ownerId = "o", name = "Casa", address = "Test", touristTaxMaxNights = 3)
        val tax = TaxDraft("3", "1", "3").record(stay, property, null)
        assertEquals(2700L, tax.expectedCents); assertEquals(16, tax.minAge)
        assertTrue(TouristTaxRules.subjectByAge(16, tax.minAge)); assertFalse(TouristTaxRules.subjectByAge(15, tax.minAge))
        assertThrows(IllegalArgumentException::class.java) { TaxDraft("3", "1", "4").record(stay, property, null) }
        assertThrows(IllegalArgumentException::class.java) { TaxDraft("4", "1", "3").record(stay, property, null) }
    }
    @Test fun manualTaxOverrideRequiresReasonAndSnapshotsRemainHistorical() {
        val stay = booking("10/06/2026", "14/06/2026").copy(guestCount = 4)
        val property = PropertyEntity(ownerId = "o", name = "Casa", address = "Test")
        assertThrows(IllegalArgumentException::class.java) { TaxDraft("3", "1", "4", "30").record(stay, property, null) }
        val tax = TaxDraft("3", "1", "4", "30", "Esenzione documentata").record(stay, property, null)
        assertEquals(3600L, tax.calculatedCents); assertEquals(3000L, tax.expectedCents)
        val edited = TaxDraft.from(tax).record(stay, property.copy(touristTaxNightCents = 900), tax)
        assertEquals(300L, edited.nightCents)
    }
    @Test fun expenseCompetenceCanDifferFromActualDateAndActualAmountPrevails() {
        val expense = ExpenseDraft(propertyId = "p", date = "01/12/2026", month = "2026-11", categoryId = "cleaning", quantity = "3", unit = "8", amount = "25").record(AccountingDefaults.categories.first())
        assertEquals("2026-11", expense.competenceMonth); assertEquals(2400L, expense.calculatedCents); assertEquals(2500L, expense.actualCents)
        assertEquals(2500L, operationalSummary("2026-11", "p", emptyList(), listOf(expense), emptyList(), emptyList()).reimbursable)
        assertEquals(0L, operationalSummary("2026-12", "p", emptyList(), listOf(expense), emptyList(), emptyList()).expenses)
        assertThrows(IllegalArgumentException::class.java) { Competence.month("2026-13") }
    }
    @Test fun summaryUsesOneExpensePerInterventionAndSeparateCompensation() {
        val stay = booking("10/06/2026", "14/06/2026")
        val costs = listOf(ExpenseDraft(propertyId = "p", date = "10/06/2026", month = "2026-06", categoryId = "cleaning", quantity = "3", unit = "8").record(AccountingDefaults.categories.first()),
            ExpenseDraft(propertyId = "p", date = "10/06/2026", month = "2026-06", categoryId = "laundry", quantity = "2", unit = "5").record(AccountingDefaults.categories[1]),
            ExpenseDraft(propertyId = "p", date = "10/06/2026", month = "2026-06", amount = "5").record(AccountingDefaults.categories.last()))
        val collected = MoneyMovementEntity("cash", "p", stay.id, "COLLECTION", 3000, stay.checkInEpochDay, "2026-06", "")
        val comp = collected.copy(id = "comp", kind = "COMPENSATION", sourceId = "cash")
        val tax = TaxAssessmentEntity(stay.id, 1, 0, 4, 300, 16, null, "", 1200, 3000, "Rettifica")
        val unrecognized = operationalSummary("2026-06", "p", listOf(stay), costs, listOf(tax), listOf(collected))
        assertEquals(20000L, unrecognized.balance)
        val recognized = operationalSummary("2026-06", "p", listOf(stay), costs, listOf(tax), listOf(collected, comp))
        assertEquals(17000L, recognized.balance); assertEquals(3900L, recognized.expenses)
        assertEquals(2400L, recognized.cleaning); assertEquals(1000L, recognized.laundry)
    }
    @Test fun cancelledFeesVoidedExpensesAndReversedMoneyDoNotInflateTotals() {
        val stay = booking("10/06/2026", "14/06/2026").copy(status = "CANCELLED")
        val expense = ExpenseDraft(propertyId = "p", date = "10/06/2026", month = "2026-06", amount = "5").record(AccountingDefaults.categories.last()).copy(voided = true)
        val payment = MoneyMovementEntity("pay", "p", null, "OWNER_PAYMENT", 1000, stay.checkInEpochDay, "2026-06", "")
        val reversal = payment.copy(id = "reverse", kind = "REVERSAL", reversesId = "pay")
        assertEquals(0L, operationalSummary("2026-06", "p", listOf(stay), listOf(expense), emptyList(), listOf(payment, reversal)).balance)
        assertEquals("Eccedenza da riconciliare", Ledger.taxStatus(500, 1000))
    }
    @Test fun cancelledStayDoesNotRequestTaxButCashIsStillVisibleForReconciliation() {
        val stay = booking("10/06/2026", "14/06/2026").copy(status = "CANCELLED")
        val tax = TaxAssessmentEntity(stay.id, 1, 0, 4, 300, 16, null, "", 1200, 1200, "")
        val cash = MoneyMovementEntity("cash", "p", stay.id, "COLLECTION", 600, stay.checkInEpochDay, "2026-06", "")
        val summary = operationalSummary("2026-06", "p", listOf(stay), emptyList(), listOf(tax), listOf(cash))
        assertEquals(0L, summary.fees); assertEquals(0L, summary.expectedTax); assertEquals(0L, summary.residualTax)
        assertEquals(600L, summary.collectedTax)
    }

}
