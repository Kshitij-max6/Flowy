package com.example.core.util

import android.content.Context
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CreditCardEntity
import com.example.data.local.entity.GoalContributionEntity
import com.example.data.local.entity.GoalEntity
import com.example.data.local.entity.LoanEntity
import com.example.data.local.entity.LoanPaymentEntity
import com.example.data.local.entity.NetWorthSnapshotEntity
import com.example.data.local.entity.RecurringTransactionEntity
import com.example.data.local.entity.SavedFilterEntity
import com.example.data.local.entity.SplitExpenseEntity
import com.example.data.local.entity.SplitFriendEntity
import com.example.data.local.entity.SplitSettlementEntity
import com.example.data.local.entity.SplitShareEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserSettingsEntity
import com.example.domain.model.BackupValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    private val timestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    private val readableFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    /**
     * Serializes the entire local database to a versioned JSON String.
     */
    suspend fun createBackupJson(database: AppDatabase): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        val now = System.currentTimeMillis()

        root.put("version", 1)
        root.put("appName", "Flowly")
        root.put("appVersion", "1.0")
        root.put("timestamp", now)
        root.put("createdAtFormatted", readableFormat.format(Date(now)))

        val dataObj = JSONObject()

        // 1. User Settings
        val userSettings = database.userSettingsDao().getUserSettings().first()
        if (userSettings != null) {
            val usObj = JSONObject().apply {
                put("id", userSettings.id)
                put("userName", userSettings.userName)
                put("defaultCurrency", userSettings.defaultCurrency)
                put("dailyBudget", userSettings.dailyBudget)
                put("monthlyBudget", userSettings.monthlyBudget)
                put("themeMode", userSettings.themeMode)
                put("hasCompletedOnboarding", userSettings.hasCompletedOnboarding)
                put("enableHaptics", userSettings.enableHaptics)
                put("hideAmounts", userSettings.hideAmounts)
                put("hideAmountsPersist", userSettings.hideAmountsPersist)
                put("appLockEnabled", userSettings.appLockEnabled)
                put("appLockTimeoutMinutes", userSettings.appLockTimeoutMinutes)
                put("homeSectionsOrder", userSettings.homeSectionsOrder)
                put("hiddenHomeSections", userSettings.hiddenHomeSections)
            }
            dataObj.put("userSettings", usObj)
        }

        // 2. Categories
        val categories = database.categoryDao().getAllCategories().first()
        val catArray = JSONArray()
        for (c in categories) {
            catArray.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("iconName", c.iconName)
                put("type", c.type)
                put("colorHex", c.colorHex)
                put("isCustom", c.isCustom)
                put("parentCategoryId", c.parentCategoryId ?: JSONObject.NULL)
                put("isFavorite", c.isFavorite)
                put("isArchived", c.isArchived)
                put("sortOrder", c.sortOrder)
            })
        }
        dataObj.put("categories", catArray)

        // 3. Accounts
        val accounts = database.accountDao().getAllAccounts().first()
        val accArray = JSONArray()
        for (a in accounts) {
            accArray.put(JSONObject().apply {
                put("id", a.id)
                put("name", a.name)
                put("type", a.type)
                put("balance", a.balance)
                put("currency", a.currency)
                put("iconName", a.iconName)
                put("colorHex", a.colorHex)
                put("isDefault", a.isDefault)
            })
        }
        dataObj.put("accounts", accArray)

        // 4. Transactions
        val transactions = database.transactionDao().getAllTransactions().first()
        val txArray = JSONArray()
        for (t in transactions) {
            txArray.put(JSONObject().apply {
                put("id", t.id)
                put("amount", t.amount)
                put("type", t.type)
                put("categoryId", t.categoryId)
                put("accountId", t.accountId)
                put("destinationAccountId", t.destinationAccountId ?: JSONObject.NULL)
                put("dateTime", t.dateTime)
                put("note", t.note)
                put("attachmentUri", t.attachmentUri ?: JSONObject.NULL)
                put("createdAt", t.createdAt)
                put("updatedAt", t.updatedAt)
            })
        }
        dataObj.put("transactions", txArray)

        // 5. Recurring Transactions
        val recurring = database.recurringTransactionDao().getAllRecurring().first()
        val recArray = JSONArray()
        for (r in recurring) {
            recArray.put(JSONObject().apply {
                put("id", r.id)
                put("name", r.name)
                put("amount", r.amount)
                put("type", r.type)
                put("categoryId", r.categoryId)
                put("accountId", r.accountId)
                put("frequency", r.frequency)
                put("nextExecutionDate", r.nextExecutionDate)
                put("active", r.active)
                put("note", r.note)
            })
        }
        dataObj.put("recurringTransactions", recArray)

        // 6. Budgets
        val budgets = database.budgetDao().getAllBudgets().first()
        val bgArray = JSONArray()
        for (b in budgets) {
            bgArray.put(JSONObject().apply {
                put("id", b.id)
                put("name", b.name)
                put("amount", b.amount)
                put("period", b.period)
                put("categoryId", b.categoryId ?: JSONObject.NULL)
                put("startDate", b.startDate)
                put("endDate", b.endDate)
                put("active", b.active)
            })
        }
        dataObj.put("budgets", bgArray)

        // 7. Goals & Contributions
        val goals = database.goalDao().getAllGoals().first()
        val gArray = JSONArray()
        for (g in goals) {
            gArray.put(JSONObject().apply {
                put("id", g.id)
                put("name", g.name)
                put("targetAmount", g.targetAmount)
                put("currentAmount", g.currentAmount)
                put("currency", g.currency)
                put("targetDate", g.targetDate ?: JSONObject.NULL)
                put("createdDate", g.createdDate)
                put("linkedAccountId", g.linkedAccountId ?: JSONObject.NULL)
                put("iconName", g.iconName)
                put("status", g.status)
            })
        }
        dataObj.put("goals", gArray)

        val contributions = database.goalDao().getAllContributions().first()
        val gcArray = JSONArray()
        for (gc in contributions) {
            gcArray.put(JSONObject().apply {
                put("id", gc.id)
                put("goalId", gc.goalId)
                put("amount", gc.amount)
                put("date", gc.date)
                put("note", gc.note)
            })
        }
        dataObj.put("goalContributions", gcArray)

        // 8. Credit Cards
        val cards = database.creditCardDao().getAllCards().first()
        val ccArray = JSONArray()
        for (c in cards) {
            ccArray.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("bankName", c.bankName)
                put("cardNumberMasked", c.cardNumberMasked)
                put("creditLimit", c.creditLimit)
                put("outstandingBalance", c.outstandingBalance)
                put("billingCycleDay", c.billingCycleDay)
                put("paymentDueDay", c.paymentDueDay)
                put("linkedAccountId", c.linkedAccountId ?: JSONObject.NULL)
                put("currency", c.currency)
                put("cardColorHex", c.cardColorHex)
                put("status", c.status)
            })
        }
        dataObj.put("creditCards", ccArray)

        // 9. Loans & Payments
        val loans = database.loanDao().getAllLoans().first()
        val lnArray = JSONArray()
        for (l in loans) {
            lnArray.put(JSONObject().apply {
                put("id", l.id)
                put("name", l.name)
                put("lender", l.lender)
                put("loanType", l.loanType)
                put("originalAmount", l.originalAmount)
                put("remainingBalance", l.remainingBalance)
                put("interestRate", l.interestRate)
                put("monthlyPayment", l.monthlyPayment)
                put("nextPaymentDate", l.nextPaymentDate)
                put("startDate", l.startDate)
                put("endDate", l.endDate ?: JSONObject.NULL)
                put("currency", l.currency)
                put("status", l.status)
                put("linkedAccountId", l.linkedAccountId ?: JSONObject.NULL)
            })
        }
        dataObj.put("loans", lnArray)

        val payments = database.loanDao().getAllPayments().first()
        val lpArray = JSONArray()
        for (p in payments) {
            lpArray.put(JSONObject().apply {
                put("id", p.id)
                put("loanId", p.loanId)
                put("amount", p.amount)
                put("principalAmount", p.principalAmount)
                put("interestAmount", p.interestAmount)
                put("paymentDate", p.paymentDate)
                put("accountId", p.accountId ?: JSONObject.NULL)
                put("note", p.note)
            })
        }
        dataObj.put("loanPayments", lpArray)

        // 10. Net Worth Snapshots
        val snapshots = database.netWorthDao().getAllSnapshots().first()
        val nwArray = JSONArray()
        for (s in snapshots) {
            nwArray.put(JSONObject().apply {
                put("id", s.id)
                put("date", s.date)
                put("totalAssets", s.totalAssets)
                put("totalLiabilities", s.totalLiabilities)
                put("netWorth", s.netWorth)
            })
        }
        dataObj.put("netWorthSnapshots", nwArray)

        // 11. Saved Filters
        val filters = database.savedFilterDao().getAllSavedFilters().first()
        val sfArray = JSONArray()
        for (f in filters) {
            sfArray.put(JSONObject().apply {
                put("id", f.id)
                put("name", f.name)
                put("datePreset", f.datePreset)
                put("customStartDate", f.customStartDate ?: JSONObject.NULL)
                put("customEndDate", f.customEndDate ?: JSONObject.NULL)
                put("transactionType", f.transactionType ?: JSONObject.NULL)
                put("minAmount", f.minAmount ?: JSONObject.NULL)
                put("maxAmount", f.maxAmount ?: JSONObject.NULL)
                put("categoryIds", f.categoryIds)
                put("accountIds", f.accountIds)
                put("sourceFilter", f.sourceFilter)
                put("hasNote", f.hasNote)
                put("searchQuery", f.searchQuery)
            })
        }
        dataObj.put("savedFilters", sfArray)

        // 12. Split with Friends
        val splitDao = database.splitDao()
        val splitFriends = splitDao.getFriends().first()
        val splitExpenses = splitDao.getExpenses().first()
        val splitShares = splitDao.getShares().first()
        val splitSettlements = splitDao.getSettlements().first()
        dataObj.put("splitFriends", JSONArray().also { arr ->
            splitFriends.forEach { arr.put(JSONObject().put("id", it.id).put("name", it.name).put("createdAt", it.createdAt)) }
        })
        dataObj.put("splitExpenses", JSONArray().also { arr ->
            splitExpenses.forEach {
                arr.put(JSONObject().apply {
                    put("id", it.id); put("description", it.description); put("totalAmount", it.totalAmount)
                    put("paidByFriendId", it.paidByFriendId ?: JSONObject.NULL)
                    put("splitType", it.splitType); put("dateTime", it.dateTime); put("createdAt", it.createdAt)
                })
            }
        })
        dataObj.put("splitShares", JSONArray().also { arr ->
            splitShares.forEach {
                arr.put(JSONObject().apply {
                    put("id", it.id); put("expenseId", it.expenseId)
                    put("friendId", it.friendId ?: JSONObject.NULL); put("shareAmount", it.shareAmount)
                })
            }
        })
        dataObj.put("splitSettlements", JSONArray().also { arr ->
            splitSettlements.forEach {
                arr.put(JSONObject().apply {
                    put("id", it.id); put("friendId", it.friendId); put("amount", it.amount)
                    put("friendPaidMe", it.friendPaidMe); put("dateTime", it.dateTime); put("note", it.note)
                })
            }
        })

        root.put("data", dataObj)
        root.toString(2)
    }

    /**
     * Writes backup JSON to a file in cache directory.
     */
    fun writeBackupFile(context: Context, jsonString: String): File {
        val backupDir = File(context.cacheDir, "backups").apply { mkdirs() }
        val filename = "flowly_backup_${timestampFormat.format(Date())}.json"
        val file = File(backupDir, filename)
        FileOutputStream(file).use { out ->
            out.write(jsonString.toByteArray(Charsets.UTF_8))
        }
        return file
    }

    /**
     * Validates a backup JSON string and parses metadata.
     */
    fun validateBackupJson(jsonString: String): BackupValidationResult {
        return try {
            val root = JSONObject(jsonString)
            if (!root.has("version") || !root.has("data")) {
                return BackupValidationResult(
                    isValid = false,
                    errorMessage = "Invalid Flowly backup: Missing format version or data block."
                )
            }

            val version = root.optInt("version", 1)
            if (version > 1) {
                return BackupValidationResult(
                    isValid = false,
                    errorMessage = "This backup format version ($version) is newer than supported."
                )
            }

            val timestamp = root.optLong("timestamp", System.currentTimeMillis())
            val createdAtFormatted = root.optString("createdAtFormatted", readableFormat.format(Date(timestamp)))
            val dataObj = root.getJSONObject("data")

            val txCount = dataObj.optJSONArray("transactions")?.length() ?: 0
            val accCount = dataObj.optJSONArray("accounts")?.length() ?: 0
            val catCount = dataObj.optJSONArray("categories")?.length() ?: 0
            val bgCount = dataObj.optJSONArray("budgets")?.length() ?: 0
            val gCount = dataObj.optJSONArray("goals")?.length() ?: 0
            val recCount = dataObj.optJSONArray("recurringTransactions")?.length() ?: 0
            val ccCount = dataObj.optJSONArray("creditCards")?.length() ?: 0
            val lnCount = dataObj.optJSONArray("loans")?.length() ?: 0

            BackupValidationResult(
                isValid = true,
                timestamp = timestamp,
                createdAtFormatted = createdAtFormatted,
                transactionCount = txCount,
                accountCount = accCount,
                categoryCount = catCount,
                budgetCount = bgCount,
                goalCount = gCount,
                recurringCount = recCount,
                creditCardCount = ccCount,
                loanCount = lnCount
            )
        } catch (e: Exception) {
            BackupValidationResult(
                isValid = false,
                errorMessage = "Failed to parse backup file: ${e.localizedMessage ?: "Corrupted file"}"
            )
        }
    }

    /**
     * Atomically restores database state from a validated backup JSON.
     */
    suspend fun restoreDatabaseFromJson(database: AppDatabase, jsonString: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val dataObj = root.getJSONObject("data")

            // Atomic database replacement transaction
            database.withTransaction {
                // Clear all existing data tables
                database.clearAllTables()

                // 1. Categories
                if (dataObj.has("categories")) {
                    val catArr = dataObj.getJSONArray("categories")
                    val catList = mutableListOf<CategoryEntity>()
                    for (i in 0 until catArr.length()) {
                        val o = catArr.getJSONObject(i)
                        val parentId = if (o.isNull("parentCategoryId")) null else o.optLong("parentCategoryId")
                        catList.add(
                            CategoryEntity(
                                id = o.getLong("id"),
                                name = o.getString("name"),
                                iconName = o.getString("iconName"),
                                type = o.getString("type"),
                                colorHex = o.getString("colorHex"),
                                isCustom = o.optBoolean("isCustom", false),
                                parentCategoryId = if (parentId != null && parentId > 0) parentId else null,
                                isFavorite = o.optBoolean("isFavorite", false),
                                isArchived = o.optBoolean("isArchived", false),
                                sortOrder = o.optInt("sortOrder", 0)
                            )
                        )
                    }
                    database.categoryDao().insertCategories(catList)
                }

                // 2. Accounts
                if (dataObj.has("accounts")) {
                    val accArr = dataObj.getJSONArray("accounts")
                    val accList = mutableListOf<AccountEntity>()
                    for (i in 0 until accArr.length()) {
                        val o = accArr.getJSONObject(i)
                        accList.add(
                            AccountEntity(
                                id = o.getLong("id"),
                                name = o.getString("name"),
                                type = o.getString("type"),
                                balance = o.getDouble("balance"),
                                currency = o.getString("currency"),
                                iconName = o.getString("iconName"),
                                colorHex = o.getString("colorHex"),
                                isDefault = o.optBoolean("isDefault", false)
                            )
                        )
                    }
                    database.accountDao().insertAccounts(accList)
                }

                // 3. Transactions
                if (dataObj.has("transactions")) {
                    val txArr = dataObj.getJSONArray("transactions")
                    for (i in 0 until txArr.length()) {
                        val o = txArr.getJSONObject(i)
                        val destAcc = when {
                            !o.isNull("destinationAccountId") -> o.getLong("destinationAccountId")
                            !o.isNull("toAccountId") -> o.getLong("toAccountId")
                            else -> null
                        }
                        val dt = when {
                            !o.isNull("dateTime") -> o.getLong("dateTime")
                            !o.isNull("date") -> o.getLong("date")
                            else -> System.currentTimeMillis()
                        }
                        val created = o.optLong("createdAt", dt)
                        val updated = o.optLong("updatedAt", created)
                        val attUri = if (o.isNull("attachmentUri")) null else o.getString("attachmentUri")
                        database.transactionDao().insertTransaction(
                            TransactionEntity(
                                id = o.getLong("id"),
                                amount = o.getDouble("amount"),
                                type = o.getString("type"),
                                categoryId = o.getLong("categoryId"),
                                accountId = o.getLong("accountId"),
                                destinationAccountId = destAcc,
                                dateTime = dt,
                                note = o.optString("note", ""),
                                attachmentUri = attUri,
                                createdAt = created,
                                updatedAt = updated
                            )
                        )
                    }
                }

                // 4. Recurring
                if (dataObj.has("recurringTransactions")) {
                    val rArr = dataObj.getJSONArray("recurringTransactions")
                    val rList = mutableListOf<RecurringTransactionEntity>()
                    for (i in 0 until rArr.length()) {
                        val o = rArr.getJSONObject(i)
                        val nextExec = when {
                            !o.isNull("nextExecutionDate") -> o.getLong("nextExecutionDate")
                            !o.isNull("nextDueDate") -> o.getLong("nextDueDate")
                            else -> System.currentTimeMillis()
                        }
                        val isActive = when {
                            !o.isNull("active") -> o.getBoolean("active")
                            !o.isNull("isActive") -> o.getBoolean("isActive")
                            else -> true
                        }
                        rList.add(
                            RecurringTransactionEntity(
                                id = o.getLong("id"),
                                name = o.getString("name"),
                                amount = o.getDouble("amount"),
                                type = o.getString("type"),
                                categoryId = o.getLong("categoryId"),
                                accountId = o.getLong("accountId"),
                                frequency = o.getString("frequency"),
                                nextExecutionDate = nextExec,
                                active = isActive,
                                note = o.optString("note", "")
                            )
                        )
                    }
                    database.recurringTransactionDao().insertRecurringList(rList)
                }

                // 5. Budgets
                if (dataObj.has("budgets")) {
                    val bArr = dataObj.getJSONArray("budgets")
                    val bList = mutableListOf<BudgetEntity>()
                    for (i in 0 until bArr.length()) {
                        val o = bArr.getJSONObject(i)
                        val catId = if (o.isNull("categoryId")) null else o.getLong("categoryId")
                        bList.add(
                            BudgetEntity(
                                id = o.getLong("id"),
                                name = o.getString("name"),
                                amount = o.getDouble("amount"),
                                period = o.getString("period"),
                                categoryId = catId,
                                startDate = o.getLong("startDate"),
                                endDate = o.getLong("endDate"),
                                active = o.optBoolean("active", true)
                            )
                        )
                    }
                    database.budgetDao().insertBudgets(bList)
                }

                // 6. Goals & Contributions
                if (dataObj.has("goals")) {
                    val gArr = dataObj.getJSONArray("goals")
                    val gList = mutableListOf<GoalEntity>()
                    for (i in 0 until gArr.length()) {
                        val o = gArr.getJSONObject(i)
                        val targetDate = if (o.isNull("targetDate")) null else o.getLong("targetDate")
                        val linkedAcc = if (o.isNull("linkedAccountId")) null else o.getLong("linkedAccountId")
                        gList.add(
                            GoalEntity(
                                id = o.getLong("id"),
                                name = o.getString("name"),
                                targetAmount = o.getDouble("targetAmount"),
                                currentAmount = o.getDouble("currentAmount"),
                                currency = o.getString("currency"),
                                targetDate = targetDate,
                                createdDate = o.getLong("createdDate"),
                                linkedAccountId = linkedAcc,
                                iconName = o.getString("iconName"),
                                status = o.getString("status")
                            )
                        )
                    }
                    database.goalDao().insertGoals(gList)
                }

                if (dataObj.has("goalContributions")) {
                    val gcArr = dataObj.getJSONArray("goalContributions")
                    val gcList = mutableListOf<GoalContributionEntity>()
                    for (i in 0 until gcArr.length()) {
                        val o = gcArr.getJSONObject(i)
                        gcList.add(
                            GoalContributionEntity(
                                id = o.getLong("id"),
                                goalId = o.getLong("goalId"),
                                amount = o.getDouble("amount"),
                                date = o.getLong("date"),
                                note = o.optString("note", "")
                            )
                        )
                    }
                    database.goalDao().insertContributions(gcList)
                }

                // 7. Credit Cards
                if (dataObj.has("creditCards")) {
                    val ccArr = dataObj.getJSONArray("creditCards")
                    val ccList = mutableListOf<CreditCardEntity>()
                    for (i in 0 until ccArr.length()) {
                        val o = ccArr.getJSONObject(i)
                        val linkedAcc = if (o.isNull("linkedAccountId")) null else o.getLong("linkedAccountId")
                        ccList.add(
                            CreditCardEntity(
                                id = o.getLong("id"),
                                name = o.getString("name"),
                                bankName = o.getString("bankName"),
                                cardNumberMasked = o.getString("cardNumberMasked"),
                                creditLimit = o.getDouble("creditLimit"),
                                outstandingBalance = o.getDouble("outstandingBalance"),
                                billingCycleDay = o.getInt("billingCycleDay"),
                                paymentDueDay = o.getInt("paymentDueDay"),
                                linkedAccountId = linkedAcc,
                                currency = o.getString("currency"),
                                cardColorHex = o.getString("cardColorHex"),
                                status = o.getString("status")
                            )
                        )
                    }
                    database.creditCardDao().insertCards(ccList)
                }

                // 8. Loans & Payments
                if (dataObj.has("loans")) {
                    val lArr = dataObj.getJSONArray("loans")
                    val lList = mutableListOf<LoanEntity>()
                    for (i in 0 until lArr.length()) {
                        val o = lArr.getJSONObject(i)
                        val endDate = if (o.isNull("endDate")) null else o.getLong("endDate")
                        val linkedAcc = if (o.isNull("linkedAccountId")) null else o.getLong("linkedAccountId")
                        lList.add(
                            LoanEntity(
                                id = o.getLong("id"),
                                name = o.getString("name"),
                                lender = o.getString("lender"),
                                loanType = o.getString("loanType"),
                                originalAmount = o.getDouble("originalAmount"),
                                remainingBalance = o.getDouble("remainingBalance"),
                                interestRate = o.getDouble("interestRate"),
                                monthlyPayment = o.getDouble("monthlyPayment"),
                                nextPaymentDate = o.getLong("nextPaymentDate"),
                                startDate = o.getLong("startDate"),
                                endDate = endDate,
                                currency = o.getString("currency"),
                                status = o.getString("status"),
                                linkedAccountId = linkedAcc
                            )
                        )
                    }
                    database.loanDao().insertLoans(lList)
                }

                if (dataObj.has("loanPayments")) {
                    val lpArr = dataObj.getJSONArray("loanPayments")
                    val lpList = mutableListOf<LoanPaymentEntity>()
                    for (i in 0 until lpArr.length()) {
                        val o = lpArr.getJSONObject(i)
                        val accId = if (o.isNull("accountId")) null else o.getLong("accountId")
                        lpList.add(
                            LoanPaymentEntity(
                                id = o.getLong("id"),
                                loanId = o.getLong("loanId"),
                                amount = o.getDouble("amount"),
                                principalAmount = o.getDouble("principalAmount"),
                                interestAmount = o.getDouble("interestAmount"),
                                paymentDate = o.getLong("paymentDate"),
                                accountId = accId,
                                note = o.optString("note", "")
                            )
                        )
                    }
                    database.loanDao().insertPayments(lpList)
                }

                // 9. Net Worth Snapshots
                if (dataObj.has("netWorthSnapshots")) {
                    val nwArr = dataObj.getJSONArray("netWorthSnapshots")
                    val nwList = mutableListOf<NetWorthSnapshotEntity>()
                    for (i in 0 until nwArr.length()) {
                        val o = nwArr.getJSONObject(i)
                        nwList.add(
                            NetWorthSnapshotEntity(
                                id = o.getLong("id"),
                                date = o.getLong("date"),
                                totalAssets = o.getDouble("totalAssets"),
                                totalLiabilities = o.getDouble("totalLiabilities"),
                                netWorth = o.getDouble("netWorth")
                            )
                        )
                    }
                    database.netWorthDao().insertSnapshots(nwList)
                }

                // 10. Saved Filters
                if (dataObj.has("savedFilters")) {
                    val sfArr = dataObj.getJSONArray("savedFilters")
                    val sfList = mutableListOf<SavedFilterEntity>()
                    for (i in 0 until sfArr.length()) {
                        val o = sfArr.getJSONObject(i)
                        sfList.add(
                            SavedFilterEntity(
                                id = o.getLong("id"),
                                name = o.getString("name"),
                                datePreset = o.getString("datePreset"),
                                customStartDate = if (o.isNull("customStartDate")) null else o.getLong("customStartDate"),
                                customEndDate = if (o.isNull("customEndDate")) null else o.getLong("customEndDate"),
                                transactionType = if (o.isNull("transactionType")) null else o.getString("transactionType"),
                                minAmount = if (o.isNull("minAmount")) null else o.getDouble("minAmount"),
                                maxAmount = if (o.isNull("maxAmount")) null else o.getDouble("maxAmount"),
                                categoryIds = o.getString("categoryIds"),
                                accountIds = o.getString("accountIds"),
                                sourceFilter = o.getString("sourceFilter"),
                                hasNote = o.optBoolean("hasNote", false),
                                searchQuery = o.getString("searchQuery")
                            )
                        )
                    }
                    database.savedFilterDao().insertSavedFilters(sfList)
                }

                // 10b. Split with Friends (absent in backups made before this feature existed)
                if (dataObj.has("splitFriends")) {
                    val arr = dataObj.getJSONArray("splitFriends")
                    database.splitDao().insertFriends((0 until arr.length()).map { i ->
                        val o = arr.getJSONObject(i)
                        SplitFriendEntity(o.getLong("id"), o.getString("name"), o.optLong("createdAt", System.currentTimeMillis()))
                    })
                }
                if (dataObj.has("splitExpenses")) {
                    val arr = dataObj.getJSONArray("splitExpenses")
                    database.splitDao().insertExpenses((0 until arr.length()).map { i ->
                        val o = arr.getJSONObject(i)
                        SplitExpenseEntity(
                            id = o.getLong("id"),
                            description = o.getString("description"),
                            totalAmount = o.getDouble("totalAmount"),
                            paidByFriendId = if (o.isNull("paidByFriendId")) null else o.getLong("paidByFriendId"),
                            splitType = o.optString("splitType", "EQUAL"),
                            dateTime = o.getLong("dateTime"),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    })
                }
                if (dataObj.has("splitShares")) {
                    val arr = dataObj.getJSONArray("splitShares")
                    database.splitDao().insertShares((0 until arr.length()).map { i ->
                        val o = arr.getJSONObject(i)
                        SplitShareEntity(
                            id = o.getLong("id"),
                            expenseId = o.getLong("expenseId"),
                            friendId = if (o.isNull("friendId")) null else o.getLong("friendId"),
                            shareAmount = o.getDouble("shareAmount")
                        )
                    })
                }
                if (dataObj.has("splitSettlements")) {
                    val arr = dataObj.getJSONArray("splitSettlements")
                    database.splitDao().insertSettlements((0 until arr.length()).map { i ->
                        val o = arr.getJSONObject(i)
                        SplitSettlementEntity(
                            id = o.getLong("id"),
                            friendId = o.getLong("friendId"),
                            amount = o.getDouble("amount"),
                            friendPaidMe = o.getBoolean("friendPaidMe"),
                            dateTime = o.getLong("dateTime"),
                            note = o.optString("note", "")
                        )
                    })
                }

                // 11. User Settings
                if (dataObj.has("userSettings")) {
                    val o = dataObj.getJSONObject("userSettings")
                    val settings = UserSettingsEntity(
                        id = o.optInt("id", 1),
                        userName = o.optString("userName", "Kshitij"),
                        defaultCurrency = o.optString("defaultCurrency", "₹"),
                        dailyBudget = o.optDouble("dailyBudget", 1000.0),
                        monthlyBudget = o.optDouble("monthlyBudget", 30000.0),
                        themeMode = o.optString("themeMode", "SYSTEM"),
                        hasCompletedOnboarding = o.optBoolean("hasCompletedOnboarding", true),
                        enableHaptics = o.optBoolean("enableHaptics", true),
                        hideAmounts = o.optBoolean("hideAmounts", false),
                        hideAmountsPersist = o.optBoolean("hideAmountsPersist", true),
                        appLockEnabled = o.optBoolean("appLockEnabled", false),
                        appLockTimeoutMinutes = o.optInt("appLockTimeoutMinutes", 0),
                        homeSectionsOrder = o.optString("homeSectionsOrder", "spending,budget,recent,insight,upcoming,accounts"),
                        hiddenHomeSections = o.optString("hiddenHomeSections", ""),
                        prestigeTheme = o.optString("prestigeTheme", "OBSIDIAN_GOLD"),
                        heroFont = o.optString("heroFont", "LUXURY_SERIF")
                    )
                    database.userSettingsDao().insertOrUpdate(settings)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
