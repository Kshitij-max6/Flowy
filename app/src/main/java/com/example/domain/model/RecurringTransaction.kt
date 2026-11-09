package com.example.domain.model

data class RecurringTransaction(
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val type: TransactionType = TransactionType.EXPENSE,
    val categoryId: Long,
    val accountId: Long,
    val frequency: Frequency = Frequency.MONTHLY,
    val nextExecutionDate: Long,
    val active: Boolean = true,
    val note: String = ""
)

data class RecurringDetail(
    val recurring: RecurringTransaction,
    val category: Category?,
    val account: Account?
)
