package it.elledue.control

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.elledue.control.data.AppDatabase
import it.elledue.control.data.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class SettingsRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: SettingsRepository

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        repository = SettingsRepository(database.settingsDao())
    }
    @After fun tearDown() { database.close() }

    @Test fun emptyDatabaseProvidesDefaults() = runBlocking {
        assertEquals("Elledue", repository.settings.first().organizationName)
    }
    @Test fun savingReplacesTheSingletonAndTrimsWhitespace() = runBlocking {
        repository.saveOrganizationName("  Azienda Uno  ")
        repository.saveOrganizationName("  Azienda Due  ")
        assertEquals("Azienda Due", repository.settings.first().organizationName)
        database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM app_settings").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
    }
    @Test fun blankNameDoesNotOverwriteSavedSettings() {
        runBlocking { repository.saveOrganizationName("Azienda") }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.saveOrganizationName("  ") }
        }
        runBlocking { assertEquals("Azienda", repository.settings.first().organizationName) }
    }
    @Test fun savedSettingsSurviveDatabaseReopening() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "settings-persistence-test.db"
        context.deleteDatabase(name)
        try {
            Room.databaseBuilder(context, AppDatabase::class.java, name).build().withClosing { db ->
                SettingsRepository(db.settingsDao()).saveOrganizationName("Persistente")
            }
            Room.databaseBuilder(context, AppDatabase::class.java, name).build().withClosing { db ->
                assertEquals("Persistente", SettingsRepository(db.settingsDao()).settings.first().organizationName)
            }
        } finally { context.deleteDatabase(name) }
    }
}
