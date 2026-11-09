package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.lifecycleScope
import com.example.core.safety.DatabaseSafety
import com.example.core.util.AppLockManager
import com.example.feature.update.RecoveryScreen
import com.example.core.util.CurrencyFormatter
import com.example.data.local.AppDatabase
import com.example.data.repository.FlowlyRepositoryImpl
import com.example.domain.model.UserSettings
import com.example.navigation.FlowlyApp
import com.example.navigation.Screen
import com.example.ui.theme.FlowlyTheme
import com.example.ui.theme.HeroFontOption
import com.example.ui.theme.PrestigeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: FlowlyRepositoryImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Safety copy of the data on the first launch of every new app version, before Room touches the file.
        DatabaseSafety.backupIfAppVersionChanged(applicationContext)

        // Open the database up front. If it can't be opened, show the recovery screen instead of crashing.
        if (AppDatabase.openOrReport(applicationContext) != null) {
            setContent {
                FlowlyTheme(
                    prestigeTheme = PrestigeTheme.fromString(null),
                    heroFont = HeroFontOption.fromString(null),
                    darkTheme = isSystemInDarkTheme()
                ) { RecoveryScreen() }
            }
            return
        }

        val database = AppDatabase.getDatabase(applicationContext)

        // One-time removal of the demo data older versions installed. Only exact demo rows are touched.
        removeSampleDataOnce(database)
        repository = FlowlyRepositoryImpl(
            categoryDao = database.categoryDao(),
            accountDao = database.accountDao(),
            transactionDao = database.transactionDao(),
            recurringDao = database.recurringTransactionDao(),
            userSettingsDao = database.userSettingsDao(),
            budgetDao = database.budgetDao(),
            goalDao = database.goalDao(),
            creditCardDao = database.creditCardDao(),
            loanDao = database.loanDao(),
            netWorthDao = database.netWorthDao(),
            savedFilterDao = database.savedFilterDao(),
            recentSearchDao = database.recentSearchDao(),
            database = database
        )

        lifecycleScope.launch(Dispatchers.IO) {
            repository.checkAndSeedInitialData()
            val settings = repository.getUserSettings().first()
            if (settings.hideAmountsPersist) {
                CurrencyFormatter.isPrivacyActive = settings.hideAmounts
            } else {
                CurrencyFormatter.isPrivacyActive = false
            }
            AppLockManager.setInitialLockState(settings.appLockEnabled)
            setupDynamicShortcuts()
        }

        val navigateExtra = intent?.getStringExtra("navigate_to")
        val initialVoice = navigateExtra == "voice_expense"
        val startDest = when (navigateExtra) {
            "add_transaction" -> Screen.AddTransaction.createRoute(-1L)
            "expense_books" -> Screen.ExpenseBooks.route
            "insights" -> Screen.Insights.route
            else -> Screen.Home.route
        }

        setContent {
            val userSettingsFlow = remember { repository.getUserSettings() }
            val userSettings by userSettingsFlow.collectAsState(initial = UserSettings())
            val isSystemDark = isSystemInDarkTheme()

            val isDarkTheme = when (userSettings.themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemDark
            }

            FlowlyTheme(
                prestigeTheme = PrestigeTheme.fromString(userSettings.prestigeTheme),
                heroFont = HeroFontOption.fromString(userSettings.heroFont),
                darkTheme = isDarkTheme
            ) {
                FlowlyApp(
                    repository = repository,
                    startDestination = startDest,
                    initialShowVoiceInput = initialVoice
                )
            }
        }
    }

    private fun setupDynamicShortcuts() {
        try {
            val voiceShortcut = ShortcutInfoCompat.Builder(this, "dynamic_voice_expense")
                .setShortLabel(getString(R.string.shortcut_voice_expense_short))
                .setLongLabel(getString(R.string.shortcut_voice_expense_long))
                .setIcon(IconCompat.createWithResource(this, R.drawable.ic_shortcut_mic))
                .setIntent(
                    Intent(this, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        putExtra("navigate_to", "voice_expense")
                    }
                )
                .build()

            val addShortcut = ShortcutInfoCompat.Builder(this, "dynamic_add_expense")
                .setShortLabel(getString(R.string.shortcut_add_expense_short))
                .setLongLabel(getString(R.string.shortcut_add_expense_long))
                .setIcon(IconCompat.createWithResource(this, R.drawable.ic_shortcut_add))
                .setIntent(
                    Intent(this, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        putExtra("navigate_to", "add_transaction")
                    }
                )
                .build()

            ShortcutManagerCompat.addDynamicShortcuts(this, listOf(voiceShortcut, addShortcut))
        } catch (_: Exception) {
            // Gracefully ignore shortcut errors on older devices / test runners
        }
    }

    private fun removeSampleDataOnce(database: AppDatabase) {
        val prefs = getSharedPreferences("flowly_safety", MODE_PRIVATE)
        if (prefs.getBoolean("sample_data_removed_v1", false)) return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // A fresh install has no demo rows; an upgraded one gets them removed.
                com.example.core.util.SampleDataCleaner.removeSampleData(database)
                prefs.edit().putBoolean("sample_data_removed_v1", true).apply()
            } catch (_: Exception) {
                // Try again next launch; nothing was changed because the cleanup runs in one transaction.
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!::repository.isInitialized) return
        lifecycleScope.launch(Dispatchers.IO) {
            val settings = repository.getUserSettings().first()
            AppLockManager.onAppForegrounded(
                appLockEnabled = settings.appLockEnabled,
                timeoutMinutes = settings.appLockTimeoutMinutes
            )
        }
    }

    override fun onStop() {
        super.onStop()
        AppLockManager.onAppBackgrounded()
    }
}
