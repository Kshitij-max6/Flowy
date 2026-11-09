package com.example.domain.model

data class FlowlyBackupData(
    val version: Int = 1,
    val appName: String = "Flowly",
    val appVersion: String = "1.0",
    val timestamp: Long = System.currentTimeMillis(),
    val createdAtFormatted: String = "",
    val userSettings: UserSettings? = null,
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val recurringTransactions: List<RecurringTransaction> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val goalContributions: List<GoalContribution> = emptyList(),
    val creditCards: List<CreditCard> = emptyList(),
    val loans: List<Loan> = emptyList(),
    val loanPayments: List<LoanPayment> = emptyList(),
    val savedFilters: List<SavedTransactionFilter> = emptyList()
)

data class BackupValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val backupData: FlowlyBackupData? = null,
    val timestamp: Long = 0L,
    val createdAtFormatted: String = "",
    val transactionCount: Int = 0,
    val accountCount: Int = 0,
    val categoryCount: Int = 0,
    val budgetCount: Int = 0,
    val goalCount: Int = 0,
    val recurringCount: Int = 0,
    val creditCardCount: Int = 0,
    val loanCount: Int = 0
)

enum class RestoreMode {
    REPLACE
}
