package com.example.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.R
import com.example.domain.model.BudgetDetail
import com.example.domain.model.Category
import com.example.domain.model.ExportPreviewSummary
import com.example.domain.model.SplitOverview
import com.example.domain.model.TransactionDetail
import com.example.domain.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Branded A4 financial report.
 *
 * Page 1: header with logo, summary cards, spending-by-category donut, spending timeline, budgets.
 * Later pages (flowing): compact branded header, top spending by description, split balances,
 * then the transaction ledger. Every page carries "Page X of N".
 *
 * Layout runs twice: a dry pass to learn N, then the real pass.
 */
object PdfReportExporter {

    private const val W = 595
    private const val H = 842
    private const val M = 36f                    // side margin
    private const val CONTENT_BOTTOM = 790f
    private const val FOOTER_Y = 812f

    private val timestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    private val genFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("d MMM", Locale.getDefault())
    private val ledgerFormat = SimpleDateFormat("dd MMM yy", Locale.getDefault())

    // Palette taken from the approved design
    private val HEADER_BG = Color.parseColor("#060909")
    private val GOLD = Color.parseColor("#C9A227")
    private val GOLD_TEXT = Color.parseColor("#D4A62A")
    private val INK = Color.parseColor("#0F172A")
    private val MUTED = Color.parseColor("#64748B")
    private val LINE = Color.parseColor("#E2E8F0")
    private val CARD_BG = Color.parseColor("#F8FAFC")
    private val GREEN = Color.parseColor("#16A34A")
    private val GREEN_BAR = Color.parseColor("#059669")
    private val RED = Color.parseColor("#DC2626")
    private val ORANGE = Color.parseColor("#F59E0B")
    private val DONUT_COLORS = listOf("#10B981", "#F59E0B", "#3B82F6", "#8B5CF6", "#EC4899").map { Color.parseColor(it) }
    private val OTHER_GRAY = Color.parseColor("#94A3B8")

    fun generateFinancialReportPdf(
        context: Context,
        transactions: List<TransactionDetail>,
        summary: ExportPreviewSummary,
        currencySymbol: String = "₹",
        accountFilterName: String = "All Accounts",
        categoryFilterName: String = "All Categories",
        budgets: List<BudgetDetail> = emptyList(),
        categories: List<Category> = emptyList(),
        splitOverview: SplitOverview? = null
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "Flowly_Report_${timestampFormat.format(Date())}.pdf")

        val logo = runCatching { BitmapFactory.decodeResource(context.resources, R.drawable.flowly_logo) }.getOrNull()
        val data = ReportData(transactions, summary, currencySymbol, accountFilterName, categoryFilterName, budgets, categories, splitOverview, logo)

