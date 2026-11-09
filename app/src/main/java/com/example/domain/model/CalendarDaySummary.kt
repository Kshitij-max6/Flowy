package com.example.domain.model

data class CalendarDaySummary(
    val dayOfMonth: Int,
    val dateMillis: Long,
    val totalExpense: Double = 0.0,
    val totalIncome: Double = 0.0,
    val netCashFlow: Double = 0.0,
    val transactions: List<TransactionDetail> = emptyList(),
    val upcomingRecurring: List<RecurringDetail> = emptyList(),
    val intensity: SpendingIntensity = SpendingIntensity.NONE,
    val hasIncome: Boolean = false,
    val hasRecurring: Boolean = false
)
