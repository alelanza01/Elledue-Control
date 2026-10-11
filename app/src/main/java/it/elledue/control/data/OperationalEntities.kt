package it.elledue.control.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Schema foundations only: workflows are implemented in subsequent milestones. Dates = epoch day. */
@Entity(tableName = "reservations", foreignKeys = [
    ForeignKey(entity = PropertyEntity::class, parentColumns = ["id"], childColumns = ["propertyId"], onDelete = ForeignKey.RESTRICT),
], indices = [Index("propertyId")])
data class ReservationEntity(
    @PrimaryKey val id: String,
    val propertyId: String,
    val guestFirstName: String,
    val guestLastName: String,
    val guestCount: Int,
    val checkInEpochDay: Long,
    val checkOutEpochDay: Long,
    val platform: String,
    val grossCents: Long,
    val appliedOtaBasisPoints: Int,
    val actualOtaCents: Long?,
    val appliedManagementBasisPoints: Int,
    val status: String,
)

@Entity(tableName = "expenses", foreignKeys = [
    ForeignKey(entity = PropertyEntity::class, parentColumns = ["id"], childColumns = ["propertyId"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = ReservationEntity::class, parentColumns = ["id"], childColumns = ["reservationId"], onDelete = ForeignKey.RESTRICT),
], indices = [Index("propertyId"), Index("reservationId")])
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val propertyId: String,
    val reservationId: String?,
    val dateEpochDay: Long,
    val description: String,
    val category: String,
    val actualCents: Long,
    val advancedByElledue: Boolean,
    val reimbursable: Boolean,
)

@Entity(tableName = "activities", foreignKeys = [
    ForeignKey(entity = PropertyEntity::class, parentColumns = ["id"], childColumns = ["propertyId"], onDelete = ForeignKey.RESTRICT),
], indices = [Index("propertyId")])
data class ActivityEntity(
    @PrimaryKey val id: String,
    val propertyId: String?,
    val title: String,
    val dueEpochMillis: Long?,
    val zoneId: String = "Europe/Rome",
    val completed: Boolean = false,
)

@Entity(tableName = "reports", foreignKeys = [
    ForeignKey(entity = PropertyEntity::class, parentColumns = ["id"], childColumns = ["propertyId"], onDelete = ForeignKey.RESTRICT),
], indices = [Index(value = ["propertyId", "yearMonth", "version"], unique = true)])
data class ReportEntity(
    @PrimaryKey val id: String,
    val propertyId: String,
    val yearMonth: String,
    val version: Int,
    val status: String,
    val snapshotJson: String?,
    val confirmedAt: Long?,
)
