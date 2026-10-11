package it.elledue.control

import android.app.Application
import androidx.room.Room
import it.elledue.control.data.AppDatabase
import it.elledue.control.data.SettingsRepository

class ElledueApplication : Application() {
    private val database by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "elledue-control.db").addMigrations(it.elledue.control.data.DatabaseMigrations.MIGRATION_1_2, it.elledue.control.data.DatabaseMigrations.MIGRATION_2_3, it.elledue.control.data.DatabaseMigrations.MIGRATION_3_4).build()
    }
    val reportRepository by lazy { it.elledue.control.data.ReportRepository(database, this) }
    val backupService by lazy { it.elledue.control.data.BackupService(database, this) }
    val accountingRepository by lazy { it.elledue.control.data.AccountingRepository(database) }
    val reservationRepository by lazy { it.elledue.control.data.ReservationRepository(database) }
    val propertyRepository by lazy { it.elledue.control.data.PropertyRepository(database) }
    val settingsRepository by lazy { SettingsRepository(database.settingsDao()) }
}
