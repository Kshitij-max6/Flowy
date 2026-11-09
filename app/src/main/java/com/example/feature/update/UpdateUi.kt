package com.example.feature.update

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.BuildConfig

/** One shared instance for the whole activity, so Settings and the pop-up see the same state. */
@Composable
fun activityUpdateViewModel(): UpdateViewModel {
    val activity = LocalContext.current as ComponentActivity
    return viewModel(viewModelStoreOwner = activity)
}

/** Put once near the root of the app. Checks daily and shows the pop-up once per version. */
@Composable
fun AppUpdateHost(viewModel: UpdateViewModel = activityUpdateViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.autoCheck() }

    val info = state.available
    if (state.showPrompt && info != null) {
        val required = info.required
        AlertDialog(
            onDismissRequest = { if (!required && !state.downloading) viewModel.later() },
            title = { Text(if (required) "Required update" else "Update available") },
            text = {
                Column {
                    Text("Flowly ${info.versionName} is ready (you have ${BuildConfig.VERSION_NAME}).")
                    if (required) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "This is an important update and must be installed to keep using Flowly.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (info.notes.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(info.notes.take(600), style = MaterialTheme.typography.bodySmall)
                    }
                    if (state.downloading) {
                        Spacer(Modifier.height(14.dp))
                        LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                    }
                    state.message?.let {
                        Spacer(Modifier.height(10.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "A safety copy of your data is saved before installing.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                if (state.readyFile != null) {
                    TextButton(onClick = { viewModel.install() }, modifier = Modifier.testTag("update_install")) { Text("Install") }
                } else {
                    TextButton(
                        onClick = { viewModel.startDownload() },
                        enabled = !state.downloading,
                        modifier = Modifier.testTag("update_download")
                    ) { Text(if (state.downloading) "Downloading…" else "Update") }
                }
            },
            dismissButton = {
                if (!required && !state.downloading) {
                    Column {
                        TextButton(onClick = { viewModel.later() }) { Text("Later") }
                        TextButton(onClick = { viewModel.skip() }) { Text("Skip this version") }
                    }
                }
            }
        )
    } else if (state.manual && state.message != null && !state.showPrompt) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissMessage() },
            title = { Text("App updates") },
            text = { Text(state.message ?: "") },
            confirmButton = { TextButton(onClick = { viewModel.dismissMessage() }) { Text("OK") } }
        )
    }
}
