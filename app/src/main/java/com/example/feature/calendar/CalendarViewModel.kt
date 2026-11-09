package com.example.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.FlowlyRepository
import com.example.domain.model.CalendarDaySummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class CalendarUiState(
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH), // 0-based
    val selectedDayOfMonth: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
    val monthTitle: String = "",
    val dayDataMap: Map<Int, CalendarDaySummary> = emptyMap(),
    val totalMonthExpense: Double = 0.0,
    val totalMonthIncome: Double = 0.0,
    val netMonthCashFlow: Double = 0.0,
    val defaultCurrency: String = "₹",
    val firstDayOfWeekOffset: Int = 0, // 0 = Sun, 1 = Mon ...
    val daysInMonth: Int = 30
)

class CalendarViewModel(
    private val repository: FlowlyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        val cal = Calendar.getInstance()
        updateMonthState(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
    }

    fun onPreviousMonth() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _uiState.value.selectedYear)
            set(Calendar.MONTH, _uiState.value.selectedMonth)
            add(Calendar.MONTH, -1)
        }
        updateMonthState(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1)
    }

    fun onNextMonth() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _uiState.value.selectedYear)
            set(Calendar.MONTH, _uiState.value.selectedMonth)
            add(Calendar.MONTH, 1)
        }
        updateMonthState(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1)
    }

    fun onTodayClicked() {
        val cal = Calendar.getInstance()
        updateMonthState(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
    }

    fun onDaySelected(day: Int) {
        _uiState.update { it.copy(selectedDayOfMonth = day) }
    }

    private fun updateMonthState(year: Int, month: Int, selectedDay: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val title = monthFormat.format(cal.time)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 for Sunday
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        _uiState.update {
            it.copy(
                selectedYear = year,
                selectedMonth = month,
                selectedDayOfMonth = selectedDay.coerceIn(1, maxDays),
                monthTitle = title,
                firstDayOfWeekOffset = firstDayOfWeek,
                daysInMonth = maxDays
            )
        }

        viewModelScope.launch {
            combine(
                repository.getCalendarDayData(year, month),
                repository.getUserSettings()
            ) { dataMap, settings ->
                val monthExpense = dataMap.values.sumOf { it.totalExpense }
                val monthIncome = dataMap.values.sumOf { it.totalIncome }
                val netFlow = monthIncome - monthExpense

                _uiState.update {
                    it.copy(
                        dayDataMap = dataMap,
                        totalMonthExpense = monthExpense,
                        totalMonthIncome = monthIncome,
                        netMonthCashFlow = netFlow,
                        defaultCurrency = settings.defaultCurrency
                    )
                }
            }.collect {}
        }
    }
}
