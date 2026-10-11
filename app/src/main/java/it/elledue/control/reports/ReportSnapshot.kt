package it.elledue.control.reports

import com.google.gson.Gson
import it.elledue.control.data.*
import it.elledue.control.domain.*
import java.security.MessageDigest

/** Frozen complete data, not references to mutable rows. Tax is external to booking gross. */
data class ReportSnapshot(
    val format: Int = 1, val draft: Boolean = true, val id: String, val month: String, val version: Int, val previousId: String?, val revisionReason: String,
    val issuedAt: Long, val property: PropertyEntity, val owner: OwnerEntity, val organization: String,
    val logoBase64: String, val notes: String, val bookings: List<ReservationEntity>, val expenses: List<ExpenseEntity>,
    val taxes: List<TaxAssessmentEntity>, val movements: List<MoneyMovementEntity>,
) {
    fun summary() = operationalSummary(month, property.id, bookings, expenses, taxes, movements)
    fun gross() = OperationalMoney.total(bookings.filter { it.status != "CANCELLED" }.map { it.grossCents })
    fun ota() = OperationalMoney.total(bookings.filter { it.status != "CANCELLED" }.map { it.otaCents })
    fun net() = OperationalMoney.total(bookings.filter { it.status != "CANCELLED" }.map { it.netCents })
    fun allocations(): List<ReportAllocationEntity> = bookings.filter { it.status != "CANCELLED" }.map { ReportAllocationEntity("BOOKING", it.id, property.id, month) } +
        expenses.filter { !it.voided }.map { ReportAllocationEntity("EXPENSE", it.id, property.id, month) } +
        Ledger.active(movements).filter { it.competenceMonth == month && it.kind in listOf("COMPENSATION", "OWNER_PAYMENT") }.map { ReportAllocationEntity("MOVEMENT", it.id, property.id, month) }
    fun issues(): List<ReportIssue> = buildList {
        bookings.forEach { booking ->
            if (booking.propertyId != property.id || Competence.booking(booking.checkInEpochDay) != month) add(ReportIssue(true, "Prenotazione con competenza o immobile incoerente."))
            val correct = runCatching { BookingFinance.calculate(booking.grossCents, booking.appliedOtaBasisPoints, booking.actualOtaCents, booking.appliedManagementBasisPoints) }.getOrNull()
            if (correct == null || correct.otaCents != booking.otaCents || correct.netCents != booking.netCents || correct.managementCents != booking.managementCents) add(ReportIssue(true, "Dettaglio e importi economici incoerenti per ${booking.guestFirstName} ${booking.guestLastName}."))
            if (booking.status != "CANCELLED") {
                if (booking.actualOtaCents == null) add(ReportIssue(false, "OTA stimata: ${booking.guestFirstName} ${booking.guestLastName}."))
                val tax = taxes.firstOrNull { it.reservationId == booking.id }
                if (tax == null) add(ReportIssue(false, "Tassa non configurata: ${booking.guestFirstName} ${booking.guestLastName}; dati fiscali incompleti."))
                if (tax?.needsReview == true) add(ReportIssue(true, "Conteggi fiscali da rivedere: ${booking.guestFirstName} ${booking.guestLastName}."))
            } else if (Ledger.total(movements.filter { it.reservationId == booking.id }, "COLLECTION") > 0) add(ReportIssue(false, "Prenotazione annullata con riscossioni da riconciliare: ${booking.guestFirstName} ${booking.guestLastName}."))
        }
        expenses.forEach { if (it.propertyId != property.id || it.competenceMonth != month) add(ReportIssue(true, "Spesa con competenza o immobile incoerente.")) }
        if (allocations().map { it.kind to it.sourceId }.distinct().size != allocations().size) add(ReportIssue(true, "Voce duplicata nel rendiconto."))
        runCatching { summary(); gross(); ota(); net() }.onFailure { add(ReportIssue(true, "Importi non validi o oltre il limite contabile.")) }
    }
}
data class ReportIssue(val blocking: Boolean, val message: String)
object ReportCodec {
    val gson = Gson()
    fun encode(snapshot: ReportSnapshot): String = gson.toJson(snapshot)
    fun decode(json: String): ReportSnapshot = gson.fromJson(json, ReportSnapshot::class.java).also { require(it.format == 1 && (it.draft || it.issues().none { issue -> issue.blocking })) { "Snapshot non valido." } }
    fun hash(json: String): String = MessageDigest.getInstance("SHA-256").digest(json.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
