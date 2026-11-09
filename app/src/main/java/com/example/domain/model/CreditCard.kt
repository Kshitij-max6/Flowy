package com.example.domain.model

data class CreditCard(
    val id: Long = 0,
    val name: String,
    val bankName: String = "",
    val cardNumberMasked: String = "•••• 4821",
    val creditLimit: Double,
    val outstandingBalance: Double,
    val billingCycleDay: Int = 15,
    val paymentDueDay: Int = 5,
    val linkedAccountId: Long? = null,
    val currency: String = "₹",
    val cardColorHex: String = "#1E293B",
    val status: CreditCardStatus = CreditCardStatus.ACTIVE
) {
    val availableCredit: Double
        get() = (creditLimit - outstandingBalance).coerceAtLeast(0.0)

    val utilizationPercentage: Double
        get() = if (creditLimit > 0) ((outstandingBalance / creditLimit) * 100).coerceIn(0.0, 100.0) else 0.0

    val isOverLimit: Boolean
        get() = outstandingBalance > creditLimit
}
