package com.example.feature.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.FlowlyRepository
import com.example.domain.model.Account
import com.example.domain.model.Budget
import com.example.domain.model.BudgetDetail
import com.example.domain.model.BudgetPeriod
import com.example.domain.model.Category
import com.example.domain.model.CategoryType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class BudgetUiState(
    val budgetDetails: List<BudgetDetail> = emptyList(),
    val categories: List<Category> = emptyList(),
    val defaultCurrency: String = "₹",
    val totalBudgeted: Double = 0.0,
    val totalSpent: Double = 0.0,
    val totalRemaining: Double = 0.0,
    val overallPercentage: Double = 0.0,
    val showAddEditDialog: Boolean = false,
    val editingBudget: Budget? = null,
    val nameInput: String = "",
    val amountInput: String = "",
    val selectedPeriod: BudgetPeriod = BudgetPeriod.MONTHLY,
    val selectedCategoryId: Long? = null, // null for Overall
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis(),
    val errorMessage: String? = null,
    val budgetToDelete: BudgetDetail? = null
)

class BudgetViewModel(
    private val repository: FlowlyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getBudgetDetails(),
                repository.getCategoriesByType(CategoryType.EXPENSE),
                repository.getUserSettings()
            ) { budgets, categories, settings ->
                // Calculate totals for active budgets
                val overallBudget = budgets.find { it.budget.categoryId == null }
                val totalBudget = overallBudget?.budget?.amount ?: budgets.sumOf { it.budget.amount }
                val totalSpent = overallBudget?.spentAmount ?: budgets.sumOf { it.spentAmount }
                val totalRemaining = maxOf(0.0, totalBudget - totalSpent)
                val overallPct = if (totalBudget > 0) (totalSpent / totalBudget) * 100.0 else 0.0

                _uiState.update { current ->
                    current.copy(
                        budgetDetails = budgets,
                        categories = categories,
                        defaultCurrency = settings.defaultCurrency,
                        totalBudgeted = totalBudget,
                        totalSpent = totalSpent,
                        totalRemaining = totalRemaining,
                        overallPercentage = overallPct
                    )
                }
            }.collect {}
        }
    }

    fun openAddDialog() {
        val (start, end) = getPeriodDates(BudgetPeriod.MONTHLY)
        _uiState.update {
            it.copy(
                showAddEditDialog = true,
                editingBudget = null,
                nameInput = "",
                amountInput = "",
                selectedPeriod = BudgetPeriod.MONTHLY,
                selectedCategoryId = null,
                startDate = start,
                endDate = end,
                errorMessage = null
            )
        }
    }

    fun openEditDialog(detail: BudgetDetail) {
        val budget = detail.budget
        _uiState.update {
            it.copy(
                showAddEditDialog = true,
                editingBudget = budget,
                nameInput = budget.name,
                amountInput = if (budget.amount > 0) budget.amount.toInt().toString() else "",
                selectedPeriod = budget.period,
                selectedCategoryId = budget.categoryId,
                startDate = budget.startDate,
                endDate = budget.endDate,
                errorMessage = null
            )
        }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(showAddEditDialog = false, editingBudget = null, errorMessage = null) }
    }

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(nameInput = name, errorMessage = null) }
    }

    fun onAmountChanged(amount: String) {
        _uiState.update { it.copy(amountInput = amount, errorMessage = null) }
    }

    fun onPeriodChanged(period: BudgetPeriod) {
        val (start, end) = getPeriodDates(period)
        _uiState.update {
            it.copy(
                selectedPeriod = period,
                startDate = start,
                endDate = end
            )
        }
    }

    fun onCategorySelected(categoryId: Long?) {
        val categoryName = _uiState.value.categories.find { it.id == categoryId }?.name
        val suggestedName = if (categoryId == null) "Overall Monthly Budget" else "$categoryName Budget"
        _uiState.update {
            it.copy(
                selectedCategoryId = categoryId,
                nameInput = if (it.nameInput.isBlank() || it.nameInput.contains("Budget")) suggestedName else it.nameInput
            )
        }
    }

    fun onCustomDatesChanged(start: Long, end: Long) {
        _uiState.update { it.copy(startDate = start, endDate = end) }
    }

    fun saveBudget() {
        val state = _uiState.value
        val amount = state.amountInput.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid budget amount") }
            return
        }

        val name = state.nameInput.ifBlank {
            if (state.selectedCategoryId == null) "Overall Budget" else "Category Budget"
        }

        viewModelScope.launch {
            val budget = Budget(
                id = state.editingBudget?.id ?: 0,
                name = name,
                amount = amount,
                period = state.selectedPeriod,
                categoryId = state.selectedCategoryId,
                startDate = state.startDate,
                endDate = state.endDate,
                active = true
            )

            if (state.editingBudget != null) {
                repository.updateBudget(budget)
            } else {
                repository.addBudget(budget)
            }

            dismissDialog()
        }
    }

    fun promptDelete(detail: BudgetDetail) {
        _uiState.update { it.copy(budgetToDelete = detail) }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(budgetToDelete = null) }
    }

    fun confirmDelete() {
        val toDelete = _uiState.value.budgetToDelete?.budget ?: return
        viewModelScope.launch {
            repository.deleteBudget(toDelete)
            dismissDelete()
        }
    }

    private fun getPeriodDates(period: BudgetPeriod): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        return when (period) {
            BudgetPeriod.DAILY -> {
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                Pair(start, cal.timeInMillis)
            }
            BudgetPeriod.WEEKLY -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_WEEK, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                Pair(start, cal.timeInMillis)
            }
            BudgetPeriod.MONTHLY -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                Pair(start, cal.timeInMillis)
            }
            BudgetPeriod.CUSTOM -> {
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_MONTH, 30)
                Pair(start, cal.timeInMillis)
            }
        }
    }
}
