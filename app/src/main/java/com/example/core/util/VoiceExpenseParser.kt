package com.example.core.util

import com.example.domain.model.Account
import com.example.domain.model.Category
import com.example.domain.model.CategoryType
import com.example.domain.model.ExpenseBook
import com.example.domain.model.TransactionType
import java.util.Locale
import java.util.regex.Pattern

data class ParsedVoiceExpense(
    val amount: Double?,
    val type: TransactionType,
    val matchedCategory: Category?,
    val matchedAccount: Account?,
    val matchedBook: ExpenseBook?,
    val merchantOrNote: String,
    val fullSpokenText: String,
    val confidence: Float = 0.9f
)

object VoiceExpenseParser {

    private val NUMBER_WORDS = mapOf(
        "zero" to 0.0, "one" to 1.0, "two" to 2.0, "three" to 3.0, "four" to 4.0,
        "five" to 5.0, "six" to 6.0, "seven" to 7.0, "eight" to 8.0, "nine" to 9.0,
        "ten" to 10.0, "twenty" to 20.0, "thirty" to 30.0, "forty" to 40.0, "fifty" to 50.0,
        "sixty" to 60.0, "seventy" to 70.0, "eighty" to 80.0, "ninety" to 90.0,
        "hundred" to 100.0, "thousand" to 1000.0, "lakh" to 100000.0, "million" to 1000000.0
    )

    private val INCOME_KEYWORDS = listOf(
        "salary", "income", "received", "credited", "earned", "dividend", "freelance",
        "refund", "cashback", "bonus", "deposit", "interest", "got money", "sold"
    )

    private val TRANSFER_KEYWORDS = listOf(
        "transfer", "transferred", "moved to", "sent to account", "shift to", "withdrew to"
    )

    private val KNOWN_MERCHANTS = listOf(
        "starbucks", "swiggy", "zomato", "mcdonalds", "mcdonald's", "subway", "dominos",
        "domino's", "kfc", "burger king", "pizza hut", "uber", "ola", "rapido", "dmart",
        "blinkit", "zepto", "instamart", "bigbasket", "amazon", "flipkart", "myntra",
        "zara", "h&m", "apple", "netflix", "spotify", "hotstar", "shell", "hp petrol",
        "indian oil", "apollo pharmacy", "medplus", "cult fit", "cultfit", "decathlon",
        "star market", "nature's basket", "ikea", "lenskart", "nykaa"
    )

    private val CATEGORY_KEYWORD_MAP = mapOf(
        "food" to listOf("food", "lunch", "dinner", "breakfast", "snack", "coffee", "tea", "chai", "restaurant", "cafe", "burger", "pizza", "swiggy", "zomato", "meal", "eating", "dining", "bakery", "cake", "bar", "drinks", "pub", "samosa", "boba"),
        "transport" to listOf("fuel", "petrol", "diesel", "gas", "uber", "ola", "auto", "cab", "taxi", "metro", "bus", "train", "flight", "parking", "toll", "commute", "bike", "car service"),
        "grocery" to listOf("grocery", "groceries", "supermarket", "vegetables", "fruits", "milk", "bread", "dmart", "blinkit", "zepto", "instamart", "bigbasket", "ration", "market", "eggs", "provisions"),
        "shopping" to listOf("shopping", "amazon", "flipkart", "clothes", "shoes", "shirt", "dress", "myntra", "zara", "mall", "electronics", "gadget", "watch", "perfume", "accessories", "bought"),
        "bills" to listOf("bill", "electricity", "water", "wifi", "broadband", "internet", "recharge", "mobile bill", "rent", "maintenance", "dth", "cylinder", "lpg", "utility"),
        "entertainment" to listOf("movie", "cinema", "netflix", "spotify", "hotstar", "game", "gaming", "concert", "party", "outing", "tickets", "show", "playstation", "steam"),
        "health" to listOf("medicine", "pharmacy", "doctor", "hospital", "clinic", "gym", "workout", "fitness", "supplement", "dentist", "dental", "pills", "apollo", "medplus"),
        "education" to listOf("books", "course", "tuition", "school", "college", "fees", "udemy", "class", "training", "stationery"),
        "travel" to listOf("hotel", "resort", "trip", "vacation", "flight ticket", "airbnb", "sightseeing", "scuba", "tour", "goa"),
        "personal care" to listOf("haircut", "salon", "spa", "grooming", "cosmetics", "skincare", "massage", "parlour"),
        "salary" to listOf("salary", "paycheck", "wages", "pay"),
        "freelance" to listOf("freelance", "client project", "consulting", "upwork", "fiverr"),
        "investments" to listOf("sip", "mutual fund", "stocks", "crypto", "gold", "shares", "deposit")
    )

