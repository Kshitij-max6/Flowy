package com.example.domain.model

data class UserSettings(
    val id: Int = 1,
    val userName: String = "Kshitij",
    val defaultCurrency: String = "₹",
    val dailyBudget: Double = 1000.0,
    val monthlyBudget: Double = 30000.0,
    val themeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val hasCompletedOnboarding: Boolean = false,
    val enableHaptics: Boolean = true,
    val hideAmounts: Boolean = false,
    val hideAmountsPersist: Boolean = true,
    val appLockEnabled: Boolean = false,
    val appLockTimeoutMinutes: Int = 0, // 0 = immediately, 1 = 1m, 5 = 5m, 15 = 15m
    val homeSectionsOrder: String = "spending,budget,recent,insight,upcoming,accounts",
    val hiddenHomeSections: String = "",
    val prestigeTheme: String = "OBSIDIAN_GOLD",
    val heroFont: String = "LUXURY_SERIF"
)
