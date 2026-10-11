package it.elledue.control.ui

import androidx.lifecycle.*
import it.elledue.control.data.*
import it.elledue.control.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AccountingData(val expenses: List<ExpenseEntity>, val categories: List<ExpenseCategoryEntity>, val taxes: List<TaxAssessmentEntity>, val movements: List<MoneyMovementEntity>, val financialChanges: List<BookingFinancialChangeEntity>)
sealed interface AccountingState {
    data object Loading : AccountingState
    data object Failed : AccountingState
    data class Ready(val data: AccountingData) : AccountingState
}
class AccountingViewModel(private val repository: AccountingRepository) : ViewModel() {
    private val retry = MutableStateFlow(0)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val state = retry.flatMapLatest {
        flow { repository.initialize(); emitAll(combine(repository.expenses, repository.categories, repository.assessments, repository.movements, repository.financialChanges) { e, c, t, m, f -> AccountingState.Ready(AccountingData(e, c, t, m, f)) as AccountingState }) }
            .onStart { emit(AccountingState.Loading) }.catch { emit(AccountingState.Failed) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccountingState.Loading)
    val busy = MutableStateFlow(false); val error = MutableStateFlow<String?>(null)
    val saved = MutableStateFlow<String?>(null)
    val expenseHistory = MutableStateFlow<List<ExpenseRevisionEntity>>(emptyList())
    val taxHistory = MutableStateFlow<List<TaxAssessmentRevisionEntity>>(emptyList())
    fun retry() { retry.value++ }
    fun clear() { error.value = null; saved.value = null }
    fun saveExpense(draft: ExpenseDraft) = execute { saved.value = repository.saveExpense(draft) }
    fun voidExpense(id: String, reason: String) = execute { repository.voidExpense(id, reason); expenseHistory.value = repository.expenseHistory(id); saved.value = "void" }
    fun saveCategory(id: String, label: String, kind: String, archived: Boolean) = execute { repository.saveCategory(id, label, kind, archived); saved.value = "category" }
    fun saveTax(id: String, draft: TaxDraft) = execute { repository.saveTax(id, draft); saved.value = "tax"; taxHistory.value = repository.taxHistory(id) }
    fun post(item: MoneyMovementEntity) = execute { repository.post(item); saved.value = "movement" }
    fun loadExpenseHistory(id: String) { expenseHistory.value = emptyList(); execute { expenseHistory.value = repository.expenseHistory(id) } }
    fun loadTaxHistory(id: String) { taxHistory.value = emptyList(); execute { taxHistory.value = repository.taxHistory(id) } }
    private fun execute(action: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true; error.value = null
        viewModelScope.launch {
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (e: IllegalArgumentException) { error.value = e.message ?: "Controlla i dati." }
            catch (_: ArithmeticException) { error.value = "Importo troppo grande. Controlla i valori." }
            catch (_: Exception) { error.value = "Operazione non riuscita; i dati precedenti sono conservati." }
            finally { busy.value = false }
        }
    }
    class Factory(private val repository: AccountingRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AccountingViewModel::class.java))
            @Suppress("UNCHECKED_CAST") return AccountingViewModel(repository) as T
        }
    }
}
