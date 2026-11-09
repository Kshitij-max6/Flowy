package com.example.feature.insights

import android.content.Intent
import com.example.ui.theme.asAmount
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.util.CategoryIconHelper
import com.example.core.util.CurrencyFormatter
import com.example.feature.books.parseColorSafe
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PrestigeGold
import com.example.ui.theme.RoyalGold
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TOTAL_SLIDES = 5
private const val SLIDE_DURATION_MS = 6000L

@Composable
fun MonthlySpendingStoryDialog(
    storyData: MonthlyStoryData,
    currency: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentSlide by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(currentSlide, isPaused) {
        if (!isPaused) {
            progress = 0f
            val stepMs = 50L
            val steps = SLIDE_DURATION_MS / stepMs
            for (i in 1..steps) {
                if (isPaused) break
                delay(stepMs)
                progress = i.toFloat() / steps.toFloat()
            }
            if (!isPaused && progress >= 0.99f) {
                if (currentSlide < TOTAL_SLIDES - 1) {
                    currentSlide++
                } else {
                    onDismiss()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0B0B0D),
                            Color(0xFF050506),
                            Color(0xFF000000)
                        )
                    )
                )
                .statusBarsPadding()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                        onTap = { offset ->
                            val screenWidth = size.width
                            if (offset.x < screenWidth / 3) {
                                if (currentSlide > 0) currentSlide--
                            } else {
                                if (currentSlide < TOTAL_SLIDES - 1) {
                                    currentSlide++
                                } else {
                                    onDismiss()
                                }
                            }
                        }
                    )
                }
                .testTag("monthly_story_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Segmented Progress Bar Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (i in 0 until TOTAL_SLIDES) {
                        val slideProgress = when {
                            i < currentSlide -> 1f
                            i == currentSlide -> progress
                            else -> 0f
                        }
                        LinearProgressIndicator(
                            progress = { slideProgress },
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = RoyalGold,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                    }
                }

                // Top Bar with Month & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = RoyalGold.copy(alpha = 0.2f)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = RoyalGold,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${storyData.monthName} Story",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_story_button")
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Slide Content Animation
                AnimatedContent(
                    targetState = currentSlide,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    label = "story_slide"
                ) { slide ->
                    when (slide) {
                        0 -> SlideOverview(storyData, currency)
                        1 -> SlideSpendingVelocity(storyData, currency)
                        2 -> SlideTopCategory(storyData, currency)
                        3 -> SlidePeakAndSplurges(storyData, currency)
                        4 -> SlidePersonaBadge(storyData, currency, onShare = {
                            val shareText = buildString {
                                appendLine("✨ My ${storyData.monthName} Spending Story on Flowly:")
                                appendLine("💰 Total Spent: ${CurrencyFormatter.format(storyData.totalExpense, currency)}")
                                if (storyData.totalIncome > 0) {
                                    appendLine("📈 Savings Rate: ${storyData.savingsRate}%")
                                }
                                if (storyData.topCategoryName != null) {
                                    appendLine("🏷️ Top Category: ${storyData.topCategoryName} (${storyData.topCategoryPercentage}%)")
                                }
                                appendLine("🎖️ Financial Persona: ${storyData.personaBadge}")
                                appendLine("#Flowly #SmartFinances")
                            }
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Monthly Story"))
                        })
                    }
                }

                // Bottom Hint
                Text(
                    text = "Tap right to advance • Tap left to go back • Hold to pause",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Color.White.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SlideOverview(story: MonthlyStoryData, currency: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "THE BIG PICTURE",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
                color = RoyalGold
            )
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "You spent a total of",
            style = MaterialTheme.typography.bodyLarge.copy(color = Color.White.copy(alpha = 0.8f))
        )
        Text(
            text = CurrencyFormatter.format(story.totalExpense, currency),
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ).asAmount()
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Cards for Income & Savings
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Total Income",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.6f))
                        )
                        Text(
                            text = CurrencyFormatter.format(story.totalIncome, currency),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            ).asAmount()
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Net Saved",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.6f))
                        )
                        Text(
                            text = CurrencyFormatter.format(story.netSavings, currency),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = RoyalGold
                            ).asAmount()
                        )
                    }
                }

                if (story.totalIncome > 0) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Savings Rate",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.8f))
                        )
                        Text(
                            text = "${story.savingsRate}% of income",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (story.savingsRate >= 20) IncomeGreen else PrestigeGold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SlideSpendingVelocity(story: MonthlyStoryData, currency: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val isMore = story.expenseDiffPercent > 0
        val diffFormatted = String.format(Locale.getDefault(), "%.1f", kotlin.math.abs(story.expenseDiffPercent))

        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (isMore) ExpenseRed.copy(alpha = 0.2f) else IncomeGreen.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isMore) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                contentDescription = null,
                tint = if (isMore) ExpenseRed else IncomeGreen,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "SPENDING MOMENTUM",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
                color = RoyalGold
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (story.expenseDiffPercent == 0.0) "Consistent Spending" else if (isMore) "$diffFormatted% Higher" else "$diffFormatted% Lower",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )

        Text(
            text = "compared to last month",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.7f))
        )

        Spacer(modifier = Modifier.height(28.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Total Recorded Entries",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.6f))
                )
                Text(
                    text = "${story.totalTransactionsCount} Transactions",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
    }
}

