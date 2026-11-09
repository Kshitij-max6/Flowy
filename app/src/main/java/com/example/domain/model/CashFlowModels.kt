package com.example.domain.model

data class ForecastEvent(
    val id: String,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val date: Long,
    val dateFormatted: String,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColorHex: String,
    val accountName: String,
    val runningBalance: Double
)

data class CashFlowForecast(
    val currentBalance: Double = 0.0,
    val expectedIncome: Double = 0.0,
    val expectedExpenses: Double = 0.0,
    val projectedBalance: Double = 0.0,
    val netCashFlow: Double = 0.0,
    val period: ForecastPeriod = ForecastPeriod.NEXT_30_DAYS,
    val periodDays: Int = 30,
    val periodLabel: String = "Next 30 Days",
    val timelineEvents: List<ForecastEvent> = emptyList(),
    val currency: String = "₹"
)
