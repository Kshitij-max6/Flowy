package com.example.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.util.CurrencyFormatter
import com.example.core.util.MerchantIntelligenceHelper
import com.example.core.util.PeriodRanges
import com.example.data.repository.FlowlyRepository
import com.example.domain.model.Category
import com.example.domain.model.InsightType
import com.example.domain.model.SpendingInsight
import com.example.domain.model.TransactionDetail
import com.example.domain.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToInt

// =============================================================================
// Analytics 2.0 — state
// =============================================================================

enum class AnalyticsPeriod { WEEK, MONTH, YEAR, CUSTOM }

/** One bar of the spending trend (a day, a week or a month depending on the range). */
data class TrendPoint(val label: String, val amount: Double)

/** Spending of one (parent) category in the selected period, plus the same category in the comparison period. */
data class CategorySpend(
    val category: Category?,
    val name: String,
    val amount: Double,
    val share: Float,
    val previousAmount: Double
) {
    /** Percentage change against the comparison period; null when there is nothing to compare with. */
    val changePercent: Double?
        get() = if (previousAmount > 0.0) (amount - previousAmount) / previousAmount * 100.0 else null
}

data class MerchantSpend(val name: String, val total: Double, val count: Int)

data class AnalyticsUiState(
    val period: AnalyticsPeriod = AnalyticsPeriod.MONTH,
    /** e.g. "1 – 26 Nov 2026" */
    val rangeLabel: String = "",
    /** e.g. "last month" — shown as "vs last month". */
    val comparisonLabel: String = "",
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val previousExpense: Double = 0.0,
    val trend: List<TrendPoint> = emptyList(),
    val transactionCount: Int = 0,
    val dailyAverage: Double = 0.0,
    val largestExpense: Double = 0.0,
    val largestExpenseCategory: String? = null,
    val categories: List<CategorySpend> = emptyList(),
    /** Average spend per weekday, Monday first (7 values). */
    val weekdayAverages: List<Double> = List(7) { 0.0 },
    val topMerchants: List<MerchantSpend> = emptyList(),
    val insights: List<SpendingInsight> = emptyList(),
    val defaultCurrency: String = "₹",
    val isLoading: Boolean = true
)

internal data class Selection(
    val period: AnalyticsPeriod = AnalyticsPeriod.MONTH,
    /** Local start-of-day millis of the first / last day of a custom range. */
    val customStart: Long? = null,
    val customEnd: Long? = null
)

// =============================================================================
// ViewModel
// =============================================================================

class AnalyticsViewModel(
    repository: FlowlyRepository
) : ViewModel() {

    private val selection = MutableStateFlow(Selection())

    val uiState: StateFlow<AnalyticsUiState> = combine(
        repository.getTransactionDetailsFlow(),
        repository.getAllCategories(),
        repository.getUserSettings(),
        selection
    ) { transactions, categories, settings, sel ->
        compute(transactions, categories, settings.defaultCurrency, sel)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState())

    fun onPeriodSelected(period: AnalyticsPeriod) {
        if (period == AnalyticsPeriod.CUSTOM) return // custom needs a range: use onCustomRangeSelected
        selection.value = selection.value.copy(period = period)
    }

    /** [startUtcMillis] / [endUtcMillis] are the UTC-midnight values returned by Material's date range picker. */
    fun onCustomRangeSelected(startUtcMillis: Long, endUtcMillis: Long) {
        val a = utcToLocalDayStart(startUtcMillis)
        val b = utcToLocalDayStart(endUtcMillis)
        selection.value = Selection(AnalyticsPeriod.CUSTOM, minOf(a, b), maxOf(a, b))
    }

    class Factory(private val repository: FlowlyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AnalyticsViewModel(repository) as T
        }
    }
}

// =============================================================================
// Calculation
// =============================================================================

private const val DAY_MS = 24L * 60 * 60 * 1000

