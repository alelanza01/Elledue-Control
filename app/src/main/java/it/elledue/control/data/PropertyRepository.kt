package it.elledue.control.data

import androidx.room.withTransaction
import it.elledue.control.domain.PropertyDraft

class PropertyRepository(private val database: AppDatabase,private val drafts:DraftStore?=null) {
    private val dao = database.propertyDao()
    val properties = dao.observeAll()
    suspend fun get(id: String) = dao.get(id)

    suspend fun save(draft: PropertyDraft):Unit {
        database.withTransaction {
            val previous = dao.get(draft.id)
            require(draft.baseUpdatedAt==null || previous?.property?.updatedAt==draft.baseUpdatedAt){"L’immobile è cambiato dopo l’apertura della bozza. Ricarica i dati correnti prima di salvare."}
            val validated = draft.toRecord(previous?.property)
            // Editing access instructions must preserve other future guest templates.
            val retainedTemplates = previous?.templates.orEmpty().filterNot {
                it.kind == "access" && it.language in listOf("it", "en")
            }
            dao.save(validated.copy(templates = validated.templates + retainedTemplates))
            if(previous!=null && (previous.property.touristTaxNightCents!=validated.property.touristTaxNightCents || previous.property.touristTaxMaxNights!=validated.property.touristTaxMaxNights || previous.property.touristTaxMinAge!=validated.property.touristTaxMinAge || previous.property.touristTaxExemptions!=validated.property.touristTaxExemptions)) {
                database.reportDao().bookings().filter{it.propertyId==draft.id && it.status!="CANCELLED"}.forEach{TaxForecast.update(database,it,validated.property)}
            }
        }
        if(drafts?.load("property/new",PropertyDraft::class.java)?.id==draft.id)drafts.clear("property/new")
        drafts?.clear("property/${draft.id}/edit")
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
        listOf("reservations", "expenses", "activities", "reports", "money_movements", "report_allocations", "guest_messages").forEach { table ->
            sql.query("SELECT 1 FROM `$table` WHERE propertyId = ? LIMIT 1", arrayOf(id)).use { require(!it.moveToFirst()) { "Immobile con storico: utilizza l’archiviazione per conservare i dati." } }
        }
        sql.execSQL("DELETE FROM platform_commissions WHERE propertyId = ?", arrayOf(id))
        sql.execSQL("DELETE FROM message_templates WHERE propertyId = ?", arrayOf(id))
        sql.execSQL("DELETE FROM properties WHERE id = ?", arrayOf(id))
    }
}
