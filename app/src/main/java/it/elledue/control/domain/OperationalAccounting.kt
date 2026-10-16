package it.elledue.control.domain

import it.elledue.control.data.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

object Competence {
    fun booking(checkIn: Long): String = YearMonth.from(LocalDate.ofEpochDay(checkIn)).toString()
    fun month(value: String): String {
        require(Regex("\\d{4}-\\d{2}").matches(value)) { "Usa il mese nel formato AAAA-MM." }
        return try { YearMonth.parse(value).toString() } catch (_: Exception) { throw IllegalArgumentException("Mese di competenza non valido.") }
    }
}

object OperationalMoney {
    fun total(values: Iterable<Long>): Long = values.fold(BigDecimal.ZERO) { a, b -> a + BigDecimal.valueOf(b) }.longValueExact()
    fun quantity(value: String): BigDecimal {
        require(Regex("\\d+(?:[.,]\\d{1,2})?").matches(value.trim())) { "Quantità positiva, con al massimo due decimali." }
        return BigDecimal(value.trim().replace(',', '.')).also { require(it.signum() > 0) { "La quantità deve essere maggiore di zero." } }
    }
    fun cost(quantity: String, unitCents: Long): Long {
        require(unitCents >= 0)
        return quantity(quantity).multiply(BigDecimal.valueOf(unitCents)).setScale(0, RoundingMode.HALF_UP).longValueExact()
    }
}

data class ExpenseDraft(
    val id: String = UUID.randomUUID().toString(), val propertyId: String = "", val reservationId: String = "",
    val date: String = BookingDates.display(BookingDates.today().toEpochDay()),
    val month: String = YearMonth.from(BookingDates.today()).toString(), val categoryId: String = "other",
    val description: String = "", val amount: String = "", val quantity: String = "", val unit: String = "",
    val advanced: Boolean = true, val reimbursable: Boolean = true, val notes: String = "",
    val setDefinition: String = "Set matrimoniale: biancheria e asciugamani", val reason: String = "", val revision: Int = 0,
) {
    fun record(category: ExpenseCategoryEntity): ExpenseEntity {
        val qty = quantity.takeIf { it.isNotBlank() }; val rate = unit.takeIf { it.isNotBlank() }?.let(Amounts::parseCents)
        require((qty == null) == (rate == null)) { "Inserisci sia quantità sia tariffa, oppure lasciale entrambe vuote." }
        if (category.kind != "OTHER") require(qty != null) { "Inserisci ore o set e tariffa." }
        if (category.kind == "LAUNDRY") {
            require(qty != null && OperationalMoney.quantity(qty).stripTrailingZeros().scale() <= 0) { "I set devono essere un numero intero positivo." }
            require(setDefinition.isNotBlank()) { "Descrivi la tipologia del set." }
        }
        val calculated = qty?.let { OperationalMoney.cost(it, rate!!) }
        val actual = amount.takeIf { it.isNotBlank() }?.let(Amounts::parseCents) ?: calculated
        require(actual != null) { "Inserisci l’importo della spesa." }
        return ExpenseEntity(id, propertyId, reservationId.ifBlank { null }, BookingDates.parse(date).toEpochDay(),
            description.trim().ifBlank { category.label }, category.label, actual, advanced, reimbursable,
            Competence.month(month), category.id, category.kind, qty?.replace(',', '.'), rate, calculated, notes.trim(),
            if (category.kind == "LAUNDRY") setDefinition.trim() else "", false, revision + 1, System.currentTimeMillis())
    }
    companion object {
        fun from(item: ExpenseEntity) = with(item) { ExpenseDraft(id, propertyId, reservationId ?: "", BookingDates.display(dateEpochDay), competenceMonth,
            categoryId ?: "other", description, Amounts.editable(actualCents), quantity ?: "", unitCents?.let(Amounts::editable) ?: "",
            advancedByElledue, reimbursable, notes, setDefinition, "", revision) }
    }
}

data class TaxDraft(val taxable: String = "", val exempt: String = "", val nights: String = "", val actualExpected: String = "", val reason: String = "", val responsible: String = "UNDEFINED", val remittanceNotes: String = "", val revision: Int = 0, val useCurrentRules:Boolean=false) {
    fun record(booking: ReservationEntity, property: PropertyEntity, previous: TaxAssessmentEntity?): TaxAssessmentEntity {
        val subjects = taxable.toIntOrNull(); val exemptions = exempt.toIntOrNull(); val count = nights.toIntOrNull()
        require(subjects != null && exemptions != null && subjects >= 0 && exemptions >= 0 && subjects.toLong() + exemptions == booking.guestCount.toLong()) { "Soggetti più esenti devono corrispondere agli ospiti totali." }
        val basis = previous?.takeIf { !useCurrentRules && it.propertyId == booking.propertyId }
        val max = basis?.maxNights ?: if (basis == null) property.touristTaxMaxNights else null
        val rate = basis?.nightCents ?: property.touristTaxNightCents
        require(count != null && count >= 0 && count.toLong() <= booking.checkOutEpochDay - booking.checkInEpochDay && (max == null || count <= max)) { "Notti soggette oltre il soggiorno o il limite configurato." }
        val calculated = BigDecimal.valueOf(rate).multiply(BigDecimal.valueOf(subjects.toLong())).multiply(BigDecimal.valueOf(count.toLong())).longValueExact()
        val expected = actualExpected.takeIf { it.isNotBlank() }?.let(Amounts::parseCents) ?: calculated
        require(expected == calculated || reason.isNotBlank()) { "Motiva la rettifica manuale della tassa." }
        require(responsible in listOf("UNDEFINED", "OWNER", "ELLEDUE", "OTHER")) { "Soggetto riversamento non valido." }
        return TaxAssessmentEntity(booking.id, subjects, exemptions, count, rate, basis?.minAge ?: property.touristTaxMinAge,
            max, basis?.exemptions ?: property.touristTaxExemptions, calculated, expected, reason.trim(), responsible, remittanceNotes.trim(), false, revision + 1, booking.propertyId)
    }
    companion object {
        fun from(item: TaxAssessmentEntity) = with(item) { TaxDraft(taxableGuests.toString(), exemptGuests.toString(), taxableNights.toString(),
            if (expectedCents != calculatedCents) Amounts.editable(expectedCents) else "", correctionReason, remittanceResponsible, remittanceNotes, revision) }
    }
}

