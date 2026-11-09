package com.example.feature.add_transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.model.Category
import com.example.domain.model.CategoryType
import com.example.domain.model.TransactionType

/** Editable preview for "chai 40 and auto 120 yesterday": adjust each row, then save them separately. */
@Composable
fun MultiEntryPreviewDialog(
    drafts: List<EntryDraft>,
    categories: List<Category>,
    currency: String,
    isSaving: Boolean,
    onChange: (Int, (EntryDraft) -> EntryDraft) -> Unit,
    onRemove: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val validCount = drafts.count { (it.amountText.toDoubleOrNull() ?: 0.0) > 0.0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${drafts.size} entries") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(drafts, key = { it.key }) { d ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
                        Column(Modifier.padding(10.dp)) {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = d.description,
                                    onValueChange = { v -> onChange(d.key) { it.copy(description = v) } },
                                    singleLine = true,
                                    label = { Text("Description") },
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { onRemove(d.key) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove entry")
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = d.amountText,
                                    onValueChange = { v -> onChange(d.key) { it.copy(amountText = v.filter { c -> c.isDigit() || c == '.' }) } },
                                    singleLine = true,
                                    prefix = { Text(currency) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.width(120.dp).testTag("multi_amount_${d.key}")
                                )
                                Spacer(Modifier.width(8.dp))
                                CategoryPicker(d, categories) { id -> onChange(d.key) { it.copy(categoryId = id) } }
                            }
                            Text(
                                when (d.daysAgo) { 0 -> "Today"; 1 -> "Yesterday"; else -> "${d.daysAgo} days ago" } +
                                    if (d.type == TransactionType.INCOME) " · Income" else "",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = validCount > 0 && !isSaving, modifier = Modifier.testTag("multi_confirm")) {
                Text(if (isSaving) "Saving…" else "Save $validCount")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CategoryPicker(draft: EntryDraft, categories: List<Category>, onPick: (Long) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val wanted = if (draft.type == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
    val options = categories.filter { it.type == wanted && !it.isArchived }
    val current = categories.firstOrNull { it.id == draft.categoryId }
    val label = current?.let { c ->
        val parent = categories.firstOrNull { it.id == c.parentCategoryId }
        if (parent != null) "${parent.name} › ${c.name}" else c.name
    } ?: "Category"
    Box {
        OutlinedButton(onClick = { open = true }) { Text(label, maxLines = 1) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { c ->
                val parent = categories.firstOrNull { it.id == c.parentCategoryId }
                DropdownMenuItem(
                    text = { Text(if (parent != null) "${parent.name} › ${c.name}" else c.name) },
                    onClick = { onPick(c.id); open = false }
                )
            }
        }
    }
}
