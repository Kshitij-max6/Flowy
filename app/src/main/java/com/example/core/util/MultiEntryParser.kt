package com.example.core.util

import com.example.domain.model.TransactionType

/** One expense or income pulled out of a sentence like "chai 40 and auto 120 yesterday". */
data class ParsedEntry(
    val description: String,
    val amount: Double,
    val type: TransactionType,
    /** 0 = today, 1 = yesterday, 2 = day before yesterday. */
    val daysAgo: Int
)

/**
 * Splits one typed sentence into several entries. Pure Kotlin.
 * A sentence only counts as "multi" when at least two pieces each carry their own amount.
 */
object MultiEntryParser {

    private val SPLITTER = Regex("""\s*(?:(?<!\d),|,(?!\d)|;|\+|&|\band\b|\baur\b|\bphir\b|\bthen\b)\s*""", RegexOption.IGNORE_CASE)
    private val AMOUNT = Regex("""(?:₹|rs\.?|inr)?\s*(\d{1,3}(?:,\d{2,3})+|\d+)(?:\.(\d{1,2}))?\s*(k\b)?\s*(?:₹|rs\b\.?|rupees|rupaye|/-)?""", RegexOption.IGNORE_CASE)

    private val DATE_WORDS = listOf(
        "day before yesterday" to 2, "parso" to 2,
        "yesterday" to 1, "kal" to 1,
        "today" to 0, "aaj" to 0
    )
    private val INCOME_WORDS = setOf("salary", "received", "credited", "got paid", "refund", "cashback", "bonus", "interest", "dividend")
    private val FILLER = Regex("""\b(?:paid|spent|for|on|at|to|bought|buy|of|rs|inr|rupees|rupaye|the|a|an)\b""", RegexOption.IGNORE_CASE)

    fun parse(sentence: String): List<ParsedEntry> {
        val text = sentence.trim()
        if (text.length < 5) return emptyList()

        val rawParts = SPLITTER.split(text).map { it.trim() }.filter { it.isNotEmpty() }
        if (rawParts.size < 2) return emptyList()

        // A piece with no number ("salt", "pepper 50") belongs to the next piece that has one.
        val pieces = mutableListOf<String>()
        var carry = ""
        for (part in rawParts) {
            val merged = if (carry.isEmpty()) part else "$carry $part"
            if (AMOUNT.containsMatchIn(merged)) { pieces += merged; carry = "" } else carry = merged
        }
        if (carry.isNotEmpty() && pieces.isNotEmpty()) pieces[pieces.lastIndex] = pieces.last() + " " + carry
        if (pieces.size < 2) return emptyList()

        // Each piece may name its own day; a day said once (usually at the end) applies to pieces without one.
        val perPieceDays = pieces.map { dayIn(it) }
        val shared = perPieceDays.filterNotNull().lastOrNull() ?: 0

        val entries = pieces.mapIndexedNotNull { i, piece ->
            val m = AMOUNT.find(piece) ?: return@mapIndexedNotNull null
            val amount = amountOf(m) ?: return@mapIndexedNotNull null
            if (amount <= 0.0) return@mapIndexedNotNull null

            var description = piece.removeRange(m.range)
            DATE_WORDS.forEach { (word, _) -> description = description.replace(Regex("""\b${Regex.escape(word)}\b""", RegexOption.IGNORE_CASE), " ") }
            description = description.replace(FILLER, " ").replace(Regex("""\s+"""), " ").trim(' ', '-', '.', ':')

            val lower = piece.lowercase()
            val type = if (INCOME_WORDS.any { lower.contains(it) }) TransactionType.INCOME else TransactionType.EXPENSE
            ParsedEntry(
                description = description.replaceFirstChar { it.uppercase() },
                amount = amount,
                type = type,
                daysAgo = perPieceDays[i] ?: shared
            )
        }
        return if (entries.size >= 2) entries else emptyList()
    }

    private fun dayIn(piece: String): Int? {
        val lower = piece.lowercase()
        return DATE_WORDS.firstOrNull { (word, _) -> Regex("""\b${Regex.escape(word)}\b""").containsMatchIn(lower) }?.second
    }

    private fun amountOf(m: MatchResult): Double? {
        val whole = m.groupValues[1].replace(",", "")
        val frac = m.groupValues[2]
        var v = (if (frac.isNotEmpty()) "$whole.$frac" else whole).toDoubleOrNull() ?: return null
        if (m.groupValues[3].isNotEmpty()) v *= 1000.0
        return v
    }
}
