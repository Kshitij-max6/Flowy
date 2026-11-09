package com.example.domain.model

data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long,
    val accountId: Long,
    val destinationAccountId: Long? = null,
    val bookId: Long = 1L,
    val dateTime: Long = System.currentTimeMillis(),
    val note: String = "",
    val attachmentUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class TransactionDetail(
    val transaction: Transaction,
    val category: Category?,
    val sourceAccount: Account?,
    val destinationAccount: Account? = null,
    val book: ExpenseBook? = null
)
