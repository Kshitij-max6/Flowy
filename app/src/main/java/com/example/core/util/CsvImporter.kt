package com.example.core.util

import android.content.Context
import android.net.Uri
import com.example.data.repository.FlowlyRepository
import com.example.domain.model.Account
import com.example.domain.model.Category
import com.example.domain.model.CategoryType
import com.example.domain.model.ExpenseBook
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CsvImportSummary(
    val totalRowsRead: Int = 0,
    val importedCount: Int = 0,
    val skippedCount: Int = 0,
    val errors: List<String> = emptyList()
)

object CsvImporter {

    private val DATE_FORMATS = listOf(
        SimpleDateFormat("yyyy-MM-dd", Locale.US),
        SimpleDateFormat("dd/MM/yyyy", Locale.US),
        SimpleDateFormat("dd-MM-yyyy", Locale.US),
        SimpleDateFormat("MM/dd/yyyy", Locale.US),
        SimpleDateFormat("yyyy/MM/dd", Locale.US)
    )

    private val TIME_FORMATS = listOf(
        SimpleDateFormat("HH:mm:ss", Locale.US),
        SimpleDateFormat("hh:mm a", Locale.US),
        SimpleDateFormat("HH:mm", Locale.US)
    )

    /**
     * Parses a CSV file containing transactions and persists them into the Flowly repository.
     */
    suspend fun importCsv(
        context: Context,
        uri: Uri,
        repository: FlowlyRepository,
        existingCategories: List<Category>,
        existingAccounts: List<Account>,
        existingBooks: List<ExpenseBook>
    ): CsvImportSummary = withContext(Dispatchers.IO) {
        val lines = mutableListOf<String>()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    if (line.isNotBlank()) {
                        lines.add(line)
                    }
                    line = reader.readLine()
                }
            }
        }

        if (lines.isEmpty()) {
            return@withContext CsvImportSummary(errors = listOf("The selected CSV file is empty."))
        }

        // Header parsing
        val headerLine = lines.first()
        val headers = parseCsvRow(headerLine).map { it.trim().lowercase(Locale.ROOT) }

        val dateIdx = headers.indexOfFirst { it.contains("date") }
        val timeIdx = headers.indexOfFirst { it.contains("time") }
        val amountIdx = headers.indexOfFirst { it.contains("amount") || it.contains("total") || it.contains("sum") }
        val typeIdx = headers.indexOfFirst { it.contains("type") }
        val catIdx = headers.indexOfFirst { it.contains("category") || it.contains("cat") }
        val accIdx = headers.indexOfFirst { it.contains("account") || it.contains("acc") || it.contains("wallet") }
        val noteIdx = headers.indexOfFirst { it.contains("note") || it.contains("description") || it.contains("merchant") || it.contains("title") }

        if (dateIdx == -1 || amountIdx == -1) {
            return@withContext CsvImportSummary(
                totalRowsRead = lines.size - 1,
                errors = listOf("Invalid CSV format: Missing required 'Date' or 'Amount' columns.")
            )
        }

        var importedCount = 0
        var skippedCount = 0
        val errors = mutableListOf<String>()

        val defaultAccount = existingAccounts.firstOrNull { it.isDefault } ?: existingAccounts.firstOrNull()
        val defaultExpenseCat = existingCategories.firstOrNull { it.type == CategoryType.EXPENSE }
        val defaultIncomeCat = existingCategories.firstOrNull { it.type == CategoryType.INCOME }
        val defaultBook = existingBooks.firstOrNull { it.isDefault } ?: existingBooks.firstOrNull()

        // Iterate data rows
        for (i in 1 until lines.size) {
            val rawRow = lines[i]
            val tokens = parseCsvRow(rawRow)
            if (tokens.size <= maxOf(dateIdx, amountIdx)) {
                skippedCount++
                continue
            }

            try {
                // 1. Amount
                val rawAmountStr = tokens[amountIdx].replace("$", "").replace("₹", "").replace("€", "").replace("£", "").replace(",", "").trim()
                val rawAmount = rawAmountStr.toDoubleOrNull()
                if (rawAmount == null || rawAmount == 0.0) {
                    skippedCount++
                    continue
                }
                val absAmount = kotlin.math.abs(rawAmount)

                // 2. Type
                var type = when {
                    rawAmount < 0 -> TransactionType.EXPENSE
                    else -> TransactionType.EXPENSE // Default assumption
                }

                if (typeIdx != -1 && typeIdx < tokens.size) {
                    val typeStr = tokens[typeIdx].trim().uppercase(Locale.ROOT)
                    when {
                        typeStr.contains("INCOME") || typeStr.contains("CREDIT") -> type = TransactionType.INCOME
                        typeStr.contains("TRANSFER") -> type = TransactionType.TRANSFER
                        typeStr.contains("EXPENSE") || typeStr.contains("DEBIT") -> type = TransactionType.EXPENSE
                    }
                }

                // 3. Date & Time
                val dateStr = tokens[dateIdx].trim()
                var parsedDate: Date? = null
                for (fmt in DATE_FORMATS) {
                    try {
                        parsedDate = fmt.parse(dateStr)
                        if (parsedDate != null) break
                    } catch (_: Exception) {}
                }
                val baseTime = parsedDate?.time ?: System.currentTimeMillis()

                // 4. Category
                val catName = if (catIdx != -1 && catIdx < tokens.size) tokens[catIdx].trim() else ""
                val targetCat = existingCategories.firstOrNull { it.name.equals(catName, ignoreCase = true) }
                    ?: (if (type == TransactionType.INCOME) defaultIncomeCat else defaultExpenseCat)

                // 5. Account
                val accName = if (accIdx != -1 && accIdx < tokens.size) tokens[accIdx].trim() else ""
                val targetAcc = existingAccounts.firstOrNull { it.name.equals(accName, ignoreCase = true) }
                    ?: defaultAccount

                // 6. Note
                val note = if (noteIdx != -1 && noteIdx < tokens.size) tokens[noteIdx].trim() else "CSV Import"

                if (targetCat != null && targetAcc != null) {
                    val tx = Transaction(
                        amount = absAmount,
                        type = type,
                        categoryId = targetCat.id,
                        accountId = targetAcc.id,
                        bookId = defaultBook?.id ?: 1L,
                        dateTime = baseTime,
                        note = note
                    )
                    repository.addTransaction(tx)
                    importedCount++
                } else {
                    skippedCount++
                }
            } catch (e: Exception) {
                skippedCount++
                if (errors.size < 5) {
                    errors.add("Row ${i + 1}: ${e.localizedMessage ?: "Parsing error"}")
                }
            }
        }

        return@withContext CsvImportSummary(
            totalRowsRead = lines.size - 1,
            importedCount = importedCount,
            skippedCount = skippedCount,
            errors = errors
        )
    }

    private fun parseCsvRow(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = java.lang.StringBuilder()
        var inQuotes = false

        for (c in line) {
            when {
                c == '\"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    result.add(sb.toString().trim())
                    sb.setLength(0)
                }
                else -> sb.append(c)
            }
        }
        result.add(sb.toString().trim())
        return result
    }
}
