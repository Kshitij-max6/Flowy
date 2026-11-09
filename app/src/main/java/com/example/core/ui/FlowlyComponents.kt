package com.example.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.example.ui.theme.FlowlyMotion
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.CategoryIconHelper
import com.example.core.util.CurrencyFormatter
import com.example.core.util.DateUtils
import com.example.domain.model.RecurringDetail
import com.example.domain.model.TransactionDetail
import com.example.domain.model.TransactionType
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.OnTint
import com.example.ui.theme.TransferCyan
import com.example.ui.theme.financialAmountStyle
import com.example.ui.theme.pressable

// ---------------------------------------------------------------------------
// Layout constants (4/8pt grid)
// ---------------------------------------------------------------------------
object FlowlyDimens {
    val ScreenMargin = 20.dp
    val RowMinHeight = 56.dp
    val CardRadius = 20.dp
    val GroupRadius = 14.dp
    val TouchTarget = 44.dp
}

private fun parseColorOr(hex: String?, fallback: Color): Color {
    if (hex == null) return fallback
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        fallback
    }
}

/** Hairline that starts at [startInset] (aligns with row text, like UITableView separators). */
fun Modifier.hairlineBottom(color: Color, startInset: Float, thickness: Float = 0.5f): Modifier =
    this.drawBehind {
        val y = size.height - thickness / 2f
        drawLine(color, Offset(startInset, y), Offset(size.width, y), strokeWidth = thickness)
    }

// ---------------------------------------------------------------------------
// Transaction row: flat, hairline-separated, no card chrome.
// ---------------------------------------------------------------------------
@Composable
fun TransactionItemRow(
    item: TransactionDetail,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val tx = item.transaction
    val category = item.category
    val isExpense = tx.type == TransactionType.EXPENSE
    val isIncome = tx.type == TransactionType.INCOME
    val isTransfer = tx.type == TransactionType.TRANSFER

    val amountColor = when {
        isExpense -> MaterialTheme.colorScheme.onSurface
        isIncome -> IncomeGreen
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val amountPrefix = when {
        isExpense -> "\u2212"
        isIncome -> "+"
        else -> ""
    }

    val iconVector = if (isTransfer) Icons.Default.SwapHoriz
    else CategoryIconHelper.getIconByName(category?.iconName ?: "Category")

    val accent = if (isTransfer) TransferCyan
    else parseColorOr(category?.colorHex, MaterialTheme.colorScheme.primary)

    val titleText = if (tx.note.isNotBlank()) tx.note else (category?.name ?: "Transaction")
    val subtitleText = when {
        isTransfer -> "${item.sourceAccount?.name ?: "Account"} \u2192 ${item.destinationAccount?.name ?: "Account"}"
        else -> "${category?.name ?: "General"} \u00B7 ${item.sourceAccount?.name ?: "Account"}"
    }

    val separator = MaterialTheme.colorScheme.outlineVariant
    val density = androidx.compose.ui.platform.LocalDensity.current
    val inset = with(density) { (FlowlyDimens.ScreenMargin + 40.dp + 14.dp).toPx() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressable(pressedScale = 0.99f, pressedAlpha = 0.7f, role = Role.Button, onClick = onClick)
            .hairlineBottom(separator, inset)
            .heightIn(min = FlowlyDimens.RowMinHeight)
            .padding(horizontal = FlowlyDimens.ScreenMargin, vertical = 12.dp)
            .testTag("transaction_row_${tx.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = category?.name ?: "Category",
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titleText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$amountPrefix${CurrencyFormatter.format(tx.amount, currencySymbol)}",
                style = financialAmountStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium),
                color = amountColor
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = DateUtils.formatTime(tx.dateTime),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Upcoming payment: a single grouped cell.
// ---------------------------------------------------------------------------
@Composable
fun UpcomingPaymentCard(
    item: RecurringDetail,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val recurring = item.recurring
    val category = item.category
    val accent = parseColorOr(category?.colorHex, MaterialTheme.colorScheme.primary)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FlowlyDimens.GroupRadius))
            .pressable(pressedScale = 0.985f, role = Role.Button, onClick = onClick)
            .testTag("upcoming_payment_card_${recurring.id}"),
        shape = RoundedCornerShape(FlowlyDimens.GroupRadius),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = FlowlyDimens.RowMinHeight)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CategoryIconHelper.getIconByName(category?.iconName ?: "Event"),
                    contentDescription = recurring.name,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recurring.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${item.account?.name ?: "Account"} \u00B7 ${DateUtils.formatRelativeDate(recurring.nextExecutionDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = CurrencyFormatter.format(recurring.amount, currencySymbol),
                style = financialAmountStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Section header: Title 2 (emphasised) + plain tinted action.
// ---------------------------------------------------------------------------
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionTitle: String? = null,
    onActionClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = FlowlyDimens.ScreenMargin, end = FlowlyDimens.ScreenMargin, top = 16.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (actionTitle != null) {
            Box(
                modifier = Modifier
                    .heightIn(min = FlowlyDimens.TouchTarget)
                    .pressable(pressedScale = 1f, pressedAlpha = 0.5f, onClick = onActionClick)
                    .testTag("section_action_${title.lowercase().replace(" ", "_")}"),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = actionTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Empty state: tertiary symbol, Title 3, one line, generous space.
// ---------------------------------------------------------------------------
@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    icon: ImageVector = Icons.Default.Info,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.size(52.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ---------------------------------------------------------------------------
// Card: opaque grouped surface, no border, no shadow.
// ---------------------------------------------------------------------------
@Composable
fun FlowlyCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = Color.Transparent,
    shape: Shape = RoundedCornerShape(FlowlyDimens.CardRadius),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        border = if (borderColor == Color.Transparent) null else BorderStroke(0.5.dp, borderColor)
    ) {
        Column(content = content)
    }
}

// ---------------------------------------------------------------------------
// Destructive confirmation: Cancel always present, destructive in red.
// ---------------------------------------------------------------------------
@Composable
fun ConfirmDeleteDialog(
    title: String = "Delete Transaction",
    message: String = "Are you sure you want to delete this transaction? This will automatically restore your account balance.",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(text = title, style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text("Delete", color = ExpenseRed, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_delete_button")
            ) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

// ===========================================================================
// New shared building blocks
// ===========================================================================

/** Large-title header for root screens. Sits under the status bar, owns its own spacing. */
@Composable
fun LargeTitleHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = FlowlyDimens.ScreenMargin, end = 12.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
    }
}

/** Inline navigation bar for pushed screens: Back chevron, centred headline, optional trailing. */
@Composable
fun InlineNavBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .heightIn(min = 52.dp)
            .padding(horizontal = 4.dp)
    ) {
        AppleIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 56.dp)
        )
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            content = trailing
        )
    }
}

