package com.example.data.local

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.RecurringTransactionEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserSettingsEntity
import java.util.Calendar

object DatabasePrepopulate {

    fun getDefaultCategories(): List<CategoryEntity> {
        return listOf(
            // ==========================================
            // Main Expense Categories (Preserving IDs 1..12)
            // ==========================================
            CategoryEntity(id = 1, name = "Food & Dining", iconName = "Restaurant", type = "EXPENSE", colorHex = "#F59E0B", isCustom = false, parentCategoryId = null, sortOrder = 1),
            CategoryEntity(id = 2, name = "Transportation", iconName = "DirectionsSubway", type = "EXPENSE", colorHex = "#3B82F6", isCustom = false, parentCategoryId = null, sortOrder = 2),
            CategoryEntity(id = 3, name = "Shopping", iconName = "ShoppingBag", type = "EXPENSE", colorHex = "#EC4899", isCustom = false, parentCategoryId = null, sortOrder = 3),
            CategoryEntity(id = 4, name = "Bills & Utilities", iconName = "ReceiptLong", type = "EXPENSE", colorHex = "#EF4444", isCustom = false, parentCategoryId = null, sortOrder = 4),
            CategoryEntity(id = 5, name = "Entertainment", iconName = "Movie", type = "EXPENSE", colorHex = "#8B5CF6", isCustom = false, parentCategoryId = null, sortOrder = 5),
            CategoryEntity(id = 6, name = "Health & Wellness", iconName = "FitnessCenter", type = "EXPENSE", colorHex = "#10B981", isCustom = false, parentCategoryId = null, sortOrder = 6),
            CategoryEntity(id = 7, name = "Education", iconName = "School", type = "EXPENSE", colorHex = "#6366F1", isCustom = false, parentCategoryId = null, sortOrder = 7),
            CategoryEntity(id = 8, name = "Travel & Trips", iconName = "Flight", type = "EXPENSE", colorHex = "#06B6D4", isCustom = false, parentCategoryId = null, sortOrder = 8),
            CategoryEntity(id = 9, name = "Housing & Rent", iconName = "Home", type = "EXPENSE", colorHex = "#14B8A6", isCustom = false, parentCategoryId = null, sortOrder = 9),
            CategoryEntity(id = 10, name = "Groceries", iconName = "LocalGroceryStore", type = "EXPENSE", colorHex = "#84CC16", isCustom = false, parentCategoryId = null, sortOrder = 10),
            CategoryEntity(id = 11, name = "Personal Care", iconName = "Spa", type = "EXPENSE", colorHex = "#D946EF", isCustom = false, parentCategoryId = null, sortOrder = 11),
            CategoryEntity(id = 12, name = "Miscellaneous", iconName = "MoreHoriz", type = "EXPENSE", colorHex = "#64748B", isCustom = false, parentCategoryId = null, sortOrder = 25),

            // Additional Main Expense Categories
            CategoryEntity(id = 19, name = "Family & Kids", iconName = "ChildCare", type = "EXPENSE", colorHex = "#FB923C", isCustom = false, parentCategoryId = null, sortOrder = 12),
            CategoryEntity(id = 20, name = "Pets", iconName = "Pets", type = "EXPENSE", colorHex = "#A855F7", isCustom = false, parentCategoryId = null, sortOrder = 13),
            CategoryEntity(id = 21, name = "Gifts & Donations", iconName = "CardGiftcard", type = "EXPENSE", colorHex = "#F43F5E", isCustom = false, parentCategoryId = null, sortOrder = 14),
            CategoryEntity(id = 22, name = "Subscriptions", iconName = "Repeat", type = "EXPENSE", colorHex = "#6366F1", isCustom = false, parentCategoryId = null, sortOrder = 15),
            CategoryEntity(id = 23, name = "Investments & Savings", iconName = "TrendingUp", type = "EXPENSE", colorHex = "#059669", isCustom = false, parentCategoryId = null, sortOrder = 16),
            CategoryEntity(id = 24, name = "Fees & Charges", iconName = "AccountBalanceWallet", type = "EXPENSE", colorHex = "#B91C1C", isCustom = false, parentCategoryId = null, sortOrder = 17),

            // ==========================================
            // Main Income Categories (Preserving IDs 13..18)
            // ==========================================
            CategoryEntity(id = 13, name = "Salary", iconName = "Payments", type = "INCOME", colorHex = "#10B981", isCustom = false, parentCategoryId = null, sortOrder = 1),
            CategoryEntity(id = 14, name = "Freelance & Projects", iconName = "LaptopMac", type = "INCOME", colorHex = "#3B82F6", isCustom = false, parentCategoryId = null, sortOrder = 2),
            CategoryEntity(id = 15, name = "Business Revenue", iconName = "Storefront", type = "INCOME", colorHex = "#8B5CF6", isCustom = false, parentCategoryId = null, sortOrder = 3),
            CategoryEntity(id = 16, name = "Investments & Returns", iconName = "TrendingUp", type = "INCOME", colorHex = "#059669", isCustom = false, parentCategoryId = null, sortOrder = 4),
            CategoryEntity(id = 17, name = "Gifts & Rewards", iconName = "CardGiftcard", type = "INCOME", colorHex = "#EC4899", isCustom = false, parentCategoryId = null, sortOrder = 5),
            CategoryEntity(id = 18, name = "Other Income", iconName = "AccountBalanceWallet", type = "INCOME", colorHex = "#64748B", isCustom = false, parentCategoryId = null, sortOrder = 8),
            CategoryEntity(id = 25, name = "Rental Income", iconName = "Home", type = "INCOME", colorHex = "#0284C7", isCustom = false, parentCategoryId = null, sortOrder = 6),
            CategoryEntity(id = 26, name = "Refunds & Cashback", iconName = "CurrencyExchange", type = "INCOME", colorHex = "#06B6D4", isCustom = false, parentCategoryId = null, sortOrder = 7),

            // ==========================================
            // Expense Subcategories
            // ==========================================
            // Food & Dining (parent: 1)
            CategoryEntity(id = 101, name = "Restaurants", iconName = "Restaurant", type = "EXPENSE", colorHex = "#F59E0B", isCustom = false, parentCategoryId = 1, sortOrder = 1),
            CategoryEntity(id = 102, name = "Cafes & Coffee", iconName = "LocalCafe", type = "EXPENSE", colorHex = "#F59E0B", isCustom = false, parentCategoryId = 1, sortOrder = 2),
            CategoryEntity(id = 103, name = "Fast Food", iconName = "Fastfood", type = "EXPENSE", colorHex = "#F59E0B", isCustom = false, parentCategoryId = 1, sortOrder = 3),
            CategoryEntity(id = 104, name = "Food Delivery", iconName = "Fastfood", type = "EXPENSE", colorHex = "#F59E0B", isCustom = false, parentCategoryId = 1, sortOrder = 4),
            CategoryEntity(id = 105, name = "Bakery & Snacks", iconName = "BakeryDining", type = "EXPENSE", colorHex = "#F59E0B", isCustom = false, parentCategoryId = 1, sortOrder = 5),
            CategoryEntity(id = 106, name = "Drinks & Nightlife", iconName = "LocalBar", type = "EXPENSE", colorHex = "#F59E0B", isCustom = false, parentCategoryId = 1, sortOrder = 6),

            // Transportation (parent: 2)
            CategoryEntity(id = 201, name = "Fuel & Petrol", iconName = "LocalGasStation", type = "EXPENSE", colorHex = "#3B82F6", isCustom = false, parentCategoryId = 2, sortOrder = 1),
            CategoryEntity(id = 202, name = "Metro & Subway", iconName = "DirectionsSubway", type = "EXPENSE", colorHex = "#3B82F6", isCustom = false, parentCategoryId = 2, sortOrder = 2),
            CategoryEntity(id = 203, name = "Taxi & Cab", iconName = "LocalTaxi", type = "EXPENSE", colorHex = "#3B82F6", isCustom = false, parentCategoryId = 2, sortOrder = 3),
            CategoryEntity(id = 204, name = "Parking & Tolls", iconName = "LocalParking", type = "EXPENSE", colorHex = "#3B82F6", isCustom = false, parentCategoryId = 2, sortOrder = 4),
            CategoryEntity(id = 205, name = "Vehicle Service", iconName = "Build", type = "EXPENSE", colorHex = "#3B82F6", isCustom = false, parentCategoryId = 2, sortOrder = 5),

            // Shopping (parent: 3)
            CategoryEntity(id = 301, name = "Clothing & Apparel", iconName = "Checkroom", type = "EXPENSE", colorHex = "#EC4899", isCustom = false, parentCategoryId = 3, sortOrder = 1),
            CategoryEntity(id = 302, name = "Electronics & Gadgets", iconName = "Devices", type = "EXPENSE", colorHex = "#EC4899", isCustom = false, parentCategoryId = 3, sortOrder = 2),
            CategoryEntity(id = 303, name = "Footwear", iconName = "ShoppingBag", type = "EXPENSE", colorHex = "#EC4899", isCustom = false, parentCategoryId = 3, sortOrder = 3),
            CategoryEntity(id = 304, name = "Online Shopping", iconName = "ShoppingCart", type = "EXPENSE", colorHex = "#EC4899", isCustom = false, parentCategoryId = 3, sortOrder = 4),
            CategoryEntity(id = 305, name = "Jewelry & Accessories", iconName = "Diamond", type = "EXPENSE", colorHex = "#EC4899", isCustom = false, parentCategoryId = 3, sortOrder = 5),

            // Bills & Utilities (parent: 4)
            CategoryEntity(id = 401, name = "Electricity Bill", iconName = "Bolt", type = "EXPENSE", colorHex = "#EF4444", isCustom = false, parentCategoryId = 4, sortOrder = 1),
            CategoryEntity(id = 402, name = "Water Bill", iconName = "WaterDrop", type = "EXPENSE", colorHex = "#EF4444", isCustom = false, parentCategoryId = 4, sortOrder = 2),
            CategoryEntity(id = 403, name = "Internet & WiFi", iconName = "Wifi", type = "EXPENSE", colorHex = "#EF4444", isCustom = false, parentCategoryId = 4, sortOrder = 3),
            CategoryEntity(id = 404, name = "Mobile Recharge", iconName = "Smartphone", type = "EXPENSE", colorHex = "#EF4444", isCustom = false, parentCategoryId = 4, sortOrder = 4),
            CategoryEntity(id = 405, name = "Gas & Cylinder", iconName = "Bolt", type = "EXPENSE", colorHex = "#EF4444", isCustom = false, parentCategoryId = 4, sortOrder = 5),
            CategoryEntity(id = 406, name = "DTH & Cable", iconName = "Tv", type = "EXPENSE", colorHex = "#EF4444", isCustom = false, parentCategoryId = 4, sortOrder = 6),

            // Entertainment (parent: 5)
            CategoryEntity(id = 501, name = "Movies & Cinema", iconName = "Movie", type = "EXPENSE", colorHex = "#8B5CF6", isCustom = false, parentCategoryId = 5, sortOrder = 1),
            CategoryEntity(id = 502, name = "Music & Podcasts", iconName = "MusicNote", type = "EXPENSE", colorHex = "#8B5CF6", isCustom = false, parentCategoryId = 5, sortOrder = 2),
            CategoryEntity(id = 503, name = "Video Games", iconName = "SportsEsports", type = "EXPENSE", colorHex = "#8B5CF6", isCustom = false, parentCategoryId = 5, sortOrder = 3),
            CategoryEntity(id = 504, name = "Streaming (OTT)", iconName = "SmartDisplay", type = "EXPENSE", colorHex = "#8B5CF6", isCustom = false, parentCategoryId = 5, sortOrder = 4),
            CategoryEntity(id = 505, name = "Concerts & Events", iconName = "ConfirmationNumber", type = "EXPENSE", colorHex = "#8B5CF6", isCustom = false, parentCategoryId = 5, sortOrder = 5),
            CategoryEntity(id = 506, name = "Hobbies & Art", iconName = "Palette", type = "EXPENSE", colorHex = "#8B5CF6", isCustom = false, parentCategoryId = 5, sortOrder = 6),

            // Health & Wellness (parent: 6)
            CategoryEntity(id = 601, name = "Doctor & Clinic", iconName = "LocalHospital", type = "EXPENSE", colorHex = "#10B981", isCustom = false, parentCategoryId = 6, sortOrder = 1),
            CategoryEntity(id = 602, name = "Pharmacy & Meds", iconName = "Medication", type = "EXPENSE", colorHex = "#10B981", isCustom = false, parentCategoryId = 6, sortOrder = 2),
            CategoryEntity(id = 603, name = "Gym & Workout", iconName = "FitnessCenter", type = "EXPENSE", colorHex = "#10B981", isCustom = false, parentCategoryId = 6, sortOrder = 3),
            CategoryEntity(id = 604, name = "Health Checkups", iconName = "LocalHospital", type = "EXPENSE", colorHex = "#10B981", isCustom = false, parentCategoryId = 6, sortOrder = 4),

            // Education (parent: 7)
            CategoryEntity(id = 701, name = "Tuition & Fees", iconName = "School", type = "EXPENSE", colorHex = "#6366F1", isCustom = false, parentCategoryId = 7, sortOrder = 1),
            CategoryEntity(id = 702, name = "Books & Stationery", iconName = "MenuBook", type = "EXPENSE", colorHex = "#6366F1", isCustom = false, parentCategoryId = 7, sortOrder = 2),
            CategoryEntity(id = 703, name = "Online Courses", iconName = "LaptopChromebook", type = "EXPENSE", colorHex = "#6366F1", isCustom = false, parentCategoryId = 7, sortOrder = 3),

            // Travel & Trips (parent: 8)
            CategoryEntity(id = 801, name = "Flights", iconName = "Flight", type = "EXPENSE", colorHex = "#06B6D4", isCustom = false, parentCategoryId = 8, sortOrder = 1),
            CategoryEntity(id = 802, name = "Hotels & Stays", iconName = "Hotel", type = "EXPENSE", colorHex = "#06B6D4", isCustom = false, parentCategoryId = 8, sortOrder = 2),
            CategoryEntity(id = 803, name = "Trains & Buses", iconName = "Train", type = "EXPENSE", colorHex = "#06B6D4", isCustom = false, parentCategoryId = 8, sortOrder = 3),
            CategoryEntity(id = 804, name = "Sightseeing & Tours", iconName = "Landscape", type = "EXPENSE", colorHex = "#06B6D4", isCustom = false, parentCategoryId = 8, sortOrder = 4),

            // Housing & Rent (parent: 9)
            CategoryEntity(id = 901, name = "House Rent", iconName = "Home", type = "EXPENSE", colorHex = "#14B8A6", isCustom = false, parentCategoryId = 9, sortOrder = 1),
            CategoryEntity(id = 902, name = "Maintenance & Society", iconName = "Apartment", type = "EXPENSE", colorHex = "#14B8A6", isCustom = false, parentCategoryId = 9, sortOrder = 2),
            CategoryEntity(id = 903, name = "Repairs & Tools", iconName = "Build", type = "EXPENSE", colorHex = "#14B8A6", isCustom = false, parentCategoryId = 9, sortOrder = 3),
            CategoryEntity(id = 904, name = "Furniture & Decor", iconName = "Chair", type = "EXPENSE", colorHex = "#14B8A6", isCustom = false, parentCategoryId = 9, sortOrder = 4),

            // Groceries (parent: 10)
            CategoryEntity(id = 1001, name = "Fruits & Veggies", iconName = "LocalGroceryStore", type = "EXPENSE", colorHex = "#84CC16", isCustom = false, parentCategoryId = 10, sortOrder = 1),
            CategoryEntity(id = 1002, name = "Dairy & Bread", iconName = "BakeryDining", type = "EXPENSE", colorHex = "#84CC16", isCustom = false, parentCategoryId = 10, sortOrder = 2),
            CategoryEntity(id = 1003, name = "Household Cleaners", iconName = "CleaningServices", type = "EXPENSE", colorHex = "#84CC16", isCustom = false, parentCategoryId = 10, sortOrder = 3),

            // Personal Care (parent: 11)
            CategoryEntity(id = 1101, name = "Salon & Haircut", iconName = "ContentCut", type = "EXPENSE", colorHex = "#D946EF", isCustom = false, parentCategoryId = 11, sortOrder = 1),
            CategoryEntity(id = 1102, name = "Skincare & Spa", iconName = "Spa", type = "EXPENSE", colorHex = "#D946EF", isCustom = false, parentCategoryId = 11, sortOrder = 2),
            CategoryEntity(id = 1103, name = "Grooming Products", iconName = "Face", type = "EXPENSE", colorHex = "#D946EF", isCustom = false, parentCategoryId = 11, sortOrder = 3),

            // ==========================================
            // Income Subcategories
            // ==========================================
            // Salary (parent: 13)
            CategoryEntity(id = 1301, name = "Monthly Salary", iconName = "Payments", type = "INCOME", colorHex = "#10B981", isCustom = false, parentCategoryId = 13, sortOrder = 1),
            CategoryEntity(id = 1302, name = "Performance Bonus", iconName = "CardGiftcard", type = "INCOME", colorHex = "#10B981", isCustom = false, parentCategoryId = 13, sortOrder = 2),
            CategoryEntity(id = 1303, name = "Overtime Compensation", iconName = "Schedule", type = "INCOME", colorHex = "#10B981", isCustom = false, parentCategoryId = 13, sortOrder = 3),

            // Freelance (parent: 14)
            CategoryEntity(id = 1401, name = "Client Projects", iconName = "LaptopMac", type = "INCOME", colorHex = "#3B82F6", isCustom = false, parentCategoryId = 14, sortOrder = 1),
            CategoryEntity(id = 1402, name = "Consulting Fee", iconName = "Work", type = "INCOME", colorHex = "#3B82F6", isCustom = false, parentCategoryId = 14, sortOrder = 2),
            CategoryEntity(id = 1403, name = "Content & Royalties", iconName = "Palette", type = "INCOME", colorHex = "#3B82F6", isCustom = false, parentCategoryId = 14, sortOrder = 3),

            // Investments (parent: 16)
            CategoryEntity(id = 1601, name = "Stock Dividends", iconName = "Paid", type = "INCOME", colorHex = "#059669", isCustom = false, parentCategoryId = 16, sortOrder = 1),
            CategoryEntity(id = 1602, name = "Bank Interest", iconName = "Savings", type = "INCOME", colorHex = "#059669", isCustom = false, parentCategoryId = 16, sortOrder = 2),
            CategoryEntity(id = 1603, name = "Mutual Fund Gains", iconName = "TrendingUp", type = "INCOME", colorHex = "#059669", isCustom = false, parentCategoryId = 16, sortOrder = 3)
        )
    }

