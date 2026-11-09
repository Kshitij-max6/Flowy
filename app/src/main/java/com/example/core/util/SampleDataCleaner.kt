package com.example.core.util

import com.example.data.local.AppDatabase
import com.example.data.local.DatabasePrepopulate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Removes the old demo data that earlier versions of Flowly put into every new install
 * (sample transactions, budgets, goals, cards, loans, recurring items, books, accounts...).
 *
 * Safety rules, so nothing the user typed is ever deleted:
 *  - a row is removed only if BOTH its id and its distinctive content (note / name / amount) match the demo row;
 *  - a demo account or book is removed only if nothing remaining still uses it;
 *  - nothing is touched when the user has no demo rows (returns 0).
 */
object SampleDataCleaner {

    suspend fun removeSampleData(database: AppDatabase): Int = withContext(Dispatchers.IO) {
        val db = database.openHelper.writableDatabase
        var removed = 0
        db.beginTransaction()
        try {
            fun del(table: String, id: Long, column: String, value: Any): Int =
                db.delete(table, "id = ? AND $column = ?", arrayOf(id.toString(), value.toString()))

            DatabasePrepopulate.getInitialSampleTransactions().forEach { removed += del("transactions", it.id, "note", it.note) }
            DatabasePrepopulate.getDefaultRecurring().forEach { removed += del("recurring_transactions", it.id, "name", it.name) }
            DatabasePrepopulate.getDefaultBudgets().forEach { removed += del("budgets", it.id, "name", it.name) }
            DatabasePrepopulate.getDefaultGoalContributions().forEach { removed += del("goal_contributions", it.id, "amount", it.amount) }
            DatabasePrepopulate.getDefaultGoals().forEach { removed += del("goals", it.id, "name", it.name) }
            DatabasePrepopulate.getDefaultLoanPayments().forEach { removed += del("loan_payments", it.id, "amount", it.amount) }
            DatabasePrepopulate.getDefaultLoans().forEach { removed += del("loans", it.id, "name", it.name) }
            DatabasePrepopulate.getDefaultCreditCards().forEach { removed += del("credit_cards", it.id, "name", it.name) }
            DatabasePrepopulate.getDefaultNetWorthSnapshots().forEach { removed += del("net_worth_snapshots", it.id, "netWorth", it.netWorth) }
            DatabasePrepopulate.getDefaultSavedFilters().forEach { removed += del("saved_transaction_filters", it.id, "name", it.name) }

            // Demo books (never the default ledger id 1) that nothing uses any more
            DatabasePrepopulate.getDefaultExpenseBooks().filter { it.id != 1L }.forEach { book ->
                db.execSQL(
                    "DELETE FROM expense_books WHERE id = ? AND name = ? AND NOT EXISTS (SELECT 1 FROM transactions WHERE bookId = expense_books.id)",
                    arrayOf<Any>(book.id, book.name)
                )
            }
            // Default ledger: drop the demo budget figure, keep the book itself
            db.execSQL("UPDATE expense_books SET budget = 0, description = '' WHERE id = 1 AND name = 'Personal & Daily' AND budget = 40000.0")

            // Demo accounts that no remaining record refers to
            DatabasePrepopulate.getDefaultAccounts().forEach { acc ->
                db.execSQL(
                    """DELETE FROM accounts WHERE id = ? AND name = ?
                       AND NOT EXISTS (SELECT 1 FROM transactions WHERE accountId = accounts.id OR destinationAccountId = accounts.id)
                       AND NOT EXISTS (SELECT 1 FROM recurring_transactions WHERE accountId = accounts.id)
                       AND NOT EXISTS (SELECT 1 FROM loan_payments WHERE accountId = accounts.id)
                       AND NOT EXISTS (SELECT 1 FROM goals WHERE linkedAccountId = accounts.id)
                       AND NOT EXISTS (SELECT 1 FROM loans WHERE linkedAccountId = accounts.id)
                       AND NOT EXISTS (SELECT 1 FROM credit_cards WHERE linkedAccountId = accounts.id)""",
                    arrayOf<Any>(acc.id, acc.name)
                )
            }
            // Never leave the user with zero accounts
            db.execSQL(
                """INSERT INTO accounts (name, type, balance, currency, iconName, colorHex, isDefault)
                   SELECT 'Cash', 'CASH', 0.0, '₹', 'Payments', '#10B981', 1
                   WHERE NOT EXISTS (SELECT 1 FROM accounts)"""
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        removed
    }

    /** True if any demo transaction is still present (used to decide whether to show the Settings row). */
    suspend fun hasSampleData(database: AppDatabase): Boolean = withContext(Dispatchers.IO) {
        val db = database.openHelper.readableDatabase
        DatabasePrepopulate.getInitialSampleTransactions().any { tx ->
            db.query("SELECT 1 FROM transactions WHERE id = ? AND note = ?", arrayOf<Any>(tx.id, tx.note)).use { it.moveToFirst() }
        }
    }
}