private fun compute(
    transactions: List<TransactionDetail>,
    categories: List<Category>,
    currency: String,
    sel: Selection
): AnalyticsUiState {
    val now = System.currentTimeMillis()
    val range = rangeFor(sel, now)
    val previousRange = previousRangeFor(sel, range, now)
    val days = daysBetween(range.start, range.end)
    val comparison = comparisonLabel(sel.period)

    val current = transactions.filter { it.transaction.dateTime in range }
    val previous = transactions.filter { it.transaction.dateTime in previousRange }
    val expenses = current.filter { it.transaction.type == TransactionType.EXPENSE }
    val previousExpenses = previous.filter { it.transaction.type == TransactionType.EXPENSE }
    val income = current.filter { it.transaction.type == TransactionType.INCOME }

    val totalExpense = expenses.sumOf { it.transaction.amount }
    val totalIncome = income.sumOf { it.transaction.amount }
    val previousTotal = previousExpenses.sumOf { it.transaction.amount }
    val largest = expenses.maxByOrNull { it.transaction.amount }

    // ---- Trend ---------------------------------------------------------------
    val trend = buildBuckets(range, sel.period).map { bucket ->
        TrendPoint(
            label = bucket.label,
            amount = expenses
                .filter { it.transaction.dateTime >= bucket.start && it.transaction.dateTime < bucket.end }
                .sumOf { it.transaction.amount }
        )
    }

    // ---- Categories (sub-categories roll up into their parent) -----------------
    val byId = categories.associateBy { it.id }
    fun rootId(id: Long): Long {
        val parent = byId[id]?.parentCategoryId
        return if (parent != null && byId.containsKey(parent)) parent else id
    }
    val currentByCategory = expenses.groupBy { rootId(it.transaction.categoryId) }
        .mapValues { (_, list) -> list.sumOf { it.transaction.amount } }
    val previousByCategory = previousExpenses.groupBy { rootId(it.transaction.categoryId) }
        .mapValues { (_, list) -> list.sumOf { it.transaction.amount } }
    val categorySpend = currentByCategory.map { (id, sum) ->
        val category = byId[id]
        CategorySpend(
            category = category,
            name = category?.name ?: "Other",
            amount = sum,
            share = if (totalExpense > 0) (sum / totalExpense).toFloat() else 0f,
            previousAmount = previousByCategory[id] ?: 0.0
        )
    }.sortedByDescending { it.amount }

    // ---- Weekday pattern (average per occurrence of that weekday) ----------------
    val weekdayTotals = DoubleArray(7)
    expenses.forEach { weekdayTotals[mondayIndex(it.transaction.dateTime)] += it.transaction.amount }
    val occurrences = IntArray(7)
    var cursor = range.start
    var guard = 0
    while (cursor < range.end && guard < 4000) {
        occurrences[mondayIndex(cursor)]++
        cursor = addDays(cursor, 1)
        guard++
    }
    val weekdayAverages = List(7) { i -> if (occurrences[i] > 0) weekdayTotals[i] / occurrences[i] else 0.0 }

    // ---- Merchants in this period -------------------------------------------------
    val merchants = expenses
        .groupBy { MerchantIntelligenceHelper.extractMerchantName(it.transaction.note, it.category?.name ?: "") }
        .map { (name, list) -> MerchantSpend(name, list.sumOf { it.transaction.amount }, list.size) }
        .sortedByDescending { it.total }
        .take(5)

    // ---- Insights (computed only from the numbers above) ------------------------
    val insights = buildList {
        val comparable = categorySpend.filter { it.changePercent != null && it.share >= 0.05f }
        comparable.filter { (it.changePercent ?: 0.0) <= -5.0 }
            .minByOrNull { it.changePercent ?: 0.0 }
            ?.let {
                add(
                    SpendingInsight(
                        title = "${it.name} spending is down ${abs(it.changePercent ?: 0.0).roundToInt()}%",
                        description = "Compared with $comparison.",
                        type = InsightType.POSITIVE,
                        iconName = "TrendingDown"
                    )
                )
            }
        comparable.filter { (it.changePercent ?: 0.0) >= 5.0 }
            .maxByOrNull { it.changePercent ?: 0.0 }
            ?.let {
                add(
                    SpendingInsight(
                        title = "${it.name} spending is up ${(it.changePercent ?: 0.0).roundToInt()}%",
                        description = "Compared with $comparison.",
                        type = InsightType.WARNING,
                        iconName = "TrendingUp"
                    )
                )
            }
        if (largest != null) {
            val date = SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(largest.transaction.dateTime))
            add(
                SpendingInsight(
                    title = "Largest expense: ${CurrencyFormatter.format(largest.transaction.amount, currency)}",
                    description = "${largest.category?.name ?: "Other"} · $date",
                    type = InsightType.INFO,
                    iconName = "PieChart"
                )
            )
        }
    }

    return AnalyticsUiState(
        period = sel.period,
        rangeLabel = rangeLabel(range),
        comparisonLabel = comparison,
        totalIncome = totalIncome,
        totalExpense = totalExpense,
        previousExpense = previousTotal,
        trend = trend,
        transactionCount = expenses.size,
        dailyAverage = totalExpense / days,
        largestExpense = largest?.transaction?.amount ?: 0.0,
        largestExpenseCategory = largest?.category?.name,
        categories = categorySpend,
        weekdayAverages = weekdayAverages,
        topMerchants = merchants,
        insights = insights,
        defaultCurrency = currency,
        isLoading = false
    )
}