    /** What a brand-new install gets: one empty account so the first entry can be added. No sample money. */
    fun getStarterAccounts(): List<AccountEntity> = listOf(
        AccountEntity(id = 1, name = "Cash", type = "CASH", balance = 0.0, currency = "₹", iconName = "Payments", colorHex = "#10B981", isDefault = true)
    )

    /** A brand-new install gets only the default ledger, with no budget and no sample books. */
    fun getStarterExpenseBooks(): List<com.example.data.local.entity.ExpenseBookEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            com.example.data.local.entity.ExpenseBookEntity(
                id = 1, name = "Personal & Daily", description = "", iconName = "Wallet", colorHex = "#E5C378",
                budget = 0.0, isArchived = false, isDefault = true, createdAt = now, updatedAt = now
            )
        )
    }

    // NOTE: the getDefault* / getInitialSample* lists below are the OLD demo data. They are no longer
    // inserted anywhere; they are kept only so SampleDataCleaner can recognise and remove them from
    // installs that already received them.
    fun getDefaultAccounts(): List<AccountEntity> {
        return listOf(
            AccountEntity(id = 1, name = "Main Bank", type = "BANK", balance = 34500.0, currency = "₹", iconName = "AccountBalance", colorHex = "#3B82F6", isDefault = true),
            AccountEntity(id = 2, name = "UPI / PhonePe", type = "UPI", balance = 2850.0, currency = "₹", iconName = "QrCode", colorHex = "#8B5CF6", isDefault = false),
            AccountEntity(id = 3, name = "Cash Wallet", type = "CASH", balance = 1200.0, currency = "₹", iconName = "Payments", colorHex = "#10B981", isDefault = false),
            AccountEntity(id = 4, name = "Savings Deposit", type = "SAVINGS", balance = 85000.0, currency = "₹", iconName = "Savings", colorHex = "#059669", isDefault = false),
            AccountEntity(id = 5, name = "Credit Card", type = "CREDIT_CARD", balance = -4200.0, currency = "₹", iconName = "CreditCard", colorHex = "#F43F5E", isDefault = false)
        )
    }

    fun getDefaultUserSettings(): UserSettingsEntity {
        return UserSettingsEntity(
            id = 1,
            userName = "Kshitij",
            defaultCurrency = "₹",
            dailyBudget = 1000.0,
            monthlyBudget = 30000.0,
            themeMode = "SYSTEM",
            hasCompletedOnboarding = true,
            enableHaptics = true,
            hideAmounts = false,
            hideAmountsPersist = true,
            appLockEnabled = false,
            appLockTimeoutMinutes = 0,
            homeSectionsOrder = "spending,budget,recent,insight,upcoming,accounts",
            hiddenHomeSections = "",
            prestigeTheme = "OBSIDIAN_GOLD",
            heroFont = "LUXURY_SERIF"
        )
    }

    fun getDefaultRecurring(): List<RecurringTransactionEntity> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 1) // Tomorrow
        val tomorrow = cal.timeInMillis

        cal.add(Calendar.DAY_OF_MONTH, 3)
        val inFourDays = cal.timeInMillis

        cal.add(Calendar.DAY_OF_MONTH, 6)
        val inTenDays = cal.timeInMillis

        return listOf(
            RecurringTransactionEntity(
                id = 1,
                name = "Netflix",
                amount = 199.0,
                type = "EXPENSE",
                categoryId = 5, // Entertainment
                accountId = 1, // Main Bank
                frequency = "MONTHLY",
                nextExecutionDate = tomorrow,
                active = true,
                note = "Monthly streaming subscription"
            ),
            RecurringTransactionEntity(
                id = 2,
                name = "Spotify Premium",
                amount = 119.0,
                type = "EXPENSE",
                categoryId = 5,
                accountId = 2, // UPI
                frequency = "MONTHLY",
                nextExecutionDate = inFourDays,
                active = true,
                note = "Individual plan"
            ),
            RecurringTransactionEntity(
                id = 3,
                name = "Fiber Broadband",
                amount = 799.0,
                type = "EXPENSE",
                categoryId = 4, // Bills
                accountId = 1,
                frequency = "MONTHLY",
                nextExecutionDate = inTenDays,
                active = true,
                note = "Home high-speed internet"
            )
        )
    }

    fun getInitialSampleTransactions(): List<TransactionEntity> {
        val now = System.currentTimeMillis()
        val oneHourAgo = now - (60 * 60 * 1000)
        val threeHoursAgo = now - (3 * 60 * 60 * 1000)
        val yesterday = now - (24 * 60 * 60 * 1000)
        val twoDaysAgo = now - (2 * 24 * 60 * 60 * 1000)
        val fiveDaysAgo = now - (5 * 24 * 60 * 60 * 1000)

        return listOf(
            // Today's spending = 350 + 40 + 30 = 420 (matching the exact design example ₹420!)
            TransactionEntity(
                id = 1,
                amount = 350.0,
                type = "EXPENSE",
                categoryId = 1, // Food
                accountId = 2, // UPI
                destinationAccountId = null,
                dateTime = threeHoursAgo,
                note = "Swiggy lunch order",
                createdAt = threeHoursAgo,
                updatedAt = threeHoursAgo
            ),
            TransactionEntity(
                id = 2,
                amount = 40.0,
                type = "EXPENSE",
                categoryId = 2, // Transport
                accountId = 2, // UPI
                destinationAccountId = null,
                dateTime = oneHourAgo,
                note = "Metro card recharge",
                createdAt = oneHourAgo,
                updatedAt = oneHourAgo
            ),
            TransactionEntity(
                id = 3,
                amount = 30.0,
                type = "EXPENSE",
                categoryId = 1, // Food
                accountId = 3, // Cash
                destinationAccountId = null,
                dateTime = now - (30 * 60 * 1000),
                note = "Chai & snacks",
                createdAt = now - (30 * 60 * 1000),
                updatedAt = now - (30 * 60 * 1000)
            ),
            // Past days
            TransactionEntity(
                id = 4,
                amount = 50000.0,
                type = "INCOME",
                categoryId = 13, // Salary
                accountId = 1, // Bank
                destinationAccountId = null,
                dateTime = fiveDaysAgo,
                note = "Monthly Salary Credited",
                createdAt = fiveDaysAgo,
                updatedAt = fiveDaysAgo
            ),
            TransactionEntity(
                id = 5,
                amount = 1450.0,
                type = "EXPENSE",
                categoryId = 10, // Groceries
                accountId = 1, // Bank
                destinationAccountId = null,
                dateTime = yesterday,
                note = "Weekly organic groceries",
                createdAt = yesterday,
                updatedAt = yesterday
            ),
            TransactionEntity(
                id = 6,
                amount = 5000.0,
                type = "TRANSFER",
                categoryId = 12,
                accountId = 1, // From Main Bank
                destinationAccountId = 4, // To Savings Deposit
                dateTime = twoDaysAgo,
                note = "Monthly automatic savings reserve",
                createdAt = twoDaysAgo,
                updatedAt = twoDaysAgo
            )
        )
    }

    fun getDefaultBudgets(): List<com.example.data.local.entity.BudgetEntity> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfMonth = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endOfMonth = cal.timeInMillis

        return listOf(
            com.example.data.local.entity.BudgetEntity(
                id = 1,
                name = "Monthly Overall Budget",
                amount = 20000.0,
                period = "MONTHLY",
                categoryId = null,
                startDate = startOfMonth,
                endDate = endOfMonth,
                active = true
            ),
            com.example.data.local.entity.BudgetEntity(
                id = 2,
                name = "Food & Dining",
                amount = 5000.0,
                period = "MONTHLY",
                categoryId = 1,
                startDate = startOfMonth,
                endDate = endOfMonth,
                active = true
            ),
            com.example.data.local.entity.BudgetEntity(
                id = 3,
                name = "Transport & Commute",
                amount = 2000.0,
                period = "MONTHLY",
                categoryId = 2,
                startDate = startOfMonth,
                endDate = endOfMonth,
                active = true
            ),
            com.example.data.local.entity.BudgetEntity(
                id = 4,
                name = "Entertainment & Subs",
                amount = 1500.0,
                period = "MONTHLY",
                categoryId = 5,
                startDate = startOfMonth,
                endDate = endOfMonth,
                active = true
            )
        )
    }

    fun getDefaultGoals(): List<com.example.data.local.entity.GoalEntity> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, 3)
        val in3Months = cal.timeInMillis

        cal.add(Calendar.MONTH, 6)
        val in9Months = cal.timeInMillis

        cal.add(Calendar.MONTH, 3)
        val in12Months = cal.timeInMillis

        val now = System.currentTimeMillis()

        return listOf(
            com.example.data.local.entity.GoalEntity(
                id = 1,
                name = "New Laptop",
                targetAmount = 35000.0,
                currentAmount = 12500.0,
                currency = "₹",
                targetDate = in3Months,
                createdDate = now - (30L * 24 * 60 * 60 * 1000),
                linkedAccountId = 4, // Savings Deposit
                iconName = "LaptopMac",
                status = "ACTIVE"
            ),
            com.example.data.local.entity.GoalEntity(
                id = 2,
                name = "Emergency Fund",
                targetAmount = 100000.0,
                currentAmount = 45000.0,
                currency = "₹",
                targetDate = in12Months,
                createdDate = now - (60L * 24 * 60 * 60 * 1000),
                linkedAccountId = 4,
                iconName = "Shield",
                status = "ACTIVE"
            ),
            com.example.data.local.entity.GoalEntity(
                id = 3,
                name = "Vacation Trip",
                targetAmount = 50000.0,
                currentAmount = 8000.0,
                currency = "₹",
                targetDate = in9Months,
                createdDate = now - (15L * 24 * 60 * 60 * 1000),
                linkedAccountId = 1,
                iconName = "Flight",
                status = "ACTIVE"
            )
        )
    }

    fun getDefaultGoalContributions(): List<com.example.data.local.entity.GoalContributionEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            com.example.data.local.entity.GoalContributionEntity(
                id = 1,
                goalId = 1,
                amount = 5000.0,
                date = now - (20L * 24 * 60 * 60 * 1000),
                note = "Initial laptop fund allocation"
            ),
            com.example.data.local.entity.GoalContributionEntity(
                id = 2,
                goalId = 1,
                amount = 7500.0,
                date = now - (5L * 24 * 60 * 60 * 1000),
                note = "Bonus deposit"
            ),
            com.example.data.local.entity.GoalContributionEntity(
                id = 3,
                goalId = 2,
                amount = 25000.0,
                date = now - (50L * 24 * 60 * 60 * 1000),
                note = "Emergency baseline"
            ),
            com.example.data.local.entity.GoalContributionEntity(
                id = 4,
                goalId = 2,
                amount = 20000.0,
                date = now - (10L * 24 * 60 * 60 * 1000),
                note = "Savings transfer"
            ),
            com.example.data.local.entity.GoalContributionEntity(
                id = 5,
                goalId = 3,
                amount = 8000.0,
                date = now - (8L * 24 * 60 * 60 * 1000),
                note = "Flight booking savings"
            )
        )
    }

    fun getDefaultCreditCards(): List<com.example.data.local.entity.CreditCardEntity> {
        return listOf(
            com.example.data.local.entity.CreditCardEntity(
                id = 1,
                name = "HDFC Regalia Gold",
                bankName = "HDFC Bank",
                cardNumberMasked = "•••• 4821",
                creditLimit = 150000.0,
                outstandingBalance = 18500.0,
                billingCycleDay = 18,
                paymentDueDay = 8,
                linkedAccountId = 5,
                currency = "₹",
                cardColorHex = "#1E293B",
                status = "ACTIVE"
            ),
            com.example.data.local.entity.CreditCardEntity(
                id = 2,
                name = "ICICI Amazon Pay",
                bankName = "ICICI Bank",
                cardNumberMasked = "•••• 9104",
                creditLimit = 80000.0,
                outstandingBalance = 6400.0,
                billingCycleDay = 24,
                paymentDueDay = 14,
                linkedAccountId = null,
                currency = "₹",
                cardColorHex = "#065F46",
                status = "ACTIVE"
            )
        )
    }

    fun getDefaultLoans(): List<com.example.data.local.entity.LoanEntity> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 7)
        val nextDue1 = cal.timeInMillis

        cal.add(Calendar.DAY_OF_MONTH, 12)
        val nextDue2 = cal.timeInMillis

        val now = System.currentTimeMillis()

        return listOf(
            com.example.data.local.entity.LoanEntity(
                id = 1,
                name = "Education Loan",
                lender = "HDFC Credila",
                loanType = "EDUCATION",
                originalAmount = 500000.0,
                remainingBalance = 320000.0,
                interestRate = 8.75,
                monthlyPayment = 15400.0,
                nextPaymentDate = nextDue1,
                startDate = now - (365L * 24 * 60 * 60 * 1000),
                endDate = now + (3L * 365 * 24 * 60 * 60 * 1000),
                currency = "₹",
                status = "ACTIVE",
                linkedAccountId = 1
            ),
            com.example.data.local.entity.LoanEntity(
                id = 2,
                name = "Vehicle / Bike Loan",
                lender = "SBI Auto",
                loanType = "VEHICLE",
                originalAmount = 140000.0,
                remainingBalance = 42000.0,
                interestRate = 9.2,
                monthlyPayment = 4800.0,
                nextPaymentDate = nextDue2,
                startDate = now - (180L * 24 * 60 * 60 * 1000),
                endDate = now + (180L * 24 * 60 * 60 * 1000),
                currency = "₹",
                status = "ACTIVE",
                linkedAccountId = 1
            )
        )
    }

    fun getDefaultLoanPayments(): List<com.example.data.local.entity.LoanPaymentEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            com.example.data.local.entity.LoanPaymentEntity(
                id = 1,
                loanId = 1,
                amount = 15400.0,
                principalAmount = 13100.0,
                interestAmount = 2300.0,
                paymentDate = now - (23L * 24 * 60 * 60 * 1000),
                accountId = 1,
                note = "Monthly EMI payment auto-debited"
            ),
            com.example.data.local.entity.LoanPaymentEntity(
                id = 2,
                loanId = 1,
                amount = 15400.0,
                principalAmount = 12900.0,
                interestAmount = 2500.0,
                paymentDate = now - (53L * 24 * 60 * 60 * 1000),
                accountId = 1,
                note = "Monthly EMI payment auto-debited"
            ),
            com.example.data.local.entity.LoanPaymentEntity(
                id = 3,
                loanId = 2,
                amount = 4800.0,
                principalAmount = 4500.0,
                interestAmount = 300.0,
                paymentDate = now - (18L * 24 * 60 * 60 * 1000),
                accountId = 1,
                note = "SBI Auto EMI"
            )
        )
    }

    fun getDefaultNetWorthSnapshots(): List<com.example.data.local.entity.NetWorthSnapshotEntity> {
        val now = System.currentTimeMillis()
        val dayMillis = 24L * 60 * 60 * 1000
        return listOf(
            com.example.data.local.entity.NetWorthSnapshotEntity(
                id = 1,
                date = now - (120L * dayMillis),
                totalAssets = 92000.0,
                totalLiabilities = 410000.0,
                netWorth = -318000.0
            ),
            com.example.data.local.entity.NetWorthSnapshotEntity(
                id = 2,
                date = now - (90L * dayMillis),
                totalAssets = 104500.0,
                totalLiabilities = 398000.0,
                netWorth = -293500.0
            ),
            com.example.data.local.entity.NetWorthSnapshotEntity(
                id = 3,
                date = now - (60L * dayMillis),
                totalAssets = 118000.0,
                totalLiabilities = 385000.0,
                netWorth = -267000.0
            ),
            com.example.data.local.entity.NetWorthSnapshotEntity(
                id = 4,
                date = now - (30L * dayMillis),
                totalAssets = 135000.0,
                totalLiabilities = 372000.0,
                netWorth = -237000.0
            ),
            com.example.data.local.entity.NetWorthSnapshotEntity(
                id = 5,
                date = now,
                totalAssets = 158550.0,
                totalLiabilities = 362000.0,
                netWorth = -203450.0
            )
        )
    }

    fun getDefaultSavedFilters(): List<com.example.data.local.entity.SavedFilterEntity> {
        return listOf(
            com.example.data.local.entity.SavedFilterEntity(
                id = 1,
                name = "Food & Dining This Month",
                datePreset = "THIS_MONTH",
                customStartDate = null,
                customEndDate = null,
                transactionType = "EXPENSE",
                minAmount = null,
                maxAmount = null,
                categoryIds = "1,10",
                accountIds = "",
                sourceFilter = "ALL",
                hasNote = false,
                searchQuery = ""
            ),
            com.example.data.local.entity.SavedFilterEntity(
                id = 2,
                name = "Large Expenses > ₹1,000",
                datePreset = "ALL",
                customStartDate = null,
                customEndDate = null,
                transactionType = "EXPENSE",
                minAmount = 1000.0,
                maxAmount = null,
                categoryIds = "",
                accountIds = "",
                sourceFilter = "ALL",
                hasNote = false,
                searchQuery = ""
            ),
            com.example.data.local.entity.SavedFilterEntity(
                id = 3,
                name = "UPI Transactions",
                datePreset = "ALL",
                customStartDate = null,
                customEndDate = null,
                transactionType = null,
                minAmount = null,
                maxAmount = null,
                categoryIds = "",
                accountIds = "2",
                sourceFilter = "ALL",
                hasNote = false,
                searchQuery = ""
            )
        )
    }

    fun getDefaultExpenseBooks(): List<com.example.data.local.entity.ExpenseBookEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            com.example.data.local.entity.ExpenseBookEntity(
                id = 1,
                name = "Personal & Daily",
                description = "Primary household & personal day-to-day ledger",
                iconName = "Wallet",
                colorHex = "#E5C378",
                budget = 40000.0,
                isArchived = false,
                isDefault = true,
                createdAt = now,
                updatedAt = now
            ),
            com.example.data.local.entity.ExpenseBookEntity(
                id = 2,
                name = "Goa Vacation Trip",
                description = "Flights, resort, beach dining, scuba diving",
                iconName = "Flight",
                colorHex = "#38BDF8",
                budget = 35000.0,
                isArchived = false,
                isDefault = false,
                createdAt = now,
                updatedAt = now
            ),
            com.example.data.local.entity.ExpenseBookEntity(
                id = 3,
                name = "Home Renovation",
                description = "Furniture, interior decor, smart lighting & paint",
                iconName = "Home",
                colorHex = "#00E599",
                budget = 120000.0,
                isArchived = false,
                isDefault = false,
                createdAt = now,
                updatedAt = now
            ),
            com.example.data.local.entity.ExpenseBookEntity(
                id = 4,
                name = "Work & Reimbursable",
                description = "Client dinners, airport taxis & coworking passes",
                iconName = "Work",
                colorHex = "#A78BFA",
                budget = 20000.0,
                isArchived = false,
                isDefault = false,
                createdAt = now,
                updatedAt = now
            )
        )
    }
}
