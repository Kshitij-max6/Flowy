package com.example.domain.model

data class Account(
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val balance: Double,
    val currency: String = "₹",
    val iconName: String = "AccountBalance",
    val colorHex: String = "#10B981",
    val isDefault: Boolean = false
)
