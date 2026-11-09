package com.example.core.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {

    /**
     * When true, all currency amounts are masked across the app as ₹••••••
     */
    var isPrivacyActive: Boolean = false

    fun mask(currencySymbol: String = "₹"): String = "$currencySymbol••••••"

    fun maskWithoutSymbol(): String = "••••••"

    fun format(
        amount: Double,
        currencySymbol: String = "₹",
        showDecimalIfZero: Boolean = false,
        forceShow: Boolean = false
    ): String {
        if (isPrivacyActive && !forceShow) {
            return mask(currencySymbol)
        }
        val absAmount = kotlin.math.abs(amount)
        val format = if (!showDecimalIfZero && absAmount % 1.0 == 0.0) {
            val df = DecimalFormat("#,##,##0")
            df.format(absAmount)
        } else {
            val df = DecimalFormat("#,##,##0.00")
            df.format(absAmount)
        }
        val prefix = if (amount < 0) "-" else ""
        return "$prefix$currencySymbol$format"
    }

    fun formatWithoutSymbol(amount: Double, forceShow: Boolean = false): String {
        if (isPrivacyActive && !forceShow) {
            return maskWithoutSymbol()
        }
        val absAmount = kotlin.math.abs(amount)
        val format = if (absAmount % 1.0 == 0.0) {
            val df = DecimalFormat("#,##,##0")
            df.format(absAmount)
        } else {
            val df = DecimalFormat("#,##,##0.00")
            df.format(absAmount)
        }
        val prefix = if (amount < 0) "-" else ""
        return "$prefix$format"
    }

    fun formatCompact(
        amount: Double,
        currencySymbol: String = "₹",
        forceShow: Boolean = false
    ): String {
        if (isPrivacyActive && !forceShow) {
            return mask(currencySymbol)
        }
        val absAmount = kotlin.math.abs(amount)
        val prefix = if (amount < 0) "-" else ""
        return when {
            absAmount >= 10_000_000 -> "$prefix$currencySymbol${DecimalFormat("#,##0.#").format(absAmount / 10_000_000)}Cr"
            absAmount >= 100_000 -> "$prefix$currencySymbol${DecimalFormat("#,##0.#").format(absAmount / 100_000)}L"
            absAmount >= 1_000 -> "$prefix$currencySymbol${DecimalFormat("#,##0.#").format(absAmount / 1_000)}k"
            else -> format(amount, currencySymbol, showDecimalIfZero = false, forceShow = forceShow)
        }
    }

    val availableCurrencies = listOf(
        CurrencyOption("₹", "INR", "Indian Rupee"),
        CurrencyOption("$", "USD", "US Dollar"),
        CurrencyOption("€", "EUR", "Euro"),
        CurrencyOption("£", "GBP", "British Pound"),
        CurrencyOption("¥", "JPY", "Japanese Yen"),
        CurrencyOption("₩", "KRW", "South Korean Won"),
        CurrencyOption("C$", "CAD", "Canadian Dollar"),
        CurrencyOption("A$", "AUD", "Australian Dollar"),
        CurrencyOption("AED", "AED", "UAE Dirham"),
        CurrencyOption("S$", "SGD", "Singapore Dollar"),
        CurrencyOption("CHF", "CHF", "Swiss Franc")
    )
}

data class CurrencyOption(
    val symbol: String,
    val code: String,
    val name: String
)