// =============================================================================
// Ranges and buckets
// =============================================================================

private fun rangeFor(sel: Selection, now: Long): PeriodRanges.Range {
    val tomorrow = startOfDay(now, 1)
    return when (sel.period) {
        // The last 7 days, today included.
        AnalyticsPeriod.WEEK -> PeriodRanges.Range(startOfDay(now, -6), tomorrow)
        // The calendar month so far (1st up to and including today).
        AnalyticsPeriod.MONTH -> PeriodRanges.Range(PeriodRanges.monthStart(now), tomorrow)
        // The calendar year so far.
        AnalyticsPeriod.YEAR -> PeriodRanges.Range(startOfYear(now), tomorrow)
        AnalyticsPeriod.CUSTOM -> {
            val first = sel.customStart ?: PeriodRanges.monthStart(now)
            val last = sel.customEnd ?: first
            PeriodRanges.Range(first, startOfDay(last, 1))
        }
    }
}

/** The window the selected period is compared with — always the same number of days, so a half month is never set against a full one. */
private fun previousRangeFor(sel: Selection, range: PeriodRanges.Range, now: Long): PeriodRanges.Range {
    val days = daysBetween(range.start, range.end)
    return when (sel.period) {
        AnalyticsPeriod.MONTH -> {
            val start = PeriodRanges.monthStart(now, -1)
            PeriodRanges.Range(start, minOf(addDays(start, days), range.start))
        }
        AnalyticsPeriod.YEAR -> {
            val start = startOfYear(now, -1)
            PeriodRanges.Range(start, minOf(addDays(start, days), range.start))
        }
        else -> PeriodRanges.Range(addDays(range.start, -days), range.start)
    }
}

private fun comparisonLabel(period: AnalyticsPeriod): String = when (period) {
    AnalyticsPeriod.WEEK -> "previous 7 days"
    AnalyticsPeriod.MONTH -> "last month"
    AnalyticsPeriod.YEAR -> "last year"
    AnalyticsPeriod.CUSTOM -> "previous period"
}

private enum class BucketUnit { DAY, WEEK, MONTH }

private data class Bucket(val start: Long, val end: Long, val label: String)

