package it.elledue.control.data

import androidx.room.withTransaction
import it.elledue.control.domain.*

data class ConflictToken(val id: String, val updatedAt: Long)
class OverlapException(val conflicts: List<ReservationEntity>) : IllegalArgumentException("Le date si sovrappongono ad altre prenotazioni dell’immobile.")

class ReservationRepository(private val database: AppDatabase,private val drafts:DraftStore?=null) {
    private val dao = database.reservationDao()
    val reservations = dao.observeAll()
    suspend fun get(id: String) = dao.get(id)
    suspend fun save(draft: BookingDraft, approved: Set<ConflictToken> = emptySet()):Unit {
        database.withTransaction {
            val previous = dao.get(draft.id)
            require(draft.baseUpdatedAt==null || previous?.updatedAt==draft.baseUpdatedAt){"La prenotazione è cambiata dopo l’apertura della bozza. Scarta la bozza per ricaricare i dati correnti; nessun dato è stato sovrascritto."}
            val property = requireNotNull(database.propertyDao().get(draft.propertyId)) { "Seleziona un immobile presente nel database." }
            require(!property.property.archived || previous?.propertyId == draft.propertyId) { "Non puoi aggiungere prenotazioni a un immobile archiviato." }
            require(previous?.accountingAllocationId == null) { "Questa prenotazione è già attribuita a un rendiconto: occorre una rettifica." }
            val candidate = draft.toRecord(previous, false)
            ReportGuard.allow(database, "BOOKING", draft.id, candidate.propertyId, candidate.competenceMonth)
            if (previous != null && (candidate.propertyId != previous.propertyId || candidate.guestCount != previous.guestCount || candidate.checkInEpochDay != previous.checkInEpochDay || candidate.checkOutEpochDay != previous.checkOutEpochDay)) {
                val hasMoney = it.elledue.control.domain.Ledger.active(database.accountingDao().movements()).any { it.reservationId == previous.id }
                require(candidate.propertyId == previous.propertyId || !hasMoney) { "Storna i movimenti prima di cambiare immobile." }
                database.accountingDao().markTaxForReview(previous.id)
            }
            require(candidate.guestCount <= property.property.maxGuests) { "Gli ospiti superano la capienza dell’immobile (${property.property.maxGuests})." }
            val financialChange = previous != null && (previous.grossCents != candidate.grossCents || previous.appliedOtaBasisPoints != candidate.appliedOtaBasisPoints || previous.actualOtaCents != candidate.actualOtaCents || previous.appliedManagementBasisPoints != candidate.appliedManagementBasisPoints || previous.appliedOtaTreatment!=candidate.appliedOtaTreatment || previous.appliedOtaVatBasisPoints!=candidate.appliedOtaVatBasisPoints)
            require(!financialChange || draft.correctionReason.isNotBlank()) { "Motiva la rettifica economica della prenotazione." }
            val conflicts = if (candidate.status == BookingStatus.CANCELLED.name) emptyList() else dao.overlaps(candidate.propertyId, candidate.id, candidate.checkInEpochDay, candidate.checkOutEpochDay)
            val currentTokens = conflicts.map { ConflictToken(it.id, it.updatedAt) }.toSet()
            if (currentTokens.isNotEmpty() && currentTokens != approved) throw OverlapException(conflicts)
            val saved = candidate.copy(overlapAcknowledged = conflicts.isNotEmpty())
            dao.save(saved)
            StayCostRepository(database).update(saved,property.property)
            if(previous==null || saved.taxableGuests!=previous.taxableGuests || saved.guestCount!=previous.guestCount || saved.checkInEpochDay!=previous.checkInEpochDay || saved.checkOutEpochDay!=previous.checkOutEpochDay || saved.propertyId!=previous.propertyId) TaxForecast.update(database,saved,property.property)
            if(previous!=null && previous.propertyId!=saved.propertyId) database.activityDao().moveWithBooking(saved.id,saved.propertyId)
            if(previous!=null && previous.propertyId!=saved.propertyId) database.openHelper.writableDatabase.execSQL("UPDATE property_issues SET reservationId=NULL WHERE reservationId=? AND propertyId<>?",arrayOf(saved.id,saved.propertyId))
            val advanceChange=previous!=null && (previous.automaticTaxAdvance!=saved.automaticTaxAdvance || previous.taxAdvanceOverrideCents!=saved.taxAdvanceOverrideCents || previous.taxAdvanceManual!=saved.taxAdvanceManual)
            if (financialChange || advanceChange) database.accountingDao().insertFinancialChange(BookingFinancialChangeEntity(java.util.UUID.randomUUID().toString(), saved.id,
                AccountingRepository.snapshot(previous!!), AccountingRepository.snapshot(saved), draft.correctionReason.trim().ifBlank{"Eccezione anticipo convenzionale tassa di soggiorno"}, saved.updatedAt))
        }
        if(drafts?.load("booking/new",BookingDraft::class.java)?.id==draft.id)drafts.clear("booking/new")
        drafts?.clear("booking/${draft.id}/edit")
    }
    suspend fun cancel(id: String) = database.withTransaction {
        val previous = requireNotNull(dao.get(id)) { "Prenotazione non trovata." }
        require(previous.accountingAllocationId == null) { "La prenotazione è già attribuita a un rendiconto: occorre una rettifica." }
        ReportGuard.allow(database, "BOOKING", id)
        dao.cancel(id, maxOf(System.currentTimeMillis(), previous.updatedAt + 1))
    }
}
