package com.example.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.util.DateUtils
import com.example.data.repository.FlowlyRepository
import com.example.domain.model.Account
import com.example.domain.model.Category
import com.example.domain.model.SavedTransactionFilter
import com.example.domain.model.TransactionDetail
import com.example.domain.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

enum class DateFilterType {
    ALL,
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    CUSTOM
}

enum class SortOrder {
    DATE_DESC,
    DATE_ASC,
    AMOUNT_DESC,
    AMOUNT_ASC
}

data class TransactionsUiState(
    val allTransactions: List<TransactionDetail> = emptyList(),
    val filteredGroupedTransactions: Map<String, List<TransactionDetail>> = emptyMap(),
    val totalExpense: Double = 0.0,
    val totalIncome: Double = 0.0,
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val savedFilters: List<SavedTransactionFilter> = emptyList(),
    val activeSavedFilterId: Long? = null,
    // Filters
    val searchQuery: String = "",
    val selectedType: TransactionType? = null,
    val selectedCategoryId: Long? = null,
    val selectedAccountId: Long? = null,
    val selectedDateFilter: DateFilterType = DateFilterType.ALL,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
    // Dialogs & Sheets
    val showFilterSheet: Boolean = false,
    val showSaveFilterDialog: Boolean = false,
    val saveFilterNameInput: String = "",
    val defaultCurrency: String = "₹",
    val transactionToDelete: TransactionDetail? = null,
    val isLoading: Boolean = true
) {
    val activeFilterCount: Int
        get() {
            var count = 0
            if (searchQuery.isNotBlank()) count++
            if (selectedType != null) count++
            if (selectedCategoryId != null) count++
            if (selectedAccountId != null) count++
            if (selectedDateFilter != DateFilterType.ALL) count++
            if (minAmount != null || maxAmount != null) count++
            return count
        }
}

