package com.example.domain.model

data class FrequentTransactionPattern(
    val title: String,
    val amount: Double,
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String,
    val accountId: Long,
    val accountName: String,
    val note: String,
    val type: TransactionType,
    val count: Int
)

data class QuickAddSuggestions(
    val recentlyUsedCategories: List<Category> = emptyList(),
    val recentlyUsedAccounts: List<Account> = emptyList(),
    val frequentPatterns: List<FrequentTransactionPattern> = emptyList(),
    val lastTransaction: TransactionDetail? = null,
    val topMerchants: List<String> = emptyList()
)
