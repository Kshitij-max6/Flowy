package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.core.util.CurrencyFormatter
import com.example.core.util.DateUtils
import com.example.data.local.AppDatabase
import com.example.domain.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class TodaySpendingWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val settings = db.userSettingsDao().getUserSettings().firstOrNull()
                val currency = settings?.defaultCurrency ?: "₹"
                val dailyBudget = settings?.dailyBudget ?: 1000.0

                val startOfDay = DateUtils.getStartOfToday()
                val endOfDay = DateUtils.getEndOfToday()
                val allTx = db.transactionDao().getAllTransactions().firstOrNull() ?: emptyList()
                val todaySpend = allTx
                    .filter { it.dateTime in startOfDay..endOfDay && it.type == "EXPENSE" }
                    .sumOf { it.amount }

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_today_spending)
                    views.setTextViewText(R.id.widget_today_amount, CurrencyFormatter.format(todaySpend, currency))
                    views.setTextViewText(R.id.widget_budget_target, "of ${CurrencyFormatter.format(dailyBudget, currency)} daily budget")
                    views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_today_spending)
                    views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}
