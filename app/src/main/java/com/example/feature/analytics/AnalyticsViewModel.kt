package com.example.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.FlowlyRepository
import com.example.domain.model.Category
import com.example.domain.model.SpendingInsight
import com.example.domain.model.TimeFilter
import com.example.domain.model.TransactionDetail
import com.example.domain.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class CategorySpendItem(
    val category: Category?,
    val amount: Double,
    val percentage: Float,
    val colorHex: String
)

data class AnalyticsUiState(
    val selectedTimeFilter: TimeFilter = TimeFilter.THIS_MONTH,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netCashFlow: Double = 0.0,
    val spendingTrend: List<Double> = emptyList(),
    /** One short date label per bucket in [spendingTrend] (e.g. "12 Oct"). */
    val trendLabels: List<String> = emptyList(),
    val transactionCount: Int = 0,
    val dailyAverage: Double = 0.0,
    val largestExpense: Double = 0.0,
    val largestExpenseCategory: String? = null,
    val previousPeriodExpense: Double = 0.0,
    val categoryBreakdown: List<CategorySpendItem> = emptyList(),
    val topMerchants: List<com.example.domain.model.MerchantSummary> = emptyList(),
    val insights: List<SpendingInsight> = emptyList(),
    val defaultCurrency: String = "₹",
    val isLoading: Boolean = true
)

class AnalyticsViewModel(
    private val repository: FlowlyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                repository.getTransactionDetailsFlow(),
                repository.getAllCategories(),
                repository.getUserSettings(),
                _uiState
            ) { transactions, categories, settings, state ->
                val now = System.currentTimeMillis()
                val (filteredTx, _) = when (state.selectedTimeFilter) {
                    TimeFilter.THIS_WEEK -> {
                        val weekAgo = now - (7L * 24 * 60 * 60 * 1000)
                        Pair(transactions.filter { it.transaction.dateTime >= weekAgo }, null)
                    }
                    TimeFilter.THIS_MONTH -> {
                        val monthAgo = now - (30L * 24 * 60 * 60 * 1000)
                        Pair(transactions.filter { it.transaction.dateTime >= monthAgo }, null)
                    }
                    TimeFilter.LAST_MONTH -> {
                        val monthAgo = now - (30L * 24 * 60 * 60 * 1000)
                        val twoMonthsAgo = now - (60L * 24 * 60 * 60 * 1000)
                        Pair(transactions.filter { it.transaction.dateTime in twoMonthsAgo..monthAgo }, null)
                    }
                    TimeFilter.ALL_TIME -> {
                        Pair(transactions, null)
                    }
                }

                val periodStart = filteredTx.minOfOrNull { it.transaction.dateTime } ?: now
                val periodLength = when (state.selectedTimeFilter) {
                    TimeFilter.THIS_WEEK -> 7L * 24 * 60 * 60 * 1000
                    TimeFilter.THIS_MONTH, TimeFilter.LAST_MONTH -> 30L * 24 * 60 * 60 * 1000
                    TimeFilter.ALL_TIME -> (now - periodStart).coerceAtLeast(1L)
                }
                val previousEnd = when (state.selectedTimeFilter) {
                    TimeFilter.LAST_MONTH -> now - 60L * 24 * 60 * 60 * 1000
                    else -> now - periodLength
                }
                val previousExpense = transactions.filter {
                    it.transaction.type == TransactionType.EXPENSE &&
                        it.transaction.dateTime >= previousEnd - periodLength &&
                        it.transaction.dateTime < previousEnd
                }.sumOf { it.transaction.amount }
                val trendStart = when (state.selectedTimeFilter) {
                    TimeFilter.LAST_MONTH -> now - 60L * 24 * 60 * 60 * 1000
                    TimeFilter.ALL_TIME -> periodStart
                    else -> now - periodLength
                }
                val trend = List(7) { index ->
                    val start = trendStart + periodLength * index / 7
                    val end = trendStart + periodLength * (index + 1) / 7
                    filteredTx.filter {
                        it.transaction.type == TransactionType.EXPENSE &&
                            it.transaction.dateTime >= start && it.transaction.dateTime < end
                    }.sumOf { it.transaction.amount }
                }
                val dateFormat = java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault())
                val trendLabels = List(7) { index ->
                    dateFormat.format(java.util.Date(trendStart + periodLength * index / 7))
                }
                val expenses = filteredTx.filter { it.transaction.type == TransactionType.EXPENSE }
                val income = filteredTx.filter { it.transaction.type == TransactionType.INCOME }

                val totalExp = expenses.sumOf { it.transaction.amount }
                val totalInc = income.sumOf { it.transaction.amount }
                val netFlow = totalInc - totalExp
                val periodDays = (periodLength / (24L * 60 * 60 * 1000)).toInt().coerceAtLeast(1)
                val largest = expenses.maxByOrNull { it.transaction.amount }

                // Group expenses by category
                val catMap = categories.associateBy { it.id }
                val catSpending = expenses.groupBy { it.transaction.categoryId }
                    .map { (catId, items) ->
                        val sum = items.sumOf { it.transaction.amount }
                        val cat = catMap[catId]
                        val pct = if (totalExp > 0) (sum / totalExp).toFloat() else 0f
                        CategorySpendItem(
                            category = cat,
                            amount = sum,
                            percentage = pct,
                            colorHex = cat?.colorHex ?: "#10B981"
                        )
                    }
                    .sortedByDescending { it.amount }

                _uiState.value = state.copy(
                    totalIncome = totalInc,
                    totalExpense = totalExp,
                    netCashFlow = netFlow,
                    spendingTrend = trend,
                    trendLabels = trendLabels,
                    transactionCount = expenses.size,
                    dailyAverage = totalExp / periodDays,
                    largestExpense = largest?.transaction?.amount ?: 0.0,
                    largestExpenseCategory = largest?.category?.name,
                    previousPeriodExpense = previousExpense,
                    categoryBreakdown = catSpending,
                    defaultCurrency = settings.defaultCurrency,
                    isLoading = false
                )
            }.collect {}
        }

        // Collect insights reactively
        viewModelScope.launch {
            _uiState.collect { state ->
                repository.getSpendingInsights(state.selectedTimeFilter).collect { insightsList ->
                    _uiState.value = _uiState.value.copy(insights = insightsList)
                }
            }
        }

        // Collect top merchants
        viewModelScope.launch {
            repository.getTopMerchants(8).collect { merchants ->
                _uiState.value = _uiState.value.copy(topMerchants = merchants)
            }
        }
    }

    fun onTimeFilterSelected(filter: TimeFilter) {
        _uiState.value = _uiState.value.copy(selectedTimeFilter = filter)
    }

    class Factory(private val repository: FlowlyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AnalyticsViewModel(repository) as T
        }
    }
}