@Composable
private fun SlideTopCategory(story: MonthlyStoryData, currency: String) {
    val categoryColor = parseColorSafe(story.topCategoryColor)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "YOUR #1 APPETITE",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
                color = RoyalGold
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(categoryColor.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                CategoryIconHelper.getIcon(story.topCategoryIcon),
                contentDescription = null,
                tint = categoryColor,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = story.topCategoryName ?: "General",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )

        Text(
            text = "${story.topCategoryPercentage}% of your total monthly outflow",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = RoyalGold,
                fontWeight = FontWeight.SemiBold
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Category Spend Total",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.6f))
                )
                Text(
                    text = CurrencyFormatter.format(story.topCategoryAmount, currency),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ).asAmount()
                )
            }
        }
    }
}

@Composable
private fun SlidePeakAndSplurges(story: MonthlyStoryData, currency: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "HIGHLIGHT MOMENTS",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
                color = RoyalGold
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Peak Day Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = RoyalGold.copy(alpha = 0.2f)
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = RoyalGold,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Peak Spending Day",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.6f))
                    )
                    Text(
                        text = story.peakSpendDayName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Biggest Transaction
        story.biggestTransaction?.let { bigTx ->
            val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Biggest Single Purchase",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.6f))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = bigTx.transaction.note.ifBlank { bigTx.category?.name ?: "Purchase" },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                maxLines = 1
                            )
                            Text(
                                text = dateFormat.format(Date(bigTx.transaction.dateTime)),
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.5f))
                            )
                        }

                        Text(
                            text = CurrencyFormatter.format(bigTx.transaction.amount, currency),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            ).asAmount()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SlidePersonaBadge(
    story: MonthlyStoryData,
    currency: String,
    onShare: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "YOUR FINANCIAL PERSONA",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
                color = RoyalGold
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            shape = CircleShape,
            color = RoyalGold.copy(alpha = 0.2f),
            border = BorderStroke(2.dp, RoyalGold)
        ) {
            Icon(
                Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = RoyalGold,
                modifier = Modifier
                    .padding(20.dp)
                    .size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = story.personaBadge,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = story.personaDescription,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.White.copy(alpha = 0.8f),
                lineHeight = 22.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onShare,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RoyalGold,
                contentColor = ObsidianBlack
            ),
            modifier = Modifier.testTag("share_story_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Story", fontWeight = FontWeight.Bold)
            }
        }
    }
}
