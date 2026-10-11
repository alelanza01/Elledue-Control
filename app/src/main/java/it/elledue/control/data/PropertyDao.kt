package it.elledue.control.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PropertyDao {
    @Transaction
    @Query("SELECT * FROM properties ORDER BY archived ASC, name COLLATE NOCASE ASC")
    abstract fun observeAll(): Flow<List<PropertyWithOwner>>

    @Transaction
    @Query("SELECT * FROM properties WHERE id = :id")
    abstract suspend fun get(id: String): PropertyWithOwner?

    @Upsert protected abstract suspend fun upsertOwner(owner: OwnerEntity)
    @Upsert protected abstract suspend fun upsertProperty(property: PropertyEntity)
    @Upsert protected abstract suspend fun upsertCommissions(items: List<PlatformCommissionEntity>)
    @Upsert protected abstract suspend fun upsertTemplates(items: List<MessageTemplateEntity>)
    @Query("DELETE FROM platform_commissions WHERE propertyId = :id")
    protected abstract suspend fun clearCommissions(id: String)
    @Query("DELETE FROM message_templates WHERE propertyId = :id")
    protected abstract suspend fun clearTemplates(id: String)

    @Transaction
    open suspend fun save(item: PropertyWithOwner) {
        upsertOwner(item.owner)
        upsertProperty(item.property)
        clearCommissions(item.property.id)
        upsertCommissions(item.platformCommissions)
        clearTemplates(item.property.id)
        upsertTemplates(item.templates)
    }

    @Query("UPDATE properties SET archived = :archived, updatedAt = :now WHERE id = :id")
    abstract suspend fun setArchived(id: String, archived: Boolean, now: Long)
}
