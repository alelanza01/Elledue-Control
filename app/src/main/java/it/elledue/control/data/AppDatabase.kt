package it.elledue.control.data

import androidx.room.Database
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val organizationName: String = "Elledue",
    @androidx.room.ColumnInfo(defaultValue = "''") val operatorFirstName: String = "",
    @androidx.room.ColumnInfo(defaultValue = "''") val operatorLastName: String = "",
    val operatorPhotoBase64: String? = null,
)

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observe(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun get(): AppSettings?

    @Upsert
    suspend fun save(settings: AppSettings)
}

@Database(
    entities = [AppSettings::class, OwnerEntity::class, PropertyEntity::class,
        PlatformCommissionEntity::class, MessageTemplateEntity::class, ReservationEntity::class,
        ExpenseEntity::class, ActivityEntity::class, ReportEntity::class,
        ExpenseCategoryEntity::class, ExpenseRevisionEntity::class, TaxAssessmentEntity::class,
        TaxAssessmentRevisionEntity::class, MoneyMovementEntity::class, BookingFinancialChangeEntity::class, ReportAllocationEntity::class],
    version = 5, exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun settingsDao(): SettingsDao
    abstract fun propertyDao(): PropertyDao
    abstract fun reservationDao(): ReservationDao
    abstract fun reportDao(): ReportDao
    abstract fun accountingDao(): AccountingDao
}
