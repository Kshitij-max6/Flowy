package com.example.feature.analytics

import com.example.core.util.PeriodRanges
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
                        // The calendar month we are in, e.g. 1 Nov - 30 Nov (not "the last 30 days").
                        val month = PeriodRanges.thisMonth(now)
                        Pair(transactions.filter { it.transaction.dateTime in month }, null)
                    }
                    TimeFilter.LAST_MONTH -> {
                        val month = PeriodRanges.lastMonth(now)
                        Pair(transactions.filter { it.transaction.dateTime in month }, null)
                    }
                    TimeFilter.ALL_TIME -> {
                        Pair(transactions, null)
                    }
                }

                val periodStart = filteredTx.minOfOrNull { it.transaction.dateTime } ?: now
                val periodLength = when (state.selectedTimeFilter) {
                    TimeFilter.THIS_WEEK -> 7L * 24 * 60 * 60 * 1000
                    TimeFilter.THIS_MONTH -> PeriodRanges.thisMonth(now).length
                    TimeFilter.LAST_MONTH -> PeriodRanges.lastMonth(now).length
                    TimeFilter.ALL_TIME -> (now - periodStart).coerceAtLeast(1L)
                }
                // Comparison window. This month is compared with the same number of days of last month,
                // so a half-finished month is not set against a complete one.
                val (previousFrom, previousTo) = when (state.selectedTimeFilter) {
                    TimeFilter.THIS_MONTH -> {
                        val last = PeriodRanges.lastMonth(now)
                        val elapsed = (now - PeriodRanges.thisMonth(now).start).coerceIn(1L, last.length)
                        last.start to (last.start + elapsed)
                    }
                    TimeFilter.LAST_MONTH -> PeriodRanges.monthStart(now, -2) to PeriodRanges.lastMonth(now).start
                    else -> (now - periodLength * 2) to (now - periodLength)
                }
                val previousExpense = transactions.filter {
                    it.transaction.type == TransactionType.EXPENSE &&
                        it.transaction.dateTime >= previousFrom &&
                        it.transaction.dateTime < previousTo
                }.sumOf { it.transaction.amount }
                val trendStart = when (state.selectedTimeFilter) {
                    TimeFilter.THIS_MONTH -> PeriodRanges.thisMonth(now).start
                    TimeFilter.LAST_MONTH -> PeriodRanges.lastMonth(now).start
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
                val dayMs = 24L * 60 * 60 * 1000
                val periodDays = when (state.selectedTimeFilter) {
                    // Daily average of the days that have actually happened so far this month
                    TimeFilter.THIS_MONTH -> (((now - PeriodRanges.thisMonth(now).start) / dayMs).toInt() + 1)
                    else -> (periodLength / dayMs).toInt()
                }.coerceAtLeast(1)
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
