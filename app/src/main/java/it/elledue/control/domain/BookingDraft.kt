package it.elledue.control.domain

import it.elledue.control.data.PropertyWithOwner
import it.elledue.control.data.ReservationEntity
import java.util.UUID

/** Applied percentages are explicit in the form and snapshot; editing never refreshes them implicitly. */
data class BookingDraft(
    val id: String = UUID.randomUUID().toString(),
    val propertyId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val guests: String = "1",
    val platform: String = "Booking.com",
    val checkIn: String = BookingDates.today().format(BookingDates.inputFormat),
    val checkOut: String = BookingDates.today().plusDays(2).format(BookingDates.inputFormat),
    val expectedArrival: String = "15:00",
    val actualArrival: String = "",
    val expectedDeparture: String = "10:00",
    val actualDeparture: String = "",
    val gross: String = "",
    val otaPercent: String = "19,50",
    val actualOta: String = "",
    val managementPercent: String = "20,00",
    val notes: String = "",
    val status: String = BookingStatus.CONFIRMED.name,
    val correctionReason: String = "",
    val businessType:String = "MANAGED",
    val taxableGuestCount:String = "",
    val baseUpdatedAt:Long?=null,
) {
    fun totals(): BookingTotals = BookingFinance.calculate(
        Amounts.parseCents(gross), Amounts.parseBasisPoints(if(otaPercent.isBlank() && actualOta.isNotBlank()) "0" else otaPercent),
        actualOta.takeIf { it.isNotBlank() }?.let(Amounts::parseCents), if(businessType=="OWNED")0 else Amounts.parseBasisPoints(managementPercent),
    )
    fun withPropertyDefaults(item: PropertyWithOwner): BookingDraft = copy(
        propertyId = item.property.id, expectedArrival = item.property.checkInTime,
        expectedDeparture = item.property.checkOutTime,
        managementPercent = if(item.property.businessType=="OWNED")"0" else Amounts.editable(item.property.managementBasisPoints.toLong()),
        businessType=item.property.businessType,
        otaPercent = otaDefault(item, platform),
    )
    fun toRecord(previous: ReservationEntity?, overlapAcknowledged: Boolean): ReservationEntity {
        require(propertyId.isNotBlank()) { "Seleziona un immobile." }
        require(firstName.isNotBlank() && lastName.isNotBlank()) { "Inserisci nome e cognome dell’ospite." }
        val count = guests.toIntOrNull()
        require(count != null && count > 0) { "Il numero degli ospiti deve essere un intero positivo." }
        require(platform.isNotBlank()) { "Inserisci la piattaforma di provenienza." }
        val start = BookingDates.parse(checkIn).toEpochDay()
        val end = BookingDates.parse(checkOut).toEpochDay()
        require(end > start) { "Il check-out deve essere successivo al check-in." }
        val parsedStatus = try { BookingStatus.valueOf(status) } catch (_: Exception) { throw IllegalArgumentException("Stato non valido.") }
        require(businessType in listOf("MANAGED","OWNED"))
        val subjects=taxableGuestCount.takeIf{it.isNotBlank()}?.let{it.toIntOrNull() ?: throw IllegalArgumentException("Ospiti soggetti: inserisci un numero intero.")}
        require(subjects==null || subjects in 0..count) {"Gli ospiti soggetti non possono superare il totale."}
        val totals = totals()
        return ReservationEntity(
            id, propertyId, firstName.trim(), lastName.trim(), count, start, end, platform.trim(),
            Amounts.parseCents(gross), Amounts.parseBasisPoints(if(otaPercent.isBlank() && actualOta.isNotBlank()) "0" else otaPercent),
            actualOta.takeIf { it.isNotBlank() }?.let(Amounts::parseCents), if(businessType=="OWNED")0 else Amounts.parseBasisPoints(managementPercent), parsedStatus.name,
            BookingDates.time(expectedArrival)!!, BookingDates.time(actualArrival, true),
            BookingDates.time(expectedDeparture)!!, BookingDates.time(actualDeparture, true),
            totals.otaCents, totals.netCents, totals.managementCents, notes.trim(), overlapAcknowledged,
            previous?.createdAt ?: System.currentTimeMillis(), maxOf(System.currentTimeMillis(), (previous?.updatedAt ?: 0) + 1),
            subjects, subjects?.let{count-it}, previous?.expectedTaxCents,
            previous?.collectedTaxCents ?: 0, previous?.accountingAllocationId,
            java.time.YearMonth.from(java.time.LocalDate.ofEpochDay(start)).toString(),
            businessType,
        )
    }
    companion object {
        fun otaDefault(item: PropertyWithOwner, platform: String): String {
            if(platform.equals("Prenotazione diretta",true)) return "0,00"
            val configured=item.platformCommissions.firstOrNull {it.platform.equals(platform.trim(),true)}?.basisPoints
            if(configured!=null) return Amounts.editable(configured.toLong())
            // A legacy generic default is not evidence of a configured rate for another portal.
            return ""
        }
        fun from(item: ReservationEntity): BookingDraft = with(item) {
            BookingDraft(id, propertyId, guestFirstName, guestLastName, guestCount.toString(), platform,
                BookingDates.display(checkInEpochDay), BookingDates.display(checkOutEpochDay),
                expectedArrivalTime, actualArrivalTime ?: "", expectedDepartureTime, actualDepartureTime ?: "",
                Amounts.editable(grossCents), Amounts.editable(appliedOtaBasisPoints.toLong()),
                actualOtaCents?.let(Amounts::editable) ?: "", Amounts.editable(appliedManagementBasisPoints.toLong()), notes, status, businessType=appliedBusinessType, taxableGuestCount=taxableGuests?.toString() ?: "",baseUpdatedAt=updatedAt)
        }
    }
}
