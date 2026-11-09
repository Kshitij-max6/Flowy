package com.example.feature.update

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.safety.DatabaseSafety
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Shown instead of crashing when the database cannot be opened after an update.
 * Nothing here deletes the user's data.
 */
@Composable
fun RecoveryScreen() {
    val context = LocalContext.current
    val updateVm = activityUpdateViewModel()
    val updateState by updateVm.state.collectAsState()
    var status by remember { mutableStateOf<String?>(null) }
    var confirmRestore by remember { mutableStateOf(false) }

    val backupTime = remember { DatabaseSafety.preUpdateBackupTime(context) }
    val hasBackup = remember { DatabaseSafety.hasPreUpdateBackup(context) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            status = if (DatabaseSafety.exportRawDatabase(context, uri)) "Your data was exported to the file you chose."
            else "Export failed. Try a different location."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Flowly needs a moment", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(10.dp))
        Text(
            "After the last update, your data file couldn't be opened. Your data has not been deleted. Choose how to continue:",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))

        Button(onClick = { updateVm.checkNow() }, modifier = Modifier.fillMaxWidth()) {
            Text(if (updateState.checking) "Looking…" else "Look for a fix (check for an update)")
        }
        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = { confirmRestore = true },
            enabled = hasBackup,
            modifier = Modifier.fillMaxWidth()
        ) {
            val whenText = backupTime?.let { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(it)) }
            Text(if (hasBackup) "Restore the saved copy" + (whenText?.let { " ($it)" } ?: "") else "No saved copy available")
        }
        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = { exportLauncher.launch("flowly-data-export.db") },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Export my data as a file") }

        status?.let {
            Spacer(Modifier.height(18.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
        if (confirmRestore) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { confirmRestore = false },
                title = { Text("Restore saved copy?") },
                text = { Text("This replaces the current data file with the copy saved before the update. The current file is kept aside, not deleted.") },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        confirmRestore = false
                        if (DatabaseSafety.restorePreUpdateBackup(context)) DatabaseSafety.restartApp(context)
                        else status = "Couldn't restore the saved copy."
                    }) { Text("Restore") }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { confirmRestore = false }) { Text("Cancel") }
                }
            )
        }
    }
    AppUpdateHost(updateVm)
}
