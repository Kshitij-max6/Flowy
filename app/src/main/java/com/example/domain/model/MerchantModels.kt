package com.example.domain.model

data class MerchantSummary(
    val name: String,
    val totalSpent: Double,
    val transactionCount: Int,
    val averageSpend: Double,
    val firstDate: Long,
    val lastDate: Long,
    val topCategory: Category?,
    val topCategoryPercentage: Float,
    val recentTransactions: List<TransactionDetail> = emptyList(),
    val monthlyTrend: List<MerchantMonthlyPoint> = emptyList()
)

data class MerchantMonthlyPoint(
    val monthLabel: String,
    val amount: Double
)
