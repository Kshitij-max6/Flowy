package com.example.feature.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.FlowlyRepository
import com.example.domain.model.Account
import com.example.domain.model.Category
import com.example.domain.model.CategoryType
import com.example.domain.model.Frequency
import com.example.domain.model.RecurringDetail
import com.example.domain.model.RecurringTransaction
import com.example.domain.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

data class RecurringUiState(
    val recurringList: List<RecurringDetail> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val defaultCurrency: String = "₹",
    val monthlyTotalCommitment: Double = 0.0,
    val showAddEditDialog: Boolean = false,
    val editingRecurring: RecurringTransaction? = null,
    val nameInput: String = "",
    val amountInput: String = "",
    val selectedFrequency: Frequency = Frequency.MONTHLY,
    val selectedCategoryId: Long? = null,
    val selectedAccountId: Long? = null,
    val selectedNextDate: Long = System.currentTimeMillis(),
    val noteInput: String = "",
    val recurringToDelete: RecurringDetail? = null,
    val errorMessage: String? = null
)

class RecurringViewModel(
    private val repository: FlowlyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecurringUiState())
    val uiState: StateFlow<RecurringUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                repository.getAllRecurring(),
                repository.getAllCategories(),
                repository.getAllAccounts(),
                repository.getUserSettings()
            ) { recurring, categories, accounts, settings ->
                // Calculate monthly commitment
                val monthlyTotal = recurring.filter { it.recurring.active }.sumOf { item ->
                    when (item.recurring.frequency) {
                        Frequency.DAILY -> item.recurring.amount * 30
                        Frequency.WEEKLY -> item.recurring.amount * 4.33
                        Frequency.MONTHLY -> item.recurring.amount
                        Frequency.YEARLY -> item.recurring.amount / 12
                    }
                }

                _uiState.value = _uiState.value.copy(
                    recurringList = recurring,
                    categories = categories,
                    accounts = accounts,
                    defaultCurrency = settings.defaultCurrency,
                    monthlyTotalCommitment = monthlyTotal
                )
            }.collect {}
        }
    }

    fun openAddDialog() {
        val defaultAcc = _uiState.value.accounts.firstOrNull()?.id
        val defaultCat = _uiState.value.categories.firstOrNull()?.id
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }

        _uiState.value = _uiState.value.copy(
            showAddEditDialog = true,
            editingRecurring = null,
            nameInput = "",
            amountInput = "",
            selectedFrequency = Frequency.MONTHLY,
            selectedCategoryId = defaultCat,
            selectedAccountId = defaultAcc,
            selectedNextDate = cal.timeInMillis,
            noteInput = "",
            errorMessage = null
        )
    }

    fun openEditDialog(item: RecurringDetail) {
        val rec = item.recurring
        _uiState.value = _uiState.value.copy(
            showAddEditDialog = true,
            editingRecurring = rec,
            nameInput = rec.name,
            amountInput = rec.amount.toString(),
            selectedFrequency = rec.frequency,
            selectedCategoryId = rec.categoryId,
            selectedAccountId = rec.accountId,
            selectedNextDate = rec.nextExecutionDate,
            noteInput = rec.note,
            errorMessage = null
        )
    }

    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(showAddEditDialog = false, editingRecurring = null)
    }

    fun onNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(nameInput = name, errorMessage = null)
    }

    fun onAmountChanged(amount: String) {
        val cleaned = amount.filter { it.isDigit() || it == '.' }
        _uiState.value = _uiState.value.copy(amountInput = cleaned, errorMessage = null)
    }

    fun onFrequencyChanged(freq: Frequency) {
        _uiState.value = _uiState.value.copy(selectedFrequency = freq)
    }

    fun onCategoryChanged(catId: Long) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = catId)
    }

    fun onAccountChanged(accId: Long) {
        _uiState.value = _uiState.value.copy(selectedAccountId = accId)
    }

    fun onNextDateChanged(timestamp: Long) {
        _uiState.value = _uiState.value.copy(selectedNextDate = timestamp)
    }

    fun onNoteChanged(note: String) {
        _uiState.value = _uiState.value.copy(noteInput = note)
    }

    fun toggleActive(item: RecurringDetail) {
        viewModelScope.launch {
            val updated = item.recurring.copy(active = !item.recurring.active)
            repository.updateRecurring(updated)
        }
    }

    fun saveRecurring() {
        val state = _uiState.value
        val name = state.nameInput.trim()
        if (name.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please enter a subscription/recurring name")
            return
        }

        val amount = state.amountInput.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.value = state.copy(errorMessage = "Please enter a valid amount")
            return
        }

        val catId = state.selectedCategoryId ?: state.categories.firstOrNull()?.id ?: 1L
        val accId = state.selectedAccountId ?: state.accounts.firstOrNull()?.id ?: 1L

        viewModelScope.launch {
            if (state.editingRecurring != null) {
                val updated = state.editingRecurring.copy(
                    name = name,
                    amount = amount,
                    categoryId = catId,
                    accountId = accId,
                    frequency = state.selectedFrequency,
                    nextExecutionDate = state.selectedNextDate,
                    note = state.noteInput.trim()
                )
                repository.updateRecurring(updated)
            } else {
                val newRec = RecurringTransaction(
                    name = name,
                    amount = amount,
                    type = TransactionType.EXPENSE,
                    categoryId = catId,
                    accountId = accId,
                    frequency = state.selectedFrequency,
                    nextExecutionDate = state.selectedNextDate,
                    active = true,
                    note = state.noteInput.trim()
                )
                repository.addRecurring(newRec)
            }
            _uiState.value = _uiState.value.copy(showAddEditDialog = false, editingRecurring = null)
        }
    }

    fun promptDelete(item: RecurringDetail) {
        _uiState.value = _uiState.value.copy(recurringToDelete = item)
    }

    fun dismissDelete() {
        _uiState.value = _uiState.value.copy(recurringToDelete = null)
    }

    fun confirmDelete() {
        val item = _uiState.value.recurringToDelete ?: return
        viewModelScope.launch {
            repository.deleteRecurring(item.recurring)
            _uiState.value = _uiState.value.copy(recurringToDelete = null)
        }
    }

    class Factory(private val repository: FlowlyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RecurringViewModel(repository) as T
        }
    }
}
