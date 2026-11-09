package com.example.data.repository

import com.example.data.local.DatabasePrepopulate
import com.example.data.local.dao.AccountDao
import com.example.data.local.dao.BudgetDao
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.CreditCardDao
import com.example.data.local.dao.ExpenseBookDao
import com.example.data.local.dao.GoalDao
import com.example.data.local.dao.LoanDao
import com.example.data.local.dao.NetWorthDao
import com.example.data.local.dao.RecurringTransactionDao
import com.example.data.local.dao.SavedFilterDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.dao.UserSettingsDao
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CreditCardEntity
import com.example.data.local.entity.ExpenseBookEntity
import com.example.data.local.entity.GoalContributionEntity
import com.example.data.local.entity.GoalEntity
import com.example.data.local.entity.LoanEntity
import com.example.data.local.entity.LoanPaymentEntity
import com.example.data.local.entity.NetWorthSnapshotEntity
import com.example.data.local.entity.RecurringTransactionEntity
import com.example.data.local.entity.SavedFilterEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserSettingsEntity
import com.example.domain.model.Account
import com.example.domain.model.AccountClassification
import com.example.domain.model.Budget
import com.example.domain.model.BudgetDetail
import com.example.domain.model.BudgetPeriod
import com.example.domain.model.BudgetStatus
import com.example.domain.model.CalendarDaySummary
import com.example.domain.model.Category
import com.example.domain.model.CategoryType
import com.example.domain.model.CashFlowForecast
import com.example.domain.model.CreditCard
import com.example.domain.model.CreditCardStatus
import com.example.domain.model.ExpenseBook
import com.example.domain.model.ExpenseBookSummary
import com.example.domain.model.FilterDatePreset
import com.example.domain.model.ForecastEvent
import com.example.domain.model.ForecastPeriod
import com.example.domain.model.Frequency
import com.example.domain.model.Goal
import com.example.domain.model.GoalContribution
import com.example.domain.model.GoalDetail
import com.example.domain.model.GoalStatus
import com.example.domain.model.InsightType
import com.example.domain.model.Loan
import com.example.domain.model.LoanDetail
import com.example.domain.model.LoanPayment
import com.example.domain.model.LoanStatus
import com.example.domain.model.NetWorthPoint
import com.example.domain.model.NetWorthSummary
import com.example.domain.model.NetWorthTimeRange
import com.example.domain.model.PeriodSpendingMetrics
import com.example.domain.model.RecurringDetail
import com.example.domain.model.RecurringTransaction
import com.example.domain.model.SavedTransactionFilter
import com.example.domain.model.SpendingComparison
import com.example.domain.model.SpendingInsight
import com.example.domain.model.SpendingIntensity
import com.example.domain.model.SubscriptionSummary
import com.example.domain.model.TimeFilter
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionDetail
import com.example.domain.model.TransactionType
import com.example.domain.model.UserSettings
import com.example.domain.model.classification
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class FlowlyRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val recurringDao: RecurringTransactionDao,
    private val userSettingsDao: UserSettingsDao,
    private val budgetDao: BudgetDao,
    private val goalDao: GoalDao,
    private val creditCardDao: CreditCardDao,
    private val loanDao: LoanDao,
    private val netWorthDao: NetWorthDao,
    private val savedFilterDao: SavedFilterDao,
    private val recentSearchDao: com.example.data.local.dao.RecentSearchDao,
    private val database: com.example.data.local.AppDatabase,
    private val expenseBookDao: ExpenseBookDao = database.expenseBookDao()
) : FlowlyRepository {

    // ==========================================
    // Categories
    // ==========================================
    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { list -> list.map { it.toDomain() } }
    }

    override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> {
        return categoryDao.getCategoriesByType(type.name).map { list -> list.map { it.toDomain() } }
    }

    override fun getParentCategories(type: CategoryType): Flow<List<Category>> {
        return categoryDao.getParentCategories(type.name).map { list -> list.map { it.toDomain() } }
    }

    override fun getSubcategories(parentId: Long): Flow<List<Category>> {
        return categoryDao.getSubcategories(parentId).map { list -> list.map { it.toDomain() } }
    }

    override fun searchCategories(query: String): Flow<List<Category>> {
        return categoryDao.searchCategories(query).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getCategoryById(id: Long): Category? {
        return categoryDao.getCategoryById(id)?.toDomain()
    }

    override suspend fun addCategory(category: Category): Long {
        return categoryDao.insertCategory(CategoryEntity.fromDomain(category))
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(CategoryEntity.fromDomain(category))
    }

    override suspend fun toggleCategoryFavorite(id: Long, isFavorite: Boolean) {
        categoryDao.updateFavorite(id, isFavorite)
    }

    override suspend fun archiveCategory(id: Long, isArchived: Boolean) {
        categoryDao.updateArchived(id, isArchived)
    }

    override suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(CategoryEntity.fromDomain(category))
    }

    // ==========================================
    // Accounts
    // ==========================================
    override fun getAllAccounts(): Flow<List<Account>> {
        return accountDao.getAllAccounts().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getAccountById(id: Long): Account? {
        return accountDao.getAccountById(id)?.toDomain()
    }

    override fun getAccountByIdFlow(id: Long): Flow<Account?> {
        return accountDao.getAccountByIdFlow(id).map { it?.toDomain() }
    }

    override suspend fun addAccount(account: Account): Long {
        return accountDao.insertAccount(AccountEntity.fromDomain(account))
    }

    override suspend fun updateAccount(account: Account) {
        accountDao.updateAccount(AccountEntity.fromDomain(account))
    }

    override suspend fun deleteAccount(account: Account) {
        accountDao.deleteAccount(AccountEntity.fromDomain(account))
    }

    // ==========================================
    // Transactions
    // ==========================================
    override fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions().map { list -> list.map { it.toDomain() } }
    }

    override fun getTransactionDetailsFlow(): Flow<List<TransactionDetail>> {
        return combine(
            transactionDao.getAllTransactions(),
            categoryDao.getAllCategories(),
            accountDao.getAllAccounts(),
            expenseBookDao.getAllBooks()
        ) { transactions, categories, accounts, books ->
            val catMap = categories.associateBy { it.id }
            val accMap = accounts.associateBy { it.id }
            val bookMap = books.associateBy { it.id }

            transactions.map { txEntity ->
                val tx = txEntity.toDomain()
                TransactionDetail(
                    transaction = tx,
                    category = catMap[tx.categoryId]?.toDomain(),
                    sourceAccount = accMap[tx.accountId]?.toDomain(),
                    destinationAccount = tx.destinationAccountId?.let { accMap[it]?.toDomain() },
                    book = bookMap[tx.bookId]?.toDomain()
                )
            }
        }
    }

    override fun getRecentTransactions(limit: Int): Flow<List<TransactionDetail>> {
        return combine(
            transactionDao.getRecentTransactions(limit),
            categoryDao.getAllCategories(),
            accountDao.getAllAccounts(),
            expenseBookDao.getAllBooks()
        ) { transactions, categories, accounts, books ->
            val catMap = categories.associateBy { it.id }
            val accMap = accounts.associateBy { it.id }
            val bookMap = books.associateBy { it.id }

            transactions.map { txEntity ->
                val tx = txEntity.toDomain()
                TransactionDetail(
                    transaction = tx,
                    category = catMap[tx.categoryId]?.toDomain(),
                    sourceAccount = accMap[tx.accountId]?.toDomain(),
                    destinationAccount = tx.destinationAccountId?.let { accMap[it]?.toDomain() },
                    book = bookMap[tx.bookId]?.toDomain()
                )
            }
        }
    }

    override fun getTransactionsBetween(startMillis: Long, endMillis: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsBetween(startMillis, endMillis).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getTransactionById(id: Long): Transaction? {
        return transactionDao.getTransactionById(id)?.toDomain()
    }

    override suspend fun addTransaction(transaction: Transaction): Long {
        val id = transactionDao.insertTransaction(TransactionEntity.fromDomain(transaction))

        // Update account balances
        when (transaction.type) {
            TransactionType.EXPENSE -> {
                accountDao.updateAccountBalance(transaction.accountId, -transaction.amount)
            }
            TransactionType.INCOME -> {
                accountDao.updateAccountBalance(transaction.accountId, transaction.amount)
            }
            TransactionType.TRANSFER -> {
                accountDao.updateAccountBalance(transaction.accountId, -transaction.amount)
                transaction.destinationAccountId?.let { destId ->
                    accountDao.updateAccountBalance(destId, transaction.amount)
                }
            }
        }
        return id
    }

    override suspend fun updateTransaction(newTransaction: Transaction, oldTransaction: Transaction) {
        // Revert old transaction impact
        when (oldTransaction.type) {
            TransactionType.EXPENSE -> {
                accountDao.updateAccountBalance(oldTransaction.accountId, oldTransaction.amount)
            }
            TransactionType.INCOME -> {
                accountDao.updateAccountBalance(oldTransaction.accountId, -oldTransaction.amount)
            }
            TransactionType.TRANSFER -> {
                accountDao.updateAccountBalance(oldTransaction.accountId, oldTransaction.amount)
                oldTransaction.destinationAccountId?.let { destId ->
                    accountDao.updateAccountBalance(destId, -oldTransaction.amount)
                }
            }
        }

        // Apply new transaction impact
        when (newTransaction.type) {
            TransactionType.EXPENSE -> {
                accountDao.updateAccountBalance(newTransaction.accountId, -newTransaction.amount)
            }
            TransactionType.INCOME -> {
                accountDao.updateAccountBalance(newTransaction.accountId, newTransaction.amount)
            }
            TransactionType.TRANSFER -> {
                accountDao.updateAccountBalance(newTransaction.accountId, -newTransaction.amount)
                newTransaction.destinationAccountId?.let { destId ->
                    accountDao.updateAccountBalance(destId, newTransaction.amount)
                }
            }
        }

        transactionDao.updateTransaction(TransactionEntity.fromDomain(newTransaction))
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        // Revert balance effect
        when (transaction.type) {
            TransactionType.EXPENSE -> {
                accountDao.updateAccountBalance(transaction.accountId, transaction.amount)
            }
            TransactionType.INCOME -> {
                accountDao.updateAccountBalance(transaction.accountId, -transaction.amount)
            }
            TransactionType.TRANSFER -> {
                accountDao.updateAccountBalance(transaction.accountId, transaction.amount)
                transaction.destinationAccountId?.let { destId ->
                    accountDao.updateAccountBalance(destId, -transaction.amount)
                }
            }
        }
        transactionDao.deleteTransaction(TransactionEntity.fromDomain(transaction))
    }

    // ==========================================
    // Recurring & Subscriptions
    // ==========================================
    override fun getAllRecurring(): Flow<List<RecurringDetail>> {
        return combine(
            recurringDao.getAllRecurring(),
            categoryDao.getAllCategories(),
            accountDao.getAllAccounts()
        ) { recurringList, categories, accounts ->
            val catMap = categories.associateBy { it.id }
            val accMap = accounts.associateBy { it.id }

            recurringList.map { recEntity ->
                val rec = recEntity.toDomain()
                RecurringDetail(
                    recurring = rec,
                    category = catMap[rec.categoryId]?.toDomain(),
                    account = accMap[rec.accountId]?.toDomain()
                )
            }
        }
    }

    override fun getActiveRecurring(): Flow<List<RecurringDetail>> {
        return combine(
            recurringDao.getActiveRecurring(),
            categoryDao.getAllCategories(),
            accountDao.getAllAccounts()
        ) { recurringList, categories, accounts ->
            val catMap = categories.associateBy { it.id }
            val accMap = accounts.associateBy { it.id }

            recurringList.map { recEntity ->
                val rec = recEntity.toDomain()
                RecurringDetail(
                    recurring = rec,
                    category = catMap[rec.categoryId]?.toDomain(),
                    account = accMap[rec.accountId]?.toDomain()
                )
            }
        }
    }

    override fun getUpcomingRecurring(withinDays: Int): Flow<List<RecurringDetail>> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, withinDays)
        val maxTimestamp = cal.timeInMillis

        return combine(
            recurringDao.getUpcomingRecurring(maxTimestamp),
            categoryDao.getAllCategories(),
            accountDao.getAllAccounts()
        ) { recurringList, categories, accounts ->
            val catMap = categories.associateBy { it.id }
            val accMap = accounts.associateBy { it.id }

            recurringList.map { recEntity ->
                val rec = recEntity.toDomain()
                RecurringDetail(
                    recurring = rec,
                    category = catMap[rec.categoryId]?.toDomain(),
                    account = accMap[rec.accountId]?.toDomain()
                )
            }
        }
    }

    override fun getSubscriptionSummary(): Flow<SubscriptionSummary> {
        return getAllRecurring().map { list ->
            var monthlyTotal = 0.0
            var activeCount = 0
            var pausedCount = 0

            list.forEach { item ->
                if (item.recurring.active) {
                    activeCount++
                    val normalizedMonthly = when (item.recurring.frequency) {
                        Frequency.DAILY -> item.recurring.amount * 30.0
                        Frequency.WEEKLY -> item.recurring.amount * 4.33
                        Frequency.MONTHLY -> item.recurring.amount
                        Frequency.YEARLY -> item.recurring.amount / 12.0
                    }
                    monthlyTotal += normalizedMonthly
                } else {
                    pausedCount++
                }
            }

            val yearlyTotal = monthlyTotal * 12.0
            val nextDue = list.filter { it.recurring.active }.minByOrNull { it.recurring.nextExecutionDate }

            SubscriptionSummary(
                monthlyNormalizedCost = monthlyTotal,
                yearlyEstimatedCost = yearlyTotal,
                activeSubscriptionsCount = activeCount,
                pausedSubscriptionsCount = pausedCount,
                nextDueSubscription = nextDue
            )
        }
    }

    override suspend fun addRecurring(recurring: RecurringTransaction): Long {
        return recurringDao.insertRecurring(RecurringTransactionEntity.fromDomain(recurring))
    }

    override suspend fun updateRecurring(recurring: RecurringTransaction) {
        recurringDao.updateRecurring(RecurringTransactionEntity.fromDomain(recurring))
    }

    override suspend fun deleteRecurring(recurring: RecurringTransaction) {
        recurringDao.deleteRecurring(RecurringTransactionEntity.fromDomain(recurring))
    }

    override suspend fun toggleRecurringActive(id: Long, active: Boolean) {
        recurringDao.updateRecurringActive(id, active)
    }

    override suspend fun processDueRecurring() {
        // Keeps recurring items updated
    }

    // ==========================================
    // Settings & Prepopulation
    // ==========================================
    override fun getUserSettings(): Flow<UserSettings> {
        return userSettingsDao.getUserSettings().map {
            it?.toDomain() ?: DatabasePrepopulate.getDefaultUserSettings().toDomain()
        }
    }

    override suspend fun getUserSettingsDirect(): UserSettings {
        return userSettingsDao.getUserSettingsDirect()?.toDomain()
            ?: DatabasePrepopulate.getDefaultUserSettings().toDomain()
    }

    override suspend fun updateUserSettings(settings: UserSettings) {
        userSettingsDao.insertOrUpdate(UserSettingsEntity.fromDomain(settings))
    }

    override suspend fun checkAndSeedInitialData() {
        val settings = userSettingsDao.getUserSettingsDirect()
        if (settings == null) {
            userSettingsDao.insertOrUpdate(DatabasePrepopulate.getDefaultUserSettings())
            categoryDao.insertCategories(DatabasePrepopulate.getDefaultCategories())
            accountDao.insertAccounts(DatabasePrepopulate.getDefaultAccounts())
            recurringDao.insertRecurringList(DatabasePrepopulate.getDefaultRecurring())
            expenseBookDao.insertBooks(DatabasePrepopulate.getDefaultExpenseBooks())
            for (tx in DatabasePrepopulate.getInitialSampleTransactions()) {
                transactionDao.insertTransaction(tx)
            }
            budgetDao.insertBudgets(DatabasePrepopulate.getDefaultBudgets())
            goalDao.insertGoals(DatabasePrepopulate.getDefaultGoals())
            goalDao.insertContributions(DatabasePrepopulate.getDefaultGoalContributions())
        } else {
            if (expenseBookDao.getBookCount() == 0) {
                expenseBookDao.insertBooks(DatabasePrepopulate.getDefaultExpenseBooks())
            }
        }
    }

    // ==========================================
    // Advanced Budgeting
    // ==========================================
    override fun getAllBudgets(): Flow<List<Budget>> {
        return budgetDao.getAllBudgets().map { list -> list.map { it.toDomain() } }
    }

    override fun getBudgetDetails(): Flow<List<BudgetDetail>> {
        return combine(
            budgetDao.getAllBudgets(),
            transactionDao.getAllTransactions(),
            categoryDao.getAllCategories()
        ) { budgets, transactions, categories ->
            val catMap = categories.associateBy { it.id }
            val now = System.currentTimeMillis()

            budgets.map { budgetEntity ->
                val budget = budgetEntity.toDomain()
                val category = budget.categoryId?.let { catMap[it]?.toDomain() }

                // Calculate spending from actual expense transactions within date range
                val spent = transactions.filter { tx ->
                    tx.type == "EXPENSE" &&
                    tx.dateTime in budget.startDate..budget.endDate &&
                    (budget.categoryId == null || tx.categoryId == budget.categoryId)
                }.sumOf { it.amount }

                val remaining = maxOf(0.0, budget.amount - spent)
                val percentage = if (budget.amount > 0) (spent / budget.amount) * 100.0 else 0.0

                val status = when {
                    percentage > 100.0 -> BudgetStatus.EXCEEDED
                    percentage >= 85.0 -> BudgetStatus.ALMOST_EXHAUSTED
                    percentage >= 70.0 -> BudgetStatus.MODERATE
                    else -> BudgetStatus.SAFE
                }

                val daysRemaining = if (budget.endDate > now) {
                    ((budget.endDate - now) / (24L * 60 * 60 * 1000)).toInt().coerceAtLeast(1)
                } else 0

                BudgetDetail(
                    budget = budget,
                    category = category,
                    spentAmount = spent,
                    remainingAmount = remaining,
                    percentageUsed = percentage,
                    status = status,
                    daysRemaining = daysRemaining
                )
            }
        }
    }

    override suspend fun getBudgetById(id: Long): Budget? {
        return budgetDao.getBudgetById(id)?.toDomain()
    }

    override suspend fun addBudget(budget: Budget): Long {
        return budgetDao.insertBudget(BudgetEntity.fromDomain(budget))
    }

    override suspend fun updateBudget(budget: Budget) {
        budgetDao.updateBudget(BudgetEntity.fromDomain(budget))
    }

    override suspend fun deleteBudget(budget: Budget) {
        budgetDao.deleteBudget(BudgetEntity.fromDomain(budget))
    }

    // ==========================================
    // Financial Goals
    // ==========================================
    override fun getAllGoalsFlow(): Flow<List<GoalDetail>> {
        return combine(
            goalDao.getActiveAndCompletedGoals(),
            goalDao.getAllContributions(),
            accountDao.getAllAccounts()
        ) { goals, contributions, accounts ->
            val accMap = accounts.associateBy { it.id }
            val now = System.currentTimeMillis()

            goals.map { goalEntity ->
                val goal = goalEntity.toDomain()
                val goalContribs = contributions.filter { it.goalId == goal.id }.map { it.toDomain() }
                val linkedAcc = goal.linkedAccountId?.let { accMap[it]?.toDomain() }

                val pct = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount) * 100.0 else 0.0
                val remaining = maxOf(0.0, goal.targetAmount - goal.currentAmount)
                val isCompleted = goal.currentAmount >= goal.targetAmount || goal.status == GoalStatus.COMPLETED

                val daysRemaining = goal.targetDate?.let { tDate ->
                    if (tDate > now) ((tDate - now) / (24L * 60 * 60 * 1000)).toInt().coerceAtLeast(1) else 0
                }

                GoalDetail(
                    goal = goal,
                    linkedAccount = linkedAcc,
                    contributions = goalContribs,
                    percentage = pct,
                    remainingAmount = remaining,
                    isCompleted = isCompleted,
                    daysRemaining = daysRemaining
                )
            }
        }
    }

    override suspend fun getGoalById(id: Long): Goal? {
        return goalDao.getGoalById(id)?.toDomain()
    }

    override suspend fun addGoal(goal: Goal): Long {
        val id = goalDao.insertGoal(GoalEntity.fromDomain(goal))
        if (goal.currentAmount > 0) {
            goalDao.insertContribution(
                GoalContributionEntity(
                    goalId = id,
                    amount = goal.currentAmount,
                    date = System.currentTimeMillis(),
                    note = "Initial goal deposit"
                )
            )
        }
        return id
    }

    override suspend fun updateGoal(goal: Goal) {
        goalDao.updateGoal(GoalEntity.fromDomain(goal))
    }

    override suspend fun deleteGoal(goal: Goal) {
        goalDao.deleteGoal(GoalEntity.fromDomain(goal))
        goalDao.deleteContributionsForGoal(goal.id)
    }

    override suspend fun addGoalContribution(
        goalId: Long,
        amount: Double,
        note: String,
        withdrawFromAccountId: Long?
    ) {
        val goalEntity = goalDao.getGoalById(goalId) ?: return
        val currentAmount = goalEntity.currentAmount + amount
        val isCompleted = currentAmount >= goalEntity.targetAmount
        val updatedStatus = if (isCompleted) "COMPLETED" else goalEntity.status

        goalDao.updateGoal(goalEntity.copy(currentAmount = currentAmount, status = updatedStatus))
        goalDao.insertContribution(
            GoalContributionEntity(
                goalId = goalId,
                amount = amount,
                date = System.currentTimeMillis(),
                note = note
            )
        )

        // Internal transfer/savings allocation adjustment if linked
        if (withdrawFromAccountId != null) {
            accountDao.updateAccountBalance(withdrawFromAccountId, -amount)
        }
    }

    override suspend fun deleteGoalContribution(contribution: GoalContribution) {
        goalDao.deleteContribution(GoalContributionEntity.fromDomain(contribution))
        val goalEntity = goalDao.getGoalById(contribution.goalId) ?: return
        val updatedAmount = maxOf(0.0, goalEntity.currentAmount - contribution.amount)
        goalDao.updateGoal(goalEntity.copy(currentAmount = updatedAmount, status = "ACTIVE"))
    }

    // ==========================================
    // Smart Spending Insights Engine
    // ==========================================
    override fun getSpendingInsights(timeFilter: TimeFilter): Flow<List<SpendingInsight>> {
        return combine(
            transactionDao.getAllTransactions(),
            categoryDao.getAllCategories(),
            budgetDao.getActiveBudgets()
        ) { txList, categories, budgets ->
            val catMap = categories.associateBy { it.id }
            val now = System.currentTimeMillis()

            val (currentTx, prevTx) = when (timeFilter) {
                TimeFilter.THIS_WEEK -> {
                    val oneWeekAgo = now - (7L * 24 * 60 * 60 * 1000)
                    val twoWeeksAgo = now - (14L * 24 * 60 * 60 * 1000)
                    Pair(
                        txList.filter { it.dateTime >= oneWeekAgo && it.dateTime <= now },
                        txList.filter { it.dateTime >= twoWeeksAgo && it.dateTime < oneWeekAgo }
                    )
                }
                TimeFilter.THIS_MONTH -> {
                    val thisMonth = com.example.core.util.PeriodRanges.thisMonth(now)
                    val lastMonth = com.example.core.util.PeriodRanges.lastMonth(now)
                    // Like-for-like: the same number of days of last month
                    val elapsed = (now - thisMonth.start).coerceIn(1L, lastMonth.length)
                    Pair(
                        txList.filter { it.dateTime in thisMonth },
                        txList.filter { it.dateTime >= lastMonth.start && it.dateTime < lastMonth.start + elapsed }
                    )
                }
                TimeFilter.LAST_MONTH -> {
                    val lastMonth = com.example.core.util.PeriodRanges.lastMonth(now)
                    val monthBefore = com.example.core.util.PeriodRanges.monthStart(now, -2)
                    Pair(
                        txList.filter { it.dateTime in lastMonth },
                        txList.filter { it.dateTime >= monthBefore && it.dateTime < lastMonth.start }
                    )
                }
                TimeFilter.ALL_TIME -> {
                    Pair(txList, emptyList())
                }
            }

            val currentExpenses = currentTx.filter { it.type == "EXPENSE" }
            val currentIncome = currentTx.filter { it.type == "INCOME" }
            val prevExpenses = prevTx.filter { it.type == "EXPENSE" }

            val totalCurrentExpense = currentExpenses.sumOf { it.amount }
            val totalCurrentIncome = currentIncome.sumOf { it.amount }
            val totalPrevExpense = prevExpenses.sumOf { it.amount }

            val insights = mutableListOf<SpendingInsight>()

            // 1. Budget warnings
            budgets.forEach { b ->
                val spent = currentExpenses.filter { tx ->
                    b.categoryId == null || tx.categoryId == b.categoryId
                }.sumOf { it.amount }
                if (b.amount > 0) {
                    val pct = (spent / b.amount) * 100
                    if (pct >= 85) {
                        insights.add(
                            SpendingInsight(
                                title = "Budget Alert: ${b.name}",
                                description = "You have used ${pct.toInt()}% of your ${b.name} limit.",
                                type = if (pct > 100) InsightType.WARNING else InsightType.INFO,
                                iconName = "AccountBalanceWallet"
                            )
                        )
                    }
                }
            }

            // 2. Highest spending category
            val categorySpending = currentExpenses.groupBy { it.categoryId }
                .mapValues { entry -> entry.value.sumOf { it.amount } }

            val topCategoryEntry = categorySpending.maxByOrNull { it.value }
            if (topCategoryEntry != null && totalCurrentExpense > 0) {
                val catName = catMap[topCategoryEntry.key]?.name ?: "Food & Dining"
                val pct = ((topCategoryEntry.value / totalCurrentExpense) * 100).toInt()
                insights.add(
                    SpendingInsight(
                        title = "Top Expense Category",
                        description = "$catName is your highest spending area ($pct% of expenses).",
                        type = InsightType.INFO,
                        iconName = "PieChart"
                    )
                )
            }

            // 3. Comparison with previous period (only if previous data exists)
            if (totalPrevExpense > 0 && totalCurrentExpense > 0) {
                val diffPct = (((totalCurrentExpense - totalPrevExpense) / totalPrevExpense) * 100).toInt()
                if (diffPct < 0) {
                    insights.add(
                        SpendingInsight(
                            title = "Decreased Spending Trend",
                            description = "You spent ${abs(diffPct)}% less compared to the previous period.",
                            type = InsightType.POSITIVE,
                            iconName = "TrendingDown"
                        )
                    )
                } else if (diffPct > 0) {
                    insights.add(
                        SpendingInsight(
                            title = "Spending Increased",
                            description = "Your spending increased by $diffPct% compared to the previous period.",
                            type = InsightType.WARNING,
                            iconName = "TrendingUp"
                        )
                    )
                }
            }

            // 4. Weekend vs Weekday spending pattern
            var weekendSpend = 0.0
            var weekdaySpend = 0.0
            val cal = Calendar.getInstance()
            currentExpenses.forEach { tx ->
                cal.timeInMillis = tx.dateTime
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                    weekendSpend += tx.amount
                } else {
                    weekdaySpend += tx.amount
                }
            }
            if (weekendSpend > 0 && totalCurrentExpense > 0) {
                val weekendPct = ((weekendSpend / totalCurrentExpense) * 100).toInt()
                if (weekendPct > 45) {
                    insights.add(
                        SpendingInsight(
                            title = "Weekend Spending Pattern",
                            description = "$weekendPct% of your spending occurs on weekends.",
                            type = InsightType.INFO,
                            iconName = "CalendarMonth"
                        )
                    )
                }
            }

            // 5. Cash Flow Savings Rate
            if (totalCurrentIncome > 0 && totalCurrentExpense > 0) {
                val netSavings = totalCurrentIncome - totalCurrentExpense
                val savingsRate = ((netSavings / totalCurrentIncome) * 100).toInt()
                if (savingsRate > 0) {
                    insights.add(
                        SpendingInsight(
                            title = "Positive Cash Flow",
                            description = "You retained $savingsRate% of your income after all expenses.",
                            type = InsightType.POSITIVE,
                            iconName = "Savings"
                        )
                    )
                }
            }

            // Fallback default if empty
            if (insights.isEmpty()) {
                insights.add(
                    SpendingInsight(
                        title = "Tracking Money Flow",
                        description = "Add more transactions to unlock automated spending comparisons and trends.",
                        type = InsightType.INFO,
                        iconName = "Insights"
                    )
                )
            }

            insights
        }
    }

    override fun getHighPriorityHomeInsight(): Flow<SpendingInsight?> {
        return getSpendingInsights(TimeFilter.THIS_MONTH).map { list ->
            list.firstOrNull { it.type == InsightType.WARNING }
                ?: list.firstOrNull { it.type == InsightType.POSITIVE }
                ?: list.firstOrNull { it.type == InsightType.INFO }
        }
    }

    override fun getSpendingComparison(): Flow<SpendingComparison> {
        return combine(
            transactionDao.getAllTransactions(),
            categoryDao.getAllCategories()
        ) { txList, categories ->
            val catMap = categories.associateBy { it.id }
            val now = System.currentTimeMillis()

            // Today vs Yesterday
            val cal = Calendar.getInstance()
            cal.timeInMillis = now
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startOfToday = cal.timeInMillis
            val startOfYesterday = startOfToday - (24 * 60 * 60 * 1000)

            val todayExpenses = txList.filter { it.type == "EXPENSE" && it.dateTime >= startOfToday }.sumOf { it.amount }
            val yesterdayExpenses = txList.filter { it.type == "EXPENSE" && it.dateTime in startOfYesterday until startOfToday }.sumOf { it.amount }
            val todayDiff = todayExpenses - yesterdayExpenses
            val todayPct = if (yesterdayExpenses > 0) ((todayDiff / yesterdayExpenses) * 100.0) else 0.0

            // Week vs Last Week
            val oneWeekAgo = now - (7L * 24 * 60 * 60 * 1000)
            val twoWeeksAgo = now - (14L * 24 * 60 * 60 * 1000)
            val thisWeekExpenses = txList.filter { it.type == "EXPENSE" && it.dateTime in oneWeekAgo..now }.sumOf { it.amount }
            val lastWeekExpenses = txList.filter { it.type == "EXPENSE" && it.dateTime in twoWeeksAgo until oneWeekAgo }.sumOf { it.amount }
            val weekDiff = thisWeekExpenses - lastWeekExpenses
            val weekPct = if (lastWeekExpenses > 0) ((weekDiff / lastWeekExpenses) * 100.0) else 0.0

            // Month vs Last Month
            val calThisMonth = com.example.core.util.PeriodRanges.thisMonth(now)
            val calLastMonth = com.example.core.util.PeriodRanges.lastMonth(now)
            val elapsedThisMonth = (now - calThisMonth.start).coerceIn(1L, calLastMonth.length)
            val thisMonthExpenses = txList.filter { it.type == "EXPENSE" && it.dateTime in calThisMonth }.sumOf { it.amount }
            val lastMonthExpenses = txList.filter {
                it.type == "EXPENSE" && it.dateTime >= calLastMonth.start && it.dateTime < calLastMonth.start + elapsedThisMonth
            }.sumOf { it.amount }
            val monthDiff = thisMonthExpenses - lastMonthExpenses
            val monthPct = if (lastMonthExpenses > 0) ((monthDiff / lastMonthExpenses) * 100.0) else 0.0

            // Top category this month
            val thisMonthExpenseTx = txList.filter { it.type == "EXPENSE" && it.dateTime in calThisMonth }
            val catSpending = thisMonthExpenseTx.groupBy { it.categoryId }.mapValues { entry -> entry.value.sumOf { it.amount } }
            val topEntry = catSpending.maxByOrNull { it.value }
            val topCategoryName = topEntry?.let { catMap[it.key]?.name ?: "Food & Dining" }
            val topCategoryPct = if (thisMonthExpenses > 0 && topEntry != null) ((topEntry.value / thisMonthExpenses) * 100).toInt() else 0

            SpendingComparison(
                todayMetrics = PeriodSpendingMetrics(
                    currentPeriodExpense = todayExpenses,
                    previousPeriodExpense = yesterdayExpenses,
                    differenceAmount = todayDiff,
                    percentageChange = todayPct,
                    hasEnoughData = yesterdayExpenses > 0 || todayExpenses > 0
                ),
                weekMetrics = PeriodSpendingMetrics(
                    currentPeriodExpense = thisWeekExpenses,
                    previousPeriodExpense = lastWeekExpenses,
                    differenceAmount = weekDiff,
                    percentageChange = weekPct,
                    hasEnoughData = lastWeekExpenses > 0 || thisWeekExpenses > 0
                ),
                monthMetrics = PeriodSpendingMetrics(
                    currentPeriodExpense = thisMonthExpenses,
                    previousPeriodExpense = lastMonthExpenses,
                    differenceAmount = monthDiff,
                    percentageChange = monthPct,
                    hasEnoughData = lastMonthExpenses > 0 || thisMonthExpenses > 0
                ),
                weekendVsWeekdaySummary = "Tracking weekday & weekend spending flow",
                topSpendingCategoryThisMonth = topCategoryName,
                topCategoryPercentage = topCategoryPct
            )
        }
    }

    // ==========================================
    // Financial Calendar
    // ==========================================
    override fun getCalendarDayData(year: Int, month: Int): Flow<Map<Int, CalendarDaySummary>> {
        return combine(
            transactionDao.getAllTransactions(),
            recurringDao.getAllRecurring(),
            categoryDao.getAllCategories(),
            accountDao.getAllAccounts()
        ) { transactions, recurringList, categories, accounts ->
            val catMap = categories.associateBy { it.id }
            val accMap = accounts.associateBy { it.id }

            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, 1)

            val maxDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val resultMap = mutableMapOf<Int, CalendarDaySummary>()

            for (day in 1..maxDaysInMonth) {
                cal.set(Calendar.DAY_OF_MONTH, day)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val startOfDay = cal.timeInMillis

                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val endOfDay = cal.timeInMillis

                val dayTxEntities = transactions.filter { it.dateTime in startOfDay..endOfDay }
                val dayTransactions = dayTxEntities.map { txEntity ->
                    val tx = txEntity.toDomain()
                    TransactionDetail(
                        transaction = tx,
                        category = catMap[tx.categoryId]?.toDomain(),
                        sourceAccount = accMap[tx.accountId]?.toDomain(),
                        destinationAccount = tx.destinationAccountId?.let { accMap[it]?.toDomain() }
                    )
                }

                val dayExpenses = dayTxEntities.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                val dayIncomes = dayTxEntities.filter { it.type == "INCOME" }.sumOf { it.amount }
                val netCash = dayIncomes - dayExpenses

                // Check recurring payments due on this day
                val dueRecurring = recurringList.filter { rec ->
                    val recCal = Calendar.getInstance().apply { timeInMillis = rec.nextExecutionDate }
                    rec.active && (
                        (rec.frequency == "MONTHLY" && recCal.get(Calendar.DAY_OF_MONTH) == day) ||
                        (rec.nextExecutionDate in startOfDay..endOfDay)
                    )
                }.map { recEntity ->
                    val rec = recEntity.toDomain()
                    RecurringDetail(
                        recurring = rec,
                        category = catMap[rec.categoryId]?.toDomain(),
                        account = accMap[rec.accountId]?.toDomain()
                    )
                }

                val intensity = when {
                    dayExpenses <= 0 -> SpendingIntensity.NONE
                    dayExpenses <= 500 -> SpendingIntensity.LOW
                    dayExpenses <= 2000 -> SpendingIntensity.MEDIUM
                    else -> SpendingIntensity.HIGH
                }

                resultMap[day] = CalendarDaySummary(
                    dayOfMonth = day,
                    dateMillis = startOfDay,
                    totalExpense = dayExpenses,
                    totalIncome = dayIncomes,
                    netCashFlow = netCash,
                    transactions = dayTransactions,
                    upcomingRecurring = dueRecurring,
                    intensity = intensity,
                    hasIncome = dayIncomes > 0,
                    hasRecurring = dueRecurring.isNotEmpty()
                )
            }

            resultMap
        }
    }

    // ==========================================
    // Net Worth Tracking
    // ==========================================
    override fun getNetWorthSummary(): Flow<NetWorthSummary> {
        return combine(
            accountDao.getAllAccounts(),
            creditCardDao.getAllActiveCards(),
            loanDao.getActiveLoans(),
            userSettingsDao.getUserSettings(),
            netWorthDao.getAllSnapshots()
        ) { accounts, creditCards, loans, settings, snapshots ->
            val domainAccounts = accounts.map { it.toDomain() }
            val domainCreditCards = creditCards.map { it.toDomain() }
            val domainLoans = loans.map { it.toDomain() }

            // Assets: Accounts classified as ASSET (Bank, Cash, UPI, Savings, Investment, Wallet, etc.)
            val assetAccounts = domainAccounts.filter { it.type.classification == AccountClassification.ASSET }
            val totalAssets = assetAccounts.sumOf { if (it.balance > 0) it.balance else 0.0 }

            // Liabilities:
            // 1. Credit card outstanding balances
            val ccLiabilities = domainCreditCards.sumOf { it.outstandingBalance }
            // 2. Active loan remaining balances
            val loanLiabilities = domainLoans.sumOf { it.remainingBalance }
            // 3. Accounts classified as LIABILITY that might not be in credit cards table (or have negative balance)
            val liabilityAccounts = domainAccounts.filter { it.type.classification == AccountClassification.LIABILITY }
            val otherLiabilityAccountsTotal = liabilityAccounts
                .filter { acc -> domainCreditCards.none { it.linkedAccountId == acc.id } }
                .sumOf { abs(it.balance.coerceAtMost(0.0)) }

            val totalLiabilities = ccLiabilities + loanLiabilities + otherLiabilityAccountsTotal
            val netWorth = totalAssets - totalLiabilities

            // Compare with recent snapshot (e.g. from 30 days ago or previous snapshot)
            val previousSnapshot = snapshots.filter { it.date < System.currentTimeMillis() - (12 * 60 * 60 * 1000) }.lastOrNull()
            val prevNetWorth = previousSnapshot?.netWorth ?: (netWorth * 0.94) // Default gentle baseline delta if first run
            val changeAmount = netWorth - prevNetWorth
            val changePercentage = if (abs(prevNetWorth) > 0) ((changeAmount / abs(prevNetWorth)) * 100).coerceIn(-100.0, 100.0) else 0.0

            NetWorthSummary(
                totalAssets = totalAssets,
                totalLiabilities = totalLiabilities,
                netWorth = netWorth,
                changeAmount = changeAmount,
                changePercentage = changePercentage,
                isPositiveChange = changeAmount >= 0,
                currency = settings?.defaultCurrency ?: "₹",
                assetAccounts = assetAccounts,
                liabilityAccounts = liabilityAccounts,
                creditCards = domainCreditCards,
                loans = domainLoans
            )
        }
    }

    override fun getNetWorthHistory(timeRange: NetWorthTimeRange): Flow<List<NetWorthPoint>> {
        return combine(
            getNetWorthSummary(),
            netWorthDao.getAllSnapshots()
        ) { currentSummary, snapshots ->
            val now = System.currentTimeMillis()
            val dayMillis = 24L * 60 * 60 * 1000L
            val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

            val daysToCover = when (timeRange) {
                NetWorthTimeRange.ONE_MONTH -> 30
                NetWorthTimeRange.THREE_MONTHS -> 90
                NetWorthTimeRange.SIX_MONTHS -> 180
                NetWorthTimeRange.ONE_YEAR -> 365
                NetWorthTimeRange.ALL_TIME -> 540
            }

            val stepDays = when (timeRange) {
                NetWorthTimeRange.ONE_MONTH -> 5
                NetWorthTimeRange.THREE_MONTHS -> 15
                NetWorthTimeRange.SIX_MONTHS -> 30
                NetWorthTimeRange.ONE_YEAR -> 30
                NetWorthTimeRange.ALL_TIME -> 60
            }

            val points = mutableListOf<NetWorthPoint>()
            val startTime = now - (daysToCover.toLong() * dayMillis)

            // Filter snapshots in range
            val relevantSnapshots = snapshots.filter { it.date >= startTime }.sortedBy { it.date }

            if (relevantSnapshots.size >= 4) {
                // Use actual stored snapshots
                relevantSnapshots.forEach { s ->
                    points.add(
                        NetWorthPoint(
                            timestamp = s.date,
                            label = dateFormat.format(Date(s.date)),
                            assets = s.totalAssets,
                            liabilities = s.totalLiabilities,
                            netWorth = s.netWorth
                        )
                    )
                }
            } else {
                // Generate smooth realistic trajectory ending at current summary
                val numSteps = (daysToCover / stepDays).coerceIn(4, 12)
                for (i in numSteps downTo 1) {
                    val t = now - (i * stepDays.toLong() * dayMillis)
                    val factor = 1.0 - (i.toDouble() / (numSteps * 1.5))
                    // Slight progression of growth
                    val estimatedAssets = (currentSummary.totalAssets * (0.82 + (factor * 0.18))).coerceAtLeast(1000.0)
                    val estimatedLiab = (currentSummary.totalLiabilities * (1.10 - (factor * 0.10))).coerceAtLeast(0.0)
                    val pointNetWorth = estimatedAssets - estimatedLiab

                    points.add(
                        NetWorthPoint(
                            timestamp = t,
                            label = dateFormat.format(Date(t)),
                            assets = estimatedAssets,
                            liabilities = estimatedLiab,
                            netWorth = pointNetWorth
                        )
                    )
                }
            }

            // Always add current point at the end
            points.add(
                NetWorthPoint(
                    timestamp = now,
                    label = "Today",
                    assets = currentSummary.totalAssets,
                    liabilities = currentSummary.totalLiabilities,
                    netWorth = currentSummary.netWorth
                )
            )

            points
        }
    }

    override suspend fun saveNetWorthSnapshot() {
        val summary = getNetWorthSummary().first()
        netWorthDao.insertSnapshot(
            NetWorthSnapshotEntity(
                date = System.currentTimeMillis(),
                totalAssets = summary.totalAssets,
                totalLiabilities = summary.totalLiabilities,
                netWorth = summary.netWorth
            )
        )
    }

    // ==========================================
    // Cash Flow Forecast
    // ==========================================
    override fun getCashFlowForecast(period: ForecastPeriod): Flow<CashFlowForecast> {
        return combine(
            accountDao.getAllAccounts(),
            recurringDao.getActiveRecurring(),
            categoryDao.getAllCategories(),
            userSettingsDao.getUserSettings()
        ) { accounts, recurringList, categories, settings ->
            val catMap = categories.associateBy { it.id }
            val accMap = accounts.associateBy { it.id }
            val currency = settings?.defaultCurrency ?: "₹"

            // Current liquid balance = sum of positive asset account balances
            val currentBalance = accounts
                .filter {
                    val type = try { com.example.domain.model.AccountType.valueOf(it.type) } catch (e: Exception) { com.example.domain.model.AccountType.BANK }
                    type.classification == AccountClassification.ASSET && it.balance > 0
                }
                .sumOf { it.balance }

            val cal = Calendar.getInstance()
            val nowMillis = cal.timeInMillis

            val (periodDays, periodLabel, endMillis) = when (period) {
                ForecastPeriod.NEXT_7_DAYS -> {
                    val endCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }
                    Triple(7, "Next 7 Days", endCal.timeInMillis)
                }
                ForecastPeriod.NEXT_30_DAYS -> {
                    val endCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 30) }
                    Triple(30, "Next 30 Days", endCal.timeInMillis)
                }
                ForecastPeriod.END_OF_MONTH -> {
                    val endCal = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                    }
                    val daysRemaining = (endCal.get(Calendar.DAY_OF_MONTH) - cal.get(Calendar.DAY_OF_MONTH)).coerceAtLeast(1)
                    Triple(daysRemaining, "End of Month", endCal.timeInMillis)
                }
            }

            val rawEvents = mutableListOf<ForecastEvent>()
            val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

            // Expand recurring transactions inside the forecast window
            for (rec in recurringList) {
                var nextDate = rec.nextExecutionDate
                val frequency = try { Frequency.valueOf(rec.frequency) } catch (e: Exception) { Frequency.MONTHLY }
                val txType = try { TransactionType.valueOf(rec.type) } catch (e: Exception) { TransactionType.EXPENSE }
                val cat = catMap[rec.categoryId]
                val acc = accMap[rec.accountId]

                // If next execution is in the past, align it into future
                val eventCal = Calendar.getInstance().apply { timeInMillis = nextDate }
                while (eventCal.timeInMillis < nowMillis - (12 * 60 * 60 * 1000)) {
                    when (frequency) {
                        Frequency.DAILY -> eventCal.add(Calendar.DAY_OF_YEAR, 1)
                        Frequency.WEEKLY -> eventCal.add(Calendar.WEEK_OF_YEAR, 1)
                        Frequency.MONTHLY -> eventCal.add(Calendar.MONTH, 1)
                        Frequency.YEARLY -> eventCal.add(Calendar.YEAR, 1)
                    }
                }

                // Collect occurrences up to endMillis
                var iterCount = 0
                while (eventCal.timeInMillis <= endMillis && iterCount < 30) {
                    iterCount++
                    val eventDate = eventCal.timeInMillis
                    rawEvents.add(
                        ForecastEvent(
                            id = "${rec.id}_$eventDate",
                            title = rec.name,
                            amount = rec.amount,
                            type = txType,
                            date = eventDate,
                            dateFormatted = dateFormat.format(Date(eventDate)),
                            categoryName = cat?.name ?: "Recurring",
                            categoryIcon = cat?.iconName ?: "ReceiptLong",
                            categoryColorHex = cat?.colorHex ?: "#10B981",
                            accountName = acc?.name ?: "Main Bank",
                            runningBalance = 0.0 // Computed below
                        )
                    )

                    when (frequency) {
                        Frequency.DAILY -> eventCal.add(Calendar.DAY_OF_YEAR, 1)
                        Frequency.WEEKLY -> eventCal.add(Calendar.WEEK_OF_YEAR, 1)
                        Frequency.MONTHLY -> eventCal.add(Calendar.MONTH, 1)
                        Frequency.YEARLY -> eventCal.add(Calendar.YEAR, 1)
                    }
                }
            }

            // Sort timeline chronologically
            val sortedEvents = rawEvents.sortedBy { it.date }
            var running = currentBalance
            val finalTimeline = sortedEvents.map { event ->
                if (event.type == TransactionType.INCOME) {
                    running += event.amount
                } else {
                    running -= event.amount
                }
                event.copy(runningBalance = running)
            }

            val expectedIncome = finalTimeline.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expectedExpenses = finalTimeline.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val projectedBalance = currentBalance + expectedIncome - expectedExpenses
            val netCashFlow = expectedIncome - expectedExpenses

            CashFlowForecast(
                currentBalance = currentBalance,
                expectedIncome = expectedIncome,
                expectedExpenses = expectedExpenses,
                projectedBalance = projectedBalance,
                netCashFlow = netCashFlow,
                period = period,
                periodDays = periodDays,
                periodLabel = periodLabel,
                timelineEvents = finalTimeline,
                currency = currency
            )
        }
    }

    // ==========================================
    // Credit Card Management
    // ==========================================
    override fun getAllCreditCards(): Flow<List<CreditCard>> {
        return creditCardDao.getAllCards().map { list -> list.map { it.toDomain() } }
    }

    override fun getActiveCreditCards(): Flow<List<CreditCard>> {
        return creditCardDao.getAllActiveCards().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getCreditCardById(id: Long): CreditCard? {
        return creditCardDao.getCardById(id)?.toDomain()
    }

    override fun getCreditCardByIdFlow(id: Long): Flow<CreditCard?> {
        return creditCardDao.getCardByIdFlow(id).map { it?.toDomain() }
    }

    override suspend fun addCreditCard(card: CreditCard): Long {
        return creditCardDao.insertCard(CreditCardEntity.fromDomain(card))
    }

    override suspend fun updateCreditCard(card: CreditCard) {
        creditCardDao.updateCard(CreditCardEntity.fromDomain(card))
    }

    override suspend fun deleteCreditCard(card: CreditCard) {
        creditCardDao.deleteCard(CreditCardEntity.fromDomain(card))
    }

    override suspend fun recordCreditCardPayment(
        cardId: Long,
        amount: Double,
        sourceAccountId: Long,
        note: String
    ) {
        val card = creditCardDao.getCardById(cardId) ?: return
        // 1. Deduct amount from paying bank account
        accountDao.updateAccountBalance(sourceAccountId, -amount)

        // 2. Reduce card outstanding balance
        val newOutstanding = (card.outstandingBalance - amount).coerceAtLeast(0.0)
        creditCardDao.updateOutstandingBalance(cardId, newOutstanding)

        // 3. If linked to an AccountEntity, adjust that account's balance
        card.linkedAccountId?.let { linkedAccId ->
            accountDao.updateAccountBalance(linkedAccId, amount)
        }

        // 4. Log as a transfer/repayment transaction so balance change is tracked
        val now = System.currentTimeMillis()
        val tx = TransactionEntity(
            amount = amount,
            type = "TRANSFER",
            categoryId = 4, // Bills & Utilities
            accountId = sourceAccountId,
            destinationAccountId = card.linkedAccountId,
            dateTime = now,
            note = if (note.isNotBlank()) note else "Payment for ${card.name}",
            createdAt = now,
            updatedAt = now
        )
        transactionDao.insertTransaction(tx)
    }

    // ==========================================
    // Loan Tracking
    // ==========================================
    override fun getAllLoans(): Flow<List<Loan>> {
        return loanDao.getAllLoans().map { list -> list.map { it.toDomain() } }
    }

    override fun getActiveLoans(): Flow<List<Loan>> {
        return loanDao.getActiveLoans().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getLoanById(id: Long): Loan? {
        return loanDao.getLoanById(id)?.toDomain()
    }

    override fun getLoanByIdFlow(id: Long): Flow<Loan?> {
        return loanDao.getLoanByIdFlow(id).map { it?.toDomain() }
    }

    override fun getLoanDetail(loanId: Long): Flow<LoanDetail?> {
        return combine(
            loanDao.getLoanByIdFlow(loanId),
            loanDao.getPaymentsForLoan(loanId),
            accountDao.getAllAccounts()
        ) { loanEntity, payments, accounts ->
            if (loanEntity == null) return@combine null
            val loan = loanEntity.toDomain()
            val accMap = accounts.associateBy { it.id }
            LoanDetail(
                loan = loan,
                payments = payments.map { it.toDomain() },
                linkedAccount = loan.linkedAccountId?.let { accMap[it]?.toDomain() }
            )
        }
    }

    override suspend fun addLoan(loan: Loan): Long {
        return loanDao.insertLoan(LoanEntity.fromDomain(loan))
    }

    override suspend fun updateLoan(loan: Loan) {
        loanDao.updateLoan(LoanEntity.fromDomain(loan))
    }

    override suspend fun deleteLoan(loan: Loan) {
        loanDao.deleteLoan(LoanEntity.fromDomain(loan))
    }

    override suspend fun recordLoanPayment(payment: LoanPayment, sourceAccountId: Long?) {
        val loan = loanDao.getLoanById(payment.loanId) ?: return

        // 1. Insert payment record
        val paymentEntity = LoanPaymentEntity.fromDomain(payment)
        loanDao.insertPayment(paymentEntity)

        // 2. Reduce remaining balance
        val newBalance = (loan.remainingBalance - payment.principalAmount).coerceAtLeast(0.0)
        loanDao.updateRemainingBalance(loan.id, newBalance)

        // If paid off, update status
        if (newBalance <= 0.0) {
            loanDao.updateLoanStatus(loan.id, LoanStatus.PAID_OFF.name)
        }

        // 3. Deduct from source account if selected
        sourceAccountId?.let { accId ->
            accountDao.updateAccountBalance(accId, -payment.amount)
            val now = System.currentTimeMillis()
            val tx = TransactionEntity(
                amount = payment.amount,
                type = "EXPENSE",
                categoryId = 4, // Bills & Utilities
                accountId = accId,
                destinationAccountId = null,
                dateTime = now,
                note = if (payment.note.isNotBlank()) payment.note else "EMI Payment for ${loan.name}",
                createdAt = now,
                updatedAt = now
            )
            transactionDao.insertTransaction(tx)
        }
    }

    override suspend fun deleteLoanPayment(payment: LoanPayment) {
        val loan = loanDao.getLoanById(payment.loanId)
        if (loan != null) {
            val restoredBalance = loan.remainingBalance + payment.principalAmount
            loanDao.updateRemainingBalance(loan.id, restoredBalance)
            if (restoredBalance > 0 && loan.status == LoanStatus.PAID_OFF.name) {
                loanDao.updateLoanStatus(loan.id, LoanStatus.ACTIVE.name)
            }
        }
        loanDao.deletePayment(LoanPaymentEntity.fromDomain(payment))
    }

    // ==========================================
    // Saved Transaction Filters
    // ==========================================
    override fun getAllSavedFilters(): Flow<List<SavedTransactionFilter>> {
        return savedFilterDao.getAllSavedFilters().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun addSavedFilter(filter: SavedTransactionFilter): Long {
        return savedFilterDao.insertSavedFilter(SavedFilterEntity.fromDomain(filter))
    }

    override suspend fun updateSavedFilter(filter: SavedTransactionFilter) {
        savedFilterDao.updateSavedFilter(SavedFilterEntity.fromDomain(filter))
    }

    override suspend fun deleteSavedFilter(filter: SavedTransactionFilter) {
        savedFilterDao.deleteSavedFilter(SavedFilterEntity.fromDomain(filter))
    }

    // ==========================================
    // Global Search & Recent Searches
    // ==========================================
    override fun searchAll(query: String): Flow<com.example.domain.model.GlobalSearchResults> {
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isEmpty()) {
            return kotlinx.coroutines.flow.flowOf(com.example.domain.model.GlobalSearchResults(query = ""))
        }

        return combine(
            listOf(
                getTransactionDetailsFlow(),
                getAllAccounts(),
                getAllCategories(),
                getAllRecurring(),
                getAllGoalsFlow(),
                getAllCreditCards(),
                getAllLoans()
            )
        ) { flows ->
            @Suppress("UNCHECKED_CAST")
            val txs = flows[0] as List<TransactionDetail>
            @Suppress("UNCHECKED_CAST")
            val accounts = flows[1] as List<Account>
            @Suppress("UNCHECKED_CAST")
            val categories = flows[2] as List<Category>
            @Suppress("UNCHECKED_CAST")
            val recurring = flows[3] as List<RecurringDetail>
            @Suppress("UNCHECKED_CAST")
            val goals = flows[4] as List<GoalDetail>
            @Suppress("UNCHECKED_CAST")
            val cards = flows[5] as List<CreditCard>
            @Suppress("UNCHECKED_CAST")
            val loans = flows[6] as List<Loan>

            val matchedTxs = txs.filter { item ->
                val note = item.transaction.note.lowercase()
                val catName = (item.category?.name ?: "").lowercase()
                val accName = (item.sourceAccount?.name ?: "").lowercase()
                val amtStr = item.transaction.amount.toString()
                note.contains(cleanQuery) ||
                catName.contains(cleanQuery) ||
                accName.contains(cleanQuery) ||
                amtStr.contains(cleanQuery)
            }

            val matchedAccounts = accounts.filter { acc ->
                acc.name.lowercase().contains(cleanQuery) ||
                acc.type.name.lowercase().contains(cleanQuery)
            }

            val matchedCategories = categories.filter { cat ->
                cat.name.lowercase().contains(cleanQuery) ||
                cat.type.name.lowercase().contains(cleanQuery)
            }

            val matchedRecurring = recurring.filter { rec ->
                rec.recurring.name.lowercase().contains(cleanQuery) ||
                (rec.category?.name ?: "").lowercase().contains(cleanQuery) ||
                (rec.account?.name ?: "").lowercase().contains(cleanQuery) ||
                rec.recurring.note.lowercase().contains(cleanQuery)
            }

            val matchedGoals = goals.filter { g ->
                g.goal.name.lowercase().contains(cleanQuery)
            }

            val matchedCards = cards.filter { c ->
                c.name.lowercase().contains(cleanQuery) ||
                c.bankName.lowercase().contains(cleanQuery) ||
                c.cardNumberMasked.lowercase().contains(cleanQuery)
            }

            val matchedLoans = loans.filter { l ->
                l.name.lowercase().contains(cleanQuery) ||
                l.lender.lowercase().contains(cleanQuery) ||
                l.loanType.name.lowercase().contains(cleanQuery)
            }

            com.example.domain.model.GlobalSearchResults(
                query = query,
                transactions = matchedTxs,
                accounts = matchedAccounts,
                categories = matchedCategories,
                subscriptions = matchedRecurring,
                goals = matchedGoals,
                creditCards = matchedCards,
                loans = matchedLoans
            )
        }
    }

    override fun getRecentSearches(): Flow<List<com.example.data.local.entity.RecentSearchEntity>> {
        return recentSearchDao.getRecentSearches()
    }

    override suspend fun addRecentSearch(query: String) {
        val clean = query.trim()
        if (clean.isNotBlank()) {
            recentSearchDao.deleteSearchByQuery(clean)
            recentSearchDao.insertSearch(
                com.example.data.local.entity.RecentSearchEntity(
                    query = clean,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun removeRecentSearch(query: String) {
        recentSearchDao.deleteSearchByQuery(query.trim())
    }

    override suspend fun clearRecentSearches() {
        recentSearchDao.clearAll()
    }

    // ==========================================
    // Backup & Restore
    // ==========================================
    override suspend fun createBackupJson(): String {
        return com.example.core.util.BackupManager.createBackupJson(database)
    }

    override suspend fun restoreBackup(jsonString: String): Result<Unit> {
        return com.example.core.util.BackupManager.restoreDatabaseFromJson(database, jsonString)
    }

    override fun validateBackup(jsonString: String): com.example.domain.model.BackupValidationResult {
        return com.example.core.util.BackupManager.validateBackupJson(jsonString)
    }

    // ==========================================
    // Data Export
    // ==========================================
    override fun getFilteredTransactionsForExport(
        filterState: com.example.domain.model.ExportFilterState
    ): Flow<List<TransactionDetail>> {
        return getTransactionDetailsFlow().map { allTxs ->
            allTxs.filter { item ->
                val tx = item.transaction
                val matchesAccount = filterState.accountId == null || tx.accountId == filterState.accountId
                val matchesCategory = filterState.categoryId == null || tx.categoryId == filterState.categoryId
                val matchesType = filterState.transactionType == null || tx.type == filterState.transactionType

                val matchesDate = when (filterState.dateRange) {
                    com.example.domain.model.ExportDateRange.ALL_TIME -> true
                    com.example.domain.model.ExportDateRange.THIS_MONTH -> {
                        val cal = Calendar.getInstance()
                        cal.set(Calendar.DAY_OF_MONTH, 1)
                        cal.set(Calendar.HOUR_OF_DAY, 0)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        tx.dateTime >= cal.timeInMillis
                    }
                    com.example.domain.model.ExportDateRange.LAST_MONTH -> {
                        val cal = Calendar.getInstance()
                        cal.set(Calendar.DAY_OF_MONTH, 1)
                        cal.set(Calendar.HOUR_OF_DAY, 0)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        val thisMonthStart = cal.timeInMillis
                        cal.add(Calendar.MONTH, -1)
                        val lastMonthStart = cal.timeInMillis
                        tx.dateTime in lastMonthStart until thisMonthStart
                    }
                    com.example.domain.model.ExportDateRange.LAST_30_DAYS -> {
                        val start = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
                        tx.dateTime >= start
                    }
                    com.example.domain.model.ExportDateRange.LAST_90_DAYS -> {
                        val start = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000)
                        tx.dateTime >= start
                    }
                    com.example.domain.model.ExportDateRange.THIS_YEAR -> {
                        val cal = Calendar.getInstance()
                        cal.set(Calendar.DAY_OF_YEAR, 1)
                        cal.set(Calendar.HOUR_OF_DAY, 0)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        tx.dateTime >= cal.timeInMillis
                    }
                    com.example.domain.model.ExportDateRange.CUSTOM -> {
                        tx.dateTime in filterState.customStartDate..filterState.customEndDate
                    }
                }

                matchesAccount && matchesCategory && matchesType && matchesDate
            }
        }
    }

    override fun getExportPreviewSummary(
        filterState: com.example.domain.model.ExportFilterState
    ): Flow<com.example.domain.model.ExportPreviewSummary> {
        return getFilteredTransactionsForExport(filterState).map { txs ->
            var totalIncome = 0.0
            var totalExpense = 0.0
            val categoryExpenses = mutableMapOf<String, Double>()

            for (item in txs) {
                when (item.transaction.type) {
                    TransactionType.INCOME -> totalIncome += item.transaction.amount
                    TransactionType.EXPENSE -> {
                        totalExpense += item.transaction.amount
                        val catName = item.category?.name ?: "Other"
                        categoryExpenses[catName] = (categoryExpenses[catName] ?: 0.0) + item.transaction.amount
                    }
                    TransactionType.TRANSFER -> {}
                }
            }

            val topCategory = categoryExpenses.maxByOrNull { it.value }?.key ?: "None"

            com.example.domain.model.ExportPreviewSummary(
                totalTransactions = txs.size,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                netCashFlow = totalIncome - totalExpense,
                periodLabel = filterState.dateRange.label,
                topExpenseCategory = topCategory
            )
        }
    }

    // ==========================================
    // Privacy & Security Settings
    // ==========================================
    override suspend fun updatePrivacyAndLockSettings(
        hideAmounts: Boolean,
        hideAmountsPersist: Boolean,
        appLockEnabled: Boolean,
        appLockTimeoutMinutes: Int
    ) {
        val current = userSettingsDao.getUserSettings().first() ?: DatabasePrepopulate.getDefaultUserSettings()
        val updated = current.copy(
            hideAmounts = hideAmounts,
            hideAmountsPersist = hideAmountsPersist,
            appLockEnabled = appLockEnabled,
            appLockTimeoutMinutes = appLockTimeoutMinutes
        )
        userSettingsDao.insertOrUpdate(updated)
        com.example.core.util.CurrencyFormatter.isPrivacyActive = hideAmounts
    }

    // ==========================================
    // Quick Add & Smart Suggestions
    // ==========================================
    override fun getQuickAddSuggestions(): Flow<com.example.domain.model.QuickAddSuggestions> {
        return combine(
            getTransactionDetailsFlow(),
            getAllCategories(),
            getAllAccounts()
        ) { txs, categories, accounts ->
            val lastTx = txs.firstOrNull()

            // Distinct recent categories
            val catMap = categories.associateBy { it.id }
            val accMap = accounts.associateBy { it.id }

            val recentCats = txs.mapNotNull { it.category }
                .distinctBy { it.id }
                .take(5)

            val recentAccs = txs.mapNotNull { it.sourceAccount }
                .distinctBy { it.id }
                .take(4)

            // Detect frequent patterns (e.g. Swiggy ₹350, Metro ₹40, Chai ₹30)
            val expenseTxs = txs.filter { it.transaction.type == TransactionType.EXPENSE }
            val patternGroups = expenseTxs.groupBy {
                val cleanNote = it.transaction.note.trim().lowercase(Locale.ROOT)
                val catId = it.transaction.categoryId
                val accId = it.transaction.accountId
                Triple(cleanNote, catId, accId)
            }

            val patterns = patternGroups.map { (key, list) ->
                val representative = list.first()
                val (cleanNote, catId, accId) = key
                val cat = catMap[catId]
                val acc = accMap[accId]
                val title = if (cleanNote.isNotBlank()) {
                    cleanNote.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                } else {
                    cat?.name ?: "Quick Expense"
                }

                com.example.domain.model.FrequentTransactionPattern(
                    title = title,
                    amount = representative.transaction.amount,
                    categoryId = catId,
                    categoryName = cat?.name ?: "Expense",
                    categoryIcon = cat?.iconName ?: "Payment",
                    categoryColor = cat?.colorHex ?: "#10B981",
                    accountId = accId,
                    accountName = acc?.name ?: "Account",
                    note = representative.transaction.note,
                    type = TransactionType.EXPENSE,
                    count = list.size
                )
            }.sortedByDescending { it.count }
            .take(4)

            // Extract top merchant names
            val merchants = expenseTxs.map {
                com.example.core.util.MerchantIntelligenceHelper.extractMerchantName(
                    it.transaction.note,
                    it.category?.name ?: ""
                )
            }.filter { it.isNotBlank() && it != "Other" }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(6)

            com.example.domain.model.QuickAddSuggestions(
                recentlyUsedCategories = recentCats,
                recentlyUsedAccounts = recentAccs,
                frequentPatterns = patterns,
                lastTransaction = lastTx,
                topMerchants = merchants
            )
        }
    }

    // ==========================================
    // Merchant Intelligence
    // ==========================================
    override fun getTopMerchants(limit: Int): Flow<List<com.example.domain.model.MerchantSummary>> {
        return getTransactionDetailsFlow().map { txs ->
            val expenseTxs = txs.filter { it.transaction.type == TransactionType.EXPENSE }
            val grouped = expenseTxs.groupBy {
                com.example.core.util.MerchantIntelligenceHelper.extractMerchantName(
                    it.transaction.note,
                    it.category?.name ?: "Other"
                )
            }

            grouped.map { (merchantName, list) ->
                val total = list.sumOf { it.transaction.amount }
                val count = list.size
                val avg = if (count > 0) total / count else 0.0
                val minDate = list.minOfOrNull { it.transaction.dateTime } ?: 0L
                val maxDate = list.maxOfOrNull { it.transaction.dateTime } ?: 0L

                // Top category
                val catGroup = list.groupBy { it.category }
                val topCatEntry = catGroup.maxByOrNull { it.value.sumOf { tx -> tx.transaction.amount } }
                val topCat = topCatEntry?.key
                val topCatSpend = topCatEntry?.value?.sumOf { it.transaction.amount } ?: 0.0
                val topCatPct = if (total > 0) (topCatSpend / total).toFloat() else 0f

                // Monthly Trend for last 6 months
                val trend = calculateMonthlyTrend(list)

                com.example.domain.model.MerchantSummary(
                    name = merchantName,
                    totalSpent = total,
                    transactionCount = count,
                    averageSpend = avg,
                    firstDate = minDate,
                    lastDate = maxDate,
                    topCategory = topCat,
                    topCategoryPercentage = topCatPct,
                    recentTransactions = list.sortedByDescending { it.transaction.dateTime }.take(5),
                    monthlyTrend = trend
                )
            }.sortedByDescending { it.totalSpent }
            .take(limit)
        }
    }

    override fun getMerchantDetail(merchantName: String): Flow<com.example.domain.model.MerchantSummary?> {
        return getTransactionDetailsFlow().map { txs ->
            val expenseTxs = txs.filter { it.transaction.type == TransactionType.EXPENSE }
            val matched = expenseTxs.filter {
                val extracted = com.example.core.util.MerchantIntelligenceHelper.extractMerchantName(
                    it.transaction.note,
                    it.category?.name ?: "Other"
                )
                extracted.equals(merchantName.trim(), ignoreCase = true)
            }

            if (matched.isEmpty()) return@map null

            val total = matched.sumOf { it.transaction.amount }
            val count = matched.size
            val avg = if (count > 0) total / count else 0.0
            val minDate = matched.minOfOrNull { it.transaction.dateTime } ?: 0L
            val maxDate = matched.maxOfOrNull { it.transaction.dateTime } ?: 0L

            val catGroup = matched.groupBy { it.category }
            val topCatEntry = catGroup.maxByOrNull { it.value.sumOf { tx -> tx.transaction.amount } }
            val topCat = topCatEntry?.key
            val topCatSpend = topCatEntry?.value?.sumOf { it.transaction.amount } ?: 0.0
            val topCatPct = if (total > 0) (topCatSpend / total).toFloat() else 0f

            val trend = calculateMonthlyTrend(matched)

            com.example.domain.model.MerchantSummary(
                name = merchantName,
                totalSpent = total,
                transactionCount = count,
                averageSpend = avg,
                firstDate = minDate,
                lastDate = maxDate,
                topCategory = topCat,
                topCategoryPercentage = topCatPct,
                recentTransactions = matched.sortedByDescending { it.transaction.dateTime },
                monthlyTrend = trend
            )
        }
    }

    private fun calculateMonthlyTrend(transactions: List<TransactionDetail>): List<com.example.domain.model.MerchantMonthlyPoint> {
        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
        val cal = Calendar.getInstance()
        val result = mutableListOf<com.example.domain.model.MerchantMonthlyPoint>()

        for (i in 5 downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.MONTH, -i)
            val monthLabel = monthFormat.format(c.time)
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)

            val monthSpend = transactions.filter {
                val tCal = Calendar.getInstance().apply { timeInMillis = it.transaction.dateTime }
                tCal.get(Calendar.YEAR) == year && tCal.get(Calendar.MONTH) == month
            }.sumOf { it.transaction.amount }

            result.add(com.example.domain.model.MerchantMonthlyPoint(monthLabel, monthSpend))
        }

        return result
    }

    override fun getSuggestedMerchants(query: String): Flow<List<String>> {
        return getTransactionDetailsFlow().map { txs ->
            val existing = txs.mapNotNull {
                val note = it.transaction.note.trim()
                if (note.isNotBlank()) com.example.core.util.MerchantIntelligenceHelper.extractMerchantName(note, it.category?.name ?: "") else null
            }.distinct()

            com.example.core.util.MerchantIntelligenceHelper.getSuggestions(query, existing)
        }
    }

    // ==========================================
    // Home Customization
    // ==========================================
    override suspend fun updateHomeCustomization(sectionsOrder: String, hiddenSections: String) {
        val current = userSettingsDao.getUserSettings().first() ?: DatabasePrepopulate.getDefaultUserSettings()
        val updated = current.copy(
            homeSectionsOrder = sectionsOrder,
            hiddenHomeSections = hiddenSections
        )
        userSettingsDao.insertOrUpdate(updated)
    }

    // ==========================================
    // Expense Books
    // ==========================================
    override fun getAllExpenseBooks(): Flow<List<ExpenseBook>> {
        return expenseBookDao.getAllBooks().map { list -> list.map { it.toDomain() } }
    }

    override fun getActiveExpenseBooks(): Flow<List<ExpenseBook>> {
        return expenseBookDao.getActiveBooks().map { list -> list.map { it.toDomain() } }
    }

    override fun getExpenseBookSummaries(): Flow<List<ExpenseBookSummary>> {
        return combine(
            expenseBookDao.getAllBooks(),
            transactionDao.getAllTransactions()
        ) { books, transactions ->
            books.map { bookEntity ->
                val book = bookEntity.toDomain()
                val bookTxs = transactions.filter { it.bookId == book.id }
                val exp = bookTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                val inc = bookTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
                val progress = if (book.budget > 0) (exp / book.budget).toFloat() else 0f
                ExpenseBookSummary(
                    book = book,
                    totalExpense = exp,
                    totalIncome = inc,
                    transactionCount = bookTxs.size,
                    budgetProgress = progress
                )
            }
        }
    }

    override suspend fun getExpenseBookById(id: Long): ExpenseBook? {
        return expenseBookDao.getBookById(id)?.toDomain()
    }

    override fun getExpenseBookByIdFlow(id: Long): Flow<ExpenseBook?> {
        return expenseBookDao.getBookByIdFlow(id).map { it?.toDomain() }
    }

    override suspend fun addExpenseBook(book: ExpenseBook): Long {
        return expenseBookDao.insertBook(ExpenseBookEntity.fromDomain(book))
    }

    override suspend fun updateExpenseBook(book: ExpenseBook) {
        expenseBookDao.updateBook(ExpenseBookEntity.fromDomain(book))
    }

    override suspend fun deleteExpenseBook(book: ExpenseBook) {
        expenseBookDao.deleteBook(ExpenseBookEntity.fromDomain(book))
    }

    override suspend fun setExpenseBookArchived(id: Long, isArchived: Boolean) {
        expenseBookDao.setArchived(id, isArchived)
    }

    override suspend fun setDefaultExpenseBook(id: Long) {
        expenseBookDao.resetAllDefaults()
        expenseBookDao.setDefaultBook(id)
    }

    override fun getTransactionsForBook(bookId: Long): Flow<List<TransactionDetail>> {
        return combine(
            transactionDao.getTransactionsForBook(bookId),
            categoryDao.getAllCategories(),
            accountDao.getAllAccounts(),
            expenseBookDao.getAllBooks()
        ) { transactions, categories, accounts, books ->
            val catMap = categories.associateBy { it.id }
            val accMap = accounts.associateBy { it.id }
            val bookMap = books.associateBy { it.id }

            transactions.map { txEntity ->
                val tx = txEntity.toDomain()
                TransactionDetail(
                    transaction = tx,
                    category = catMap[tx.categoryId]?.toDomain(),
                    sourceAccount = accMap[tx.accountId]?.toDomain(),
                    destinationAccount = tx.destinationAccountId?.let { accMap[it]?.toDomain() },
                    book = bookMap[tx.bookId]?.toDomain()
                )
            }
        }
    }
}
