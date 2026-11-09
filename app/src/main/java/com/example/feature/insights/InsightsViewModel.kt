package com.example.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.FlowlyRepository
import com.example.domain.model.PeriodSpendingMetrics
import com.example.domain.model.SpendingComparison
import com.example.domain.model.SpendingInsight
import com.example.domain.model.TimeFilter
import com.example.domain.model.TransactionDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InsightsUiState(
    val timeFilter: TimeFilter = TimeFilter.THIS_MONTH,
    val insights: List<SpendingInsight> = emptyList(),
    val comparison: SpendingComparison = SpendingComparison(),
    val storyData: MonthlyStoryData? = null,
    val allTransactions: List<TransactionDetail> = emptyList(),
    val defaultCurrency: String = "₹",
    val isLoading: Boolean = false,
    val showStoryDialog: Boolean = false
)

class InsightsViewModel(
    private val repository: FlowlyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        loadData(TimeFilter.THIS_MONTH)
    }

    fun onTimeFilterChanged(filter: TimeFilter) {
        _uiState.update { it.copy(timeFilter = filter) }
        loadData(filter)
    }

    fun setShowStoryDialog(show: Boolean) {
        _uiState.update { it.copy(showStoryDialog = show) }
    }

    private fun loadData(filter: TimeFilter) {
        viewModelScope.launch {
            combine(
                repository.getSpendingInsights(filter),
                repository.getSpendingComparison(),
                repository.getTransactionDetailsFlow(),
                repository.getUserSettings()
            ) { insights, comparison, transactions, settings ->
                val story = MonthlyStoryGenerator.generateStory(transactions, settings.defaultCurrency)
                _uiState.update {
                    it.copy(
                        insights = insights,
                        comparison = comparison,
                        allTransactions = transactions,
                        storyData = story,
                        defaultCurrency = settings.defaultCurrency
                    )
                }
            }.collect {}
        }
    }

    class Factory(private val repository: FlowlyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InsightsViewModel(repository) as T
        }
    }
}
