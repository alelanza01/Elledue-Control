package it.elledue.control

import android.app.Application
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import androidx.room.Room
import it.elledue.control.data.AppDatabase
import it.elledue.control.data.SettingsRepository

class ElledueApplication : Application() {
    val maintenanceError=kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val background = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.CoroutineExceptionHandler{_,_->maintenanceError.value="Aggiornamento promemoria o bozze interrotto. Riapri l’app e verifica i dati senza eliminarli."})
    val reminderStore by lazy { it.elledue.control.notifications.ReminderStore(this) }
    val reminders by lazy { it.elledue.control.notifications.ReminderEngine(this) }
    val appLock by lazy { it.elledue.control.security.AppLock(this) }
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        background.launch {
            database.invalidationTracker.createFlow("properties","owners","reservations","expenses","tax_assessments","money_movements","activities",emitInitialState=true)
                .debounce(250).collect {
                    runCatching { reportRepository.refreshDrafts() }
                    runCatching { reminders.reconcile() }
                }
        }
        background.launch { reminderStore.state.collect { runCatching { reminders.reconcile() } } }
    }
    override fun onTerminate() { background.cancel();super.onTerminate() }

    private val database by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "elledue-control.db").addMigrations(it.elledue.control.data.DatabaseMigrations.MIGRATION_1_2, it.elledue.control.data.DatabaseMigrations.MIGRATION_2_3, it.elledue.control.data.DatabaseMigrations.MIGRATION_3_4, it.elledue.control.data.DatabaseMigrations.MIGRATION_4_5, it.elledue.control.data.DatabaseMigrations.MIGRATION_5_6, it.elledue.control.data.DatabaseMigrations.MIGRATION_6_7).build()
    }
    val messageRepository by lazy { it.elledue.control.data.MessageRepository(database,drafts) }
    val drafts by lazy { it.elledue.control.data.DraftStore.get(this) }
    val deletionRepository by lazy { it.elledue.control.data.DeletionRepository(database,this) }
    val operations by lazy { it.elledue.control.data.OperationalRepository(database) }
    val activityRepository by lazy { it.elledue.control.data.ActivityRepository(database,drafts) }
    val reportRepository by lazy { it.elledue.control.data.ReportRepository(database, this) }
    val backupService by lazy { it.elledue.control.data.BackupService(database, this) }
    val accountingRepository by lazy { it.elledue.control.data.AccountingRepository(database,drafts) }
    val reservationRepository by lazy { it.elledue.control.data.ReservationRepository(database,drafts) }
    val propertyRepository by lazy { it.elledue.control.data.PropertyRepository(database,drafts) }
    val settingsRepository by lazy { SettingsRepository(database.settingsDao(),drafts) }
}
