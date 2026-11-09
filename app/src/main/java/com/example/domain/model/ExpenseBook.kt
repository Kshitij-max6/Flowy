package com.example.domain.model

data class ExpenseBook(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val iconName: String = "Wallet",
    val colorHex: String = "#E5C378",
    val budget: Double = 0.0,
    val isArchived: Boolean = false,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ExpenseBookSummary(
    val book: ExpenseBook,
    val totalExpense: Double = 0.0,
    val totalIncome: Double = 0.0,
    val transactionCount: Int = 0,
    val budgetProgress: Float = 0f // 0.0 to 1.0+
)
