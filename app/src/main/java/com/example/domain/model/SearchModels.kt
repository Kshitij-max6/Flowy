package com.example.domain.model

data class GlobalSearchResults(
    val query: String = "",
    val transactions: List<TransactionDetail> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val subscriptions: List<RecurringDetail> = emptyList(),
    val goals: List<GoalDetail> = emptyList(),
    val creditCards: List<CreditCard> = emptyList(),
    val loans: List<Loan> = emptyList()
) {
    val isEmpty: Boolean
        get() = transactions.isEmpty() &&
                accounts.isEmpty() &&
                categories.isEmpty() &&
                subscriptions.isEmpty() &&
                goals.isEmpty() &&
                creditCards.isEmpty() &&
                loans.isEmpty()

    val totalResultCount: Int
        get() = transactions.size + accounts.size + categories.size +
                subscriptions.size + goals.size + creditCards.size + loans.size
}
