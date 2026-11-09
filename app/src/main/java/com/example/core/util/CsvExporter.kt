package com.example.core.util

import android.content.Context
import com.example.domain.model.TransactionDetail
import com.example.domain.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val timestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun exportTransactionsToCsv(
        context: Context,
        transactions: List<TransactionDetail>,
        currencySymbol: String = "₹"
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val filename = "Flowly_Transactions_${timestampFormat.format(Date())}.csv"
        val file = File(exportDir, filename)

        FileOutputStream(file).use { fos ->
            // Write UTF-8 BOM for Microsoft Excel / Sheets compatibility
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            OutputStreamWriter(fos, Charsets.UTF_8).use { writer ->
                // Header row
                writer.write(
                    listOf(
                        "Date",
                        "Time",
                        "Transaction ID",
                        "Type",
                        "Category",
                        "Account",
                        "Amount",
                        "Currency",
                        "Note",
                        "Merchant"
                    ).joinToString(",") { escapeCsv(it) } + "\r\n"
                )

                // Data rows
                for (item in transactions) {
                    val tx = item.transaction
                    val dateObj = Date(tx.dateTime)
                    val signedAmount = when (tx.type) {
                        TransactionType.EXPENSE -> -kotlin.math.abs(tx.amount)
                        TransactionType.INCOME -> kotlin.math.abs(tx.amount)
                        TransactionType.TRANSFER -> tx.amount
                    }

                    val row = listOf(
                        dateFormat.format(dateObj),
                        timeFormat.format(dateObj),
                        tx.id.toString(),
                        tx.type.name,
                        item.category?.name ?: "Uncategorized",
                        item.sourceAccount?.name ?: "Default Account",
                        String.format(Locale.US, "%.2f", signedAmount),
                        currencySymbol,
                        tx.note,
                        tx.note // Merchant / Name
                    )

                    writer.write(row.joinToString(",") { escapeCsv(it) } + "\r\n")
                }
                writer.flush()
            }
        }

        return file
    }

    private fun escapeCsv(value: String): String {
        val containsSpecial = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")
        return if (containsSpecial) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
