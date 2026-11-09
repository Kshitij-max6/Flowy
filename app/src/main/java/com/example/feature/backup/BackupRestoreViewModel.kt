package com.example.feature.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.util.BackupManager
import com.example.core.util.CsvImporter
import com.example.data.repository.FlowlyRepository
import com.example.domain.model.BackupValidationResult
import com.example.domain.model.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class BackupRestoreViewModel(
    private val repository: FlowlyRepository
) : ViewModel() {

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _pendingValidation = MutableStateFlow<BackupValidationResult?>(null)
    val pendingValidation: StateFlow<BackupValidationResult?> = _pendingValidation.asStateFlow()

    private var pendingJsonContent: String? = null

    val userSettings: StateFlow<UserSettings> = repository.getUserSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun dismissValidationDialog() {
        _pendingValidation.value = null
        pendingJsonContent = null
    }

    fun createAndShareBackup(context: Context) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val json = repository.createBackupJson()
                val file = BackupManager.writeBackupFile(context, json)

                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Flowly Database Backup (${file.name})")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(intent, "Share Flowly Backup")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)

                _statusMessage.value = "Backup created successfully"
            } catch (e: Exception) {
                _statusMessage.value = "Failed to create backup: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun onBackupFileSelected(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val content = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
                    } ?: ""
                }

                if (content.isBlank()) {
                    _statusMessage.value = "Selected file is empty"
                    return@launch
                }

                val validation = repository.validateBackup(content)
                if (validation.isValid) {
                    pendingJsonContent = content
                    _pendingValidation.value = validation
                } else {
                    _statusMessage.value = validation.errorMessage ?: "Invalid backup file format"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error reading backup file: ${e.localizedMessage}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun confirmRestore() {
        val json = pendingJsonContent ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val result = repository.restoreBackup(json)
                if (result.isSuccess) {
                    _statusMessage.value = "Database restored successfully! All records have been updated."
                    _pendingValidation.value = null
                    pendingJsonContent = null
                } else {
                    _statusMessage.value = "Restore failed: ${result.exceptionOrNull()?.localizedMessage ?: "Database error"}"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Restore failed: ${e.localizedMessage}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun onCsvFileSelected(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val cats = repository.getAllCategories().first()
                val accs = repository.getAllAccounts().first()
                val bks = repository.getActiveExpenseBooks().first()
                val result = CsvImporter.importCsv(
                    context = context,
                    uri = uri,
                    repository = repository,
                    existingCategories = cats,
                    existingAccounts = accs,
                    existingBooks = bks
                )
                if (result.errors.isNotEmpty() && result.importedCount == 0) {
                    _statusMessage.value = "CSV Import failed: ${result.errors.first()}"
                } else {
                    _statusMessage.value = "Imported ${result.importedCount} transactions successfully (${result.skippedCount} skipped)."
                }
            } catch (e: Exception) {
                _statusMessage.value = "CSV Import failed: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isProcessing.value = false
            }
        }
    }
}