object Ledger {
    fun active(items: List<MoneyMovementEntity>): List<MoneyMovementEntity> {
        val reversed = items.mapNotNull { it.reversesId }.toSet()
        return items.filter { it.kind != "REVERSAL" && it.id !in reversed }
    }
    fun total(items: List<MoneyMovementEntity>, kind: String): Long = OperationalMoney.total(active(items).filter { it.kind == kind }.map { it.amountCents })
    fun residual(expected: Long, collected: Long): Long = maxOf(0, expected - collected)
    fun taxStatus(expected: Long?, collected: Long): String = when {
        expected == null -> "Da configurare"
        collected == 0L && expected > 0 -> "Da incassare"
        collected < expected -> "Parzialmente incassata"
        collected > expected -> "Eccedenza da riconciliare"
        else -> "Incassata / nessun residuo"
    }
}

data class OperationalSummary(val fees: Long, val expenses: Long, val advanced: Long, val reimbursable: Long, val cleaning: Long, val laundry: Long, val expectedTax: Long, val collectedTax: Long, val residualTax: Long, val compensated: Long, val payments: Long, val balance: Long)
fun operationalSummary(month: String, propertyId: String, bookings: List<ReservationEntity>, expenses: List<ExpenseEntity>, assessments: List<TaxAssessmentEntity>, movements: List<MoneyMovementEntity>): OperationalSummary {
    val stays = bookings.filter { Competence.booking(it.checkInEpochDay) == month && (propertyId.isEmpty() || it.propertyId == propertyId) }
    val costs = expenses.filter { !it.voided && it.competenceMonth == month && (propertyId.isEmpty() || it.propertyId == propertyId) }
    val taxes = assessments.filter { a -> stays.any { it.id == a.reservationId } }
    val currentTaxes = taxes.filter { tax -> stays.any { it.id == tax.reservationId && it.status != BookingStatus.CANCELLED.name } }
    val fees = OperationalMoney.total(stays.filter { it.status != BookingStatus.CANCELLED.name }.map { it.managementCents }); val reimb = OperationalMoney.total(costs.filter { it.advancedByElledue && it.reimbursable }.map { it.actualCents })
    val cash = movements.filter { (propertyId.isEmpty() || it.propertyId == propertyId) }
    val active = Ledger.active(cash)
    val collected = OperationalMoney.total(taxes.map { a -> OperationalMoney.total(active.filter { it.reservationId == a.reservationId && it.kind == "COLLECTION" }.map { it.amountCents }) })
    val comp = OperationalMoney.total(active.filter { it.competenceMonth == month && it.kind == "COMPENSATION" }.map { it.amountCents })
    val paid = OperationalMoney.total(active.filter { it.competenceMonth == month && it.kind == "OWNER_PAYMENT" }.map { it.amountCents })
    return OperationalSummary(fees, OperationalMoney.total(costs.map { it.actualCents }), OperationalMoney.total(costs.filter { it.advancedByElledue }.map { it.actualCents }), reimb,
        OperationalMoney.total(costs.filter { it.kind == "CLEANING" }.map { it.actualCents }), OperationalMoney.total(costs.filter { it.kind == "LAUNDRY" }.map { it.actualCents }),
        OperationalMoney.total(currentTaxes.map { it.expectedCents }), collected,
        OperationalMoney.total(currentTaxes.map { a -> Ledger.residual(a.expectedCents, OperationalMoney.total(active.filter { it.reservationId == a.reservationId && it.kind == "COLLECTION" }.map { it.amountCents })) }), comp, paid,
        BigDecimal.valueOf(fees).add(BigDecimal.valueOf(reimb)).subtract(BigDecimal.valueOf(comp)).subtract(BigDecimal.valueOf(paid)).longValueExact())
}

/** Operator uses this inclusive threshold when assigning counts; individual ages are never stored. */
object TouristTaxRules {
    fun subjectByAge(completedYears: Int, minimumCompletedYears: Int): Boolean {
        require(completedYears >= 0 && minimumCompletedYears >= 0)
        return completedYears >= minimumCompletedYears
    }
}
