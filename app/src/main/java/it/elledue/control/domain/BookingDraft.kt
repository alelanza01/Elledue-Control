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
    val otaManual:Boolean=false,
    val otaTreatment:String="NONE", val otaVatBasisPoints:Int=2200,
    val automaticTaxAdvance:Boolean=false, val taxAdvanceOverride:String="",
    val exemptGuestCount:String="", val taxAdvanceManual:Boolean=false,

) {
    fun totals(): BookingTotals = BookingFinance.calculate(
        Amounts.parseCents(gross), Amounts.parseBasisPoints(if(otaPercent.isBlank() && actualOta.isNotBlank()) "0" else otaPercent),
        actualOta.takeIf { it.isNotBlank() }?.let(Amounts::parseCents), if(businessType=="OWNED")0 else Amounts.parseBasisPoints(managementPercent), otaTreatment,otaVatBasisPoints,
    )
    fun withPropertyDefaults(item: PropertyWithOwner): BookingDraft = copy(
        propertyId = item.property.id, expectedArrival = item.property.checkInTime,
        expectedDeparture = item.property.checkOutTime,
        managementPercent = if(item.property.businessType=="OWNED")"0" else Amounts.editable(item.property.managementBasisPoints.toLong()),
        businessType=item.property.businessType,
        taxableGuestCount=if(taxableGuestCount.isBlank() && exemptGuestCount.isBlank())guests else taxableGuestCount,
        otaPercent = if(otaManual)otaPercent else otaDefault(item, platform),
        otaTreatment=if(otaManual)otaTreatment else otaTreatmentDefault(item,platform),otaVatBasisPoints=item.property.otaVatBasisPoints,
        automaticTaxAdvance=item.property.automaticTaxAdvance && item.property.businessType=="MANAGED",

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
        val exemptions=exemptGuestCount.takeIf{it.isNotBlank()}?.let{it.toIntOrNull() ?: throw IllegalArgumentException("Esenti: numero non valido.")}
        require(exemptions==null || exemptions in 0..count){"Gli esenti non possono superare il totale."}
        val subjects=if(exemptions!=null)count-exemptions else taxableGuestCount.takeIf{it.isNotBlank()}?.let{it.toIntOrNull() ?: throw IllegalArgumentException("Ospiti soggetti: inserisci un numero intero.")}
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
            businessType, appliedOtaTreatment=otaTreatment,appliedOtaVatBasisPoints=otaVatBasisPoints,
            automaticTaxAdvance=automaticTaxAdvance && businessType=="MANAGED",
            taxAdvanceOverrideCents=taxAdvanceOverride.takeIf{it.isNotBlank()}?.let(Amounts::parseCents),otaTreatmentManual=otaManual,taxAdvanceManual=taxAdvanceManual,
        )
    }
    companion object {
        fun otaTreatmentDefault(item:PropertyWithOwner,platform:String):String {
            val specific=item.platformCommissions.firstOrNull{it.platform.equals(platform.trim(),true)}
            return specific?.otaTreatment?.takeIf{it!="INHERIT"} ?: if(platform.equals("Prenotazione diretta",true))"NONE" else item.property.otaTreatment
        }

        fun otaDefault(item: PropertyWithOwner, platform: String): String {
            val configured=item.platformCommissions.firstOrNull {it.platform.equals(platform.trim(),true)}?.basisPoints
            if(configured!=null) return Amounts.editable(configured.toLong())
            if(platform.equals("Prenotazione diretta",true)) return "0,00"
            // Explicit portal override wins; otherwise use the property default, never a hardcoded rate.
            return Amounts.editable(item.property.otaBasisPoints.toLong())
        }
        fun from(item: ReservationEntity): BookingDraft = with(item) {
            BookingDraft(id, propertyId, guestFirstName, guestLastName, guestCount.toString(), platform,
                BookingDates.display(checkInEpochDay), BookingDates.display(checkOutEpochDay),
                expectedArrivalTime, actualArrivalTime ?: "", expectedDepartureTime, actualDepartureTime ?: "",
                Amounts.editable(grossCents), Amounts.editable(appliedOtaBasisPoints.toLong()),
                actualOtaCents?.let(Amounts::editable) ?: "", Amounts.editable(appliedManagementBasisPoints.toLong()), notes, status, businessType=appliedBusinessType, taxableGuestCount=taxableGuests?.toString() ?: "",baseUpdatedAt=updatedAt,otaTreatment=appliedOtaTreatment ?: "NONE",otaVatBasisPoints=appliedOtaVatBasisPoints,
                automaticTaxAdvance=automaticTaxAdvance,taxAdvanceOverride=taxAdvanceOverrideCents?.let(Amounts::editable) ?: "",exemptGuestCount="",otaManual=otaTreatmentManual,taxAdvanceManual=taxAdvanceManual)
        }
    }
}
