package it.elledue.control.ui

import androidx.lifecycle.*
import it.elledue.control.data.*
import it.elledue.control.domain.BookingDraft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface BookingLoadState {
    data object Loading : BookingLoadState
    data class Ready(val items: List<ReservationEntity>) : BookingLoadState
    data object Failed : BookingLoadState
}
data class PendingOverlap(val draft: BookingDraft, val conflicts: List<ReservationEntity>)

class ReservationViewModel(private val repository: ReservationRepository) : ViewModel() {
    private val retries = MutableStateFlow(0)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val state = retries.flatMapLatest {
        repository.reservations.map<List<ReservationEntity>, BookingLoadState> { BookingLoadState.Ready(it) }
            .onStart { emit(BookingLoadState.Loading) }.catch { emit(BookingLoadState.Failed) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BookingLoadState.Loading)
    private val mutableBusy = MutableStateFlow(false)
    val busy = mutableBusy.asStateFlow()
    private val mutableError = MutableStateFlow<String?>(null)
    val error = mutableError.asStateFlow()
    private val mutablePending = MutableStateFlow<PendingOverlap?>(null)
    val pending = mutablePending.asStateFlow()
    private val mutableSavedId = MutableStateFlow<String?>(null)
    val savedId = mutableSavedId.asStateFlow()
    val notice=MutableStateFlow<String?>(null)
    fun clearNotice(){notice.value=null}
    fun consumeSaved() { mutableSavedId.value = null }
    fun retry() { retries.value++ }
    fun clearError() { mutableError.value = null }
    fun dismissOverlap() { mutablePending.value = null }
    fun confirmOverlap() {
        val value = mutablePending.value ?: return
        mutablePending.value = null
        save(value.draft, value.conflicts.map { ConflictToken(it.id, it.updatedAt) }.toSet())
    }
    fun save(draft: BookingDraft, approved: Set<ConflictToken> = emptySet()) {
        execute {
            try { repository.save(draft, approved); mutableSavedId.value = draft.id; notice.value="Prenotazione salvata nel database" }
            catch (conflict: OverlapException) { mutablePending.value = PendingOverlap(draft, conflict.conflicts) }
        }
    }
    fun cancel(id: String) { execute { repository.cancel(id) } }
    private fun execute(action: suspend () -> Unit) {
        if (mutableBusy.value) return
        mutableBusy.value = true; mutableError.value = null
        viewModelScope.launch {
            try { action() }
            catch (error: CancellationException) { throw error }
            catch (error: IllegalArgumentException) { mutableError.value = error.message ?: "Controlla i dati." }
            catch (error: ArithmeticException) { mutableError.value = "Importo troppo grande. Controlla i valori." }
            catch (_: Exception) { mutableError.value = "Operazione non riuscita. I dati precedenti sono conservati; riprova." }
            finally { mutableBusy.value = false }
        }
    }
    class Factory(private val repository: ReservationRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ReservationViewModel::class.java))
            @Suppress("UNCHECKED_CAST") return ReservationViewModel(repository) as T
        }
    }
}
