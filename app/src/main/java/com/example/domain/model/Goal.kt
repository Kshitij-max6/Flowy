package com.example.domain.model

data class Goal(
    val id: Long = 0,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val currency: String = "₹",
    val targetDate: Long? = null,
    val createdDate: Long = System.currentTimeMillis(),
    val linkedAccountId: Long? = null,
    val iconName: String = "Flag",
    val status: GoalStatus = GoalStatus.ACTIVE
)

data class GoalContribution(
    val id: Long = 0,
    val goalId: Long,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val note: String = ""
)

data class GoalDetail(
    val goal: Goal,
    val linkedAccount: Account? = null,
    val contributions: List<GoalContribution> = emptyList(),
    val percentage: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val isCompleted: Boolean = false,
    val daysRemaining: Int? = null
)