    fun parse(
        spokenText: String,
        categories: List<Category>,
        accounts: List<Account>,
        books: List<ExpenseBook> = emptyList()
    ): ParsedVoiceExpense {
        val raw = spokenText.trim()
        val textLower = raw.lowercase(Locale.ROOT)

        // 1. Detect Type
        val type = when {
            INCOME_KEYWORDS.any { textLower.contains(it) } -> TransactionType.INCOME
            TRANSFER_KEYWORDS.any { textLower.contains(it) } -> TransactionType.TRANSFER
            else -> TransactionType.EXPENSE
        }

        // 2. Extract Amount
        val amount = extractAmount(textLower)

        // 3. Match Category
        val targetCatType = if (type == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
        val validCategories = categories.filter { it.type == targetCatType }
        val matchedCategory = matchCategory(textLower, validCategories)

        // 4. Match Account
        val matchedAccount = matchAccount(textLower, accounts)

        // 5. Match Expense Book
        val matchedBook = matchBook(textLower, books)

        // 6. Extract Merchant / Note
        val merchantOrNote = extractMerchantOrNote(textLower, raw, matchedCategory)

        return ParsedVoiceExpense(
            amount = amount,
            type = type,
            matchedCategory = matchedCategory,
            matchedAccount = matchedAccount,
            matchedBook = matchedBook,
            merchantOrNote = merchantOrNote,
            fullSpokenText = raw
        )
    }

    private fun extractAmount(text: String): Double? {
        // Regex for patterns like: ₹500, Rs 500, 500 rs, 500.50, 5.5k, 5k, 500 bucks, 500 rupees
        val kPattern = Pattern.compile("(?i)(\\d+(?:\\.\\d+)?)\\s*k\\b")
        val kMatcher = kPattern.matcher(text)
        if (kMatcher.find()) {
            val num = kMatcher.group(1)?.toDoubleOrNull()
            if (num != null) return num * 1000.0
        }

        val lakhPattern = Pattern.compile("(?i)(\\d+(?:\\.\\d+)?)\\s*(?:lakh|lac|lakhs)\\b")
        val lakhMatcher = lakhPattern.matcher(text)
        if (lakhMatcher.find()) {
            val num = lakhMatcher.group(1)?.toDoubleOrNull()
            if (num != null) return num * 100000.0
        }

        // Standard number extraction: match digits with optional decimals
        val numberPattern = Pattern.compile("(?i)(?:(?:rs|inr|₹|\\$|€|£)\\s*)?(\\d+(?:[.,]\\d+)?)(?:\\s*(?:rs|rupees|inr|bucks|usd|eur|pounds|₹|\\$|€|£))?")
        val matcher = numberPattern.matcher(text)
        val matches = mutableListOf<Double>()
        while (matcher.find()) {
            val numStr = matcher.group(1)?.replace(",", ".")
            val parsed = numStr?.toDoubleOrNull()
            if (parsed != null && parsed > 0) {
                matches.add(parsed)
            }
        }

        if (matches.isNotEmpty()) {
            return matches.first()
        }

        // Word-to-number fallback (e.g. "fifty", "five hundred")
        for ((word, value) in NUMBER_WORDS) {
            if (text.contains(word)) {
                return value
            }
        }

        return null
    }

    private fun matchCategory(text: String, categories: List<Category>): Category? {
        // 1. Direct name match in spoken text
        for (cat in categories) {
            val catNameLower = cat.name.lowercase(Locale.ROOT)
            if (text.contains(catNameLower)) {
                return cat
            }
        }

        // 2. Keyword dictionary match
        for ((key, keywords) in CATEGORY_KEYWORD_MAP) {
            if (keywords.any { text.contains(it) }) {
                // Find matching category in the list
                val found = categories.firstOrNull { cat ->
                    val nameLower = cat.name.lowercase(Locale.ROOT)
                    when (key) {
                        "food" -> nameLower.contains("food") || nameLower.contains("dining") || nameLower.contains("restaurant")
                        "transport" -> nameLower.contains("transport") || nameLower.contains("travel") || nameLower.contains("commute")
                        "grocery" -> nameLower.contains("grocer")
                        "shopping" -> nameLower.contains("shop")
                        "bills" -> nameLower.contains("bill") || nameLower.contains("utilit")
                        "entertainment" -> nameLower.contains("entertain") || nameLower.contains("movie")
                        "health" -> nameLower.contains("health") || nameLower.contains("well") || nameLower.contains("fitness")
                        "education" -> nameLower.contains("educat") || nameLower.contains("school")
                        "travel" -> nameLower.contains("travel") || nameLower.contains("trip")
                        "personal care" -> nameLower.contains("personal") || nameLower.contains("care")
                        "salary" -> nameLower.contains("salary")
                        "freelance" -> nameLower.contains("freelance") || nameLower.contains("project")
                        "investments" -> nameLower.contains("invest")
                        else -> false
                    }
                }
                if (found != null) return found
            }
        }

        // Default to first category if available
        return categories.firstOrNull()
    }

    private fun matchAccount(text: String, accounts: List<Account>): Account? {
        // 1. Direct name match
        for (acc in accounts) {
            val nameLower = acc.name.lowercase(Locale.ROOT)
            if (text.contains(nameLower)) return acc
        }

        // 2. Common bank / payment keyword match
        val bankKeywords = mapOf(
            "cash" to listOf("cash", "wallet", "pocket", "hand"),
            "hdfc" to listOf("hdfc"),
            "sbi" to listOf("sbi", "state bank"),
            "icici" to listOf("icici"),
            "axis" to listOf("axis"),
            "upi" to listOf("upi", "gpay", "google pay", "phonepe", "paytm"),
            "credit" to listOf("credit card", "credit"),
            "salary" to listOf("salary account")
        )

        for ((_, keywords) in bankKeywords) {
            if (keywords.any { text.contains(it) }) {
                val found = accounts.firstOrNull { acc ->
                    val accName = acc.name.lowercase(Locale.ROOT)
                    keywords.any { accName.contains(it) }
                }
                if (found != null) return found
            }
        }

        // Default account
        return accounts.firstOrNull { it.isDefault } ?: accounts.firstOrNull()
    }

    private fun matchBook(text: String, books: List<ExpenseBook>): ExpenseBook? {
        if (books.isEmpty()) return null

        for (book in books) {
            val bookName = book.name.lowercase(Locale.ROOT)
            if (text.contains(bookName)) return book
            // Match key terms like "goa", "trip", "vacation", "renovation", "work"
            val terms = bookName.split(" ").filter { it.length > 2 }
            if (terms.any { text.contains(it) }) return book
        }

        return books.firstOrNull { it.isDefault } ?: books.firstOrNull()
    }

    private fun extractMerchantOrNote(textLower: String, rawText: String, matchedCategory: Category?): String {
        // 1. Check known merchants
        for (merchant in KNOWN_MERCHANTS) {
            if (textLower.contains(merchant)) {
                return merchant.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
            }
        }

        // 2. Extract after prepositions: "at Starbucks", "for lunch", "on Amazon", "to Ramesh"
        val prepPattern = Pattern.compile("(?i)\\b(?:at|on|for|from|to|in)\\s+([a-zA-Z0-9&'\\s]{2,20})(?:\\s+(?:with|using|from|by|today|yesterday)|$)")
        val matcher = prepPattern.matcher(rawText)
        if (matcher.find()) {
            val extracted = matcher.group(1)?.trim()
            if (!extracted.isNullOrBlank() && !extracted.matches(Regex("(?i)^(cash|card|upi|bank|account|today|yesterday)$"))) {
                return extracted.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
            }
        }

        // Fallback to Category name or Clean text
        return matchedCategory?.name ?: "Voice Expense"
    }
}