class TransactionsViewModel(
    private val repository: FlowlyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                repository.getTransactionDetailsFlow(),
                repository.getAllCategories(),
                repository.getAllAccounts(),
                repository.getUserSettings(),
                repository.getAllSavedFilters()
            ) { transactions, categories, accounts, settings, savedFilters ->
                _uiState.value = _uiState.value.copy(
                    allTransactions = transactions,
                    categories = categories,
                    accounts = accounts,
                    savedFilters = savedFilters,
                    defaultCurrency = settings.defaultCurrency,
                    isLoading = false
                )
                applyFilters()
            }.collect {}
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query, activeSavedFilterId = null)
        applyFilters()
    }

    fun onTypeFilterSelected(type: TransactionType?) {
        _uiState.value = _uiState.value.copy(selectedType = type, activeSavedFilterId = null)
        applyFilters()
    }

    fun onCategoryFilterSelected(categoryId: Long?) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId, activeSavedFilterId = null)
        applyFilters()
    }

    fun onAccountFilterSelected(accountId: Long?) {
        _uiState.value = _uiState.value.copy(selectedAccountId = accountId, activeSavedFilterId = null)
        applyFilters()
    }

    fun onDateFilterSelected(dateFilter: DateFilterType) {
        _uiState.value = _uiState.value.copy(selectedDateFilter = dateFilter, activeSavedFilterId = null)
        applyFilters()
    }

    fun onCustomDateRangeSelected(start: Long?, end: Long?) {
        _uiState.value = _uiState.value.copy(
            selectedDateFilter = DateFilterType.CUSTOM,
            customStartDate = start,
            customEndDate = end,
            activeSavedFilterId = null
        )
        applyFilters()
    }

    fun onAmountRangeSelected(min: Double?, max: Double?) {
        _uiState.value = _uiState.value.copy(minAmount = min, maxAmount = max, activeSavedFilterId = null)
        applyFilters()
    }

    fun onSortOrderChanged(order: SortOrder) {
        _uiState.value = _uiState.value.copy(sortOrder = order)
        applyFilters()
    }

    fun clearAllFilters() {
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            selectedType = null,
            selectedCategoryId = null,
            selectedAccountId = null,
            selectedDateFilter = DateFilterType.ALL,
            customStartDate = null,
            customEndDate = null,
            minAmount = null,
            maxAmount = null,
            activeSavedFilterId = null
        )
        applyFilters()
    }

    fun openFilterSheet() {
        _uiState.value = _uiState.value.copy(showFilterSheet = true)
    }

    fun dismissFilterSheet() {
        _uiState.value = _uiState.value.copy(showFilterSheet = false)
    }

    fun openSaveFilterDialog() {
        _uiState.value = _uiState.value.copy(showSaveFilterDialog = true, saveFilterNameInput = "")
    }

    fun dismissSaveFilterDialog() {
        _uiState.value = _uiState.value.copy(showSaveFilterDialog = false)
    }

    fun onSaveFilterNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(saveFilterNameInput = name)
    }

    fun saveCurrentFilterPreset() {
        val s = _uiState.value
        val name = s.saveFilterNameInput.trim().ifBlank { "Filter Preset" }

        val filter = SavedTransactionFilter(
            id = 0,
            name = name,
            transactionType = s.selectedType,
            categoryIds = s.selectedCategoryId?.let { listOf(it) } ?: emptyList(),
            accountIds = s.selectedAccountId?.let { listOf(it) } ?: emptyList(),
            minAmount = s.minAmount,
            maxAmount = s.maxAmount,
            customStartDate = s.customStartDate,
            customEndDate = s.customEndDate,
            searchQuery = s.searchQuery
        )

        viewModelScope.launch {
            repository.addSavedFilter(filter)
            dismissSaveFilterDialog()
        }
    }

    fun applySavedFilter(filter: SavedTransactionFilter) {
        _uiState.value = _uiState.value.copy(
            activeSavedFilterId = filter.id,
            selectedType = filter.transactionType,
            selectedCategoryId = filter.categoryIds.firstOrNull(),
            selectedAccountId = filter.accountIds.firstOrNull(),
            minAmount = filter.minAmount,
            maxAmount = filter.maxAmount,
            customStartDate = filter.customStartDate,
            customEndDate = filter.customEndDate,
            selectedDateFilter = if (filter.customStartDate != null) DateFilterType.CUSTOM else DateFilterType.ALL,
            searchQuery = filter.searchQuery
        )
        applyFilters()
    }

    fun deleteSavedFilter(filter: SavedTransactionFilter) {
        viewModelScope.launch {
            repository.deleteSavedFilter(filter)
            if (_uiState.value.activeSavedFilterId == filter.id) {
                _uiState.value = _uiState.value.copy(activeSavedFilterId = null)
            }
        }
    }

    fun promptDeleteTransaction(item: TransactionDetail) {
        _uiState.value = _uiState.value.copy(transactionToDelete = item)
    }

    fun dismissDeleteDialog() {
        _uiState.value = _uiState.value.copy(transactionToDelete = null)
    }

    fun confirmDeleteTransaction() {
        val item = _uiState.value.transactionToDelete ?: return
        viewModelScope.launch {
            repository.deleteTransaction(item.transaction)
            _uiState.value = _uiState.value.copy(transactionToDelete = null)
        }
    }

    private fun applyFilters() {
        val state = _uiState.value
        val now = System.currentTimeMillis()

        var filtered = state.allTransactions

        // Search Query
        if (state.searchQuery.isNotBlank()) {
            val q = state.searchQuery.trim().lowercase()
            filtered = filtered.filter { item ->
                item.transaction.note.lowercase().contains(q) ||
                        (item.category?.name?.lowercase()?.contains(q) == true) ||
                        (item.sourceAccount?.name?.lowercase()?.contains(q) == true) ||
                        (item.destinationAccount?.name?.lowercase()?.contains(q) == true)
            }
        }

        // Type Filter
        if (state.selectedType != null) {
            filtered = filtered.filter { it.transaction.type == state.selectedType }
        }

        // Category Filter
        if (state.selectedCategoryId != null) {
            filtered = filtered.filter { it.transaction.categoryId == state.selectedCategoryId }
        }

        // Account Filter
        if (state.selectedAccountId != null) {
            filtered = filtered.filter {
                it.transaction.accountId == state.selectedAccountId ||
                        it.transaction.destinationAccountId == state.selectedAccountId
            }
        }

        // Min Amount
        state.minAmount?.let { min ->
            filtered = filtered.filter { it.transaction.amount >= min }
        }

        // Max Amount
        state.maxAmount?.let { max ->
            filtered = filtered.filter { it.transaction.amount <= max }
        }

        // Date Filter
        filtered = when (state.selectedDateFilter) {
            DateFilterType.ALL -> filtered
            DateFilterType.TODAY -> {
                val start = DateUtils.getStartOfToday()
                val end = DateUtils.getEndOfToday()
                filtered.filter { it.transaction.dateTime in start..end }
            }
            DateFilterType.THIS_WEEK -> {
                val weekAgo = now - (7L * 24 * 60 * 60 * 1000)
                filtered.filter { it.transaction.dateTime >= weekAgo }
            }
            DateFilterType.THIS_MONTH -> {
                val monthAgo = now - (30L * 24 * 60 * 60 * 1000)
                filtered.filter { it.transaction.dateTime >= monthAgo }
            }
            DateFilterType.CUSTOM -> {
                val start = state.customStartDate ?: 0L
                val end = state.customEndDate ?: Long.MAX_VALUE
                filtered.filter { it.transaction.dateTime in start..end }
            }
        }

        // Sort Order
        filtered = when (state.sortOrder) {
            SortOrder.DATE_DESC -> filtered.sortedByDescending { it.transaction.dateTime }
            SortOrder.DATE_ASC -> filtered.sortedBy { it.transaction.dateTime }
            SortOrder.AMOUNT_DESC -> filtered.sortedByDescending { it.transaction.amount }
            SortOrder.AMOUNT_ASC -> filtered.sortedBy { it.transaction.amount }
        }

        // Totals
        val totalExpense = filtered.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.amount }
        val totalIncome = filtered.filter { it.transaction.type == TransactionType.INCOME }.sumOf { it.transaction.amount }

        // Group by relative date (Today, Yesterday, 28 Aug 2026, etc.)
        val grouped = filtered.groupBy { DateUtils.formatRelativeDate(it.transaction.dateTime) }

        _uiState.value = state.copy(
            filteredGroupedTransactions = grouped,
            totalExpense = totalExpense,
            totalIncome = totalIncome
        )
    }

    class Factory(private val repository: FlowlyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TransactionsViewModel(repository) as T
        }
    }
}
