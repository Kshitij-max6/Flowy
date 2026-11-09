package com.example

import com.example.core.util.VoiceExpenseParser
import com.example.domain.model.Account
import com.example.domain.model.AccountType
import com.example.domain.model.Category
import com.example.domain.model.CategoryType
import com.example.domain.model.ExpenseBook
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionDetail
import com.example.domain.model.TransactionType
import com.example.feature.insights.MonthlyStoryGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    private val testCategories = listOf(
        Category(id = 1, name = "Food & Dining", iconName = "Restaurant", colorHex = "#10B981", type = CategoryType.EXPENSE),
        Category(id = 2, name = "Coffee", iconName = "LocalCafe", colorHex = "#8B5CF6", type = CategoryType.EXPENSE),
        Category(id = 3, name = "Groceries", iconName = "ShoppingCart", colorHex = "#F59E0B", type = CategoryType.EXPENSE),
        Category(id = 4, name = "Salary", iconName = "AttachMoney", colorHex = "#10B981", type = CategoryType.INCOME),
        Category(id = 5, name = "Travel", iconName = "Flight", colorHex = "#3B82F6", type = CategoryType.EXPENSE)
    )

    private val testAccounts = listOf(
        Account(id = 1, name = "HDFC Bank", type = AccountType.BANK, balance = 50000.0, colorHex = "#1E3A8A"),
        Account(id = 2, name = "Google Pay UPI", type = AccountType.UPI, balance = 12000.0, colorHex = "#3B82F6"),
        Account(id = 3, name = "Cash", type = AccountType.CASH, balance = 3500.0, colorHex = "#10B981")
    )

    private val testBooks = listOf(
        ExpenseBook(id = 1, name = "Personal", iconName = "Wallet", colorHex = "#E5C378"),
        ExpenseBook(id = 2, name = "Japan Trip", iconName = "Flight", colorHex = "#38BDF8"),
        ExpenseBook(id = 3, name = "Business", iconName = "Work", colorHex = "#A78BFA")
    )

    @Test
    fun `parse coffee voice sentence correctly`() {
        val input = "Spent 250 on coffee via Google Pay for Japan Trip"
        val parsed = VoiceExpenseParser.parse(
            spokenText = input,
            categories = testCategories,
            accounts = testAccounts,
            books = testBooks
        )

        assertEquals(250.0, parsed.amount ?: 0.0, 0.001)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals(2L, parsed.matchedCategory?.id) // Coffee
        assertEquals(2L, parsed.matchedAccount?.id) // Google Pay UPI
        assertEquals(2L, parsed.matchedBook?.id) // Japan Trip
    }

    @Test
    fun `parse salary income voice sentence correctly`() {
        val input = "Received 85000 salary in HDFC Bank"
        val parsed = VoiceExpenseParser.parse(
            spokenText = input,
            categories = testCategories,
            accounts = testAccounts,
            books = testBooks
        )

        assertEquals(85000.0, parsed.amount ?: 0.0, 0.001)
        assertEquals(TransactionType.INCOME, parsed.type)
        assertEquals(4L, parsed.matchedCategory?.id) // Salary
        assertEquals(1L, parsed.matchedAccount?.id) // HDFC Bank
    }

    @Test
    fun `monthly story generator handles transactions correctly`() {
        val now = System.currentTimeMillis()
        val txs = listOf(
            TransactionDetail(
                transaction = Transaction(
                    id = 1,
                    amount = 450.0,
                    type = TransactionType.EXPENSE,
                    categoryId = 1,
                    accountId = 1,
                    dateTime = now,
                    note = "Starbucks"
                ),
                category = testCategories[0],
                sourceAccount = testAccounts[0]
            ),
            TransactionDetail(
                transaction = Transaction(
                    id = 2,
                    amount = 75000.0,
                    type = TransactionType.INCOME,
                    categoryId = 4,
                    accountId = 1,
                    dateTime = now
                ),
                category = testCategories[3],
                sourceAccount = testAccounts[0]
            )
        )

        val story = MonthlyStoryGenerator.generateStory(txs, "₹")
        assertNotNull(story)
        assertEquals(450.0, story.totalExpense, 0.001)
        assertEquals(75000.0, story.totalIncome, 0.001)
        assertEquals(74550.0, story.netSavings, 0.001)
    }
}
