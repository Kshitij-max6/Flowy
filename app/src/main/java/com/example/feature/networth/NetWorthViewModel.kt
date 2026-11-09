package com.example.feature.networth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.FlowlyRepository
import com.example.domain.model.NetWorthPoint
import com.example.domain.model.NetWorthSummary
import com.example.domain.model.NetWorthTimeRange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NetWorthUiState(
    val summary: NetWorthSummary? = null,
    val historyPoints: List<NetWorthPoint> = emptyList(),
    val selectedTimeRange: NetWorthTimeRange = NetWorthTimeRange.THREE_MONTHS,
    val isRefreshing: Boolean = false,
    val showSnapshotSavedToast: Boolean = false
)

class NetWorthViewModel(
    private val repository: FlowlyRepository
) : ViewModel() {

    private val _selectedTimeRange = MutableStateFlow(NetWorthTimeRange.THREE_MONTHS)
    private val _isRefreshing = MutableStateFlow(false)
    private val _showSnapshotSavedToast = MutableStateFlow(false)

    val uiState: StateFlow<NetWorthUiState> = combine(
        repository.getNetWorthSummary(),
        _selectedTimeRange.flatMapLatest { timeRange -> repository.getNetWorthHistory(timeRange) },
        _selectedTimeRange,
        _isRefreshing,
        _showSnapshotSavedToast
    ) { summary, history, timeRange, refreshing, toast ->
        NetWorthUiState(
            summary = summary,
            historyPoints = history,
            selectedTimeRange = timeRange,
            isRefreshing = refreshing,
            showSnapshotSavedToast = toast
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NetWorthUiState()
    )

    fun onTimeRangeSelected(range: NetWorthTimeRange) {
        _selectedTimeRange.value = range
    }

    fun takeSnapshot() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.saveNetWorthSnapshot()
            _isRefreshing.value = false
            _showSnapshotSavedToast.value = true
        }
    }

    fun dismissToast() {
        _showSnapshotSavedToast.value = false
    }

    class Factory(private val repository: FlowlyRepository) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NetWorthViewModel(repository) as T
        }
    }
}
