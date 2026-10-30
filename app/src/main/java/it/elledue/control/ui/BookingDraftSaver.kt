package it.elledue.control.ui

import androidx.compose.runtime.saveable.listSaver
import it.elledue.control.domain.BookingDraft

val BookingDraftSaver = listSaver<BookingDraft, String>(
    save = { listOf(it.id, it.propertyId, it.firstName, it.lastName, it.guests, it.platform, it.checkIn, it.checkOut, it.expectedArrival, it.actualArrival, it.expectedDeparture, it.actualDeparture, it.gross, it.otaPercent, it.actualOta, it.managementPercent, it.notes, it.status, it.correctionReason, it.businessType, it.taxableGuestCount, it.baseUpdatedAt?.toString() ?: "",it.otaManual.toString(),it.otaTreatment,it.otaVatBasisPoints.toString(),it.automaticTaxAdvance.toString(),it.taxAdvanceOverride,it.exemptGuestCount,it.taxAdvanceManual.toString()) },
    restore = { BookingDraft(id = it[0], propertyId = it[1], firstName = it[2], lastName = it[3], guests = it[4], platform = it[5], checkIn = it[6], checkOut = it[7], expectedArrival = it[8], actualArrival = it[9], expectedDeparture = it[10], actualDeparture = it[11], gross = it[12], otaPercent = it[13], actualOta = it[14], managementPercent = it[15], notes = it[16], status = it[17], correctionReason = it.getOrElse(18) { "" }, businessType=it.getOrElse(19){"MANAGED"}, taxableGuestCount=it.getOrElse(20){""},baseUpdatedAt=it.getOrNull(21)?.toLongOrNull(),otaManual=it.getOrNull(22)?.toBoolean() ?: false,otaTreatment=it.getOrElse(23){"NONE"},otaVatBasisPoints=it.getOrNull(24)?.toIntOrNull() ?: 2200,automaticTaxAdvance=it.getOrNull(25)?.toBoolean() ?: false,taxAdvanceOverride=it.getOrElse(26){""},exemptGuestCount=it.getOrElse(27){""},taxAdvanceManual=it.getOrNull(28)?.toBoolean() ?: false) },
)
