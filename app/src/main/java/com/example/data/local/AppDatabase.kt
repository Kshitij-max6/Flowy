package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AccountDao
import com.example.data.local.dao.BudgetDao
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.CreditCardDao
import com.example.data.local.dao.ExpenseBookDao
import com.example.data.local.dao.GoalDao
import com.example.data.local.dao.LoanDao
import com.example.data.local.dao.NetWorthDao
import com.example.data.local.dao.RecentSearchDao
import com.example.data.local.dao.RecurringTransactionDao
import com.example.data.local.dao.SavedFilterDao
import com.example.data.local.dao.SplitDao
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
import com.example.data.local.entity.RecentSearchEntity
import com.example.data.local.entity.RecurringTransactionEntity
import com.example.data.local.entity.SavedFilterEntity
import com.example.data.local.entity.SplitExpenseEntity
import com.example.data.local.entity.SplitFriendEntity
import com.example.data.local.entity.SplitSettlementEntity
import com.example.data.local.entity.SplitShareEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserSettingsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CategoryEntity::class,
        AccountEntity::class,
        TransactionEntity::class,
        RecurringTransactionEntity::class,
        UserSettingsEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
        GoalContributionEntity::class,
        CreditCardEntity::class,
        LoanEntity::class,
        LoanPaymentEntity::class,
        NetWorthSnapshotEntity::class,
        SavedFilterEntity::class,
        RecentSearchEntity::class,
        ExpenseBookEntity::class,
        SplitFriendEntity::class,
        SplitExpenseEntity::class,
        SplitShareEntity::class,
        SplitSettlementEntity::class
    ],
    version = AppDatabase.DB_VERSION,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao
    abstract fun creditCardDao(): CreditCardDao
    abstract fun loanDao(): LoanDao
    abstract fun netWorthDao(): NetWorthDao
    abstract fun savedFilterDao(): SavedFilterDao
    abstract fun recentSearchDao(): RecentSearchDao
    abstract fun expenseBookDao(): ExpenseBookDao
    abstract fun splitDao(): SplitDao

    companion object {
        /** Single source of truth for the schema version (used by @Database and by recovery checks). */
        const val DB_VERSION = 9

        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `split_friends` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `split_expenses` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `description` TEXT NOT NULL,
                        `totalAmount` REAL NOT NULL,
                        `paidByFriendId` INTEGER,
                        `splitType` TEXT NOT NULL,
                        `dateTime` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `split_shares` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `expenseId` INTEGER NOT NULL,
                        `friendId` INTEGER,
                        `shareAmount` REAL NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_split_shares_expenseId` ON `split_shares` (`expenseId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_split_shares_friendId` ON `split_shares` (`friendId`)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `split_settlements` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `friendId` INTEGER NOT NULL,
                        `amount` REAL NOT NULL,
                        `friendPaidMe` INTEGER NOT NULL,
                        `dateTime` INTEGER NOT NULL,
                        `note` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_split_settlements_friendId` ON `split_settlements` (`friendId`)")
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create budgets table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `budgets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `period` TEXT NOT NULL,
                        `categoryId` INTEGER,
                        `startDate` INTEGER NOT NULL,
                        `endDate` INTEGER NOT NULL,
                        `active` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_budgets_categoryId` ON `budgets` (`categoryId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_budgets_startDate` ON `budgets` (`startDate`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_budgets_endDate` ON `budgets` (`endDate`)")

                // Create goals table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `goals` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `targetAmount` REAL NOT NULL,
                        `currentAmount` REAL NOT NULL,
                        `currency` TEXT NOT NULL,
                        `targetDate` INTEGER,
                        `createdDate` INTEGER NOT NULL,
                        `linkedAccountId` INTEGER,
                        `iconName` TEXT NOT NULL,
                        `status` TEXT NOT NULL
                    )
                """.trimIndent())

                // Create goal_contributions table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `goal_contributions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `goalId` INTEGER NOT NULL,
                        `amount` REAL NOT NULL,
                        `date` INTEGER NOT NULL,
                        `note` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_contributions_goalId` ON `goal_contributions` (`goalId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_contributions_date` ON `goal_contributions` (`date`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create credit_cards table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `credit_cards` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `bankName` TEXT NOT NULL,
                        `cardNumberMasked` TEXT NOT NULL,
                        `creditLimit` REAL NOT NULL,
                        `outstandingBalance` REAL NOT NULL,
                        `billingCycleDay` INTEGER NOT NULL,
                        `paymentDueDay` INTEGER NOT NULL,
                        `linkedAccountId` INTEGER,
                        `currency` TEXT NOT NULL,
                        `cardColorHex` TEXT NOT NULL,
                        `status` TEXT NOT NULL
                    )
                """.trimIndent())

                // Create loans table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `loans` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `lender` TEXT NOT NULL,
                        `loanType` TEXT NOT NULL,
                        `originalAmount` REAL NOT NULL,
                        `remainingBalance` REAL NOT NULL,
                        `interestRate` REAL NOT NULL,
                        `monthlyPayment` REAL NOT NULL,
                        `nextPaymentDate` INTEGER NOT NULL,
                        `startDate` INTEGER NOT NULL,
                        `endDate` INTEGER,
                        `currency` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `linkedAccountId` INTEGER
                    )
                """.trimIndent())

                // Create loan_payments table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `loan_payments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `loanId` INTEGER NOT NULL,
                        `amount` REAL NOT NULL,
                        `principalAmount` REAL NOT NULL,
                        `interestAmount` REAL NOT NULL,
                        `paymentDate` INTEGER NOT NULL,
                        `accountId` INTEGER,
                        `note` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_loan_payments_loanId` ON `loan_payments` (`loanId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_loan_payments_paymentDate` ON `loan_payments` (`paymentDate`)")

                // Create net_worth_snapshots table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `net_worth_snapshots` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` INTEGER NOT NULL,
                        `totalAssets` REAL NOT NULL,
                        `totalLiabilities` REAL NOT NULL,
                        `netWorth` REAL NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_net_worth_snapshots_date` ON `net_worth_snapshots` (`date`)")

                // Create saved_transaction_filters table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `saved_transaction_filters` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `datePreset` TEXT NOT NULL,
                        `customStartDate` INTEGER,
                        `customEndDate` INTEGER,
                        `transactionType` TEXT,
                        `minAmount` REAL,
                        `maxAmount` REAL,
                        `categoryIds` TEXT NOT NULL,
                        `accountIds` TEXT NOT NULL,
                        `sourceFilter` TEXT NOT NULL,
                        `hasNote` INTEGER NOT NULL,
                        `searchQuery` TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add privacy & app lock columns to user_settings
                db.execSQL("ALTER TABLE `user_settings` ADD COLUMN `hideAmounts` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `user_settings` ADD COLUMN `hideAmountsPersist` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `user_settings` ADD COLUMN `appLockEnabled` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `user_settings` ADD COLUMN `appLockTimeoutMinutes` INTEGER NOT NULL DEFAULT 0")

                // Create recent_searches table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `recent_searches` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `query` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recent_searches_timestamp` ON `recent_searches` (`timestamp`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `attachmentUri` TEXT")
                db.execSQL("ALTER TABLE `user_settings` ADD COLUMN `homeSectionsOrder` TEXT NOT NULL DEFAULT 'spending,budget,recent,insight,upcoming,accounts'")
                db.execSQL("ALTER TABLE `user_settings` ADD COLUMN `hiddenHomeSections` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `categories` ADD COLUMN `parentCategoryId` INTEGER")
                db.execSQL("ALTER TABLE `categories` ADD COLUMN `isFavorite` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `categories` ADD COLUMN `isArchived` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `categories` ADD COLUMN `sortOrder` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_parentCategoryId` ON `categories` (`parentCategoryId`)")

                // Insert new categories and subcategories
                val newDefaults = DatabasePrepopulate.getDefaultCategories()
                for (cat in newDefaults) {
                    val parentVal = if (cat.parentCategoryId != null) "${cat.parentCategoryId}" else "NULL"
                    val safeName = cat.name.replace("'", "''")
                    val isCust = if (cat.isCustom) 1 else 0
                    val isFav = if (cat.isFavorite) 1 else 0
                    db.execSQL("""
                        INSERT OR IGNORE INTO `categories` (`id`, `name`, `iconName`, `type`, `colorHex`, `isCustom`, `parentCategoryId`, `isFavorite`, `isArchived`, `sortOrder`)
                        VALUES (${cat.id}, '$safeName', '${cat.iconName}', '${cat.type}', '${cat.colorHex}', $isCust, $parentVal, $isFav, 0, ${cat.sortOrder})
                    """.trimIndent())
                }
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `user_settings` ADD COLUMN `prestigeTheme` TEXT NOT NULL DEFAULT 'OBSIDIAN_GOLD'")
                db.execSQL("ALTER TABLE `user_settings` ADD COLUMN `heroFont` TEXT NOT NULL DEFAULT 'LUXURY_SERIF'")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create expense_books table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `expense_books` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `iconName` TEXT NOT NULL,
                        `colorHex` TEXT NOT NULL,
                        `budget` REAL NOT NULL,
                        `isArchived` INTEGER NOT NULL,
                        `isDefault` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())

                // 2. Add bookId column to transactions table
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `bookId` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_bookId` ON `transactions` (`bookId`)")

                // 3. Prepopulate default expense books
                val books = DatabasePrepopulate.getStarterExpenseBooks()
                for (book in books) {
                    val isArch = if (book.isArchived) 1 else 0
                    val isDef = if (book.isDefault) 1 else 0
                    val safeName = book.name.replace("'", "''")
                    val safeDesc = book.description.replace("'", "''")
                    db.execSQL("""
                        INSERT OR IGNORE INTO `expense_books` (`id`, `name`, `description`, `iconName`, `colorHex`, `budget`, `isArchived`, `isDefault`, `createdAt`, `updatedAt`)
                        VALUES (${book.id}, '$safeName', '$safeDesc', '${book.iconName}', '${book.colorHex}', ${book.budget}, $isArch, $isDef, ${book.createdAt}, ${book.updatedAt})
                    """.trimIndent())
                }
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "flowly_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    // No destructive fallback: if the database can't be opened (e.g. after a downgrade),
                    // the recovery screen takes over instead of silently wiping the user's data.
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Pre-populate data on background thread
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { database ->
                                    database.categoryDao().insertCategories(DatabasePrepopulate.getDefaultCategories())
                                    database.accountDao().insertAccounts(DatabasePrepopulate.getStarterAccounts())
                                    database.userSettingsDao().insertOrUpdate(DatabasePrepopulate.getDefaultUserSettings())
                                    database.expenseBookDao().insertBooks(DatabasePrepopulate.getStarterExpenseBooks())
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /** Closes and forgets the cached instance (used by recovery and by "restore pre-update backup"). */
        fun closeAndDiscard() {
            synchronized(this) {
                try { INSTANCE?.close() } catch (_: Exception) { }
                INSTANCE = null
            }
        }

        /**
         * Forces Room to actually open (and migrate) the file now, so a broken database is detected at launch
         * rather than as a crash on the first query. Returns the failure, or null when healthy.
         */
        fun openOrReport(context: Context): Throwable? = try {
            getDatabase(context).openHelper.writableDatabase
            null
        } catch (t: Throwable) {
            closeAndDiscard()
            t
        }
    }
}
