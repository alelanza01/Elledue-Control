package it.elledue.control.ui

import androidx.compose.runtime.saveable.listSaver
import it.elledue.control.domain.*

val ExpenseDraftSaver = listSaver<ExpenseDraft, String>(save = { listOf(it.id,it.propertyId,it.reservationId,it.date,it.month,it.categoryId,it.description,it.amount,it.quantity,it.unit,it.advanced.toString(),it.reimbursable.toString(),it.notes,it.setDefinition,it.reason,it.revision.toString(),it.standardApproved.toString(),it.confirmActual.toString()) }, restore = { ExpenseDraft(it[0],it[1],it[2],it[3],it[4],it[5],it[6],it[7],it[8],it[9],it[10].toBoolean(),it[11].toBoolean(),it[12],it[13],it[14],it[15].toInt(),it.getOrElse(16){"false"}.toBoolean(),it.getOrElse(17){"false"}.toBoolean()) })
val TaxDraftSaver = listSaver<TaxDraft, String>(save = { listOf(it.taxable,it.exempt,it.nights,it.actualExpected,it.reason,it.responsible,it.remittanceNotes,it.revision.toString(),it.useCurrentRules.toString()) }, restore = { TaxDraft(it[0],it[1],it[2],it[3],it[4],it[5],it[6],it[7].toInt(),it.getOrElse(8){"false"}.toBoolean()) })
