package com.example.domain.model

data class NetWorthSummary(
    val totalAssets: Double = 0.0,
    val totalLiabilities: Double = 0.0,
    val netWorth: Double = 0.0,
    val changeAmount: Double = 0.0,
    val changePercentage: Double = 0.0,
    val isPositiveChange: Boolean = true,
    val currency: String = "₹",
    val assetAccounts: List<Account> = emptyList(),
    val liabilityAccounts: List<Account> = emptyList(),
    val creditCards: List<CreditCard> = emptyList(),
    val loans: List<Loan> = emptyList()
)

data class NetWorthPoint(
    val timestamp: Long,
    val label: String,
    val assets: Double,
    val liabilities: Double,
    val netWorth: Double
)
