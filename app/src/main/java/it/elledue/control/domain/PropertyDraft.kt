package it.elledue.control.domain

import it.elledue.control.data.*
import java.time.LocalTime
import java.util.UUID

/** Strings keep forms lossless until validated. Accounting values are parsed without floating point. */
data class PropertyDraft(
    val id: String = UUID.randomUUID().toString(),
    val ownerId: String = UUID.randomUUID().toString(),
    val name: String = "",
    val address: String = "",
    val ownerName: String = "",
    val ownerPhone: String = "",
    val ownerEmail: String = "",
    val usefulContacts: String = "",
    val beds: String = "1",
    val maxGuests: String = "1",
    val checkInTime: String = "15:00",
    val checkOutTime: String = "10:00",
    val notes: String = "",
    val managementPercent: String = "20,00",
    val otaPercent: String = "19,50",
    val platformRates: String = "",
    val cleaningRate: String = "8,00",
    val laundryRate: String = "5,00",
    val taxRate: String = "3,00",
    val taxMinAge: String = "16",
    val taxMaxNights: String = "4",
    val taxExemptions: String = "",
    val accessItalian: String = "",
    val accessEnglish: String = "",
    val businessType:String = "MANAGED",
    val baseUpdatedAt:Long?=null,
    val laundryGuests:String="2",
    val transportRate:String="",
    val otaTreatment:String="NONE", val otaVatPercent:String="22,00", val automaticTaxAdvance:Boolean=true,

) {
    fun toRecord(previous: PropertyEntity? = null): PropertyWithOwner {
        require(name.isNotBlank()) { "Inserisci il nome dell’immobile." }
        require(address.isNotBlank()) { "Inserisci l’indirizzo." }
        require(businessType in listOf("MANAGED","OWNED"))
        require(businessType=="OWNED" || ownerName.isNotBlank()) { "Inserisci il nome del proprietario." }
        val bedCount = beds.toIntOrNull()
        val guests = maxGuests.toIntOrNull()
        require(bedCount != null && bedCount > 0) { "I posti letto devono essere un numero intero positivo." }
        require(guests != null && guests > 0) { "Il numero massimo di ospiti deve essere positivo." }
        val timePattern = Regex("[0-2][0-9]:[0-5][0-9]")
        require(timePattern.matches(checkInTime) && timePattern.matches(checkOutTime)) { "Usa orari nel formato HH:mm." }
        try { LocalTime.parse(checkInTime); LocalTime.parse(checkOutTime) }
        catch (_: Exception) { throw IllegalArgumentException("Inserisci orari validi nel formato HH:mm.") }
        val minAge = taxMinAge.toIntOrNull()
        require(minAge != null && minAge in 0..120) { "L’età minima deve essere tra 0 e 120." }
        val maxNights = if (taxMaxNights.isBlank()) null else taxMaxNights.toIntOrNull()
        require(taxMaxNights.isBlank() || (maxNights != null && maxNights > 0)) { "Il limite di notti deve essere positivo, oppure vuoto." }
        val laundryGroup=requireNotNull(laundryGuests.toIntOrNull()){ "Inserisci gli ospiti per set." };require(laundryGroup>0)
        val owner = OwnerEntity(ownerId, ownerName.trim().ifBlank { "Proprietà diretta / soci da configurare" }, ownerPhone.trim(), ownerEmail.trim())
        require(otaTreatment in OtaTreatment.values){"Trattamento OTA non valido."}
        val property = PropertyEntity(
            id = id, ownerId = ownerId, name = name.trim(), address = address.trim(),
            usefulContacts = usefulContacts.trim(), beds = bedCount, maxGuests = guests,
            checkInTime = checkInTime, checkOutTime = checkOutTime, operationalNotes = notes.trim(),
            managementBasisPoints = if(businessType=="OWNED")0 else Amounts.parseBasisPoints(managementPercent),
            otaBasisPoints = Amounts.parseBasisPoints(otaPercent),
            cleaningHourlyCents = Amounts.parseCents(cleaningRate), laundrySetCents = Amounts.parseCents(laundryRate),
            touristTaxNightCents = Amounts.parseCents(taxRate), touristTaxMinAge = minAge,
            touristTaxMaxNights = maxNights, touristTaxExemptions = taxExemptions.trim(),
            archived = previous?.archived ?: false, businessType=businessType,
            coverImageBase64 = previous?.coverImageBase64,
            laundryGuestsPerSet=laundryGroup, habitualTransportCents=transportRate.takeIf{it.isNotBlank()}?.let(Amounts::parseCents),
            createdAt = previous?.createdAt ?: System.currentTimeMillis(),
            otaTreatment=otaTreatment,otaVatBasisPoints=Amounts.parseBasisPoints(otaVatPercent),automaticTaxAdvance=automaticTaxAdvance,

        )
        val rates = platformRates.lineSequence().filter { it.isNotBlank() }.map { line ->
            val parts = line.split('='); val value=parts.getOrNull(1)?.split(';').orEmpty()
            require(parts.size == 2 && parts[0].isNotBlank()) { "Commissioni portale: usa una riga Portale=percentuale." }
            PlatformCommissionEntity(id, parts[0].trim(), Amounts.parseBasisPoints(value[0]), value.getOrNull(1)?.trim()?.uppercase()?.also{require(it in OtaTreatment.values || it=="INHERIT"){"Modalità portale: BASE_PLUS_VAT, INCLUDED, NONE oppure INHERIT."}} ?: "INHERIT")
        }.toList()
        require(rates.map { it.platform.lowercase() }.distinct().size == rates.size) { "Ogni portale può comparire una sola volta." }
        val templates = listOf("it" to accessItalian, "en" to accessEnglish).filter { it.second.isNotBlank() }
            .map { MessageTemplateEntity(id, "access", it.first, it.second.trim()) }
        return PropertyWithOwner(property, owner, rates, templates)
    }

    companion object {
        fun from(item: PropertyWithOwner): PropertyDraft = with(item.property) {
            PropertyDraft(
                id = id, ownerId = ownerId, name = name, address = address, ownerName = item.owner.name,
                ownerPhone = item.owner.phone, ownerEmail = item.owner.email, usefulContacts = usefulContacts,
                beds = beds.toString(), maxGuests = maxGuests.toString(), checkInTime = checkInTime, checkOutTime = checkOutTime,
                notes = operationalNotes, managementPercent = Amounts.editable(managementBasisPoints.toLong()),
                otaPercent = Amounts.editable(otaBasisPoints.toLong()),
                otaTreatment=otaTreatment,otaVatPercent=Amounts.editable(otaVatBasisPoints.toLong()),automaticTaxAdvance=automaticTaxAdvance,

                platformRates = item.platformCommissions.joinToString("\n") { "${it.platform}=${Amounts.editable(it.basisPoints.toLong())}${if(it.otaTreatment=="INHERIT")"" else ";${it.otaTreatment}"}" },
                cleaningRate = Amounts.editable(cleaningHourlyCents), laundryRate = Amounts.editable(laundrySetCents),
                taxRate = Amounts.editable(touristTaxNightCents), taxMinAge = touristTaxMinAge.toString(),
                taxMaxNights = touristTaxMaxNights?.toString() ?: "", taxExemptions = touristTaxExemptions,
                accessItalian = item.templates.firstOrNull { it.kind == "access" && it.language == "it" }?.body ?: "",
                businessType=businessType,baseUpdatedAt=updatedAt,laundryGuests=laundryGuestsPerSet.toString(),transportRate=habitualTransportCents?.let(Amounts::editable) ?: "",
                accessEnglish = item.templates.firstOrNull { it.kind == "access" && it.language == "en" }?.body ?: "",
            )
        }
    }
}
