package com.example.feature.insights

import com.example.domain.model.TransactionDetail
import com.example.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class MonthlyStoryData(
    val monthName: String,
    val totalExpense: Double,
    val totalIncome: Double,
    val netSavings: Double,
    val savingsRate: Int,
    val expenseDiffPercent: Double, // compared to last month
    val topCategoryName: String?,
    val topCategoryAmount: Double,
    val topCategoryPercentage: Int,
    val topCategoryIcon: String,
    val topCategoryColor: String,
    val biggestTransaction: TransactionDetail?,
    val topMerchantName: String?,
    val topMerchantCount: Int,
    val topMerchantAmount: Double,
    val peakSpendDayName: String,
    val peakSpendDayAmount: Double,
    val totalTransactionsCount: Int,
    val personaBadge: String,
    val personaDescription: String
)

object MonthlyStoryGenerator {

    fun generateStory(
        transactions: List<TransactionDetail>,
        currency: String = "₹"
    ): MonthlyStoryData {
        val now = Calendar.getInstance()
        val currentMonth = now.get(Calendar.MONTH)
        val currentYear = now.get(Calendar.YEAR)

        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val monthName = monthFormat.format(now.time)

        val cal = Calendar.getInstance()
        val thisMonthTxs = transactions.filter {
            cal.timeInMillis = it.transaction.dateTime
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }

        val lastMonth = if (currentMonth == 0) 11 else currentMonth - 1
        val lastMonthYear = if (currentMonth == 0) currentYear - 1 else currentYear
        val lastMonthTxs = transactions.filter {
            cal.timeInMillis = it.transaction.dateTime
            cal.get(Calendar.MONTH) == lastMonth && cal.get(Calendar.YEAR) == lastMonthYear
        }

        val thisMonthExpenses = thisMonthTxs.filter { it.transaction.type == TransactionType.EXPENSE }
        val thisMonthIncome = thisMonthTxs.filter { it.transaction.type == TransactionType.INCOME }

        val totalExpense = thisMonthExpenses.sumOf { it.transaction.amount }
        val totalIncome = thisMonthIncome.sumOf { it.transaction.amount }
        val netSavings = (totalIncome - totalExpense).coerceAtLeast(0.0)
        val savingsRate = if (totalIncome > 0) ((netSavings / totalIncome) * 100).toInt() else 0

        val lastMonthExpense = lastMonthTxs.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.amount }
        val expenseDiffPercent = if (lastMonthExpense > 0) {
            ((totalExpense - lastMonthExpense) / lastMonthExpense) * 100
        } else 0.0

        // Top Category
        val catGroups = thisMonthExpenses.groupBy { it.category?.name ?: "Other" }
        val topCatEntry = catGroups.maxByOrNull { entry -> entry.value.sumOf { it.transaction.amount } }
        val topCategoryName = topCatEntry?.key
        val topCategoryAmount = topCatEntry?.value?.sumOf { it.transaction.amount } ?: 0.0
        val topCategoryPercentage = if (totalExpense > 0) ((topCategoryAmount / totalExpense) * 100).toInt() else 0
        val topCategoryIcon = topCatEntry?.value?.firstOrNull()?.category?.iconName ?: "ShoppingBag"
        val topCategoryColor = topCatEntry?.value?.firstOrNull()?.category?.colorHex ?: "#E5C378"

        // Biggest Transaction
        val biggestTransaction = thisMonthExpenses.maxByOrNull { it.transaction.amount }

        // Top Merchant
        val merchantGroups = thisMonthExpenses
            .filter { it.transaction.note.isNotBlank() }
            .groupBy { it.transaction.note.trim() }
        val topMerchantEntry = merchantGroups.maxByOrNull { it.value.size }
        val topMerchantName = topMerchantEntry?.key
        val topMerchantCount = topMerchantEntry?.value?.size ?: 0
        val topMerchantAmount = topMerchantEntry?.value?.sumOf { it.transaction.amount } ?: 0.0

        // Peak Day of Week
        val dayNames = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val dayAmounts = DoubleArray(7)
        thisMonthExpenses.forEach {
            cal.timeInMillis = it.transaction.dateTime
            val dayIdx = cal.get(Calendar.DAY_OF_WEEK) - 1
            if (dayIdx in 0..6) {
                dayAmounts[dayIdx] += it.transaction.amount
            }
        }
        var maxDayIdx = 0
        var maxDayAmt = 0.0
        for (i in 0..6) {
            if (dayAmounts[i] > maxDayAmt) {
                maxDayAmt = dayAmounts[i]
                maxDayIdx = i
            }
        }
        val peakSpendDayName = if (maxDayAmt > 0) dayNames[maxDayIdx] else "Weekend"

        // Persona calculation
        val (badge, desc) = when {
            savingsRate >= 40 -> "💎 Diamond Saver" to "You maintained a stellar ${savingsRate}% savings rate! True financial freedom."
            savingsRate >= 20 -> "🛡️ Conscious Builder" to "Solid financial balance with ${savingsRate}% of income tucked away safely."
            topCategoryPercentage >= 45 -> "🎯 Focused Enthusiast" to "A huge chunk ($topCategoryPercentage%) went towards $topCategoryName."
            thisMonthTxs.size >= 30 -> "⚡ High Velocity Active" to "You logged ${thisMonthTxs.size} transactions with disciplined tracking."
            else -> "🌟 Flow Master" to "Consistent money management through conscious tracking and intentional spending."
        }

        return MonthlyStoryData(
            monthName = monthName,
            totalExpense = totalExpense,
            totalIncome = totalIncome,
            netSavings = netSavings,
            savingsRate = savingsRate,
            expenseDiffPercent = expenseDiffPercent,
            topCategoryName = topCategoryName,
            topCategoryAmount = topCategoryAmount,
            topCategoryPercentage = topCategoryPercentage,
            topCategoryIcon = topCategoryIcon,
            topCategoryColor = topCategoryColor,
            biggestTransaction = biggestTransaction,
            topMerchantName = topMerchantName,
            topMerchantCount = topMerchantCount,
            topMerchantAmount = topMerchantAmount,
            peakSpendDayName = peakSpendDayName,
            peakSpendDayAmount = maxDayAmt,
            totalTransactionsCount = thisMonthTxs.size,
            personaBadge = badge,
            personaDescription = desc
        )
    }
}
