package com.example.feature.add_transaction

import android.app.Activity
import com.example.ui.theme.asAmount
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.core.ui.AppleSegmentedControl
import com.example.core.ui.AppleIconButton
import com.example.core.util.CategoryIconHelper
import com.example.core.util.CurrencyFormatter
import com.example.core.util.DateUtils
import com.example.domain.model.CategoryType
import com.example.domain.model.TransactionType
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.TransferCyan
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionScreen(
    viewModel: AddTransactionViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showFullImageDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onReceiptImageSelected(context, uri)
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                viewModel.applyVoiceInput(spoken)
            }
        }
    }

    val launchVoiceRecognizer = {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Say description or expense details (e.g. 'Coffee with friends' or 'Spent 450 on groceries')")
            }
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            // Handled gracefully if speech recognition intent is not present
        }
    }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is AddTransactionEvent.SaveSuccess -> onNavigateBack()
            }
        }
    }

    val typeColor = when (uiState.selectedType) {
        TransactionType.EXPENSE -> ExpenseRed
        TransactionType.INCOME -> IncomeGreen
        TransactionType.TRANSFER -> TransferCyan
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
            .testTag("add_transaction_screen")
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AppleIconButton(
                icon = Icons.Default.Close,
                contentDescription = "Close",
                onClick = onNavigateBack,
                modifier = Modifier.testTag("add_tx_back_button")
            )

            Text(
                text = if (uiState.editId > 0) "Edit Transaction" else "New Transaction",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Box(modifier = Modifier.size(40.dp)) // balance spacer
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Smart Quick-Fill Strip (Only in Add Mode)
            if (uiState.editId <= 0 && (uiState.quickAddSuggestions.frequentPatterns.isNotEmpty() || uiState.quickAddSuggestions.lastTransaction != null)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Smart Quick Fill",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (uiState.quickAddSuggestions.lastTransaction != null) {
                        TextButton(
                            onClick = { viewModel.onDuplicateLastTransaction() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("duplicate_last_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Repeat Last", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = EmeraldPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.quickAddSuggestions.frequentPatterns.forEach { pattern ->
                        Card(
                            onClick = { viewModel.onQuickFillPattern(pattern) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("frequent_pattern_${pattern.title}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldPrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = CategoryIconHelper.getIconByName(pattern.categoryIcon),
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = pattern.title,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = CurrencyFormatter.format(pattern.amount, uiState.defaultCurrency),
                                        style = MaterialTheme.typography.labelSmall.asAmount(),
                                        color = ExpenseRed
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Type switcher (Expense | Income | Transfer): sliding segmented control
            val transactionTypes = TransactionType.values().toList()
            AppleSegmentedControl(
                options = transactionTypes.map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                selectedIndex = transactionTypes.indexOf(uiState.selectedType).coerceAtLeast(0),
                onSelect = { viewModel.onTypeSelected(transactionTypes[it]) },
                testTags = transactionTypes.map { "type_selector_${it.name.lowercase()}" }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Input Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Amount",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = uiState.defaultCurrency,
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = typeColor
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedTextField(
                            value = uiState.amountInput,
                            onValueChange = { viewModel.onAmountChanged(it) },
                            placeholder = {
                                Text(
                                    "0",
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    )
                                )
                            },
                            textStyle = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .width(200.dp)
                                .testTag("amount_text_field")
                        )
                    }

                    // Multi-Currency preview if currency is not INR
                    val inputAmt = uiState.amountInput.toDoubleOrNull()
                    if (inputAmt != null && inputAmt > 0 && uiState.defaultCurrency != "₹") {
                        val inrEquiv = com.example.core.util.ExchangeRateManager.convert(inputAmt, uiState.defaultCurrency, "₹")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "≈ ₹${java.text.DecimalFormat("#,##0.00").format(inrEquiv)} INR",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Description / Note Box (Directly below Amount)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Description / What was this for?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Voice Dictation Button right beside description
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = RoyalGold.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { launchVoiceRecognizer() }
                        .testTag("voice_description_action_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Dictation",
                            tint = RoyalGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Voice Add",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = RoyalGold,
                                fontSize = 11.5.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = uiState.noteInput,
                onValueChange = { viewModel.onNoteChanged(it) },
                placeholder = {
                    Text(
                        "e.g. Vegetables, Dinner, Coffee, Electricity bill, Petrol...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                },
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        if (uiState.noteInput.isNotBlank()) {
                            IconButton(
                                onClick = { viewModel.onNoteChanged("") },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear description",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { launchVoiceRecognizer() },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("mic_description_field_icon")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Dictate Description",
                                tint = RoyalGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = EmeraldPrimary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("description_text_field")
            )

            // Quick item / merchant suggestions (e.g. Vegetables, Groceries, Coffee, Swiggy)
            val defaultQuickSuggestions = listOf("Vegetables", "Groceries", "Coffee & Snacks", "Dinner", "Petrol/Fuel", "Medicine", "Milk & Dairy", "Uber/Cab")
            val quickChips = if (uiState.merchantSuggestions.isNotEmpty()) {
                (uiState.merchantSuggestions + defaultQuickSuggestions).distinct().take(8)
            } else {
                defaultQuickSuggestions
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickChips.forEach { suggestion ->
                    val isSelected = uiState.noteInput.equals(suggestion, ignoreCase = true)
                    Card(
                        onClick = { viewModel.onMerchantSuggestionSelected(suggestion) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) EmeraldPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ),
                        border = BorderStroke(1.dp, if (isSelected) EmeraldPrimary else Color.Transparent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("merchant_suggestion_$suggestion")
                    ) {
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Account Selection (Source Account)
            Text(
                text = if (uiState.selectedType == TransactionType.TRANSFER) "From Account" else "Account",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.accounts.forEach { account ->
                    val isSelected = uiState.selectedAccountId == account.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onAccountSelected(account.id) },
                        label = { Text(account.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                        leadingIcon = {
                            Icon(
                                imageVector = CategoryIconHelper.getIconByName(account.iconName),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = BorderStroke(1.dp, if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("account_chip_${account.id}")
                    )
                }
            }

            // Transfer Destination Account
            if (uiState.selectedType == TransactionType.TRANSFER) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "To Account",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.accounts.forEach { account ->
                        val isSelected = uiState.selectedDestinationAccountId == account.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onDestinationAccountSelected(account.id) },
                            label = { Text(account.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                            leadingIcon = {
                                Icon(
                                    imageVector = CategoryIconHelper.getIconByName(account.iconName),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TransferCyan,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = BorderStroke(1.dp, if (isSelected) TransferCyan else MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("dest_account_chip_${account.id}")
                        )
                    }
                }
            }

            // Several entries typed as one sentence, e.g. "chai 40 and auto 120 yesterday"
            if (uiState.multiEntryCount >= 2) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.openMultiPreview() },
                    modifier = Modifier.fillMaxWidth().testTag("multi_entry_button")
                ) {
                    Text("Add ${uiState.multiEntryCount} separate entries from this sentence")
                }
            }

            // Category Selection (Only for Expense & Income)
            if (uiState.selectedType != TransactionType.TRANSFER) {
                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val selectedCategory = uiState.categories.firstOrNull { it.id == uiState.selectedCategoryId }
                        if (selectedCategory != null) {
                            val parentCat = if (selectedCategory.parentCategoryId != null) {
                                uiState.categories.firstOrNull { it.id == selectedCategory.parentCategoryId }
                            } else null
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (parentCat != null) "${parentCat.name} › ${selectedCategory.name}" else selectedCategory.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldPrimary
                                )
                                if (uiState.suggestedCategoryId == selectedCategory.id) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "✨ Suggested",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = RoyalGold,
                                        modifier = Modifier.testTag("category_suggested_label")
                                    )
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = { viewModel.showAddCategoryDialog(true) },
                        modifier = Modifier.testTag("add_custom_category_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Custom", style = MaterialTheme.typography.labelMedium, color = EmeraldPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val targetCatType = if (uiState.selectedType == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
                val allRelevantCategories = uiState.categories.filter { it.type == targetCatType }
                val parentCategories = allRelevantCategories.filter { it.parentCategoryId == null || it.parentCategoryId == 0L }

                // Search field for rapid lookup
                OutlinedTextField(
                    value = uiState.categorySearchQuery,
                    onValueChange = { viewModel.onCategorySearchChanged(it) },
                    placeholder = { Text("Search categories & subcategories...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (uiState.categorySearchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.onCategorySearchChanged("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("category_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (uiState.categorySearchQuery.isNotBlank()) {
                    // Search results view
                    val query = uiState.categorySearchQuery.trim().lowercase()
                    val filtered = allRelevantCategories.filter { it.name.lowercase().contains(query) }

                    if (filtered.isEmpty()) {
                        Text(
                            text = "No categories matching \"$query\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            filtered.forEach { category ->
                                val isSelected = uiState.selectedCategoryId == category.id
                                val parent = if (category.parentCategoryId != null) {
                                    allRelevantCategories.firstOrNull { it.id == category.parentCategoryId }
                                } else null
                                val displayName = if (parent != null) "${parent.name} › ${category.name}" else category.name

                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.onCategorySelected(category.id) },
                                    label = { Text(displayName, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = CategoryIconHelper.getIconByName(category.iconName),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldPrimary,
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White,
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = BorderStroke(1.dp, if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outlineVariant),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("category_chip_${category.id}")
                                )
                            }
                        }
                    }
                } else {
                    // Standard Hierarchical View: Main Categories horizontal row / chips
                    Text(
                        text = "Main Categories",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        parentCategories.forEach { parent ->
                            val isParentActive = uiState.selectedParentCategoryId == parent.id || uiState.selectedCategoryId == parent.id
                            FilterChip(
                                selected = isParentActive,
                                onClick = {
                                    viewModel.onParentCategoryFilterSelected(parent.id)
                                    if (uiState.selectedCategoryId == null || uiState.categories.none { it.id == uiState.selectedCategoryId && it.parentCategoryId == parent.id }) {
                                        viewModel.onCategorySelected(parent.id)
                                    }
                                },
                                label = { Text(parent.name, fontWeight = if (isParentActive) FontWeight.Bold else FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = CategoryIconHelper.getIconByName(parent.iconName),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = BorderStroke(1.dp, if (isParentActive) EmeraldPrimary else MaterialTheme.colorScheme.outlineVariant),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("parent_cat_chip_${parent.id}")
                            )
                        }
                    }

                    // Subcategories for active parent
                    val activeParentId = uiState.selectedParentCategoryId ?: parentCategories.firstOrNull()?.id
                    val subcategories = if (activeParentId != null) {
                        allRelevantCategories.filter { it.parentCategoryId == activeParentId }
                    } else emptyList()

                    if (subcategories.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Subcategories",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Option for General / Parent Category itself
                            val activeParent = parentCategories.firstOrNull { it.id == activeParentId }
                            if (activeParent != null) {
                                val isGeneralSelected = uiState.selectedCategoryId == activeParent.id
                                FilterChip(
                                    selected = isGeneralSelected,
                                    onClick = { viewModel.onCategorySelected(activeParent.id) },
                                    label = { Text("General ${activeParent.name}", fontWeight = if (isGeneralSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldPrimary.copy(alpha = 0.25f),
                                        selectedLabelColor = EmeraldPrimary,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = BorderStroke(1.dp, if (isGeneralSelected) EmeraldPrimary else Color.Transparent),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("category_chip_${activeParent.id}")
                                )
                            }

                            subcategories.forEach { sub ->
                                val isSelected = uiState.selectedCategoryId == sub.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.onCategorySelected(sub.id) },
                                    label = { Text(sub.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = CategoryIconHelper.getIconByName(sub.iconName),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldPrimary,
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White,
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = BorderStroke(1.dp, if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outlineVariant),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("category_chip_${sub.id}")
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Expense Book Selection
            if (uiState.books.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Expense Book",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val currentBook = uiState.books.firstOrNull { it.id == uiState.selectedBookId }
                    if (currentBook != null) {
                        Text(
                            text = currentBook.name,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = com.example.feature.books.parseColorSafe(currentBook.colorHex)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.books.forEach { book ->
                        val isSelected = uiState.selectedBookId == book.id
                        val bookColor = com.example.feature.books.parseColorSafe(book.colorHex)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onBookSelected(book.id) },
                            label = { Text(book.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                            leadingIcon = {
                                Icon(
                                    imageVector = com.example.feature.books.getBookIcon(book.iconName),
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else bookColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = bookColor,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = BorderStroke(1.dp, if (isSelected) bookColor else MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("expense_book_chip_${book.id}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // Date & Time Picker Row
            Text(
                text = "Date & Time",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Date Button
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            val cal = Calendar.getInstance().apply { timeInMillis = uiState.selectedTimestamp }
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val newCal = Calendar.getInstance().apply {
                                        timeInMillis = uiState.selectedTimestamp
                                        set(Calendar.YEAR, year)
                                        set(Calendar.MONTH, month)
                                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    }
                                    viewModel.onTimestampChanged(newCal.timeInMillis)
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .testTag("date_picker_button"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp), tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = DateUtils.formatDate(uiState.selectedTimestamp),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }

                // Time Button
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            val cal = Calendar.getInstance().apply { timeInMillis = uiState.selectedTimestamp }
                            TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    val newCal = Calendar.getInstance().apply {
                                        timeInMillis = uiState.selectedTimestamp
                                        set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        set(Calendar.MINUTE, minute)
                                    }
                                    viewModel.onTimestampChanged(newCal.timeInMillis)
                                },
                                cal.get(Calendar.HOUR_OF_DAY),
                                cal.get(Calendar.MINUTE),
                                false
                            ).show()
                        }
                        .testTag("time_picker_button"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp), tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = DateUtils.formatTime(uiState.selectedTimestamp),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Receipt / Attachment Section
            Text(
                text = "Receipt / Attachment (Optional)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.attachmentUri != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { showFullImageDialog = true }
                                .testTag("receipt_thumbnail_preview")
                        ) {
                            AsyncImage(
                                model = uiState.attachmentUri,
                                contentDescription = "Receipt Attachment",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (uiState.isScanningReceipt) "Scanning Receipt..." else "Receipt Attached",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = uiState.scannedReceiptMessage ?: "Tap thumbnail to view preview",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (uiState.isScanningReceipt) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { viewModel.onAttachmentChanged(null) },
                            modifier = Modifier.testTag("remove_receipt_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove Attachment",
                                tint = ExpenseRed
                            )
                        }
                    }
                }
            } else {
                Card(
                    onClick = { photoPickerLauncher.launch("image/*") },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_receipt_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add Receipt / Bill Photo",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = EmeraldPrimary
                        )
                    }
                }
            }

            // Error message display
            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = uiState.errorMessage ?: "",
                    color = ExpenseRed,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier.testTag("add_transaction_error_text")
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Primary Save Button
            Button(
                onClick = { viewModel.saveTransaction() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_transaction_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                enabled = !uiState.isSaving
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (uiState.editId > 0) "Update Transaction" else "Save Transaction",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Receipt Fullscreen Preview Dialog
    if (showFullImageDialog && uiState.attachmentUri != null) {
        Dialog(onDismissRequest = { showFullImageDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Receipt Preview",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { showFullImageDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                    ) {
                        AsyncImage(
                            model = uiState.attachmentUri,
                            contentDescription = "Receipt Full Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showFullImageDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    uiState.multiDrafts?.let { drafts ->
        MultiEntryPreviewDialog(
            drafts = drafts,
            categories = uiState.categories,
            currency = uiState.defaultCurrency,
            isSaving = uiState.isSaving,
            onChange = { key, change -> viewModel.updateDraft(key, change) },
            onRemove = { viewModel.removeDraft(it) },
            onConfirm = { viewModel.saveMultiEntries() },
            onDismiss = { viewModel.closeMultiPreview() }
        )
    }

    // Custom Category Creation Dialog
    if (uiState.showCustomCategoryDialog) {
        val targetCatType = if (uiState.selectedType == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
        val parentOptions = uiState.categories.filter { it.type == targetCatType && (it.parentCategoryId == null || it.parentCategoryId == 0L) }

        AlertDialog(
            onDismissRequest = { viewModel.showAddCategoryDialog(false) },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("New Custom Category", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = uiState.newCategoryName,
                        onValueChange = { viewModel.onNewCategoryNameChanged(it) },
                        label = { Text("Category Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("custom_cat_name_input")
                    )

                    // Parent Category Hierarchy Option
                    Text("Parent Category (Optional)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isNoneSelected = uiState.newCategoryParentId == null
                        FilterChip(
                            selected = isNoneSelected,
                            onClick = { viewModel.onNewCategoryParentSelected(null) },
                            label = { Text("None (Main Category)", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        parentOptions.forEach { parent ->
                            val isSelected = uiState.newCategoryParentId == parent.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.onNewCategoryParentSelected(parent.id) },
                                label = { Text(parent.name, fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = CategoryIconHelper.getIconByName(parent.iconName),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    // Color Selection
                    Text("Color Tag", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryIconHelper.curatedCategoryColors.forEach { colorHex ->
                            val isColorSelected = uiState.newCategoryColor.equals(colorHex, ignoreCase = true)
                            val parsedColor = try {
                                Color(android.graphics.Color.parseColor(colorHex))
                            } catch (e: Exception) {
                                EmeraldPrimary
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(parsedColor)
                                    .border(
                                        BorderStroke(2.dp, if (isColorSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent),
                                        CircleShape
                                    )
                                    .clickable { viewModel.onNewCategoryColorChanged(colorHex) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isColorSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Icon Selection
                    Text("Select Icon", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryIconHelper.availableCategoryIcons.forEach { iconName ->
                            val isSelected = uiState.newCategoryIcon == iconName
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) EmeraldPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(BorderStroke(1.dp, if (isSelected) EmeraldPrimary else Color.Transparent), CircleShape)
                                    .clickable { viewModel.onNewCategoryIconChanged(iconName) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CategoryIconHelper.getIconByName(iconName),
                                    contentDescription = iconName,
                                    tint = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.createCustomCategory() },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                    modifier = Modifier.testTag("save_custom_category_button")
                ) {
                    Text("Create", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showAddCategoryDialog(false) }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}
