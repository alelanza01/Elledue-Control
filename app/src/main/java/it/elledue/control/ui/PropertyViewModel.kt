package it.elledue.control.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.elledue.control.data.PropertyRepository
import it.elledue.control.data.PropertyWithOwner
import it.elledue.control.domain.PropertyDraft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface PropertyLoadState {
    data object Loading : PropertyLoadState
    data class Ready(val items: List<PropertyWithOwner>) : PropertyLoadState
    data object Failed : PropertyLoadState
}

class PropertyViewModel(private val repository: PropertyRepository) : ViewModel() {
    private val retry = MutableStateFlow(0)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val state: StateFlow<PropertyLoadState> = retry.flatMapLatest {
        repository.properties.map<List<PropertyWithOwner>, PropertyLoadState> { PropertyLoadState.Ready(it) }
            .onStart { emit(PropertyLoadState.Loading) }
            .catch { emit(PropertyLoadState.Failed) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PropertyLoadState.Loading)
    private val mutableBusy = MutableStateFlow(false)
    val busy = mutableBusy.asStateFlow()
    private val mutableError = MutableStateFlow<String?>(null)
    val error = mutableError.asStateFlow()

    fun retry() { retry.value++ }
    fun clearError() { mutableError.value = null }

    fun save(draft: PropertyDraft, onSaved: (String) -> Unit) = execute {
        repository.save(draft)
        onSaved(draft.id)
    }
    fun archive(id: String, archived: Boolean) = execute { repository.archive(id, archived) }

    private fun execute(action: suspend () -> Unit) {
        if (mutableBusy.value) return
        mutableBusy.value = true
        mutableError.value = null
        viewModelScope.launch {
            try { action() }
            catch (error: CancellationException) { throw error }
            catch (error: IllegalArgumentException) { mutableError.value = error.message ?: "Controlla i dati inseriti." }
            catch (_: Exception) { mutableError.value = "Operazione non riuscita. I dati precedenti sono conservati; riprova." }
            finally { mutableBusy.value = false }
        }
    }

    class Factory(private val repository: PropertyRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(PropertyViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return PropertyViewModel(repository) as T
        }
    }
}
