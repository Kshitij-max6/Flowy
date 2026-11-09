package com.example.core.util

import java.util.Locale

data class ExchangeCurrency(
    val code: String,
    val symbol: String,
    val name: String,
    val flag: String,
    val rateToInr: Double // Conversion factor: 1 Unit = rateToInr INR
)

object ExchangeRateManager {

    /**
     * Standard curated list of global currencies with base rates relative to INR (Indian Rupee).
     * Provides offline conversion calculator, multi-currency display, and quick rates.
     */
    val supportedCurrencies = listOf(
        ExchangeCurrency(code = "INR", symbol = "₹", name = "Indian Rupee", flag = "🇮🇳", rateToInr = 1.0),
        ExchangeCurrency(code = "USD", symbol = "$", name = "US Dollar", flag = "🇺🇸", rateToInr = 86.85),
        ExchangeCurrency(code = "EUR", symbol = "€", name = "Euro", flag = "🇪🇺", rateToInr = 92.40),
        ExchangeCurrency(code = "GBP", symbol = "£", name = "British Pound", flag = "🇬🇧", rateToInr = 110.15),
        ExchangeCurrency(code = "AED", symbol = "د.إ", name = "UAE Dirham", flag = "🇦🇪", rateToInr = 23.65),
        ExchangeCurrency(code = "JPY", symbol = "¥", name = "Japanese Yen", flag = "🇯🇵", rateToInr = 0.58),
        ExchangeCurrency(code = "CAD", symbol = "C$", name = "Canadian Dollar", flag = "🇨🇦", rateToInr = 62.50),
        ExchangeCurrency(code = "AUD", symbol = "A$", name = "Australian Dollar", flag = "🇦🇺", rateToInr = 56.40),
        ExchangeCurrency(code = "SGD", symbol = "S$", name = "Singapore Dollar", flag = "🇸🇬", rateToInr = 65.20),
        ExchangeCurrency(code = "CHF", symbol = "Fr", name = "Swiss Franc", flag = "🇨🇭", rateToInr = 98.10),
        ExchangeCurrency(code = "CNY", symbol = "¥", name = "Chinese Yuan", flag = "🇨🇳", rateToInr = 12.05),
        ExchangeCurrency(code = "THB", symbol = "฿", name = "Thai Baht", flag = "🇹🇭", rateToInr = 2.45),
        ExchangeCurrency(code = "SAR", symbol = "﷼", name = "Saudi Riyal", flag = "🇸🇦", rateToInr = 23.15),
        ExchangeCurrency(code = "MYR", symbol = "RM", name = "Malaysian Ringgit", flag = "🇲🇾", rateToInr = 19.85),
        ExchangeCurrency(code = "NZD", symbol = "NZ$", name = "New Zealand Dollar", flag = "🇳🇿", rateToInr = 51.30)
    )

    fun findCurrency(codeOrSymbol: String): ExchangeCurrency {
        return supportedCurrencies.firstOrNull {
            it.code.equals(codeOrSymbol, ignoreCase = true) || it.symbol == codeOrSymbol
        } ?: supportedCurrencies.first()
    }

    /**
     * Converts an amount from source currency to target currency.
     */
    fun convert(amount: Double, fromCodeOrSymbol: String, toCodeOrSymbol: String): Double {
        val from = findCurrency(fromCodeOrSymbol)
        val to = findCurrency(toCodeOrSymbol)
        if (from.code == to.code) return amount
        // Convert to INR first, then to target currency
        val inrValue = amount * from.rateToInr
        return inrValue / to.rateToInr
    }

    /**
     * Gets exchange rate between two currencies (1 From = X To).
     */
    fun getRate(fromCodeOrSymbol: String, toCodeOrSymbol: String): Double {
        val from = findCurrency(fromCodeOrSymbol)
        val to = findCurrency(toCodeOrSymbol)
        return from.rateToInr / to.rateToInr
    }

    /**
     * Formats rate description (e.g., "1 USD = 86.85 INR").
     */
    fun formatRatePair(fromCode: String, toCode: String): String {
        val rate = getRate(fromCode, toCode)
        return String.format(Locale.US, "1 %s = %.2f %s", fromCode, rate, toCode)
    }
}
