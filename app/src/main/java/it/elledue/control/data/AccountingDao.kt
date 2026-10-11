package it.elledue.control.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountingDao {
    @Query("SELECT * FROM booking_financial_changes ORDER BY recordedAt DESC") fun observeFinancialChanges(): Flow<List<BookingFinancialChangeEntity>>
    @Insert suspend fun insertFinancialChange(item: BookingFinancialChangeEntity)
    @Query("SELECT * FROM expenses ORDER BY dateEpochDay DESC, id") fun observeExpenses(): Flow<List<ExpenseEntity>>
    @Query("SELECT * FROM expenses WHERE id = :id") suspend fun expense(id: String): ExpenseEntity?
    @Upsert suspend fun saveExpense(item: ExpenseEntity)
    @Insert suspend fun insertExpenseRevision(item: ExpenseRevisionEntity)
    @Query("SELECT * FROM expense_revisions WHERE expenseId = :id ORDER BY version") suspend fun expenseHistory(id: String): List<ExpenseRevisionEntity>
    @Query("SELECT * FROM expense_categories ORDER BY label COLLATE NOCASE") fun observeCategories(): Flow<List<ExpenseCategoryEntity>>
    @Query("SELECT * FROM expense_categories WHERE id = :id") suspend fun category(id: String): ExpenseCategoryEntity?
    @Upsert suspend fun saveCategory(item: ExpenseCategoryEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun seedCategories(items: List<ExpenseCategoryEntity>)
    @Query("SELECT * FROM tax_assessments") fun observeAssessments(): Flow<List<TaxAssessmentEntity>>
    @Query("SELECT * FROM tax_assessments WHERE reservationId = :id") suspend fun assessment(id: String): TaxAssessmentEntity?
    @Upsert suspend fun saveAssessment(item: TaxAssessmentEntity)
    @Insert suspend fun insertTaxRevision(item: TaxAssessmentRevisionEntity)
    @Query("SELECT * FROM tax_assessment_revisions WHERE reservationId = :id ORDER BY version") suspend fun taxHistory(id: String): List<TaxAssessmentRevisionEntity>
    @Query("UPDATE tax_assessments SET needsReview = 1 WHERE reservationId = :id") suspend fun markTaxForReview(id: String)
    @Query("SELECT * FROM money_movements ORDER BY createdAt, id") fun observeMovements(): Flow<List<MoneyMovementEntity>>
    @Query("SELECT * FROM money_movements") suspend fun movements(): List<MoneyMovementEntity>
    @Query("SELECT * FROM money_movements WHERE id = :id") suspend fun movement(id: String): MoneyMovementEntity?
    @Insert suspend fun insertMovement(item: MoneyMovementEntity)
}
