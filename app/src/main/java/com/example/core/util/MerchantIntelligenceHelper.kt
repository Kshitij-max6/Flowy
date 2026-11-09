package com.example.core.util

import java.util.Locale

object MerchantIntelligenceHelper {

    private val KNOWN_MERCHANTS = listOf(
        // Food & Dining
        "Swiggy", "Zomato", "Starbucks", "McDonald's", "Domino's", "KFC", "Subway",
        "Burger King", "Pizza Hut", "Dunkin", "Chai Point", "Cafe Coffee Day", "Haldiram",
        // Groceries & Delivery
        "Blinkit", "Zepto", "Instamart", "BigBasket", "DMart", "Reliance Fresh", "Spencer's",
        // Shopping & Ecommerce
        "Amazon", "Flipkart", "Myntra", "Ajio", "Zara", "H&M", "Uniqlo", "Nike",
        "Adidas", "Puma", "Decathlon", "IKEA", "Croma", "Reliance Digital", "Apple", "Google",
        // Travel & Transport
        "Uber", "Ola", "Rapido", "Metro", "IRCTC", "MakeMyTrip", "Goibibo", "IndiGo",
        "Air India", "Vistara", "RedBus", "Shell", "Indian Oil", "HPCL", "BPCL", "Fastag",
        // Subscriptions & Entertainment
        "Netflix", "Spotify", "YouTube", "Disney+ Hotstar", "Hotstar", "Prime Video",
        "SonyLIV", "JioCinema", "Audible", "PlayStation", "Steam", "GitHub", "ChatGPT", "OpenAI",
        // Utilities & Telecom
        "Airtel", "Jio", "Vi", "Tata Play", "BESCOM", "Adani Electricity",
        // Health & Fitness
        "Apollo Pharmacy", "1mg", "PharmEasy", "Netmeds", "Cult.fit", "Cult"
    )

    private val NOISE_WORDS = setOf(
        "to", "for", "at", "by", "from", "with", "payment", "bill", "order", "recharge",
        "purchase", "subscription", "online", "store", "shop", "fees", "fee", "transfer",
        "paid", "lunch", "dinner", "breakfast", "coffee", "tea", "snacks", "groceries",
        "ride", "trip", "taxi", "auto", "card", "upi"
    )

    /**
     * Extracts a normalized merchant name from a transaction note or fallback.
     */
    fun extractMerchantName(note: String, fallbackCategoryName: String = ""): String {
        val cleanNote = note.trim()
        if (cleanNote.isBlank()) {
            return fallbackCategoryName.ifBlank { "Other" }
        }

        // 1. Check known merchants match
        for (known in KNOWN_MERCHANTS) {
            val pattern = "\\b${Regex.escape(known)}\\b".toRegex(RegexOption.IGNORE_CASE)
            if (pattern.containsMatchIn(cleanNote)) {
                return known
            }
        }

        // 2. Extract leading clean tokens
        val words = cleanNote.split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .map { it.replace("""[^\w'&-]""".toRegex(), "") }
            .filter { it.isNotBlank() }

        val meaningfulWords = words.filter { it.lowercase(Locale.ROOT) !in NOISE_WORDS }

        return when {
            meaningfulWords.isNotEmpty() -> {
                // Capitalize first 1-2 words
                meaningfulWords.take(2).joinToString(" ") { word ->
                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                }
            }
            words.isNotEmpty() -> {
                words.first().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }
            else -> fallbackCategoryName.ifBlank { "General Merchant" }
        }
    }

    /**
     * Returns matching merchant suggestions for auto-completion.
     */
    fun getSuggestions(query: String, existingMerchants: List<String> = emptyList()): List<String> {
        val q = query.trim().lowercase(Locale.ROOT)
        val combined = (KNOWN_MERCHANTS + existingMerchants).distinct()
        if (q.isBlank()) {
            return combined.take(8)
        }
        return combined.filter { it.lowercase(Locale.ROOT).contains(q) }.take(6)
    }
}
