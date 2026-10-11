package it.elledue.control.data

import it.elledue.control.domain.PropertyDraft

class PropertyRepository(private val database: AppDatabase) {
    private val dao = database.propertyDao()
    val properties = dao.observeAll()
    suspend fun get(id: String) = dao.get(id)

    suspend fun save(draft: PropertyDraft) {
        val previous = dao.get(draft.id)?.property
        dao.save(draft.toRecord(previous))
    }

    suspend fun archive(id: String, archived: Boolean) {
        requireNotNull(dao.get(id)) { "Immobile non trovato." }
        dao.setArchived(id, archived, System.currentTimeMillis())
    }
}
