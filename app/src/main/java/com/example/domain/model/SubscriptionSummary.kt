package com.example.domain.model

data class SubscriptionSummary(
    val monthlyNormalizedCost: Double = 0.0,
    val yearlyEstimatedCost: Double = 0.0,
    val activeSubscriptionsCount: Int = 0,
    val pausedSubscriptionsCount: Int = 0,
    val nextDueSubscription: RecurringDetail? = null
)

data class PeriodSpendingMetrics(
    val currentPeriodExpense: Double = 0.0,
    val previousPeriodExpense: Double = 0.0,
    val differenceAmount: Double = 0.0,
    val percentageChange: Double = 0.0,
    val hasEnoughData: Boolean = false
)

data class SpendingComparison(
    val todayMetrics: PeriodSpendingMetrics = PeriodSpendingMetrics(),
    val weekMetrics: PeriodSpendingMetrics = PeriodSpendingMetrics(),
    val monthMetrics: PeriodSpendingMetrics = PeriodSpendingMetrics(),
    val weekendVsWeekdaySummary: String = "",
    val topSpendingCategoryThisMonth: String? = null,
    val topCategoryPercentage: Int = 0
)
