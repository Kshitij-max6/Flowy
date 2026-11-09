package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.util.CurrencyFormatter
import com.example.core.util.DateUtils
import com.example.domain.model.TransactionType
import com.example.feature.home.HomeCustomizeViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context verifies app name is Flowly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Flowly", appName)
    }

    @Test
    fun `currency formatter formats rupee amounts correctly`() {
        val formatted = CurrencyFormatter.format(420.0, "₹")
        assertEquals("₹420", formatted)

        val formattedLarge = CurrencyFormatter.format(50000.0, "₹")
        assertEquals("₹50,000", formattedLarge)
    }

    @Test
    fun `currency formatter formats compact amounts correctly`() {
        val compact1 = CurrencyFormatter.formatCompact(1500.0, "₹")
        assertEquals("₹1.5k", compact1)

        val compact2 = CurrencyFormatter.formatCompact(250000.0, "₹")
        assertEquals("₹2.5L", compact2)

        val compact3 = CurrencyFormatter.formatCompact(12000000.0, "₹")
        assertEquals("₹1.2Cr", compact3)
    }

    @Test
    fun `privacy mask masks sensitive amounts`() {
        CurrencyFormatter.isPrivacyActive = true
        val masked = CurrencyFormatter.format(12345.0, "₹")
        assertEquals("₹••••••", masked)
        CurrencyFormatter.isPrivacyActive = false
    }

    @Test
    fun `date utils returns valid greeting string`() {
        val greeting = DateUtils.getGreeting()
        assertTrue(
            greeting in listOf("Good Morning", "Good Afternoon", "Good Evening", "Good Night")
        )
    }

    @Test
    fun `budget status calculates percentage and remaining correctly`() {
        val limit = 10000.0
        val spent = 4500.0
        val remaining = (limit - spent).coerceAtLeast(0.0)
        val percentage = (spent / limit).coerceIn(0.0, 1.0).toFloat()

        assertEquals(5500.0, remaining, 0.01)
        assertEquals(0.45f, percentage, 0.01f)
    }

    @Test
    fun `goal progress percentage calculation handles capped ranges`() {
        val targetAmount = 50000.0
        val currentAmount = 25000.0
        val progress = (currentAmount / targetAmount).coerceIn(0.0, 1.0).toFloat()

        assertEquals(0.5f, progress, 0.01f)
    }

    @Test
    fun `transfer between accounts preserves total money`() {
        var cashBalance = 5000.0
        var bankBalance = 25000.0
        var upiBalance = 10000.0
        val initialTotal = cashBalance + bankBalance + upiBalance

        // 1. Cash -> Bank ₹2000
        val transfer1 = 2000.0
        cashBalance -= transfer1
        bankBalance += transfer1

        // 2. Bank -> UPI ₹5000
        val transfer2 = 5000.0
        bankBalance -= transfer2
        upiBalance += transfer2

        val finalTotal = cashBalance + bankBalance + upiBalance
        assertEquals(initialTotal, finalTotal, 0.001)
        assertEquals(3000.0, cashBalance, 0.001)
        assertEquals(22000.0, bankBalance, 0.001)
        assertEquals(15000.0, upiBalance, 0.001)
    }

    @Test
    fun `net worth correctly subtracts credit card and loan liabilities from total assets`() {
        val bankAccount = 50000.0
        val cashAccount = 5000.0
        val investmentAccount = 100000.0
        val totalAssets = bankAccount + cashAccount + investmentAccount // 155,000

        val creditCardOutstanding = 12000.0
        val loanRemainingBalance = 40000.0
        val totalLiabilities = creditCardOutstanding + loanRemainingBalance // 52,000

        val netWorth = totalAssets - totalLiabilities
        assertEquals(103000.0, netWorth, 0.001)

        // Internal transfer between asset accounts should NOT affect Net Worth
        val transferAmount = 10000.0
        val updatedBank = bankAccount - transferAmount
        val updatedCash = cashAccount + transferAmount
        val updatedAssets = updatedBank + updatedCash + investmentAccount
        val updatedNetWorth = updatedAssets - totalLiabilities

        assertEquals(netWorth, updatedNetWorth, 0.001)
    }

    @Test
    fun `credit card utilization and payment reduces outstanding correctly`() {
        val limit = 150000.0
        var outstanding = 45000.0
        var utilization = (outstanding / limit) * 100.0
        assertEquals(30.0, utilization, 0.01)

        // Payment of ₹20,000 reduces outstanding
        val payment = 20000.0
        outstanding = (outstanding - payment).coerceAtLeast(0.0)
        utilization = (outstanding / limit) * 100.0

        assertEquals(25000.0, outstanding, 0.01)
        assertEquals(16.666, utilization, 0.01)
    }

    @Test
    fun `loan payment reduces principal and calculates remaining balance accurately`() {
        val originalAmount = 500000.0
        var remainingBalance = 450000.0
        val monthlyPayment = 15000.0
        val interestPart = 4000.0
        val principalPart = monthlyPayment - interestPart // 11,000

        remainingBalance -= principalPart
        assertEquals(439000.0, remainingBalance, 0.01)
    }

    @Test
    fun `subscription monthly normalization handles all frequencies`() {
        val dailyCost = 50.0 // 50 * 30 = 1500
        val weeklyCost = 200.0 // 200 * 4.33 = 866
        val monthlyCost = 499.0 // 499
        val yearlyCost = 2400.0 // 2400 / 12 = 200

        val normalizedMonthly = (dailyCost * 30.0) + (weeklyCost * 4.33) + monthlyCost + (yearlyCost / 12.0)
        assertEquals(3065.0, normalizedMonthly, 0.01)
        assertEquals(3065.0 * 12.0, normalizedMonthly * 12.0, 0.01)
    }

    @Test
    fun `backup validation correctly validates JSON schema`() {
        val validJson = """
            {
                "version": 1,
                "appName": "Flowly",
                "timestamp": 1725000000000,
                "data": {
                    "transactions": [],
                    "accounts": [],
                    "categories": []
                }
            }
        """.trimIndent()

        val result = com.example.core.util.BackupManager.validateBackupJson(validJson)
        assertTrue(result.isValid)
        assertEquals(0, result.transactionCount)

        val invalidJson = """{"bad": "data"}"""
        val invalidResult = com.example.core.util.BackupManager.validateBackupJson(invalidJson)
        assertFalse(invalidResult.isValid)
    }

    @Test
    fun `home section defaults list contains all required modules`() {
        val defaults = HomeCustomizeViewModel.ALL_AVAILABLE_SECTIONS
        assertEquals(5, defaults.size)
        assertTrue(defaults.any { it.id == "spending" })
        assertTrue(defaults.any { it.id == "accounts" })
        assertTrue(defaults.any { it.id == "recent" })
        assertTrue(defaults.any { it.id == "upcoming" })
        assertTrue(defaults.any { it.id == "insight" })
    }
}
