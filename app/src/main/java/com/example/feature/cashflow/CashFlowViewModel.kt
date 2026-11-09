package com.example.feature.cashflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.FlowlyRepository
import com.example.domain.model.CashFlowForecast
import com.example.domain.model.ForecastPeriod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class CashFlowUiState(
    val forecast: CashFlowForecast? = null,
    val selectedPeriod: ForecastPeriod = ForecastPeriod.NEXT_30_DAYS,
    val isLoading: Boolean = false
)

class CashFlowViewModel(
    private val repository: FlowlyRepository
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(ForecastPeriod.NEXT_30_DAYS)
    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<CashFlowUiState> = combine(
        _selectedPeriod.flatMapLatest { period -> repository.getCashFlowForecast(period) },
        _selectedPeriod,
        _isLoading
    ) { forecast, period, loading ->
        CashFlowUiState(
            forecast = forecast,
            selectedPeriod = period,
            isLoading = loading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CashFlowUiState()
    )

    fun onPeriodSelected(period: ForecastPeriod) {
        _selectedPeriod.value = period
    }

    class Factory(private val repository: FlowlyRepository) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CashFlowViewModel(repository) as T
        }
    }
}
