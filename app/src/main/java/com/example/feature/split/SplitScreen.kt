package com.example.feature.split

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.model.FriendBalance
import com.example.domain.model.SplitExpense
import com.example.domain.model.SplitFriend
import com.example.domain.model.SplitMath
import com.example.domain.model.SplitMode
import com.example.domain.model.SplitShare
import com.example.ui.theme.EmeraldPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private fun money(currency: String, v: Double) = currency + String.format(Locale.getDefault(), "%,.2f", v)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitScreen(
    viewModel: SplitViewModel,
    currency: String,
    onNavigateBack: () -> Unit
) {
    val overview by viewModel.overview.collectAsState()
    var tab by remember { mutableStateOf(0) }
    var showAddFriend by remember { mutableStateOf(false) }
    var showAddExpense by remember { mutableStateOf(false) }
    var settleTarget by remember { mutableStateOf<FriendBalance?>(null) }
    var deleteFriendTarget by remember { mutableStateOf<SplitFriend?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Split with Friends") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { showAddFriend = true }, modifier = Modifier.testTag("split_add_friend")) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add friend")
                    }
                }
            )
        },
        floatingActionButton = {
            if (overview.friends.isNotEmpty()) {
                FloatingActionButton(onClick = { showAddExpense = true }, modifier = Modifier.testTag("split_add_expense")) {
                    Icon(Icons.Default.Add, contentDescription = "Add shared expense")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            SummaryCard(currency, overview.totalOwedToMe, overview.totalIOwe)
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Balances") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Expenses") })
            }
            if (overview.friends.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Add a friend to start sharing expenses.", style = MaterialTheme.typography.bodyLarge)
                }
            } else if (tab == 0) {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(overview.balances, key = { it.friend.id }) { b ->
                        BalanceRow(b, currency, onSettle = { settleTarget = b }, onDelete = { deleteFriendTarget = b.friend })
                    }
                }
            } else {
                val friendName = { id: Long? -> if (id == null) "You" else overview.friends.firstOrNull { it.id == id }?.name ?: "Friend" }
                if (overview.expenses.isEmpty() && overview.settlements.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No shared expenses yet. Tap + to add one.")
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(overview.expenses, key = { "e${it.id}" }) { e ->
                            ExpenseRow(e, currency, friendName(e.paidByFriendId), onDelete = { viewModel.deleteExpense(e.id) })
                        }
                        items(overview.settlements, key = { "s${it.id}" }) { s ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        val who = friendName(s.friendId)
                                        Text(
                                            if (s.friendPaidMe) "$who paid you" else "You paid $who",
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            "Settlement · " + SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(s.dateTime)),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    Text(money(currency, s.amount), fontWeight = FontWeight.Bold)
                                    IconButton(onClick = { viewModel.deleteSettlement(s.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete settlement")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddFriend) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddFriend = false },
            title = { Text("Add friend") },
            text = {
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, singleLine = true, label = { Text("Name") },
                    modifier = Modifier.testTag("split_friend_name")
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.addFriend(name); showAddFriend = false }, enabled = name.isNotBlank()) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddFriend = false }) { Text("Cancel") } }
        )
    }

    if (showAddExpense) {
        AddExpenseDialog(
            friends = overview.friends,
            currency = currency,
            onDismiss = { showAddExpense = false },
            onSave = { desc, total, paidBy, mode, shares ->
                viewModel.addExpense(desc, total, paidBy, mode, shares)
                showAddExpense = false
            }
        )
    }

    settleTarget?.let { b ->
        if (abs(b.amount) < 0.005) {
            AlertDialog(
                onDismissRequest = { settleTarget = null },
                title = { Text("All settled") },
                text = { Text("You and ${b.friend.name} are even.") },
                confirmButton = { TextButton(onClick = { settleTarget = null }) { Text("OK") } }
            )
        } else {
            var amountText by remember(b) { mutableStateOf(String.format(Locale.US, "%.2f", abs(b.amount))) }
            val amount = amountText.toDoubleOrNull() ?: 0.0
            AlertDialog(
                onDismissRequest = { settleTarget = null },
                title = { Text("Settle up with ${b.friend.name}") },
                text = {
                    Column {
                        Text(
                            if (b.amount > 0) "${b.friend.name} pays you back." else "You pay ${b.friend.name}.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = amountText, onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                            singleLine = true, label = { Text("Amount ($currency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.settleUp(b.friend.id, amount, b.amount); settleTarget = null },
                        enabled = amount > 0
                    ) { Text("Settle") }
                },
                dismissButton = { TextButton(onClick = { settleTarget = null }) { Text("Cancel") } }
            )
        }
    }

    deleteFriendTarget?.let { f ->
        AlertDialog(
            onDismissRequest = { deleteFriendTarget = null },
            title = { Text("Remove ${f.name}?") },
            text = { Text("This also removes the shared expenses ${f.name} paid for and their settlements.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteFriend(f.id); deleteFriendTarget = null }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { deleteFriendTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SummaryCard(currency: String, owedToMe: Double, iOwe: Double) {
    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.12f))) {
            Column(Modifier.padding(14.dp)) {
                Text("You are owed", style = MaterialTheme.typography.labelMedium)
                Text(money(currency, owedToMe), fontWeight = FontWeight.Bold, color = EmeraldPrimary, style = MaterialTheme.typography.titleMedium)
            }
        }
        Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.10f))) {
            Column(Modifier.padding(14.dp)) {
                Text("You owe", style = MaterialTheme.typography.labelMedium)
                Text(money(currency, iOwe), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun BalanceRow(b: FriendBalance, currency: String, onSettle: () -> Unit, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(b.friend.name, fontWeight = FontWeight.SemiBold)
                val line = when {
                    b.amount > 0.005 -> "owes you ${money(currency, b.amount)}"
                    b.amount < -0.005 -> "you owe ${money(currency, -b.amount)}"
                    else -> "settled up"
                }
                Text(
                    line,
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        b.amount > 0.005 -> EmeraldPrimary
                        b.amount < -0.005 -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            if (abs(b.amount) >= 0.005) TextButton(onClick = onSettle) { Text("Settle up") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Remove friend") }
        }
    }
}

@Composable
private fun ExpenseRow(e: SplitExpense, currency: String, paidBy: String, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(e.description.ifBlank { "Shared expense" }, fontWeight = FontWeight.SemiBold)
                Text(
                    "$paidBy paid · ${e.shares.size} people · " + SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(e.dateTime)),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Text(money(currency, e.totalAmount), fontWeight = FontWeight.Bold)
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete expense") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseDialog(
    friends: List<SplitFriend>,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (String, Double, Long?, SplitMode, List<SplitShare>) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var totalText by remember { mutableStateOf("") }
    var paidBy by remember { mutableStateOf<Long?>(null) } // null = me
    var mode by remember { mutableStateOf(SplitMode.EQUAL) }
    // participants: key -1 = me, otherwise friend id
    val included = remember { mutableStateMapOf<Long, Boolean>().apply { put(-1L, true); friends.forEach { put(it.id, true) } } }
    val entered = remember { mutableStateMapOf<Long, String>() }

    val total = totalText.toDoubleOrNull() ?: 0.0
    val participants: List<Long?> = buildList {
        if (included[-1L] == true) add(null)
        friends.forEach { if (included[it.id] == true) add(it.id) }
    }
    fun key(id: Long?) = id ?: -1L

    val shares: List<SplitShare>? = when {
        total <= 0 || participants.isEmpty() -> null
        mode == SplitMode.EQUAL -> SplitMath.equalShares(total, participants)
        mode == SplitMode.AMOUNT -> {
            val list = participants.map { SplitShare(it, entered[key(it)]?.toDoubleOrNull() ?: 0.0) }
            if (abs(list.sumOf { it.amount } - total) < 0.01) list else null
        }
        else -> {
            val pcts = participants.map { it to (entered[key(it)]?.toDoubleOrNull() ?: 0.0) }
            if (abs(pcts.sumOf { it.second } - 100.0) < 0.01) SplitMath.percentShares(total, pcts) else null
        }
    }
    val hint: String? = when {
        total <= 0 -> null
        participants.isEmpty() -> "Pick at least one person."
        mode == SplitMode.AMOUNT -> {
            val sum = participants.sumOf { entered[key(it)]?.toDoubleOrNull() ?: 0.0 }
            if (abs(sum - total) < 0.01) null else "Amounts add up to ${money(currency, sum)}, need ${money(currency, total)}."
        }
        mode == SplitMode.PERCENT -> {
            val sum = participants.sumOf { entered[key(it)]?.toDoubleOrNull() ?: 0.0 }
            if (abs(sum - 100.0) < 0.01) null else "Percentages add up to ${String.format(Locale.US, "%.1f", sum)}%, need 100%."
        }
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add shared expense") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = description, onValueChange = { description = it }, singleLine = true, label = { Text("What was it for?") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = totalText, onValueChange = { totalText = it.filter { c -> c.isDigit() || c == '.' } },
                    singleLine = true, label = { Text("Total ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(Modifier.height(12.dp))
                Text("Paid by", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = paidBy == null, onClick = { paidBy = null }, label = { Text("You") })
                }
                friends.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { f -> FilterChip(selected = paidBy == f.id, onClick = { paidBy = f.id }, label = { Text(f.name) }) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Split", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SplitMode.values().forEach { m ->
                        FilterChip(
                            selected = mode == m, onClick = { mode = m },
                            label = { Text(when (m) { SplitMode.EQUAL -> "Equal"; SplitMode.AMOUNT -> "By amount"; SplitMode.PERCENT -> "By %" }) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                val everyone: List<Pair<Long?, String>> = listOf<Pair<Long?, String>>(null to "You") + friends.map { it.id to it.name }
                everyone.forEach { (id, name) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = included[key(id)] == true, onCheckedChange = { included[key(id)] = it })
                        Text(name, Modifier.weight(1f))
                        if (included[key(id)] == true) {
                            when (mode) {
                                SplitMode.EQUAL -> {
                                    val s = shares?.firstOrNull { it.friendId == id }
                                    if (s != null) Text(money(currency, s.amount), style = MaterialTheme.typography.bodySmall)
                                }
                                else -> OutlinedTextField(
                                    value = entered[key(id)] ?: "",
                                    onValueChange = { entered[key(id)] = it.filter { c -> c.isDigit() || c == '.' } },
                                    singleLine = true,
                                    suffix = { Text(if (mode == SplitMode.PERCENT) "%" else currency) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.width(120.dp)
                                )
                            }
                        }
                    }
                }
                hint?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { shares?.let { onSave(description, total, paidBy, mode, it) } },
                enabled = shares != null
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
