package it.elledue.control.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName="message_categories")
data class MessageCategoryEntity(@PrimaryKey val id: String, val label: String)
@Entity(tableName="guest_messages", foreignKeys=[
    ForeignKey(entity=PropertyEntity::class,parentColumns=["id"],childColumns=["propertyId"],onDelete=ForeignKey.RESTRICT),
    ForeignKey(entity=MessageCategoryEntity::class,parentColumns=["id"],childColumns=["categoryId"],onDelete=ForeignKey.RESTRICT)
],indices=[Index("propertyId"),Index("categoryId")])
data class GuestMessageEntity(@PrimaryKey val id:String, val propertyId:String?, val categoryId:String, val title:String, val italian:String, val english:String, val archived:Boolean=false)

@Dao interface MessageDao {
    @Query("SELECT * FROM guest_messages ORDER BY archived, title, id") fun observe(): Flow<List<GuestMessageEntity>>
    @Query("SELECT * FROM guest_messages") suspend fun all(): List<GuestMessageEntity>
    @Query("SELECT * FROM message_categories ORDER BY label") fun categories(): Flow<List<MessageCategoryEntity>>
    @Query("SELECT * FROM message_categories WHERE id = :id") suspend fun category(id:String):MessageCategoryEntity?
    @Upsert suspend fun save(item:GuestMessageEntity)
    @Upsert suspend fun category(item:MessageCategoryEntity)
    @Query("SELECT COUNT(*) FROM message_templates") suspend fun legacyCount():Int
    @Query("DELETE FROM message_templates WHERE propertyId=:propertyId AND kind=:kind") suspend fun deleteLegacy(propertyId:String,kind:String)
    @Query("DELETE FROM guest_messages WHERE id=:id") suspend fun delete(id:String)
    @Query("DELETE FROM message_categories WHERE id=:id") suspend fun deleteCategory(id:String)
}
class MessageRepository(private val db:AppDatabase,private val drafts:DraftStore?=null) {
    val messages=db.messageDao().observe(); val categories=db.messageDao().categories()
    suspend fun initialize()=db.withTransaction {
        val settings=db.settingsDao().get() ?: AppSettings()
        if(settings.messageCategoriesInitialized) return@withTransaction
        listOf("pre" to "Pre-arrivo","in" to "Check-in","stay" to "Durante il soggiorno","out" to "Check-out","post" to "Post-soggiorno","special" to "Richieste particolari","other" to "Altre comunicazioni").forEach { (id,label) ->
            if(db.messageDao().category(id)==null) db.messageDao().category(MessageCategoryEntity(id,label))
        }
        db.settingsDao().save(settings.copy(messageCategoriesInitialized=true))
    }
    suspend fun save(item:GuestMessageEntity):Unit=db.withTransaction {
        require(item.title.isNotBlank() && item.title.length<=300) { "Inserisci un titolo valido." }
        require(item.italian.isNotBlank() || item.english.isNotBlank()) { "Inserisci almeno un testo." }
        require(item.italian.length<=20000 && item.english.length<=20000)
        item.propertyId?.let { requireNotNull(db.propertyDao().get(it)) }
        requireNotNull(db.messageDao().category(item.categoryId)) { "Seleziona una categoria." }
        db.messageDao().save(item.copy(title=item.title.trim()))
        drafts?.clear("messages?draft=${item.id}")
    }
    suspend fun category(item:MessageCategoryEntity) { require(item.label.isNotBlank() && item.label.length<=100); db.messageDao().category(item.copy(label=item.label.trim())) }
    suspend fun delete(item:GuestMessageEntity)=db.withTransaction {
        if(item.id.startsWith("legacy-") && item.propertyId!=null) db.messageDao().deleteLegacy(item.propertyId,item.id.removePrefix("legacy-${item.propertyId}-"))
        db.messageDao().delete(item.id)
    }
    suspend fun deleteCategory(id:String)=db.withTransaction {
        require(id!="other" || db.messageDao().legacyCount()==0) {"Categoria usata dai testi storici degli immobili."}
        require(db.messageDao().all().none { it.categoryId==id }) { "Categoria utilizzata da un modello: riassegna prima i modelli." }
        db.messageDao().deleteCategory(id)
    }
}
