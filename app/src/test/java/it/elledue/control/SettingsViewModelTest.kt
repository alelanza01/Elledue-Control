package it.elledue.control

import androidx.lifecycle.viewModelScope
import it.elledue.control.data.AppSettings
import it.elledue.control.data.SettingsDao
import it.elledue.control.data.SettingsRepository
import it.elledue.control.ui.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @Test fun databaseReadFailureIsExposedWithoutInventingDefaultData() {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            runTest(dispatcher) {
                val failingDao = object : SettingsDao {
                    override fun observe(): Flow<AppSettings?> = flow { throw IllegalStateException("Database unavailable") }
                    override suspend fun save(settings: AppSettings) = Unit
                }
                val model = SettingsViewModel(SettingsRepository(failingDao))
                try {
                    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.settings.collect {} }
                    runCurrent()
                    assertTrue(model.loadFailed.value)
                    assertNull(model.settings.value)
                } finally { model.viewModelScope.cancel() }
            }
        } finally { Dispatchers.resetMain() }
    }
}
