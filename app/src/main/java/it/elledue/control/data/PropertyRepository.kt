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
    suspend fun setCover(id: String, photo: String?) = database.withTransaction {
        LocalImages.validate(photo)
        requireNotNull(dao.get(id)) { "Immobile non trovato." }
        database.openHelper.writableDatabase.execSQL("UPDATE properties SET coverImageBase64 = ?, updatedAt = ? WHERE id = ?", arrayOf<Any?>(photo, System.currentTimeMillis(), id))
    }
    suspend fun deleteEmpty(id: String) = database.withTransaction {
        requireNotNull(dao.get(id)) { "Immobile non trovato." }
        val sql = database.openHelper.writableDatabase
        listOf("reservations", "expenses", "activities", "reports", "money_movements", "report_allocations").forEach { table ->
            sql.query("SELECT 1 FROM `$table` WHERE propertyId = ? LIMIT 1", arrayOf(id)).use { require(!it.moveToFirst()) { "Immobile con storico: utilizza l’archiviazione per conservare i dati." } }
        }
        sql.execSQL("DELETE FROM platform_commissions WHERE propertyId = ?", arrayOf(id))
        sql.execSQL("DELETE FROM message_templates WHERE propertyId = ?", arrayOf(id))
        sql.execSQL("DELETE FROM properties WHERE id = ?", arrayOf(id))
    }
}