        val pages = Renderer(data, totalPages = 0, document = null).render()
        val document = PdfDocument()
        try {
            Renderer(data, totalPages = pages, document = document).render()
            FileOutputStream(file).use { document.writeTo(it) }
        } finally {
            document.close()
        }
        return file
    }

    // ------------------------------------------------------------------ data

    private class ReportData(
        val transactions: List<TransactionDetail>,
        val summary: ExportPreviewSummary,
        val currency: String,
        val accountName: String,
        val categoryName: String,
        val budgets: List<BudgetDetail>,
        val categories: List<Category>,
        val split: SplitOverview?,
        val logo: Bitmap?
    ) {
        val money = transactions.filter { it.transaction.type != TransactionType.TRANSFER }
        val incomes = money.filter { it.transaction.type == TransactionType.INCOME }
        val expenses = money.filter { it.transaction.type == TransactionType.EXPENSE }
        val income = incomes.sumOf { it.transaction.amount }
        val expense = expenses.sumOf { it.transaction.amount }
        val net = income - expense
        val savingsRate: Int? = if (income > 0) ((net / income) * 100).roundToInt() else null
    }

    private data class Slice(val label: String, val amount: Double, val color: Int)

    // ------------------------------------------------------------------ rendering

    private class Renderer(
        private val d: ReportData,
        private val totalPages: Int,
        private val document: PdfDocument?
    ) {
        private var pageNo = 0
        private var page: PdfDocument.Page? = null
        private val scratch: Bitmap by lazy { Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) }
        private lateinit var c: Canvas
        private var y = 0f

        private fun paint(color: Int, size: Float, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT) =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                textSize = size
                typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
                textAlign = align
            }

        private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }

        fun render(): Int {
            startPage(first = true)
            drawSummaryCards()
            drawCategoryDonut()
            drawTimeline()
            drawBudgets()

            // Flowing pages
            newPage()
            drawTopDescriptions()
            drawSplit()
            drawLedger()
            finishPage()
            return pageNo
        }

        // ---------------- page plumbing

        private fun startPage(first: Boolean) {
            pageNo++
            if (document != null) {
                page = document.startPage(PdfDocument.PageInfo.Builder(W, H, pageNo).create())
                c = page!!.canvas
            } else {
                c = Canvas(scratch)
            }
            if (first) drawFullHeader() else drawCompactHeader()
            drawFooter()
        }

        private fun finishPage() {
            page?.let { document?.finishPage(it) }
            page = null
        }

        private fun newPage() {
            finishPage()
            startPage(first = false)
        }

        private fun ensureSpace(height: Float) {
            if (y + height > CONTENT_BOTTOM) newPage()
        }

        private fun drawFooter() {
            c.drawLine(M, 800f, W - M, 800f, Paint().apply { color = LINE; strokeWidth = 0.7f })
            c.drawText("Made with Flowly  •  Private, offline personal finance", M, FOOTER_Y, paint(MUTED, 8f))
            val label = if (totalPages > 0) "Page $pageNo of $totalPages" else "Page $pageNo"
            c.drawText(label, W - M, FOOTER_Y, paint(MUTED, 8f, align = Paint.Align.RIGHT))
        }

        // ---------------- headers

        private fun drawLogo(left: Float, top: Float, size: Float) {
            val logo = d.logo ?: return
            val dest = RectF(left, top, left + size, top + size)
            c.save()
            val clip = Path().apply { addRoundRect(dest, size * 0.16f, size * 0.16f, Path.Direction.CW) }
            c.clipPath(clip)
            c.drawBitmap(logo, null, dest, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
            c.restore()
        }

        private fun drawFullHeader() {
            c.drawRect(0f, 0f, W.toFloat(), 108f, fill(HEADER_BG))
            c.drawRect(0f, 108f, W.toFloat(), 111f, fill(GOLD))
            drawLogo(22f, 10f, 88f)
            c.drawText("Generated ${genFormat.format(Date())}", W - M + 4f, 27f, paint(Color.parseColor("#9CA3AF"), 8f, align = Paint.Align.RIGHT))
            c.drawText("Financial Report", 124f, 52f, paint(Color.WHITE, 26f, bold = true))
            c.drawText("FLOWLY  •  PERSONAL FINANCE STATEMENT", 124f, 70f,
                paint(GOLD_TEXT, 9.5f, bold = true).apply { letterSpacing = 0.06f })
            c.drawText(d.summary.periodLabel.ifBlank { "All time" }, 124f, 92f, paint(Color.parseColor("#E5E7EB"), 11f))
            c.drawText("Account: ${d.accountName}   •   Category: ${d.categoryName}", M, 134f, paint(MUTED, 9f))
            y = 148f
        }

        private fun drawCompactHeader() {
            c.drawRect(0f, 0f, W.toFloat(), 56f, fill(HEADER_BG))
            c.drawRect(0f, 56f, W.toFloat(), 58f, fill(GOLD))
            drawLogo(20f, 6f, 44f)
            c.drawText("Financial Report", 74f, 28f, paint(Color.WHITE, 15f, bold = true))
            c.drawText("FLOWLY  •  ${d.summary.periodLabel.ifBlank { "All time" }}".uppercase(Locale.getDefault()), 74f, 43f,
                paint(GOLD_TEXT, 7.5f, bold = true).apply { letterSpacing = 0.05f })
            y = 78f
        }

        // ---------------- helpers

        private fun sectionHeading(title: String, right: String? = null) {
            c.drawRoundRect(RectF(M, y - 11f, M + 3f, y + 3f), 1.5f, 1.5f, fill(GREEN))
            c.drawText(title, M + 10f, y, paint(INK, 13f, bold = true))
            if (right != null) c.drawText(right, W - M, y, paint(MUTED, 8.5f, align = Paint.Align.RIGHT))
            c.drawLine(M, y + 8f, W - M, y + 8f, Paint().apply { color = LINE; strokeWidth = 0.7f })
            y += 24f
        }

        private fun fit(text: String, p: Paint, maxWidth: Float): String {
            if (p.measureText(text) <= maxWidth) return text
            val n = p.breakText(text, true, maxWidth - p.measureText("…"), null)
            return text.take(n).trimEnd() + "…"
        }

        private fun amount(v: Double, signed: Boolean = false): String {
            val body = group(abs(v))
            val sign = if (signed) (if (v >= 0) "+" else "-") else ""
            return sign + d.currency + body
        }

        /** Indian lakh grouping for ₹, western grouping for everything else. */
        private fun group(v: Double): String {
            val n = v.roundToInt().toLong()
            val s = n.toString()
            if (d.currency != "₹" || s.length <= 3) return String.format(Locale.US, "%,d", n)
            val last3 = s.takeLast(3)
            val rest = s.dropLast(3).reversed().chunked(2).joinToString(",").reversed()
            return "$rest,$last3"
        }

        // ---------------- page 1

        private fun drawSummaryCards() {
            val gap = 8f
            val cardW = (W - 2 * M - 3 * gap) / 4f
            val cardH = 60f
            val top = y + 4f
            val netColor = if (d.net >= 0) GREEN else RED
            val rate = d.savingsRate
            val cards = listOf(
                Triple("INCOME", "+" + amount(d.income), GREEN) to "${d.incomes.size} credits",
                Triple("EXPENSES", "-" + amount(d.expense), RED) to "${d.expenses.size} payments",
                Triple("NET CASH FLOW", amount(d.net, signed = true), netColor) to "${d.money.size} transactions",
                Triple("SAVINGS RATE", rate?.let { "$it%" } ?: "—", if ((rate ?: 0) > 0) GREEN else RED) to
                    (if (rate != null) "of income kept" else "no income in period")
            )
            cards.forEachIndexed { i, (main, sub) ->
                val (label, value, color) = main
                val left = M + i * (cardW + gap)
                val r = RectF(left, top, left + cardW, top + cardH)
                c.drawRoundRect(r, 7f, 7f, fill(CARD_BG))
                c.drawRoundRect(r, 7f, 7f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE; strokeWidth = 0.8f; this.color = LINE
                })
                c.drawRoundRect(RectF(left, top + 6f, left + 3f, top + cardH - 6f), 1.5f, 1.5f, fill(color))
                c.drawText(label, left + 13f, top + 17f, paint(MUTED, 7.5f, bold = true).apply { letterSpacing = 0.05f })
                var size = 17f
                val vp = paint(color, size, bold = true)
                while (vp.measureText(value) > cardW - 20f && size > 9f) { size -= 1f; vp.textSize = size }
                c.drawText(value, left + 13f, top + 39f, vp)
                c.drawText(sub, left + 13f, top + 52f, paint(MUTED, 8f))
            }
            y = top + cardH + 30f
        }

        private fun categorySlices(): List<Slice> {
            if (d.expense <= 0) return emptyList()
            val byId = d.categories.associateBy { it.id }
            val totals = LinkedHashMap<String, Double>()
            for (tx in d.expenses) {
                val cat = tx.category
                val parent = cat?.parentCategoryId?.let { byId[it] }
                val name = parent?.name ?: cat?.name ?: "Uncategorised"
                totals[name] = (totals[name] ?: 0.0) + tx.transaction.amount
            }
            val sorted = totals.entries.sortedByDescending { it.value }
            val top = sorted.take(5).mapIndexed { i, e -> Slice(e.key, e.value, DONUT_COLORS[i]) }
            val rest = sorted.drop(5).sumOf { it.value }
            return if (rest > 0.005) top + Slice("Other", rest, OTHER_GRAY) else top
        }

        private fun drawCategoryDonut() {
            sectionHeading("Where your money went", "Expenses by category")
            val slices = categorySlices()
            val blockH = 150f
            if (slices.isEmpty()) {
                c.drawText("No expenses in this period.", M, y + 20f, paint(MUTED, 10f))
                y += 60f
                return
            }
            val cx = M + 75f
            val cy = y + blockH / 2f - 4f
            val outer = 71f
            val inner = 45f
            val mid = (outer + inner) / 2f
            val ring = RectF(cx - mid, cy - mid, cx + mid, cy + mid)
            var start = -90f
            slices.forEach { s ->
                val sweep = (s.amount / d.expense * 360.0).toFloat()
                val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE; strokeWidth = outer - inner; color = s.color
                }
                val gap = if (slices.size > 1) 1.2f else 0f
                c.drawArc(ring, start, max(0.1f, sweep - gap), false, arc)
                start += sweep
            }
            c.drawText("EXPENSES", cx, cy - 3f, paint(MUTED, 7.5f, align = Paint.Align.CENTER))
            var vs = 12f
            val vp = paint(INK, vs, bold = true, align = Paint.Align.CENTER)
            val vt = amount(d.expense)
            while (vp.measureText(vt) > inner * 1.75f && vs > 7f) { vs -= 0.5f; vp.textSize = vs }
            c.drawText(vt, cx, cy + 13f, vp)

            val rowH = 22f
            var ry = y + 14f + (blockH - slices.size * rowH) / 2f - 6f
            val nameP = paint(INK, 10.5f)
            slices.forEach { s ->
                c.drawRoundRect(RectF(226f, ry - 9f, 237f, ry + 2f), 2.5f, 2.5f, fill(s.color))
                c.drawText(fit(s.label, nameP, 190f), 247f, ry, nameP)
                c.drawText(amount(s.amount), 521f, ry, paint(INK, 10.5f, bold = true, align = Paint.Align.RIGHT))
                val pct = (s.amount / d.expense * 100).roundToInt()
                c.drawText("$pct%", W - M, ry, paint(MUTED, 10f, align = Paint.Align.RIGHT))
                ry += rowH
            }
            y += blockH + 22f
        }

        private fun drawTimeline() {
            val exp = d.expenses
            if (exp.isEmpty()) return
            val cal = Calendar.getInstance()
            fun dayStart(t: Long): Long {
                cal.timeInMillis = t
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                return cal.timeInMillis
            }
            val times = d.money.map { it.transaction.dateTime }
            val first = dayStart(times.min())
            val last = dayStart(times.max())
            val dayMs = 24L * 60 * 60 * 1000
            val days = (((last - first) / dayMs).toInt() + 1).coerceAtLeast(1)

            // daily bars up to ~6 weeks, then weekly, then monthly-ish (30-day) buckets
            val bucketDays = when { days <= 45 -> 1; days <= 200 -> 7; else -> 30 }
            val bucketCount = ((days + bucketDays - 1) / bucketDays)
            val sums = DoubleArray(bucketCount)
            exp.forEach {
                val idx = (((dayStart(it.transaction.dateTime) - first) / dayMs).toInt() / bucketDays).coerceIn(0, bucketCount - 1)
                sums[idx] += it.transaction.amount
            }

            ensureSpace(190f)
            sectionHeading("Spending timeline", when (bucketDays) { 1 -> "Expenses per day"; 7 -> "Expenses per week"; else -> "Expenses per month" })

            val chartTop = y + 6f
            val chartBottom = y + 112f
            val left = M + 30f
            val right = W - M
            val peak = sums.max().coerceAtLeast(1.0)
            val axisMax = ceil(peak / 100.0) * 100.0
            fun tick(v: Double) = if (v >= 1000) String.format(Locale.US, "%.1fk", v / 1000.0) else v.roundToInt().toString()
            val gridP = Paint().apply { color = LINE; strokeWidth = 0.6f }
            listOf(0.0, 0.5, 1.0).forEach { f ->
                val gy = chartBottom - (chartBottom - chartTop) * f.toFloat()
                c.drawLine(left, gy, right, gy, gridP)
                c.drawText(if (f == 0.0) "0" else tick(axisMax * f), left - 5f, gy + 3f, paint(MUTED, 7.5f, align = Paint.Align.RIGHT))
            }
            val slot = (right - left) / bucketCount
            val barW = (slot * 0.62f).coerceAtMost(14f)
            val labelEvery = ceil(bucketCount / 10.0).toInt().coerceAtLeast(1)
            val barPaint = fill(GREEN_BAR)
            for (i in 0 until bucketCount) {
                val v = sums[i]
                val cxBar = left + slot * i + slot / 2f
                if (v > 0) {
                    val h = ((v / axisMax) * (chartBottom - chartTop)).toFloat().coerceAtLeast(1.5f)
                    val path = Path().apply {
                        addRoundRect(RectF(cxBar - barW / 2, chartBottom - h, cxBar + barW / 2, chartBottom),
                            floatArrayOf(2.5f, 2.5f, 2.5f, 2.5f, 0f, 0f, 0f, 0f), Path.Direction.CW)
                    }
                    c.drawPath(path, barPaint)
                }
                if (i % labelEvery == 0) {
                    c.drawText(dayFormat.format(Date(first + i.toLong() * bucketDays * dayMs)), cxBar, chartBottom + 13f,
                        paint(MUTED, 7.5f, align = Paint.Align.CENTER))
                }
            }
            y = chartBottom + 34f
        }

        private fun drawBudgets() {
            val list = d.budgets
                .filter { it.budget.amount > 0 }
                .sortedByDescending { it.spentAmount / it.budget.amount }
                .take(5)
            if (list.isEmpty()) return
            ensureSpace(30f + list.size * 30f)
            sectionHeading("Budgets", "Current budget periods")
            list.forEach { b ->
                val pct = (b.spentAmount / b.budget.amount * 100).roundToInt()
                val color = when { pct >= 100 -> RED; pct >= 75 -> ORANGE; else -> GREEN }
                c.drawText(fit(b.budget.name, paint(INK, 10.5f, bold = true), 260f), M, y, paint(INK, 10.5f, bold = true))
                c.drawText("${amount(b.spentAmount)} of ${amount(b.budget.amount)}  •  $pct%", W - M, y,
                    paint(color, 9.5f, bold = true, align = Paint.Align.RIGHT))
                val track = RectF(M, y + 6f, W - M, y + 12f)
                c.drawRoundRect(track, 3f, 3f, fill(LINE))
                val w = (track.width() * (pct / 100f).coerceIn(0f, 1f))
                if (w > 0) c.drawRoundRect(RectF(M, y + 6f, M + w, y + 12f), 3f, 3f, fill(color))
                y += 30f
            }
        }

        // ---------------- later pages

        private fun drawTopDescriptions() {
            sectionHeading("Top spending by description", "Where the money went, item by item")
            if (d.expenses.isEmpty()) {
                c.drawText("No expenses in this period.", M, y, paint(MUTED, 10f)); y += 30f; return
            }
            val groups = d.expenses.groupBy {
                it.transaction.note.trim().lowercase(Locale.getDefault()).ifBlank { it.category?.name?.lowercase(Locale.getDefault()) ?: "other" }
            }.map { (k, v) -> Triple(k.replaceFirstChar { ch -> ch.uppercase() }, v.size, v.sumOf { it.transaction.amount }) }
                .sortedByDescending { it.third }.take(8)
            val maxV = groups.first().third.coerceAtLeast(1.0)
            ensureSpace(groups.size * 26f + 10f)
            val nameP = paint(INK, 10f)
            groups.forEach { (name, count, total) ->
                c.drawText(fit(name, nameP, 200f), M, y, nameP)
                c.drawText("×$count", 250f, y, paint(MUTED, 8.5f))
                val barL = 285f
                val barMax = 215f
                c.drawRoundRect(RectF(barL, y - 8f, barL + barMax, y - 2f), 3f, 3f, fill(LINE))
                c.drawRoundRect(RectF(barL, y - 8f, barL + (barMax * (total / maxV)).toFloat().coerceAtLeast(3f), y - 2f), 3f, 3f, fill(GREEN_BAR))
                c.drawText(amount(total), W - M, y, paint(INK, 10f, bold = true, align = Paint.Align.RIGHT))
                y += 22f
            }
            y += 14f
        }

        private fun drawSplit() {
            val s = d.split ?: return
            if (s.friends.isEmpty()) return
            ensureSpace(60f + s.balances.size * 20f)
            sectionHeading("Split with friends", "Running balances")
            c.drawText("You are owed ${amount(s.totalOwedToMe)}", M, y, paint(GREEN, 10f, bold = true))
            c.drawText("You owe ${amount(s.totalIOwe)}", W - M, y, paint(RED, 10f, bold = true, align = Paint.Align.RIGHT))
            y += 20f
            s.balances.sortedByDescending { abs(it.amount) }.forEach { b ->
                ensureSpace(20f)
                c.drawText(fit(b.friend.name, paint(INK, 10f), 220f), M, y, paint(INK, 10f))
                val (text, color) = when {
                    b.amount > 0.005 -> "owes you ${amount(b.amount)}" to GREEN
                    b.amount < -0.005 -> "you owe ${amount(-b.amount)}" to RED
                    else -> "settled up" to MUTED
                }
                c.drawText(text, W - M, y, paint(color, 10f, bold = true, align = Paint.Align.RIGHT))
                y += 18f
            }
            y += 14f
        }

        private fun drawLedger() {
            ensureSpace(60f)
            sectionHeading("Transaction ledger", "${d.transactions.size} transactions")
            if (d.transactions.isEmpty()) {
                c.drawText("No transactions in this period.", M, y, paint(MUTED, 10f)); return
            }
            val cols = floatArrayOf(M, M + 62f, M + 230f, M + 355f, W - M)  // date | description | category | account | amount(right)
            fun header() {
                c.drawRect(M, y - 10f, W - M, y + 6f, fill(CARD_BG))
                val hp = paint(MUTED, 7.5f, bold = true)
                c.drawText("DATE", cols[0] + 4f, y, hp); c.drawText("DESCRIPTION", cols[1], y, hp)
                c.drawText("CATEGORY", cols[2], y, hp); c.drawText("ACCOUNT", cols[3], y, hp)
                c.drawText("AMOUNT", cols[4] - 4f, y, paint(MUTED, 7.5f, bold = true, align = Paint.Align.RIGHT))
                y += 16f
            }
            header()
            val text = paint(INK, 8.5f)
            val muted = paint(MUTED, 8.5f)
            d.transactions.sortedByDescending { it.transaction.dateTime }.forEachIndexed { i, t ->
                if (y + 14f > CONTENT_BOTTOM) { newPage(); y += 6f; header() }
                if (i % 2 == 1) c.drawRect(M, y - 9.5f, W - M, y + 4.5f, fill(Color.parseColor("#FBFCFD")))
                val tx = t.transaction
                c.drawText(ledgerFormat.format(Date(tx.dateTime)), cols[0] + 4f, y, muted)
                val desc = tx.note.ifBlank { t.category?.name ?: "—" }
                c.drawText(fit(desc, text, cols[2] - cols[1] - 8f), cols[1], y, text)
                c.drawText(fit(t.category?.name ?: "—", muted, cols[3] - cols[2] - 8f), cols[2], y, muted)
                c.drawText(fit(t.sourceAccount?.name ?: "—", muted, cols[4] - cols[3] - 70f), cols[3], y, muted)
                val (sign, color) = when (tx.type) {
                    TransactionType.INCOME -> "+" to GREEN
                    TransactionType.EXPENSE -> "-" to RED
                    else -> "" to MUTED
                }
                c.drawText(sign + amount(tx.amount), cols[4] - 4f, y, paint(color, 8.5f, bold = true, align = Paint.Align.RIGHT))
                y += 14f
            }
        }
    }
}
