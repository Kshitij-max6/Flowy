package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.Goal
import com.example.domain.model.GoalStatus

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val currency: String = "₹",
    val targetDate: Long? = null,
    val createdDate: Long = System.currentTimeMillis(),
    val linkedAccountId: Long? = null,
    val iconName: String = "Flag",
    val status: String = "ACTIVE"
) {
    fun toDomain(): Goal = Goal(
        id = id,
        name = name,
        targetAmount = targetAmount,
        currentAmount = currentAmount,
        currency = currency,
        targetDate = targetDate,
        createdDate = createdDate,
        linkedAccountId = linkedAccountId,
        iconName = iconName,
        status = try { GoalStatus.valueOf(status) } catch (e: Exception) { GoalStatus.ACTIVE }
    )

    companion object {
        fun fromDomain(goal: Goal): GoalEntity = GoalEntity(
            id = goal.id,
            name = goal.name,
            targetAmount = goal.targetAmount,
            currentAmount = goal.currentAmount,
            currency = goal.currency,
            targetDate = goal.targetDate,
            createdDate = goal.createdDate,
            linkedAccountId = goal.linkedAccountId,
            iconName = goal.iconName,
            status = goal.status.name
        )
    }
}
