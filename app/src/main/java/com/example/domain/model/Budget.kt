package com.example.domain.model

data class Budget(
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val categoryId: Long? = null, // null for overall budget
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis(),
    val active: Boolean = true
)

data class BudgetDetail(
    val budget: Budget,
    val category: Category? = null,
    val spentAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val percentageUsed: Double = 0.0,
    val status: BudgetStatus = BudgetStatus.SAFE,
    val daysRemaining: Int = 0
)
