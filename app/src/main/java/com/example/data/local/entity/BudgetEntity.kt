package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.Budget
import com.example.domain.model.BudgetPeriod

@Entity(
    tableName = "budgets",
    indices = [
        Index("categoryId"),
        Index("startDate"),
        Index("endDate")
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    val period: String, // DAILY, WEEKLY, MONTHLY, CUSTOM
    val categoryId: Long? = null,
    val startDate: Long,
    val endDate: Long,
    val active: Boolean = true
) {
    fun toDomain(): Budget = Budget(
        id = id,
        name = name,
        amount = amount,
        period = try { BudgetPeriod.valueOf(period) } catch (e: Exception) { BudgetPeriod.MONTHLY },
        categoryId = categoryId,
        startDate = startDate,
        endDate = endDate,
        active = active
    )

    companion object {
        fun fromDomain(budget: Budget): BudgetEntity = BudgetEntity(
            id = budget.id,
            name = budget.name,
            amount = budget.amount,
            period = budget.period.name,
            categoryId = budget.categoryId,
            startDate = budget.startDate,
            endDate = budget.endDate,
            active = budget.active
        )
    }
}
