package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.GoalContribution

@Entity(
    tableName = "goal_contributions",
    indices = [
        Index("goalId"),
        Index("date")
    ]
)
data class GoalContributionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val note: String = ""
) {
    fun toDomain(): GoalContribution = GoalContribution(
        id = id,
        goalId = goalId,
        amount = amount,
        date = date,
        note = note
    )

    companion object {
        fun fromDomain(contribution: GoalContribution): GoalContributionEntity = GoalContributionEntity(
            id = contribution.id,
            goalId = contribution.goalId,
            amount = contribution.amount,
            date = contribution.date,
            note = contribution.note
        )
    }
}
