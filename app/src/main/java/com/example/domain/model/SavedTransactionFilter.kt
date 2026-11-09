package com.example.domain.model

data class SavedTransactionFilter(
    val id: Long = 0,
    val name: String,
    val datePreset: FilterDatePreset = FilterDatePreset.ALL,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val transactionType: TransactionType? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val categoryIds: List<Long> = emptyList(),
    val accountIds: List<Long> = emptyList(),
    val sourceFilter: FilterSource = FilterSource.ALL,
    val hasNote: Boolean = false,
    val searchQuery: String = ""
)