private fun buildBuckets(range: PeriodRanges.Range, period: AnalyticsPeriod): List<Bucket> {
    val days = daysBetween(range.start, range.end)
    val unit = when (period) {
        AnalyticsPeriod.WEEK, AnalyticsPeriod.MONTH -> BucketUnit.DAY
        AnalyticsPeriod.YEAR -> BucketUnit.MONTH
        AnalyticsPeriod.CUSTOM -> when {
            days <= 31 -> BucketUnit.DAY
            days <= 140 -> BucketUnit.WEEK
            else -> BucketUnit.MONTH
        }
    }
    val dayFormat = SimpleDateFormat("d MMM", Locale.getDefault())
    val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
    val buckets = mutableListOf<Bucket>()
    var cursor = range.start
    while (cursor < range.end && buckets.size < 400) {
        val next = when (unit) {
            BucketUnit.DAY -> addDays(cursor, 1)
            BucketUnit.WEEK -> addDays(cursor, 7)
            BucketUnit.MONTH -> PeriodRanges.monthStart(cursor, 1)
        }
        val label = if (unit == BucketUnit.MONTH) monthFormat.format(Date(cursor)) else dayFormat.format(Date(cursor))
        buckets += Bucket(cursor, minOf(next, range.end), label)
        cursor = next
    }
    return buckets
}

private fun rangeLabel(range: PeriodRanges.Range): String {
    val first = Calendar.getInstance().apply { timeInMillis = range.start }
    val last = Calendar.getInstance().apply { timeInMillis = addDays(range.end, -1) }
    val sameYear = first.get(Calendar.YEAR) == last.get(Calendar.YEAR)
    val sameMonth = sameYear && first.get(Calendar.MONTH) == last.get(Calendar.MONTH)
    val locale = Locale.getDefault()
    return when {
        sameMonth && first.get(Calendar.DAY_OF_MONTH) == last.get(Calendar.DAY_OF_MONTH) ->
            SimpleDateFormat("d MMM yyyy", locale).format(Date(range.start))
        sameMonth ->
            "${first.get(Calendar.DAY_OF_MONTH)} – ${SimpleDateFormat("d MMM yyyy", locale).format(Date(last.timeInMillis))}"
        sameYear ->
            "${SimpleDateFormat("d MMM", locale).format(Date(range.start))} – ${SimpleDateFormat("d MMM yyyy", locale).format(Date(last.timeInMillis))}"
        else ->
            "${SimpleDateFormat("d MMM yyyy", locale).format(Date(range.start))} – ${SimpleDateFormat("d MMM yyyy", locale).format(Date(last.timeInMillis))}"
    }
}

// =============================================================================
// Date helpers (device time zone)
// =============================================================================

private fun calendarAt(time: Long): Calendar = Calendar.getInstance().apply { timeInMillis = time }

private fun startOfDay(time: Long, dayOffset: Int = 0): Long = calendarAt(time).apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
    add(Calendar.DAY_OF_YEAR, dayOffset)
}.timeInMillis

private fun startOfYear(time: Long, yearOffset: Int = 0): Long = calendarAt(time).apply {
    set(Calendar.MONTH, Calendar.JANUARY)
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
    add(Calendar.YEAR, yearOffset)
}.timeInMillis

private fun addDays(time: Long, days: Int): Long = calendarAt(time).apply { add(Calendar.DAY_OF_YEAR, days) }.timeInMillis

/** Whole days between two day boundaries (rounded, so a daylight-saving hour never changes the count). */
private fun daysBetween(start: Long, end: Long): Int = ((end - start).toDouble() / DAY_MS).roundToInt().coerceAtLeast(1)

/** 0 = Monday … 6 = Sunday. */
private fun mondayIndex(time: Long): Int = (calendarAt(time).get(Calendar.DAY_OF_WEEK) + 5) % 7

/** The date picker reports a calendar day as UTC midnight; turn it into the same day's start in the device time zone. */
private fun utcToLocalDayStart(utcMillis: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    return Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
    }.timeInMillis
}
