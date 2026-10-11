package it.elledue.control.ui

import androidx.lifecycle.*
import it.elledue.control.data.*
import it.elledue.control.reports.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

sealed interface ReportState {
    data object Loading : ReportState
    data object Failed : ReportState
    data class Ready(val reports: List<ReportEntity>) : ReportState
}
class ReportViewModel(private val repo: ReportRepository) : ViewModel() {
    private val retry = MutableStateFlow(0)
    @OptIn(ExperimentalCoroutinesApi::class)
    val state = retry.flatMapLatest { repo.reports.map<List<ReportEntity>, ReportState> { ReportState.Ready(it) }.onStart { emit(ReportState.Loading) }.catch { emit(ReportState.Failed) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportState.Loading)
    val busy = MutableStateFlow(false); val error = MutableStateFlow<String?>(null); val savedId = MutableStateFlow<String?>(null)
    val unassigned = MutableStateFlow<List<ReservationEntity>>(emptyList())
    fun retry() { retry.value++ }
    fun clear() { error.value = null; savedId.value = null }
    fun generate(property: String, month: String) = execute { savedId.value = repo.generate(property, month) }
    fun refresh(id: String, notes: String? = null) = execute { repo.refresh(id, notes) }
    fun revision(id: String, reason: String) = execute { savedId.value = repo.beginRevision(id, reason) }
    fun confirm(snapshot: ReportSnapshot, acknowledged: Boolean) = execute { repo.confirm(snapshot.id, ReportCodec.hash(ReportCodec.encode(snapshot)), acknowledged) }
    fun loadUnassigned() = execute { unassigned.value = repo.unassigned() }
    private fun execute(action: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true; error.value = null
        viewModelScope.launch {
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (e: IllegalArgumentException) { error.value = e.message ?: "Controlla i dati." }
            catch (_: Exception) { error.value = "Operazione non riuscita: i dati precedenti sono conservati." }
            finally { busy.value = false }
        }
    }
    class Factory(private val repo: ReportRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ReportViewModel::class.java)); @Suppress("UNCHECKED_CAST") return ReportViewModel(repo) as T
        }
    }
}
