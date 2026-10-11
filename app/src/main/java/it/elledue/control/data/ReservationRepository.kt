package it.elledue.control.data

import androidx.room.withTransaction
import it.elledue.control.domain.*

data class ConflictToken(val id: String, val updatedAt: Long)
class OverlapException(val conflicts: List<ReservationEntity>) : IllegalArgumentException("Le date si sovrappongono ad altre prenotazioni dell’immobile.")

class ReservationRepository(private val database: AppDatabase) {
    private val dao = database.reservationDao()
    val reservations = dao.observeAll()
    suspend fun get(id: String) = dao.get(id)
    suspend fun save(draft: BookingDraft, approved: Set<ConflictToken> = emptySet()) = database.withTransaction {
        val previous = dao.get(draft.id)
        val property = requireNotNull(database.propertyDao().get(draft.propertyId)) { "Seleziona un immobile presente nel database." }
        require(!property.property.archived || previous?.propertyId == draft.propertyId) { "Non puoi aggiungere prenotazioni a un immobile archiviato." }
        require(previous?.accountingAllocationId == null) { "Questa prenotazione è già attribuita a un rendiconto: occorre una rettifica." }
        val candidate = draft.toRecord(previous, false)
        require(candidate.guestCount <= property.property.maxGuests) { "Gli ospiti superano la capienza dell’immobile (${property.property.maxGuests})." }
        val conflicts = if (candidate.status == BookingStatus.CANCELLED.name) emptyList() else dao.overlaps(candidate.propertyId, candidate.id, candidate.checkInEpochDay, candidate.checkOutEpochDay)
        val currentTokens = conflicts.map { ConflictToken(it.id, it.updatedAt) }.toSet()
        if (currentTokens.isNotEmpty() && currentTokens != approved) throw OverlapException(conflicts)
        dao.save(candidate.copy(overlapAcknowledged = conflicts.isNotEmpty()))
    }
    suspend fun cancel(id: String) = database.withTransaction {
        val previous = requireNotNull(dao.get(id)) { "Prenotazione non trovata." }
        require(previous.accountingAllocationId == null) { "La prenotazione è già attribuita a un rendiconto: occorre una rettifica." }
        dao.cancel(id, maxOf(System.currentTimeMillis(), previous.updatedAt + 1))
    }
}
