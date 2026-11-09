package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.UserSettings

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val userName: String,
    val defaultCurrency: String,
    val dailyBudget: Double,
    val monthlyBudget: Double,
    val themeMode: String,
    val hasCompletedOnboarding: Boolean,
    val enableHaptics: Boolean,
    val hideAmounts: Boolean = false,
    val hideAmountsPersist: Boolean = true,
    val appLockEnabled: Boolean = false,
    val appLockTimeoutMinutes: Int = 0,
    val homeSectionsOrder: String = "spending,budget,recent,insight,upcoming,accounts",
    val hiddenHomeSections: String = "",
    val prestigeTheme: String = "OBSIDIAN_GOLD",
    val heroFont: String = "LUXURY_SERIF"
) {
    fun toDomain(): UserSettings = UserSettings(
        id = id,
        userName = userName,
        defaultCurrency = defaultCurrency,
        dailyBudget = dailyBudget,
        monthlyBudget = monthlyBudget,
        themeMode = themeMode,
        hasCompletedOnboarding = hasCompletedOnboarding,
        enableHaptics = enableHaptics,
        hideAmounts = hideAmounts,
        hideAmountsPersist = hideAmountsPersist,
        appLockEnabled = appLockEnabled,
        appLockTimeoutMinutes = appLockTimeoutMinutes,
        homeSectionsOrder = homeSectionsOrder,
        hiddenHomeSections = hiddenHomeSections,
        prestigeTheme = prestigeTheme,
        heroFont = heroFont
    )

    companion object {
        fun fromDomain(settings: UserSettings): UserSettingsEntity = UserSettingsEntity(
            id = settings.id,
            userName = settings.userName,
            defaultCurrency = settings.defaultCurrency,
            dailyBudget = settings.dailyBudget,
            monthlyBudget = settings.monthlyBudget,
            themeMode = settings.themeMode,
            hasCompletedOnboarding = settings.hasCompletedOnboarding,
            enableHaptics = settings.enableHaptics,
            hideAmounts = settings.hideAmounts,
            hideAmountsPersist = settings.hideAmountsPersist,
            appLockEnabled = settings.appLockEnabled,
            appLockTimeoutMinutes = settings.appLockTimeoutMinutes,
            homeSectionsOrder = settings.homeSectionsOrder,
            hiddenHomeSections = settings.hiddenHomeSections,
            prestigeTheme = settings.prestigeTheme,
            heroFont = settings.heroFont
        )
    }
}
