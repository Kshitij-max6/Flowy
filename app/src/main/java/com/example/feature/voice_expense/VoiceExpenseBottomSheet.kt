package com.example.feature.voice_expense

import android.app.Activity
import com.example.ui.theme.asAmount
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.CategoryIconHelper
import com.example.core.util.CurrencyFormatter
import com.example.core.util.ParsedVoiceExpense
import com.example.core.util.VoiceExpenseParser
import com.example.domain.model.Account
import com.example.domain.model.Category
import com.example.domain.model.ExpenseBook
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PrestigeGold
import com.example.ui.theme.RoyalGold
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VoiceExpenseBottomSheet(
    onDismissRequest: () -> Unit,
    categories: List<Category>,
    accounts: List<Account>,
    books: List<ExpenseBook>,
    defaultCurrency: String = "₹",
    onSaveTransaction: (Transaction) -> Unit,
    onOpenInFullEditor: (ParsedVoiceExpense) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var spokenText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var parsedResult by remember { mutableStateOf<ParsedVoiceExpense?>(null) }
    var manualInput by remember { mutableStateOf("") }

    // Speech Recognizer Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                spokenText = spoken
                manualInput = spoken
                parsedResult = VoiceExpenseParser.parse(spoken, categories, accounts, books)
            }
        }
    }

    fun startListening() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your expense (e.g. 'Spent 450 on Starbucks coffee with HDFC card')")
            }
            isListening = true
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            isListening = false
            // Speech recognition intent not available on some emulators, user can still use direct input or templates
        }
    }

    // Auto-launch speech recognition once opened
    LaunchedEffect(Unit) {
        startListening()
    }

    // Pulse animation for Mic
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.12f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
        modifier = Modifier.testTag("voice_expense_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(RoyalGold, PrestigeGold))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            tint = ObsidianBlack,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Voice Expense AI",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Natural language expense logging",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = {
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
                    }
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Animated Mic Centerpiece
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(110.dp)
                    .clickable { startListening() }
                    .testTag("voice_mic_button")
            ) {
                // Outer glow ring
                Box(
                    modifier = Modifier
                        .size(105.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(RoyalGold.copy(alpha = if (isListening) 0.35f else 0.15f))
                )

                // Middle ring
                Box(
                    modifier = Modifier
                        .size(85.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(RoyalGold.copy(alpha = 0.6f), PrestigeGold.copy(alpha = 0.8f)))
                        )
                )

                // Core mic button
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(RoyalGold, PrestigeGold))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isListening) Icons.Default.GraphicEq else Icons.Default.Mic,
                        contentDescription = "Speak Expense",
                        tint = ObsidianBlack,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isListening) "Listening... Speak naturally" else "Tap mic to speak or select template",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                color = if (isListening) RoyalGold else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Spoken text / Input Box
            OutlinedTextField(
                value = manualInput,
                onValueChange = {
                    manualInput = it
                    spokenText = it
                    parsedResult = if (it.isNotBlank()) {
                        VoiceExpenseParser.parse(it, categories, accounts, books)
                    } else null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voice_input_field"),
                placeholder = {
                    Text(
                        "e.g. 'Spent 450 for lunch at Starbucks with HDFC'",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                trailingIcon = {
                    if (manualInput.isNotBlank()) {
                        IconButton(onClick = {
                            manualInput = ""
                            spokenText = ""
                            parsedResult = null
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoyalGold,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Speech Templates
            Text(
                text = "Try examples",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val samples = listOf(
                    "₹450 Starbucks coffee with HDFC",
                    "₹1,200 Shell petrol today",
                    "₹2,500 Groceries at DMart",
                    "₹3,500 Flight ticket Goa book",
                    "₹50,000 Salary received SBI"
                )
                samples.forEach { sample ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier
                            .clickable {
                                manualInput = sample
                                spokenText = sample
                                parsedResult = VoiceExpenseParser.parse(sample, categories, accounts, books)
                            }
                            .testTag("voice_sample_chip_${sample.take(6)}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = sample,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Real-Time Parsed Summary Card
            AnimatedVisibility(
                visible = parsedResult != null && (parsedResult?.amount != null || parsedResult?.merchantOrNote?.isNotBlank() == true),
                enter = fadeIn() + slideInVertically()
            ) {
                parsedResult?.let { parsed ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("voice_parsed_preview_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        border = BorderStroke(1.2.dp, RoyalGold.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "AI Interpreted Transaction",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
                                        color = RoyalGold
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when (parsed.type) {
                                        TransactionType.INCOME -> IncomeGreen.copy(alpha = 0.18f)
                                        TransactionType.EXPENSE -> ExpenseRed.copy(alpha = 0.18f)
                                        TransactionType.TRANSFER -> EmeraldPrimary.copy(alpha = 0.18f)
                                    }
                                ) {
                                    Text(
                                        text = parsed.type.name,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                        color = when (parsed.type) {
                                            TransactionType.INCOME -> IncomeGreen
                                            TransactionType.EXPENSE -> ExpenseRed
                                            TransactionType.TRANSFER -> EmeraldPrimary
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Large Amount
                            val amt = parsed.amount ?: 0.0
                            Text(
                                text = CurrencyFormatter.format(amt, defaultCurrency),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 28.sp).asAmount(),
                                color = if (parsed.type == TransactionType.INCOME) IncomeGreen else MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Details Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Category Pill
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            CategoryIconHelper.getIcon(parsed.matchedCategory?.iconName ?: "Payment"),
                                            contentDescription = null,
                                            tint = parsed.matchedCategory?.let { Color(android.graphics.Color.parseColor(it.colorHex)) } ?: RoyalGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = parsed.matchedCategory?.name ?: "Expense",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp),
                                            maxLines = 1
                                        )
                                    }
                                }

                                // Account Pill
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.AccountBalance,
                                            contentDescription = null,
                                            tint = RoyalGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = parsed.matchedAccount?.name ?: "Default Account",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Note & Book Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Merchant / Note
                                Surface(
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = parsed.merchantOrNote.ifBlank { "No note" },
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                            maxLines = 1
                                        )
                                    }
                                }

                                // Expense Book
                                parsed.matchedBook?.let { book ->
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Book,
                                                contentDescription = null,
                                                tint = Color(android.graphics.Color.parseColor(book.colorHex)),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = book.name,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 11.5.sp),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val parsed = parsedResult ?: VoiceExpenseParser.parse(manualInput, categories, accounts, books)
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                            onDismissRequest()
                            onOpenInFullEditor(parsed)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("voice_open_editor_button"),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Full Form", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                }

                Button(
                    onClick = {
                        val parsed = parsedResult ?: VoiceExpenseParser.parse(manualInput, categories, accounts, books)
                        val amt = parsed.amount
                        if (amt != null && amt > 0.0) {
                            val catId = parsed.matchedCategory?.id ?: (categories.firstOrNull()?.id ?: 1L)
                            val accId = parsed.matchedAccount?.id ?: (accounts.firstOrNull()?.id ?: 1L)
                            val bookId = parsed.matchedBook?.id ?: 1L
                            val tx = Transaction(
                                amount = amt,
                                type = parsed.type,
                                categoryId = catId,
                                accountId = accId,
                                bookId = bookId,
                                note = parsed.merchantOrNote,
                                dateTime = System.currentTimeMillis()
                            )
                            onSaveTransaction(tx)
                            coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                                onDismissRequest()
                            }
                        }
                    },
                    enabled = (parsedResult?.amount != null && (parsedResult?.amount ?: 0.0) > 0.0) ||
                            (manualInput.isNotBlank() && VoiceExpenseParser.parse(manualInput, categories, accounts, books).amount != null),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("voice_instant_save_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoyalGold,
                        contentColor = ObsidianBlack,
                        disabledContainerColor = RoyalGold.copy(alpha = 0.4f),
                        disabledContentColor = ObsidianBlack.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log Expense", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}
