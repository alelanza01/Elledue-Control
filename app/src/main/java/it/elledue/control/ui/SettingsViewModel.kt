package it.elledue.control.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.elledue.control.data.AppSettings
import it.elledue.control.data.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SaveState {
    data object Idle : SaveState
    data object Saving : SaveState
    data object Saved : SaveState
    data object Invalid : SaveState
    data object Failed : SaveState
}

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val settings: StateFlow<AppSettings?> = repository.settings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), null,
    )
    private val mutableSaveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState = mutableSaveState.asStateFlow()

    fun clearSaveState() { mutableSaveState.value = SaveState.Idle }

    fun save(name: String) {
        if (mutableSaveState.value == SaveState.Saving) return
        if (name.isBlank()) {
            mutableSaveState.value = SaveState.Invalid
            return
        }
        mutableSaveState.value = SaveState.Saving
        viewModelScope.launch {
            try {
                repository.saveOrganizationName(name)
                mutableSaveState.value = SaveState.Saved
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableSaveState.value = SaveState.Failed
            }
        }
    }

    class Factory(private val repository: SettingsRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SettingsViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository) as T
        }
    }
}
