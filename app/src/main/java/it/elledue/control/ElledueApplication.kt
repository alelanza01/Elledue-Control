package it.elledue.control

import android.app.Application
import androidx.room.Room
import it.elledue.control.data.AppDatabase
import it.elledue.control.data.SettingsRepository

class ElledueApplication : Application() {
    private val database by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "elledue-control.db").build()
    }
    val propertyRepository by lazy { it.elledue.control.data.PropertyRepository(database) }
    val settingsRepository by lazy { SettingsRepository(database.settingsDao()) }
}
