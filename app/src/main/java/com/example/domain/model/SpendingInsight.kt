package com.example.domain.model

data class SpendingInsight(
    val title: String,
    val description: String,
    val type: InsightType,
    val iconName: String
)

enum class InsightType {
    POSITIVE,
    WARNING,
    INFO,
    TREND
}
