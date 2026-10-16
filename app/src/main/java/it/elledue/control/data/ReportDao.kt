package it.elledue.control.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "report_allocations", primaryKeys = ["kind", "sourceId"], foreignKeys = [ForeignKey(entity = PropertyEntity::class, parentColumns = ["id"], childColumns = ["propertyId"], onDelete = ForeignKey.RESTRICT)], indices = [Index("propertyId")])
data class ReportAllocationEntity(val kind: String, val sourceId: String, val propertyId: String, val yearMonth: String)

@Entity(tableName="report_booking_options",primaryKeys=["reportId","reservationId"],foreignKeys=[
    ForeignKey(entity=ReportEntity::class,parentColumns=["id"],childColumns=["reportId"],onDelete=ForeignKey.RESTRICT),
    ForeignKey(entity=ReservationEntity::class,parentColumns=["id"],childColumns=["reservationId"],onDelete=ForeignKey.RESTRICT)
],indices=[Index("reservationId")])
data class ReportBookingOption(val reportId:String,val reservationId:String,val hideDetails:Boolean=false,val excluded:Boolean=false)

@Dao
interface ReportDao {
    @Query("SELECT * FROM report_booking_options WHERE reportId=:id") suspend fun options(id:String):List<ReportBookingOption>
    @Upsert suspend fun option(item:ReportBookingOption)
    @Query("SELECT * FROM reports ORDER BY yearMonth DESC, version DESC") fun observe(): Flow<List<ReportEntity>>
    @Query("SELECT * FROM reports WHERE id = :id") suspend fun get(id: String): ReportEntity?
    @Query("SELECT * FROM reports WHERE propertyId = :property AND yearMonth = :month ORDER BY version DESC") suspend fun period(property: String, month: String): List<ReportEntity>
    @Upsert suspend fun save(item: ReportEntity)
    @Query("SELECT * FROM reservations") suspend fun bookings(): List<ReservationEntity>
    @Query("SELECT * FROM expenses") suspend fun expenses(): List<ExpenseEntity>
    @Query("SELECT * FROM tax_assessments") suspend fun taxes(): List<TaxAssessmentEntity>
    @Query("SELECT * FROM report_allocations") suspend fun allocations(): List<ReportAllocationEntity>
    @Query("SELECT * FROM report_allocations WHERE kind = :kind AND sourceId = :id") suspend fun allocation(kind: String, id: String): ReportAllocationEntity?
    @Insert suspend fun allocate(item: ReportAllocationEntity)
}

object ReportGuard {
    suspend fun allow(db: AppDatabase, kind: String, id: String, propertyId: String? = null, month: String? = null) {
        val allocation = db.reportDao().allocation(kind, id) ?: return
        require(propertyId == null || allocation.propertyId == propertyId) { "La voce rendicontata non può cambiare immobile." }
        require(month == null || allocation.yearMonth == month) { "La voce rendicontata non può cambiare competenza: usa una rettifica esplicita nel periodo originale." }
        require(db.reportDao().period(allocation.propertyId, allocation.yearMonth).any { it.status == "DRAFT" && it.previousId != null }) { "Voce già rendicontata: apri una versione rettificativa del rendiconto prima di modificarla." }
    }
}
