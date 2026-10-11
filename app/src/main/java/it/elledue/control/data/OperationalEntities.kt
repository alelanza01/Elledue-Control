package it.elledue.control.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Stay dates are local epoch days, money is EUR cents. */
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
    @ColumnInfo(defaultValue = "'15:00'") val expectedArrivalTime: String = "15:00",
    val actualArrivalTime: String? = null,
    @ColumnInfo(defaultValue = "'10:00'") val expectedDepartureTime: String = "10:00",
    val actualDepartureTime: String? = null,
    @ColumnInfo(defaultValue = "0") val otaCents: Long = 0,
    @ColumnInfo(defaultValue = "0") val netCents: Long = 0,
    @ColumnInfo(defaultValue = "0") val managementCents: Long = 0,
    @ColumnInfo(defaultValue = "''") val notes: String = "",
    @ColumnInfo(defaultValue = "0") val overlapAcknowledged: Boolean = false,
    @ColumnInfo(defaultValue = "0") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = System.currentTimeMillis(),
    val taxableGuests: Int? = null,
    val exemptGuests: Int? = null,
    val expectedTaxCents: Long? = null,
    @ColumnInfo(defaultValue = "0") val collectedTaxCents: Long = 0,
    val accountingAllocationId: String? = null,
    @ColumnInfo(defaultValue = "''") val competenceMonth: String = "",
)

@Entity(tableName = "expenses", foreignKeys = [
    ForeignKey(entity = ExpenseCategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = PropertyEntity::class, parentColumns = ["id"], childColumns = ["propertyId"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = ReservationEntity::class, parentColumns = ["id"], childColumns = ["reservationId"], onDelete = ForeignKey.RESTRICT),
], indices = [Index("propertyId"), Index("reservationId"), Index("categoryId")])
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
    @ColumnInfo(defaultValue = "''") val competenceMonth: String = "",
    val categoryId: String? = null,
    @ColumnInfo(defaultValue = "'OTHER'") val kind: String = "OTHER",
    val quantity: String? = null,
    val unitCents: Long? = null,
    val calculatedCents: Long? = null,
    @ColumnInfo(defaultValue = "''") val notes: String = "",
    @ColumnInfo(defaultValue = "''") val setDefinition: String = "",
    @ColumnInfo(defaultValue = "0") val voided: Boolean = false,
    @ColumnInfo(defaultValue = "1") val revision: Int = 1,
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0,
    val accountingAllocationId: String? = null,
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
