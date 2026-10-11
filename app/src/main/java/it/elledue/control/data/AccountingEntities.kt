package it.elledue.control.data

import androidx.room.*

@Entity(tableName = "expense_categories")
data class ExpenseCategoryEntity(@PrimaryKey val id: String, val label: String, val kind: String = "OTHER", val archived: Boolean = false)

@Entity(tableName = "expense_revisions", foreignKeys = [ForeignKey(entity = ExpenseEntity::class, parentColumns = ["id"], childColumns = ["expenseId"], onDelete = ForeignKey.RESTRICT)], indices = [Index(value = ["expenseId", "version"], unique = true)])
data class ExpenseRevisionEntity(@PrimaryKey val id: String, val expenseId: String, val version: Int, val snapshotJson: String, val reason: String, val recordedAt: Long)

@Entity(tableName = "tax_assessments", foreignKeys = [ForeignKey(entity = ReservationEntity::class, parentColumns = ["id"], childColumns = ["reservationId"], onDelete = ForeignKey.RESTRICT)])
data class TaxAssessmentEntity(
    @PrimaryKey val reservationId: String,
    val taxableGuests: Int, val exemptGuests: Int, val taxableNights: Int,
    val nightCents: Long, val minAge: Int, val maxNights: Int?, val exemptions: String,
    val calculatedCents: Long, val expectedCents: Long, val correctionReason: String,
    val remittanceResponsible: String = "UNDEFINED", val remittanceNotes: String = "",
    val needsReview: Boolean = false, val revision: Int = 1, val propertyId: String = "",
)

@Entity(tableName = "tax_assessment_revisions", foreignKeys = [ForeignKey(entity = ReservationEntity::class, parentColumns = ["id"], childColumns = ["reservationId"], onDelete = ForeignKey.RESTRICT)], indices = [Index(value = ["reservationId", "version"], unique = true)])
data class TaxAssessmentRevisionEntity(@PrimaryKey val id: String, val reservationId: String, val version: Int, val snapshotJson: String, val recordedAt: Long)

/** Append-only ledger. Reversals reference their original, never overwrite money movements. */
@Entity(tableName = "money_movements", foreignKeys = [
    ForeignKey(entity = PropertyEntity::class, parentColumns = ["id"], childColumns = ["propertyId"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = ReservationEntity::class, parentColumns = ["id"], childColumns = ["reservationId"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = MoneyMovementEntity::class, parentColumns = ["id"], childColumns = ["sourceId"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = MoneyMovementEntity::class, parentColumns = ["id"], childColumns = ["reversesId"], onDelete = ForeignKey.RESTRICT),
], indices = [Index("propertyId"), Index("reservationId"), Index("sourceId"), Index(value = ["reversesId"], unique = true)])
data class MoneyMovementEntity(
    @PrimaryKey val id: String, val propertyId: String, val reservationId: String?,
    val kind: String, val amountCents: Long, val dateEpochDay: Long, val competenceMonth: String,
    val notes: String, val sourceId: String? = null, val reversesId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "booking_financial_changes", foreignKeys = [ForeignKey(entity = ReservationEntity::class, parentColumns = ["id"], childColumns = ["reservationId"], onDelete = ForeignKey.RESTRICT)], indices = [Index("reservationId")])
data class BookingFinancialChangeEntity(@PrimaryKey val id: String, val reservationId: String, val previousJson: String, val currentJson: String, val reason: String, val recordedAt: Long)
