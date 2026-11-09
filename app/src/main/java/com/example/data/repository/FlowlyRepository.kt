package com.example.data.repository

import com.example.domain.model.Account
import com.example.domain.model.Budget
import com.example.domain.model.BudgetDetail
import com.example.domain.model.CalendarDaySummary
import com.example.domain.model.Category
import com.example.domain.model.CategoryType
import com.example.domain.model.Goal
import com.example.domain.model.GoalContribution
import com.example.domain.model.GoalDetail
import com.example.domain.model.RecurringDetail
import com.example.domain.model.RecurringTransaction
import com.example.domain.model.SpendingComparison
import com.example.domain.model.SpendingInsight
import com.example.domain.model.SubscriptionSummary
import com.example.domain.model.TimeFilter
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionDetail
import com.example.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface FlowlyRepository {

    // Categories
    fun getAllCategories(): Flow<List<Category>>
    fun getCategoriesByType(type: CategoryType): Flow<List<Category>>
    fun getParentCategories(type: CategoryType): Flow<List<Category>>
    fun getSubcategories(parentId: Long): Flow<List<Category>>
    fun searchCategories(query: String): Flow<List<Category>>
    suspend fun getCategoryById(id: Long): Category?
    suspend fun addCategory(category: Category): Long
    suspend fun updateCategory(category: Category)
    suspend fun toggleCategoryFavorite(id: Long, isFavorite: Boolean)
    suspend fun archiveCategory(id: Long, isArchived: Boolean)
    suspend fun deleteCategory(category: Category)

    // Accounts
    fun getAllAccounts(): Flow<List<Account>>
    suspend fun getAccountById(id: Long): Account?
    fun getAccountByIdFlow(id: Long): Flow<Account?>
    suspend fun addAccount(account: Account): Long
    suspend fun updateAccount(account: Account)
    suspend fun deleteAccount(account: Account)

    // Transactions
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getRecentTransactions(limit: Int = 10): Flow<List<TransactionDetail>>
    fun getTransactionDetailsFlow(): Flow<List<TransactionDetail>>
    fun getTransactionsBetween(startMillis: Long, endMillis: Long): Flow<List<Transaction>>
    suspend fun getTransactionById(id: Long): Transaction?
    suspend fun addTransaction(transaction: Transaction): Long
    suspend fun updateTransaction(newTransaction: Transaction, oldTransaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)

    // Recurring & Subscriptions
    fun getAllRecurring(): Flow<List<RecurringDetail>>
    fun getActiveRecurring(): Flow<List<RecurringDetail>>
    fun getUpcomingRecurring(withinDays: Int = 7): Flow<List<RecurringDetail>>
    fun getSubscriptionSummary(): Flow<SubscriptionSummary>
    suspend fun addRecurring(recurring: RecurringTransaction): Long
    suspend fun updateRecurring(recurring: RecurringTransaction)
    suspend fun deleteRecurring(recurring: RecurringTransaction)
    suspend fun toggleRecurringActive(id: Long, active: Boolean)
    suspend fun processDueRecurring()

    // Settings
    fun getUserSettings(): Flow<UserSettings>
    suspend fun getUserSettingsDirect(): UserSettings
    suspend fun updateUserSettings(settings: UserSettings)
    suspend fun checkAndSeedInitialData()

    // Insights
    fun getSpendingInsights(timeFilter: TimeFilter): Flow<List<SpendingInsight>>
    fun getHighPriorityHomeInsight(): Flow<SpendingInsight?>
    fun getSpendingComparison(): Flow<SpendingComparison>

    // Advanced Budgeting
    fun getAllBudgets(): Flow<List<Budget>>
    fun getBudgetDetails(): Flow<List<BudgetDetail>>
    suspend fun getBudgetById(id: Long): Budget?
    suspend fun addBudget(budget: Budget): Long
    suspend fun updateBudget(budget: Budget)
    suspend fun deleteBudget(budget: Budget)

    // Financial Goals
    fun getAllGoalsFlow(): Flow<List<GoalDetail>>
    suspend fun getGoalById(id: Long): Goal?
    suspend fun addGoal(goal: Goal): Long
    suspend fun updateGoal(goal: Goal)
    suspend fun deleteGoal(goal: Goal)
    suspend fun addGoalContribution(goalId: Long, amount: Double, note: String, withdrawFromAccountId: Long? = null)
    suspend fun deleteGoalContribution(contribution: GoalContribution)

    // Financial Calendar
    fun getCalendarDayData(year: Int, month: Int): Flow<Map<Int, CalendarDaySummary>>

    // Net Worth Tracking
    fun getNetWorthSummary(): Flow<com.example.domain.model.NetWorthSummary>
    fun getNetWorthHistory(timeRange: com.example.domain.model.NetWorthTimeRange): Flow<List<com.example.domain.model.NetWorthPoint>>
    suspend fun saveNetWorthSnapshot()

    // Cash Flow Forecast
    fun getCashFlowForecast(period: com.example.domain.model.ForecastPeriod): Flow<com.example.domain.model.CashFlowForecast>

    // Credit Cards
    fun getAllCreditCards(): Flow<List<com.example.domain.model.CreditCard>>
    fun getActiveCreditCards(): Flow<List<com.example.domain.model.CreditCard>>
    suspend fun getCreditCardById(id: Long): com.example.domain.model.CreditCard?
    fun getCreditCardByIdFlow(id: Long): Flow<com.example.domain.model.CreditCard?>
    suspend fun addCreditCard(card: com.example.domain.model.CreditCard): Long
    suspend fun updateCreditCard(card: com.example.domain.model.CreditCard)
    suspend fun deleteCreditCard(card: com.example.domain.model.CreditCard)
    suspend fun recordCreditCardPayment(cardId: Long, amount: Double, sourceAccountId: Long, note: String = "Credit Card Repayment")

    // Loan Tracking
    fun getAllLoans(): Flow<List<com.example.domain.model.Loan>>
    fun getActiveLoans(): Flow<List<com.example.domain.model.Loan>>
    suspend fun getLoanById(id: Long): com.example.domain.model.Loan?
    fun getLoanByIdFlow(id: Long): Flow<com.example.domain.model.Loan?>
    fun getLoanDetail(loanId: Long): Flow<com.example.domain.model.LoanDetail?>
    suspend fun addLoan(loan: com.example.domain.model.Loan): Long
    suspend fun updateLoan(loan: com.example.domain.model.Loan)
    suspend fun deleteLoan(loan: com.example.domain.model.Loan)
    suspend fun recordLoanPayment(payment: com.example.domain.model.LoanPayment, sourceAccountId: Long? = null)
    suspend fun deleteLoanPayment(payment: com.example.domain.model.LoanPayment)

    // Saved Filters
    fun getAllSavedFilters(): Flow<List<com.example.domain.model.SavedTransactionFilter>>
    suspend fun addSavedFilter(filter: com.example.domain.model.SavedTransactionFilter): Long
    suspend fun updateSavedFilter(filter: com.example.domain.model.SavedTransactionFilter)
    suspend fun deleteSavedFilter(filter: com.example.domain.model.SavedTransactionFilter)

    // Global Search & Recent Searches
    fun searchAll(query: String): Flow<com.example.domain.model.GlobalSearchResults>
    fun getRecentSearches(): Flow<List<com.example.data.local.entity.RecentSearchEntity>>
    suspend fun addRecentSearch(query: String)
    suspend fun removeRecentSearch(query: String)
    suspend fun clearRecentSearches()

    // Backup & Restore
    suspend fun createBackupJson(): String
    suspend fun restoreBackup(jsonString: String): Result<Unit>
    fun validateBackup(jsonString: String): com.example.domain.model.BackupValidationResult

    // Data Export
    fun getFilteredTransactionsForExport(filterState: com.example.domain.model.ExportFilterState): Flow<List<TransactionDetail>>
    fun getExportPreviewSummary(filterState: com.example.domain.model.ExportFilterState): Flow<com.example.domain.model.ExportPreviewSummary>

    // Privacy & Security Settings
    suspend fun updatePrivacyAndLockSettings(
        hideAmounts: Boolean,
        hideAmountsPersist: Boolean,
        appLockEnabled: Boolean,
        appLockTimeoutMinutes: Int
    )

    // Quick Add & Smart Suggestions
    fun getQuickAddSuggestions(): Flow<com.example.domain.model.QuickAddSuggestions>

    // Merchant Intelligence
    fun getTopMerchants(limit: Int = 10): Flow<List<com.example.domain.model.MerchantSummary>>
    fun getMerchantDetail(merchantName: String): Flow<com.example.domain.model.MerchantSummary?>
    fun getSuggestedMerchants(query: String): Flow<List<String>>

    // Home Customization
    suspend fun updateHomeCustomization(sectionsOrder: String, hiddenSections: String)

    // Expense Books
    fun getAllExpenseBooks(): Flow<List<com.example.domain.model.ExpenseBook>>
    fun getActiveExpenseBooks(): Flow<List<com.example.domain.model.ExpenseBook>>
    fun getExpenseBookSummaries(): Flow<List<com.example.domain.model.ExpenseBookSummary>>
    suspend fun getExpenseBookById(id: Long): com.example.domain.model.ExpenseBook?
    fun getExpenseBookByIdFlow(id: Long): Flow<com.example.domain.model.ExpenseBook?>
    suspend fun addExpenseBook(book: com.example.domain.model.ExpenseBook): Long
    suspend fun updateExpenseBook(book: com.example.domain.model.ExpenseBook)
    suspend fun deleteExpenseBook(book: com.example.domain.model.ExpenseBook)
    suspend fun setExpenseBookArchived(id: Long, isArchived: Boolean)
    suspend fun setDefaultExpenseBook(id: Long)
    fun getTransactionsForBook(bookId: Long): Flow<List<TransactionDetail>>
}