/** 44pt hit target, 36pt soft fill, glyph only. No border, no ripple. */
@Composable
fun AppleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    filled: Boolean = true
) {
    Box(
        modifier = modifier
            .size(FlowlyDimens.TouchTarget)
            .pressable(pressedScale = 0.92f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    when {
                        active -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        filled -> MaterialTheme.colorScheme.surfaceVariant
                        else -> Color.Transparent
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

enum class AppleButtonStyle { Prominent, Gray, Plain, Destructive }

/** Capsule button. One Prominent per view. Press = dim + gentle scale. */
@Composable
fun AppleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AppleButtonStyle = AppleButtonStyle.Prominent,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    val scheme = MaterialTheme.colorScheme
    val (container, content) = when (style) {
        AppleButtonStyle.Prominent -> scheme.primary to OnTint
        AppleButtonStyle.Gray -> scheme.surfaceVariant to scheme.primary
        AppleButtonStyle.Plain -> Color.Transparent to scheme.primary
        AppleButtonStyle.Destructive -> ExpenseRed.copy(alpha = 0.14f) to ExpenseRed
    }
    Row(
        modifier = modifier
            .heightIn(min = 50.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(if (enabled) container else scheme.surfaceVariant)
            .pressable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = if (enabled) content else scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) content else scheme.onSurfaceVariant
        )
    }
}

/** Inset-grouped container (Settings style). Rows inside draw their own hairlines. */
@Composable
fun GroupedSection(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = FlowlyDimens.ScreenMargin)) {
        if (header != null) {
            Text(
                text = header,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, bottom = 6.dp)
            )
        }
        Surface(
            shape = RoundedCornerShape(FlowlyDimens.GroupRadius),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
        if (footer != null) {
            Text(
                text = footer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp)
            )
        }
    }
}

/** Settings-style row: 29pt rounded-square icon, label, trailing value, chevron. */
@Composable
fun GroupedRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    value: String? = null,
    showChevron: Boolean = true,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val separator = MaterialTheme.colorScheme.outlineVariant
    val density = androidx.compose.ui.platform.LocalDensity.current
    val inset = with(density) { (if (icon != null) 16.dp + 29.dp + 12.dp else 16.dp).toPx() }
    val base = modifier
        .fillMaxWidth()
        .let { if (onClick != null) it.pressable(pressedScale = 1f, pressedAlpha = 0.6f, onClick = onClick) else it }
        .let { if (showDivider) it.hairlineBottom(separator, inset) else it }
    Row(
        modifier = base
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(29.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(iconTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = OnTint, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(Modifier.width(6.dp))
        }
        if (trailing != null) {
            trailing()
        } else if (showChevron && onClick != null) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


/**
 * Segmented control with a sliding thumb. The thumb moves with a transform
 * (translationX), so selection changes never trigger layout.
 */
@Composable
fun AppleSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    testTags: List<String> = emptyList()
) {
    val count = options.size.coerceAtLeast(1)
    val trackPadding = 3.dp
    val thumbOffset by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = FlowlyMotion.responsive(),
        label = "segmentThumb"
    )
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(trackPadding)
    ) {
        val segmentWidth = maxWidth / count
        val segmentWidthPx = with(LocalDensity.current) { segmentWidth.toPx() }
        Box(
            modifier = Modifier
                .width(segmentWidth)
                .fillMaxHeight()
                .graphicsLayer { translationX = thumbOffset * segmentWidthPx }
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface)
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val textColor by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = FlowlyMotion.quick(),
                    label = "segmentText"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 34.dp)
                        .pressable(pressedScale = 1f, pressedAlpha = 0.7f, role = Role.Tab, onClick = { onSelect(index) })
                        .let { m -> testTags.getOrNull(index)?.let { m.testTag(it) } ?: m },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = textColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
