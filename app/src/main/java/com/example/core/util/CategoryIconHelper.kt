package com.example.core.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsSubway
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LaptopChromebook
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Category
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconHelper {

    fun getIcon(iconName: String?): ImageVector = getIconByName(iconName)

    fun getIconByName(iconName: String?): ImageVector {
        return when (iconName) {
            "Restaurant", "Food" -> Icons.Default.Restaurant
            "Fastfood" -> Icons.Default.Fastfood
            "LocalCafe", "Coffee", "Cafe" -> Icons.Default.LocalCafe
            "BakeryDining", "Bakery" -> Icons.Default.BakeryDining
            "LocalBar", "Bar", "Drinks" -> Icons.Default.LocalBar
            "DirectionsSubway", "Transport", "Metro", "Transit" -> Icons.Default.DirectionsSubway
            "DirectionsCar", "Car", "Vehicle" -> Icons.Default.DirectionsCar
            "LocalGasStation", "Fuel", "Gas" -> Icons.Default.LocalGasStation
            "LocalTaxi", "Taxi", "Cab" -> Icons.Default.LocalTaxi
            "Commute", "RideSharing" -> Icons.Default.Commute
            "LocalParking", "Parking" -> Icons.Default.LocalParking
            "ShoppingBag", "Shopping" -> Icons.Default.ShoppingBag
            "ShoppingCart" -> Icons.Default.ShoppingCart
            "Checkroom", "Clothing", "Clothes" -> Icons.Default.Checkroom
            "Devices", "Electronics", "Gadgets" -> Icons.Default.Devices
            "Diamond", "Jewelry" -> Icons.Default.Diamond
            "ReceiptLong", "Bills" -> Icons.Default.ReceiptLong
            "Bolt", "Electricity" -> Icons.Default.Bolt
            "WaterDrop", "Water" -> Icons.Default.WaterDrop
            "Wifi", "Internet" -> Icons.Default.Wifi
            "Tv", "Television" -> Icons.Default.Tv
            "PhoneAndroid", "Mobile", "Smartphone" -> Icons.Default.Smartphone
            "Movie", "Entertainment", "Cinema" -> Icons.Default.Movie
            "MusicNote", "Music", "Audio" -> Icons.Default.MusicNote
            "SportsEsports", "Gaming", "Games" -> Icons.Default.SportsEsports
            "SmartDisplay", "Streaming", "OTT" -> Icons.Default.SmartDisplay
            "Palette", "Hobbies", "Art" -> Icons.Default.Palette
            "ConfirmationNumber", "Events", "Tickets" -> Icons.Default.ConfirmationNumber
            "FitnessCenter", "Health", "Fitness", "Gym" -> Icons.Default.FitnessCenter
            "LocalHospital", "Hospital" -> Icons.Default.LocalHospital
            "Medication", "Pharmacy", "Medicine" -> Icons.Default.Medication
            "School", "Education", "College" -> Icons.Default.School
            "MenuBook", "Books" -> Icons.Default.MenuBook
            "Flight", "Travel", "Airplane" -> Icons.Default.Flight
            "Hotel", "Stay" -> Icons.Default.Hotel
            "Train" -> Icons.Default.Train
            "Home", "Rent", "Housing" -> Icons.Default.Home
            "Apartment" -> Icons.Default.Apartment
            "Build", "Repairs", "Maintenance" -> Icons.Default.Build
            "Chair", "Furniture" -> Icons.Default.Chair
            "LocalGroceryStore", "Groceries", "Supermarket" -> Icons.Default.LocalGroceryStore
            "CleaningServices", "Housekeeping" -> Icons.Default.CleaningServices
            "Spa", "PersonalCare", "Wellness" -> Icons.Default.Spa
            "ContentCut", "Salon", "Haircut" -> Icons.Default.ContentCut
            "Face", "Grooming" -> Icons.Default.Face
            "Payments", "Salary" -> Icons.Default.Payments
            "Schedule", "Overtime" -> Icons.Default.Schedule
            "LaptopMac", "Freelance", "Laptop" -> Icons.Default.LaptopMac
            "Work", "Consulting", "Job" -> Icons.Default.Work
            "Storefront", "Business", "Shop" -> Icons.Default.Storefront
            "TrendingUp", "Investment" -> Icons.AutoMirrored.Filled.TrendingUp
            "CardGiftcard", "Gift", "Bonus" -> Icons.Default.CardGiftcard
            "Savings" -> Icons.Default.Savings
            "AccountBalanceWallet", "Wallet" -> Icons.Default.AccountBalanceWallet
            "AccountBalance", "Bank" -> Icons.Default.AccountBalance
            "CreditCard" -> Icons.Default.CreditCard
            "QrCode", "UPI" -> Icons.Default.QrCode
            "SwapHoriz", "Transfer" -> Icons.Default.SwapHoriz
            "ChildCare", "Baby", "Kids" -> Icons.Default.ChildCare
            "VolunteerActivism", "Charity", "Donation" -> Icons.Default.VolunteerActivism
            "Pets", "Pet" -> Icons.Default.Pets
            "Repeat", "Subscription" -> Icons.Default.Repeat
            "Cloud" -> Icons.Default.Cloud
            "Celebration", "Party", "Event" -> Icons.Default.Celebration
            "ShowChart", "Stocks" -> Icons.Default.ShowChart
            "Warning", "Emergency" -> Icons.Default.Warning
            "CurrencyExchange", "Refund", "Cashback" -> Icons.Default.CurrencyExchange
            "People", "Family", "Referral" -> Icons.Default.People
            "Paid" -> Icons.Default.Paid
            else -> Icons.Default.Category
        }
    }

    val availableCategoryIcons = listOf(
        "Restaurant", "Fastfood", "LocalCafe", "BakeryDining", "LocalBar",
        "DirectionsSubway", "DirectionsCar", "LocalGasStation", "LocalTaxi", "LocalParking",
        "ShoppingBag", "ShoppingCart", "Checkroom", "Devices", "Diamond",
        "ReceiptLong", "Bolt", "WaterDrop", "Wifi", "Tv",
        "Smartphone", "Movie", "MusicNote", "SportsEsports", "SmartDisplay",
        "Palette", "ConfirmationNumber", "FitnessCenter", "LocalHospital", "Medication",
        "School", "MenuBook", "Flight", "Hotel", "Train",
        "Home", "Apartment", "Build", "Chair", "LocalGroceryStore",
        "Spa", "ContentCut", "Face", "Payments", "LaptopMac",
        "Work", "Storefront", "TrendingUp", "CardGiftcard", "Savings",
        "AccountBalanceWallet", "AccountBalance", "CreditCard", "ChildCare", "VolunteerActivism",
        "Pets", "Repeat", "Celebration", "ShowChart", "Warning",
        "CurrencyExchange", "People"
    )

    val curatedCategoryColors = listOf(
        "#F59E0B", // Amber / Warm Orange
        "#3B82F6", // Blue
        "#EC4899", // Pink
        "#EF4444", // Red
        "#8B5CF6", // Purple
        "#10B981", // Emerald
        "#6366F1", // Indigo
        "#06B6D4", // Cyan
        "#14B8A6", // Teal
        "#84CC16", // Lime
        "#D946EF", // Fuchsia
        "#FB923C", // Orange
        "#F43F5E", // Rose
        "#A855F7", // Violet
        "#059669", // Dark Green
        "#64748B"  // Slate / Neutral
    )
}
