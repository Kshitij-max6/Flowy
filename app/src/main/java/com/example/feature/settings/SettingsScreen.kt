package com.example.feature.settings

import com.example.ui.theme.SystemOrange
import com.example.ui.theme.SystemTeal
import com.example.ui.theme.SystemBlue
import com.example.ui.theme.SystemIndigo
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SystemUpdate
import com.example.BuildConfig
import com.example.core.safety.DatabaseSafety
import com.example.data.local.AppDatabase
import com.example.feature.update.activityUpdateViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.heightIn
import com.example.ui.theme.pressable
import com.example.core.util.CurrencyFormatter
import com.example.ui.theme.EmeraldPrimary

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.example.ui.theme.HeroFontOption
import com.example.ui.theme.PrestigeTheme
import com.example.ui.theme.financialAmountStyle
import com.example.ui.theme.getFontFamilyForHero
import com.example.ui.theme.heroBalanceStyle

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToAccounts: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToInsights: () -> Unit,
    onNavigateToNetWorth: () -> Unit,
    onNavigateToCashFlow: () -> Unit,
    onNavigateToCreditCards: () -> Unit,
    onNavigateToLoans: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToCustomizeHome: () -> Unit = {},
    onNavigateToBooks: () -> Unit = {},
    onNavigateToAppearance: () -> Unit = {},
    onNavigateToSplit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settingsContext = LocalContext.current
    val updateViewModel = activityUpdateViewModel()
    var confirmRestoreBackup by remember { mutableStateOf(false) }
    val hasPreUpdateBackup = remember(confirmRestoreBackup) { DatabaseSafety.hasPreUpdateBackup(settingsContext) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Header
        item {
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp)) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Personalise Flowly, plan ahead and protect your data",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Profile Card
        item {
            val ringColor = MaterialTheme.colorScheme.primary
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .clickable { viewModel.openProfileDialog(true) }
                    .testTag("user_profile_card"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(ringColor.copy(alpha = 0.12f))
                                .border(1.5.dp, ringColor.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.userSettings.userName.take(1).uppercase(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = ringColor
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.userSettings.userName,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tap to edit display name",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfileChip("Offline")
                        ProfileChip("Private")
                        ProfileChip(uiState.selectedCurrency)
                    }
                }
            }
        }

        // Appearance: a single card that opens the dedicated Appearance screen.
        item {
            Spacer(modifier = Modifier.height(16.dp))
            SettingsMenuItem(
                icon = Icons.Default.Palette,
                title = "Appearance",
                subtitle = "Personalize your Flowly experience.",
                onClick = onNavigateToAppearance
            )
        }

        // Advanced Financial Tools Section
        item {
            Spacer(modifier = Modifier.height(16.dp))
            SettingsSectionLabel("Advanced financial tools")
        }

        item {
            SettingsMenuItem(
                accent = SystemIndigo,
                icon = Icons.Default.Tune,
                title = "Net Worth Tracking",
                subtitle = "Total assets vs liabilities, growth %, and trajectory",
                onClick = onNavigateToNetWorth
            )
        }

        item {
            SettingsMenuItem(
                accent = SystemIndigo,
                icon = Icons.Default.WaterDrop,
                title = "Cash Flow Forecast",
                subtitle = "Projected balance over 7, 30 days & safe daily spend",
                onClick = onNavigateToCashFlow
            )
        }

        item {
            SettingsMenuItem(
                accent = SystemIndigo,
                icon = Icons.Default.CurrencyExchange,
                title = "Credit Card Management",
                subtitle = "Track limits, utilization (<30%), and payment dues",
                onClick = onNavigateToCreditCards
            )
        }

        item {
            SettingsMenuItem(
                accent = SystemIndigo,
                icon = Icons.Default.AccountBalance,
                title = "Loans & Liabilities",
                subtitle = "Principal balance, interest breakdown, and EMI logging",
                onClick = onNavigateToLoans
            )
        }

        // Management Shortcuts
        item {
            Spacer(modifier = Modifier.height(16.dp))
            SettingsSectionLabel("Financial planning & tools")
        }

        item {
            SettingsMenuItem(
                accent = SystemBlue,
                icon = Icons.Default.Tune,
                title = "Home Screen Layout",
                subtitle = "Reorder and toggle sections on the main dashboard",
                onClick = onNavigateToCustomizeHome
            )
        }

        item {
            SettingsMenuItem(
                accent = SystemBlue,
                icon = Icons.Default.MenuBook,
                title = "Expense Books",
                subtitle = "Separate accounts for Personal, Business, Travel, Vacation",
                onClick = onNavigateToBooks
            )
        }

        item {
            SettingsMenuItem(
                accent = SystemBlue,
                icon = Icons.Default.PieChart,
                title = "Budgets & Limits",
                subtitle = "Daily, weekly, monthly & custom spending limits",
                onClick = onNavigateToBudgets
            )
        }

        item {
            SettingsMenuItem(
                accent = SystemBlue,
                icon = Icons.Default.Savings,
                title = "Financial Goals",
                subtitle = "Savings targets, emergency funds, and progress",
                onClick = onNavigateToGoals
            )
        }

        item {
            SettingsMenuItem(
                accent = SystemBlue,
                icon = Icons.Default.CalendarMonth,
                title = "Financial Calendar",
                subtitle = "Daily spending grid, recurring dues, and net flow",
                onClick = onNavigateToCalendar
            )
        }

        item {
            SettingsMenuItem(
                accent = SystemBlue,
                icon = Icons.Default.Insights,
                title = "Smart Insights",
                subtitle = "Local offline spending patterns and comparisons",
                onClick = onNavigateToInsights
            )
        }

        item {
            SettingsMenuItem(
                accent = SystemBlue,
                icon = Icons.Default.Repeat,
                title = "Subscriptions & Bills",
                subtitle = "Netflix, Spotify, monthly utilities & renewals",
                onClick = onNavigateToRecurring
            )
        }

        item {
            SettingsMenuItem(
                accent = SystemBlue,
                icon = Icons.Default.Search,
                title = "Global Search",
                subtitle = "Search transactions, accounts, categories, bills, and goals",
                onClick = onNavigateToSearch
            )
        }

        item {
            SettingsMenuItem(
                accent = SystemBlue,
                icon = Icons.Default.AccountBalance,
                title = "Accounts & Balances",
                subtitle = "Manage bank, UPI, cash, savings accounts",
                onClick = onNavigateToAccounts
            )
        }

        // Data Management Section
        item {
            Spacer(modifier = Modifier.height(16.dp))
            SettingsSectionLabel("Data management & reports")
        }

        item {
            SettingsMenuItem(
                accent = SystemTeal,
                icon = Icons.Default.FileDownload,
                title = "Data Export & Financial Reports",
                subtitle = "Generate CSV spreadsheets or printable PDF reports",
                onClick = onNavigateToExport
            )
        }

        item {
            SettingsMenuItem(
                accent = SystemTeal,
                icon = Icons.Default.FileUpload,
                title = "Backup & Restore",
                subtitle = "Export or restore full database snapshot offline",
                onClick = onNavigateToBackup
            )
        }

        item {
            SettingsMenuItem(
                accent = SystemTeal,
                icon = Icons.Default.Groups,
                title = "Split with Friends",
                subtitle = "Share expenses, see who owes whom, settle up",
                onClick = onNavigateToSplit
            )
        }

        item {
            SettingsMenuItem(
                accent = SystemTeal,
                icon = Icons.Default.SystemUpdate,
                title = "App updates",
                subtitle = "Version ${BuildConfig.VERSION_NAME} · tap to check for a new version",
                onClick = { updateViewModel.checkNow() }
            )
        }

        if (hasPreUpdateBackup) {
            item {
                SettingsMenuItem(
                    accent = SystemTeal,
                    icon = Icons.Default.Restore,
                    title = "Restore pre-update backup",
                    subtitle = "Go back to the data saved before the last update",
                    onClick = { confirmRestoreBackup = true }
                )
            }
        }

        // Security & Privacy Controls
        item {
            Spacer(modifier = Modifier.height(16.dp))
            SettingsSectionLabel("Security & privacy controls")
        }

        // Hide Amounts Switch
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(EmeraldPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (uiState.userSettings.hideAmounts) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Hide Sensitive Amounts",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Mask financial numbers across all screens",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = uiState.userSettings.hideAmounts,
                        onCheckedChange = { viewModel.toggleHideAmounts(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldPrimary
                        ),
                        modifier = Modifier.testTag("hide_amounts_switch")
                    )
                }
            }
        }

        // Remember Hide Amounts Switch
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Persist Privacy on App Restart",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Keep amounts hidden even after closing the app",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = uiState.userSettings.hideAmountsPersist,
                        onCheckedChange = { viewModel.toggleHideAmountsPersist(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldPrimary
                        ),
                        modifier = Modifier.testTag("persist_hide_amounts_switch")
                    )
                }
            }
        }

        // App Lock Switch
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(EmeraldPrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "App Lock (Biometrics & PIN)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Require device authentication to open Flowly",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = uiState.userSettings.appLockEnabled,
                            onCheckedChange = { viewModel.toggleAppLock(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldPrimary
                            ),
                            modifier = Modifier.testTag("app_lock_switch")
                        )
                    }

                    if (uiState.userSettings.appLockEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Auto-Lock Timeout",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(0 to "Instantly", 1 to "1 min", 5 to "5 min", 15 to "15 min").forEach { (mins, label) ->
                                val selected = uiState.userSettings.appLockTimeoutMinutes == mins
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selected) EmeraldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, if (selected) EmeraldPrimary else Color.Transparent),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.setAppLockTimeout(mins) }
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (selected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { com.example.core.util.AppLockManager.lockNow() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("lock_now_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = EmeraldPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Lock Flowly Now",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldPrimary
                            )
                        }
                    }
                }
            }
        }

        // Preferences Section
        item {
            Spacer(modifier = Modifier.height(16.dp))
            SettingsSectionLabel("Preferences")
        }

        item {
            SettingsMenuItem(
                accent = SystemOrange,
                icon = Icons.Default.CurrencyExchange,
                title = "Default Currency",
                subtitle = "${uiState.selectedCurrency} (Indian Rupee, Dollar, Euro...)",
                onClick = { viewModel.openCurrencyDialog(true) }
            )
        }

        item {
            SettingsMenuItem(
                accent = SystemOrange,
                icon = Icons.Default.Tune,
                title = "Daily & Monthly Budgets",
                subtitle = "Daily: ${CurrencyFormatter.format(uiState.userSettings.dailyBudget, uiState.selectedCurrency)} • Monthly: ${CurrencyFormatter.format(uiState.userSettings.monthlyBudget, uiState.selectedCurrency)}",
                onClick = { viewModel.openBudgetDialog(true) }
            )
        }

        // Privacy Section
        item {
            Spacer(modifier = Modifier.height(16.dp))
            SettingsSectionLabel("Privacy & philosophy")
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(EmeraldPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "100% Offline & Private",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Flowly stores all your financial data exclusively on your device in a local Room SQLite database. No tracking, no cloud servers, no account required.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // App Footer
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Flowly • v1.0.0",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Feel the flow of your money",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }

    // Profile Dialog
    if (uiState.showProfileDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.openProfileDialog(false) },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    "Display Name",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                OutlinedTextField(
                    value = uiState.userNameInput,
                    onValueChange = { viewModel.onUserNameChanged(it) },
                    label = { Text("Your Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.saveProfile() },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.openProfileDialog(false) }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Currency Selector Dialog
    if (uiState.showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.openCurrencyDialog(false) },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Column {
                    Text(
                        "Select Currency & Rates",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Multi-currency support with offline base conversions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(com.example.core.util.ExchangeRateManager.supportedCurrencies) { curr ->
                        val isSelected = uiState.selectedCurrency == curr.symbol
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) EmeraldPrimary.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable { viewModel.selectCurrency(curr.symbol) }
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = curr.flag,
                                    fontSize = 22.sp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${curr.name} (${curr.code})",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (curr.code != "INR") {
                                        Text(
                                            text = "1 ${curr.code} ≈ ₹${curr.rateToInr}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Text(
                                            text = "Base Currency",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldPrimary
                                        )
                                    }
                                }
                            }
                            Text(
                                text = curr.symbol,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.openCurrencyDialog(false) }) {
                    Text("Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Budget Configuration Dialog
    if (uiState.showBudgetDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.openBudgetDialog(false) },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    "Budget Limits",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = uiState.dailyBudgetInput,
                        onValueChange = { viewModel.onDailyBudgetChanged(it) },
                        label = { Text("Daily Budget Target (${uiState.selectedCurrency})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("daily_budget_input")
                    )

                    OutlinedTextField(
                        value = uiState.monthlyBudgetInput,
                        onValueChange = { viewModel.onMonthlyBudgetChanged(it) },
                        label = { Text("Monthly Budget Target (${uiState.selectedCurrency})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("monthly_budget_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.saveBudgets() },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.openBudgetDialog(false) }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (confirmRestoreBackup) {
        AlertDialog(
            onDismissRequest = { confirmRestoreBackup = false },
            title = { Text("Restore pre-update backup?") },
            text = {
                Text(
                    "Your current data will be replaced by the copy saved before the last update. " +
                        "The current data is kept aside, not deleted. Flowly will restart."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestoreBackup = false
                    AppDatabase.closeAndDiscard()
                    if (DatabaseSafety.restorePreUpdateBackup(settingsContext)) {
                        DatabaseSafety.restartApp(settingsContext)
                    }
                }) { Text("Restore", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmRestoreBackup = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun SettingsSectionLabel(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
private fun ProfileChip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    )
}

@Composable
fun SettingsMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = EmeraldPrimary
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .pressable(pressedScale = 0.985f, role = Role.Button, onClick = onClick)
            .testTag("settings_menu_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(21.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
