package it.elledue.control.data

import androidx.room.withTransaction
import it.elledue.control.domain.PropertyDraft

class PropertyRepository(private val database: AppDatabase) {
    private val dao = database.propertyDao()
    val properties = dao.observeAll()
    suspend fun get(id: String) = dao.get(id)

    suspend fun save(draft: PropertyDraft) = database.withTransaction {
        val previous = dao.get(draft.id)
        val validated = draft.toRecord(previous?.property)
        // Editing access instructions must preserve other future guest templates.
        val retainedTemplates = previous?.templates.orEmpty().filterNot {
            it.kind == "access" && it.language in listOf("it", "en")
        }
        dao.save(validated.copy(templates = validated.templates + retainedTemplates))
    }

    suspend fun archive(id: String, archived: Boolean) = database.withTransaction {
        requireNotNull(dao.get(id)) { "Immobile non trovato." }
        dao.setArchived(id, archived, System.currentTimeMillis())
    }
}
