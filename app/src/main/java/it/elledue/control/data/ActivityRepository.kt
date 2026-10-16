package it.elledue.control.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities ORDER BY completed, dueEpochMillis, title") fun observe(): Flow<List<ActivityEntity>>
    @Query("SELECT * FROM activities WHERE id=:id") suspend fun get(id:String):ActivityEntity?
    @Query("DELETE FROM activities WHERE id=:id") suspend fun delete(id:String)
    @Query("UPDATE activities SET propertyId=:propertyId WHERE reservationId=:bookingId") suspend fun moveWithBooking(bookingId:String,propertyId:String)
    @Upsert suspend fun save(item: ActivityEntity)
}
class ActivityRepository(private val db: AppDatabase,private val drafts:DraftStore?=null) {
    suspend fun delete(id:String) = db.activityDao().delete(id)
    val activities = db.activityDao().observe()
    suspend fun save(item: ActivityEntity):Unit = db.withTransaction {
        require(item.title.isNotBlank() && item.title.length <= 300) { "Inserisci un titolo valido." }
        require(item.notes.length <= 20000)
        require(item.kind in listOf("GENERAL","CLEANING","MAINTENANCE"))
        java.time.ZoneId.of(item.zoneId)
        item.propertyId?.let { requireNotNull(db.propertyDao().get(it)) }
        item.reservationId?.let { require(db.reservationDao().get(it)?.propertyId == item.propertyId) }
        db.activityDao().save(item.copy(title = item.title.trim()))
        drafts?.clear("activity/new");drafts?.clear("activity/${item.id}")
    }
}
