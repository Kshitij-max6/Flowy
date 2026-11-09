package com.example.domain.model

enum class ExportFormat(val label: String, val extension: String, val mimeType: String) {
    CSV("CSV Spreadsheet", "csv", "text/csv"),
    PDF_REPORT("PDF Financial Report", "pdf", "application/pdf"),
    JSON_BACKUP("JSON Data Export", "json", "application/json")
}

enum class ExportDateRange(val label: String) {
    ALL_TIME("All Time"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    LAST_30_DAYS("Last 30 Days"),
    LAST_90_DAYS("Last 90 Days"),
    THIS_YEAR("This Year"),
    CUSTOM("Custom Date Range")
}

data class ExportFilterState(
    val format: ExportFormat = ExportFormat.CSV,
    val dateRange: ExportDateRange = ExportDateRange.THIS_MONTH,
    val customStartDate: Long = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000),
    val customEndDate: Long = System.currentTimeMillis(),
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val transactionType: TransactionType? = null
)

data class ExportPreviewSummary(
    val totalTransactions: Int = 0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netCashFlow: Double = 0.0,
    val periodLabel: String = "",
    val topExpenseCategory: String = "None"
)
