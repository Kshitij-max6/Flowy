package com.example.feature.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.util.BackupManager
import com.example.core.util.CsvExporter
import com.example.core.util.PdfReportExporter
import com.example.data.repository.FlowlyRepository
import com.example.core.util.CsvImporter
import com.example.core.util.CsvImportSummary
import com.example.domain.model.Account
import com.example.domain.model.Category
import com.example.domain.model.ExportDateRange
import com.example.domain.model.ExportFilterState
import com.example.domain.model.ExportFormat
import com.example.domain.model.ExportPreviewSummary
import com.example.domain.model.TransactionType
import com.example.domain.model.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class ExportViewModel(
    private val repository: FlowlyRepository
) : ViewModel() {

    private val _filterState = MutableStateFlow(ExportFilterState())
    val filterState: StateFlow<ExportFilterState> = _filterState.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportStatusMessage = MutableStateFlow<String?>(null)
    val exportStatusMessage: StateFlow<String?> = _exportStatusMessage.asStateFlow()

    val previewSummary: StateFlow<ExportPreviewSummary> = _filterState
        .flatMapLatest { state ->
            repository.getExportPreviewSummary(state)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ExportPreviewSummary()
        )

    val accounts: StateFlow<List<Account>> = repository.getAllAccounts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val categories: StateFlow<List<Category>> = repository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val userSettings: StateFlow<UserSettings> = repository.getUserSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    fun setFormat(format: ExportFormat) {
        _filterState.value = _filterState.value.copy(format = format)
    }

    fun setDateRange(range: ExportDateRange) {
        _filterState.value = _filterState.value.copy(dateRange = range)
    }

    fun setCustomDateRange(start: Long, end: Long) {
        _filterState.value = _filterState.value.copy(
            dateRange = ExportDateRange.CUSTOM,
            customStartDate = start,
            customEndDate = end
        )
    }

    fun setAccountFilter(accountId: Long?) {
        _filterState.value = _filterState.value.copy(accountId = accountId)
    }

    fun setCategoryFilter(categoryId: Long?) {
        _filterState.value = _filterState.value.copy(categoryId = categoryId)
    }

    fun setTypeFilter(type: TransactionType?) {
        _filterState.value = _filterState.value.copy(transactionType = type)
    }

    fun clearStatusMessage() {
        _exportStatusMessage.value = null
    }

    fun exportAndShare(context: Context) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val state = _filterState.value
                val transactions = repository.getFilteredTransactionsForExport(state).first()
                val settings = userSettings.value
                val summary = previewSummary.value

                val file: File = when (state.format) {
                    ExportFormat.CSV -> {
                        CsvExporter.exportTransactionsToCsv(
                            context = context,
                            transactions = transactions,
                            currencySymbol = settings.defaultCurrency
                        )
                    }
                    ExportFormat.PDF_REPORT -> {
                        val accName = if (state.accountId != null) {
                            accounts.value.find { it.id == state.accountId }?.name ?: "Selected Account"
                        } else "All Accounts"
                        val catName = if (state.categoryId != null) {
                            categories.value.find { it.id == state.categoryId }?.name ?: "Selected Category"
                        } else "All Categories"

                        PdfReportExporter.generateFinancialReportPdf(
                            context = context,
                            transactions = transactions,
                            summary = summary,
                            currencySymbol = settings.defaultCurrency,
                            accountFilterName = accName,
                            categoryFilterName = catName,
                            budgets = runCatching { repository.getBudgetDetails().first() }.getOrDefault(emptyList()),
                            categories = categories.value,
                            splitOverview = runCatching {
                                com.example.data.repository.SplitRepository(
                                    com.example.data.local.AppDatabase.getDatabase(context).splitDao()
                                ).overview().first()
                            }.getOrNull()
                        )
                    }
                    ExportFormat.JSON_BACKUP -> {
                        val json = repository.createBackupJson()
                        BackupManager.writeBackupFile(context, json)
                    }
                }

                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = state.format.mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Flowly Financial Export (${state.format.label})")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(intent, "Export Flowly Data")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)

                _exportStatusMessage.value = "Export generated successfully (${transactions.size} records)"
            } catch (e: Exception) {
                _exportStatusMessage.value = "Export failed: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun saveToDownloads(context: Context) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val state = _filterState.value
                val transactions = repository.getFilteredTransactionsForExport(state).first()
                val settings = userSettings.value
                val summary = previewSummary.value

                val sourceFile: File = when (state.format) {
                    ExportFormat.CSV -> {
                        CsvExporter.exportTransactionsToCsv(
                            context = context,
                            transactions = transactions,
                            currencySymbol = settings.defaultCurrency
                        )
                    }
                    ExportFormat.PDF_REPORT -> {
                        val accName = if (state.accountId != null) {
                            accounts.value.find { it.id == state.accountId }?.name ?: "Selected Account"
                        } else "All Accounts"
                        val catName = if (state.categoryId != null) {
                            categories.value.find { it.id == state.categoryId }?.name ?: "Selected Category"
                        } else "All Categories"

                        PdfReportExporter.generateFinancialReportPdf(
                            context = context,
                            transactions = transactions,
                            summary = summary,
                            currencySymbol = settings.defaultCurrency,
                            accountFilterName = accName,
                            categoryFilterName = catName,
                            budgets = runCatching { repository.getBudgetDetails().first() }.getOrDefault(emptyList()),
                            categories = categories.value,
                            splitOverview = runCatching {
                                com.example.data.repository.SplitRepository(
                                    com.example.data.local.AppDatabase.getDatabase(context).splitDao()
                                ).overview().first()
                            }.getOrNull()
                        )
                    }
                    ExportFormat.JSON_BACKUP -> {
                        val json = repository.createBackupJson()
                        BackupManager.writeBackupFile(context, json)
                    }
                }

                // Copy to public downloads / documents folder
                withContext(Dispatchers.IO) {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (downloadsDir.exists() || downloadsDir.mkdirs()) {
                        val destFile = File(downloadsDir, sourceFile.name)
                        FileInputStream(sourceFile).use { input ->
                            FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }

                _exportStatusMessage.value = "Saved ${sourceFile.name} to device storage"
            } catch (e: Exception) {
                _exportStatusMessage.value = "Saved file to application storage: ${sourceFileFriendlyName()}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun importCsvFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val cats = repository.getAllCategories().first()
                val accs = repository.getAllAccounts().first()
                val bks = repository.getActiveExpenseBooks().first()
                val result: CsvImportSummary = CsvImporter.importCsv(
                    context = context,
                    uri = uri,
                    repository = repository,
                    existingCategories = cats,
                    existingAccounts = accs,
                    existingBooks = bks
                )
                if (result.errors.isNotEmpty() && result.importedCount == 0) {
                    _exportStatusMessage.value = "CSV Import failed: ${result.errors.first()}"
                } else {
                    _exportStatusMessage.value = "Imported ${result.importedCount} transactions successfully (${result.skippedCount} skipped)."
                }
            } catch (e: Exception) {
                _exportStatusMessage.value = "CSV Import failed: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    private fun sourceFileFriendlyName(): String {
        return "Flowly_${_filterState.value.format.name}.${_filterState.value.format.extension}"
    }
}
