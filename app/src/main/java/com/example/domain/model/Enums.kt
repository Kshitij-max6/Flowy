package com.example.domain.model

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER
}

enum class CategoryType {
    EXPENSE,
    INCOME
}

enum class AccountType {
    BANK,
    CASH,
    UPI,
    CREDIT_CARD,
    SAVINGS,
    INVESTMENT,
    WALLET,
    LOAN,
    OTHER
}

enum class AccountClassification {
    ASSET,
    LIABILITY
}

val AccountType.classification: AccountClassification
    get() = when (this) {
        AccountType.CREDIT_CARD, AccountType.LOAN -> AccountClassification.LIABILITY
        else -> AccountClassification.ASSET
    }

enum class CreditCardStatus {
    ACTIVE,
    ARCHIVED
}

enum class LoanType {
    PERSONAL,
    HOME,
    VEHICLE,
    EDUCATION,
    BUSINESS,
    OTHER
}

enum class LoanStatus {
    ACTIVE,
    PAID_OFF,
    ARCHIVED
}

enum class ForecastPeriod {
    NEXT_7_DAYS,
    NEXT_30_DAYS,
    END_OF_MONTH
}

val ForecastPeriod.label: String
    get() = when (this) {
        ForecastPeriod.NEXT_7_DAYS -> "7 Days"
        ForecastPeriod.NEXT_30_DAYS -> "30 Days"
        ForecastPeriod.END_OF_MONTH -> "End of Month"
    }

enum class NetWorthTimeRange {
    ONE_MONTH,
    THREE_MONTHS,
    SIX_MONTHS,
    ONE_YEAR,
    ALL_TIME
}

val NetWorthTimeRange.label: String
    get() = when (this) {
        NetWorthTimeRange.ONE_MONTH -> "1M"
        NetWorthTimeRange.THREE_MONTHS -> "3M"
        NetWorthTimeRange.SIX_MONTHS -> "6M"
        NetWorthTimeRange.ONE_YEAR -> "1Y"
        NetWorthTimeRange.ALL_TIME -> "All"
    }

enum class FilterDatePreset {
    ALL,
    TODAY,
    YESTERDAY,
    THIS_WEEK,
    LAST_WEEK,
    THIS_MONTH,
    LAST_MONTH,
    CUSTOM
}

enum class FilterSource {
    ALL,
    MANUAL,
    RECURRING
}

enum class Frequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

enum class TimeFilter {
    THIS_WEEK,
    THIS_MONTH,
    LAST_MONTH,
    ALL_TIME
}

enum class BudgetPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    CUSTOM
}

enum class BudgetStatus {
    SAFE,             // < 70% used - Teal/Emerald
    MODERATE,         // 70% - 85% used - Subtle warning/amber
    ALMOST_EXHAUSTED, // 85% - 100% used - Orange
    EXCEEDED          // > 100% used - Red/Coral
}

enum class GoalStatus {
    ACTIVE,
    COMPLETED,
    ARCHIVED
}

enum class SpendingIntensity {
    NONE,
    LOW,
    MEDIUM,
    HIGH
}
