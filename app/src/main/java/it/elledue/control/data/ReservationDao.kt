package it.elledue.control.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReservationDao {
    @Query("SELECT * FROM reservations ORDER BY checkInEpochDay DESC, guestLastName COLLATE NOCASE")
    fun observeAll(): Flow<List<ReservationEntity>>
    @Query("SELECT * FROM reservations WHERE id = :id")
    suspend fun get(id: String): ReservationEntity?
    @Query("SELECT * FROM reservations WHERE propertyId = :propertyId AND id != :id AND status != 'CANCELLED' AND checkInEpochDay < :end AND checkOutEpochDay > :start ORDER BY id")
    suspend fun overlaps(propertyId: String, id: String, start: Long, end: Long): List<ReservationEntity>
    @Upsert suspend fun save(item: ReservationEntity)
    @Query("UPDATE reservations SET status = 'CANCELLED', updatedAt = :now WHERE id = :id")
    suspend fun cancel(id: String, now: Long)
}
