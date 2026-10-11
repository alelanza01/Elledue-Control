package it.elledue.control.ui

import androidx.compose.runtime.saveable.listSaver
import it.elledue.control.domain.PropertyDraft

val PropertyDraftSaver = listSaver<PropertyDraft, String>(
    save = { listOf(it.id, it.ownerId, it.name, it.address, it.ownerName, it.ownerPhone, it.ownerEmail, it.usefulContacts, it.beds, it.maxGuests, it.checkInTime, it.checkOutTime, it.notes, it.managementPercent, it.otaPercent, it.platformRates, it.cleaningRate, it.laundryRate, it.taxRate, it.taxMinAge, it.taxMaxNights, it.taxExemptions, it.accessItalian, it.accessEnglish) },
    restore = { PropertyDraft(id = it[0], ownerId = it[1], name = it[2], address = it[3], ownerName = it[4], ownerPhone = it[5], ownerEmail = it[6], usefulContacts = it[7], beds = it[8], maxGuests = it[9], checkInTime = it[10], checkOutTime = it[11], notes = it[12], managementPercent = it[13], otaPercent = it[14], platformRates = it[15], cleaningRate = it[16], laundryRate = it[17], taxRate = it[18], taxMinAge = it[19], taxMaxNights = it[20], taxExemptions = it[21], accessItalian = it[22], accessEnglish = it[23]) },
)
