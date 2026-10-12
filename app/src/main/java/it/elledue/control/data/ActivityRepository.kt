package it.elledue.control.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities ORDER BY completed, dueEpochMillis, title") fun observe(): Flow<List<ActivityEntity>>
    @Upsert suspend fun save(item: ActivityEntity)
}
class ActivityRepository(private val db: AppDatabase) {
    val activities = db.activityDao().observe()
    suspend fun save(item: ActivityEntity) = db.withTransaction {
        require(item.title.isNotBlank() && item.title.length <= 300) { "Inserisci un titolo valido." }
        item.propertyId?.let { requireNotNull(db.propertyDao().get(it)) }
        item.reservationId?.let { require(db.reservationDao().get(it)?.propertyId == item.propertyId) }
        db.activityDao().save(item.copy(title = item.title.trim()))
    }
}
