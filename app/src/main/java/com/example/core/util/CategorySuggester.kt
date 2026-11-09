package com.example.core.util

import com.example.domain.model.Category
import com.example.domain.model.CategoryType
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType

/**
 * Fully offline category suggestion.
 *
 * Order of trust:
 *  1. What the user filed before (same or very similar description).
 *  2. A built-in list of common Indian spending words.
 *
 * Pure Kotlin, no Android dependencies, so it is cheap to unit test.
 */
object CategorySuggester {

    enum class Source { HISTORY, KEYWORDS }

    data class Suggestion(val categoryId: Long, val source: Source)

    fun suggest(
        text: String,
        type: TransactionType,
        categories: List<Category>,
        history: List<Transaction>
    ): Suggestion? {
        if (type == TransactionType.TRANSFER) return null
        val normalized = normalize(text)
        if (normalized.length < 2) return null

        val wantedType = if (type == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
        val usable = categories.filter { it.type == wantedType && !it.isArchived }
        if (usable.isEmpty()) return null
        val usableIds = usable.map { it.id }.toHashSet()

        fromHistory(normalized, type, usableIds, history)?.let { return Suggestion(it, Source.HISTORY) }
        fromKeywords(normalized, usable)?.let { return Suggestion(it, Source.KEYWORDS) }
        return null
    }

    // ------------------------------------------------------------------
    // History
    // ------------------------------------------------------------------

    private fun fromHistory(
        normalized: String,
        type: TransactionType,
        usableIds: Set<Long>,
        history: List<Transaction>
    ): Long? {
        val inputTokens = tokens(normalized)
        if (inputTokens.isEmpty()) return null

        val exact = HashMap<Long, Int>()
        val similar = HashMap<Long, Double>()

        for (tx in history) {
            if (tx.type != type || tx.categoryId !in usableIds) continue
            val note = normalize(tx.note)
            if (note.isEmpty()) continue
            if (note == normalized) {
                exact[tx.categoryId] = (exact[tx.categoryId] ?: 0) + 1
                continue
            }
            val noteTokens = tokens(note)
            if (noteTokens.isEmpty()) continue
            val common = inputTokens.intersect(noteTokens).size
            if (common == 0) continue
            val jaccard = common.toDouble() / (inputTokens.size + noteTokens.size - common)
            if (jaccard >= 0.5) {
                similar[tx.categoryId] = (similar[tx.categoryId] ?: 0.0) + jaccard
            }
        }

        exact.maxByOrNull { it.value }?.let { return it.key }
        similar.maxByOrNull { it.value }?.let { return it.key }
        return null
    }

    // ------------------------------------------------------------------
    // Keywords
    // ------------------------------------------------------------------

    private fun fromKeywords(normalized: String, usable: List<Category>): Long? {
        val padded = " $normalized "
        val byName = usable.associateBy { it.name.lowercase() }

        var bestLen = 0
        var bestCategory: Category? = null
        for ((categoryName, words) in RULES) {
            val category = byName[categoryName.lowercase()] ?: continue
            for (word in words) {
                if (word.length > bestLen && padded.contains(" $word ")) {
                    bestLen = word.length
                    bestCategory = category
                }
            }
        }
        return bestCategory?.id
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private val STOP_WORDS = setOf("for", "the", "and", "at", "to", "on", "in", "of", "a", "an", "my", "rs", "inr")

    internal fun normalize(raw: String): String =
        raw.lowercase()
            .replace('&', ' ')
            .replace(Regex("[^a-z0-9\\u0900-\\u097F ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun tokens(normalized: String): Set<String> =
        normalized.split(' ')
            .filter { it.length >= 2 && it !in STOP_WORDS && !it.all { c -> c.isDigit() } }
            .toSet()

    /** Category name (must match a seeded category name) -> trigger words/phrases, lowercase. */
    private val RULES: Map<String, List<String>> = mapOf(
        // ---- Food & Dining ----
        "Cafes & Coffee" to listOf("chai", "tea", "coffee", "cafe", "starbucks", "chaayos", "cappuccino", "latte", "cutting chai", "tapri", "cafe coffee day", "ccd", "chaiwala"),
        "Food Delivery" to listOf("zomato", "swiggy", "eatsure", "food delivery", "magicpin food", "dunzo food"),
        "Fast Food" to listOf("pizza", "burger", "dominos", "kfc", "mcdonalds", "mcdonald", "subway sandwich", "momos", "fries", "sandwich", "pizza hut", "burger king", "wrap", "shawarma"),
        "Bakery & Snacks" to listOf("samosa", "vada pav", "vadapav", "pav bhaji", "snacks", "snack", "biscuit", "biscuits", "bakery", "cake", "pastry", "chips", "namkeen", "chaat", "pani puri", "panipuri", "bhel", "kachori", "jalebi", "sweets", "mithai", "ice cream", "icecream", "dosa", "idli", "poha", "maggi"),
        "Restaurants" to listOf("restaurant", "dhaba", "thali", "biryani", "lunch", "dinner", "breakfast", "meal", "mess", "tiffin", "canteen", "food", "paneer", "dining", "buffet", "eating out"),
        "Drinks & Nightlife" to listOf("beer", "wine", "whisky", "whiskey", "vodka", "rum", "bar", "pub", "club", "liquor", "cocktail", "drinks", "party drinks"),

        // ---- Transport ----
        "Fuel & Petrol" to listOf("petrol", "diesel", "fuel", "cng", "pump", "petrol pump", "bike petrol", "gas station", "hp petrol", "indian oil"),
        "Metro & Subway" to listOf("metro", "metro card", "local train pass", "mumbai local"),
        "Taxi & Cab" to listOf("uber", "ola", "rapido", "cab", "taxi", "auto", "rickshaw", "autorickshaw", "bike taxi", "meru", "indrive", "auto fare"),
        "Parking & Tolls" to listOf("parking", "toll", "fastag", "fast tag", "challan"),
        "Vehicle Service" to listOf("puncture", "garage", "bike service", "car service", "servicing", "car wash", "tyre", "tire", "mechanic", "engine oil"),

        // ---- Shopping ----
        "Clothing & Apparel" to listOf("shirt", "tshirt", "t shirt", "jeans", "clothes", "clothing", "kurta", "saree", "dress", "myntra", "trousers", "jacket", "lehenga", "tailor", "stitching", "pants"),
        "Electronics & Gadgets" to listOf("mobile", "phone", "laptop", "charger", "earphones", "earbuds", "headphones", "cable", "power bank", "powerbank", "keyboard", "mouse", "smartwatch", "tablet", "tv", "gadget"),
        "Footwear" to listOf("shoes", "shoe", "chappal", "sandals", "slippers", "sneakers", "footwear", "crocs"),
        "Online Shopping" to listOf("amazon", "flipkart", "meesho", "ajio", "nykaa", "snapdeal", "online order", "tata cliq"),
        "Jewelry & Accessories" to listOf("jewellery", "jewelry", "gold", "silver", "ring", "necklace", "watch", "earrings", "bracelet", "sunglasses", "bag", "wallet"),

        // ---- Bills ----
        "Electricity Bill" to listOf("electricity", "light bill", "bijli", "mseb", "msedcl", "tata power", "adani electricity", "current bill", "electric bill", "bses"),
        "Water Bill" to listOf("water bill", "paani bill", "water tax", "water tanker"),
        "Internet & WiFi" to listOf("wifi", "broadband", "internet", "jio fiber", "jiofiber", "airtel xstream", "act fibernet", "router"),
        "Mobile Recharge" to listOf("recharge", "jio", "airtel", "vi", "vodafone", "bsnl", "prepaid", "postpaid", "mobile bill", "data pack", "sim"),
        "Gas & Cylinder" to listOf("lpg", "gas cylinder", "cylinder", "gas bill", "mahanagar gas", "mgl", "indane", "bharat gas", "hp gas", "png"),
        "DTH & Cable" to listOf("dth", "tata play", "tata sky", "dish tv", "d2h", "cable", "cable tv", "videocon d2h", "sun direct"),

        // ---- Entertainment ----
        "Movies & Cinema" to listOf("movie", "movies", "cinema", "pvr", "inox", "bookmyshow", "film", "theatre", "theater", "multiplex"),
        "Music & Podcasts" to listOf("spotify", "gaana", "jiosaavn", "wynk", "apple music", "music"),
        "Streaming (OTT)" to listOf("netflix", "prime video", "amazon prime", "hotstar", "disney hotstar", "jiocinema", "sonyliv", "zee5", "youtube premium", "ott", "subscription"),
        "Video Games" to listOf("game", "games", "steam", "playstation", "ps5", "xbox", "pubg", "bgmi", "free fire", "gaming", "google play"),
        "Concerts & Events" to listOf("concert", "event", "festival", "show ticket", "comedy show", "standup", "gig"),
        "Hobbies & Art" to listOf("hobby", "painting", "art supplies", "guitar", "canvas", "craft"),

        // ---- Health ----
        "Doctor & Clinic" to listOf("doctor", "clinic", "hospital", "consultation", "dentist", "opd", "physiotherapy", "specialist", "dr"),
        "Pharmacy & Meds" to listOf("medicine", "medicines", "pharmacy", "medical", "tablets", "tablet", "apollo pharmacy", "medplus", "netmeds", "pharmeasy", "1mg", "syrup", "chemist"),
        "Gym & Workout" to listOf("gym", "cult", "cultfit", "yoga", "workout", "protein", "whey", "fitness", "zumba", "trainer"),
        "Health Checkups" to listOf("checkup", "check up", "blood test", "lab test", "pathology", "diagnostic", "scan", "x ray", "xray", "mri", "thyrocare", "lal path", "health check"),

        // ---- Education ----
        "Tuition & Fees" to listOf("tuition", "school fees", "college fees", "fees", "coaching", "exam fees", "admission", "class fees", "semester fees"),
        "Books & Stationery" to listOf("book", "books", "notebook", "stationery", "pen", "pens", "pencil", "xerox", "photocopy", "printout", "print out"),
        "Online Courses" to listOf("udemy", "coursera", "course", "online course", "unacademy", "byjus", "skillshare", "upgrad", "linkedin learning"),

        // ---- Travel ----
        "Flights" to listOf("flight", "flights", "indigo", "air india", "airindia", "spicejet", "vistara", "akasa", "airfare", "plane ticket"),
        "Hotels & Stays" to listOf("hotel", "oyo", "airbnb", "resort", "homestay", "hostel", "lodge", "stay", "makemytrip hotel"),
        "Trains & Buses" to listOf("train", "irctc", "redbus", "bus", "bus ticket", "train ticket", "tatkal", "msrtc", "state transport", "volvo"),
        "Sightseeing & Tours" to listOf("sightseeing", "tour", "trek", "safari", "entry ticket", "museum", "monument", "trip guide"),

        // ---- Housing ----
        "House Rent" to listOf("rent", "house rent", "room rent", "flat rent", "pg rent", "pg", "landlord"),
        "Maintenance & Society" to listOf("maintenance", "society", "society charges", "association fee", "apartment maintenance"),
        "Repairs & Tools" to listOf("plumber", "electrician", "carpenter", "repair", "repairs", "painter", "hardware", "tools", "ac repair", "fridge repair", "urban company"),
        "Furniture & Decor" to listOf("furniture", "sofa", "mattress", "bed", "table", "chair", "curtains", "decor", "wardrobe", "ikea", "pepperfry"),

        // ---- Groceries ----
        "Fruits & Veggies" to listOf("sabzi", "sabji", "vegetables", "vegetable", "veggies", "fruits", "fruit", "bhaji", "bhajipala", "onion", "tomato", "potato", "banana", "apple", "mango"),
        "Dairy & Bread" to listOf("milk", "dudh", "bread", "paneer", "curd", "dahi", "butter", "ghee", "cheese", "eggs", "egg", "amul", "mother dairy", "doodh"),
        "Household Cleaners" to listOf("detergent", "surf", "harpic", "cleaner", "phenyl", "dishwash", "vim", "soap", "broom", "mop", "tissue"),
        "Groceries" to listOf("grocery", "groceries", "kirana", "bigbasket", "big basket", "blinkit", "zepto", "instamart", "dmart", "d mart", "jiomart", "ration", "atta", "rice", "dal", "oil", "sugar", "masala", "provisions", "supermarket", "reliance fresh", "more supermarket"),

        // ---- Personal care ----
        "Salon & Haircut" to listOf("haircut", "salon", "barber", "hair cut", "shaving", "beard trim", "hair color", "parlour", "parlor", "trim"),
        "Skincare & Spa" to listOf("spa", "facial", "skincare", "massage", "cleanup", "waxing", "manicure", "pedicure", "face wash", "moisturiser", "sunscreen"),
        "Grooming Products" to listOf("shampoo", "perfume", "razor", "deodorant", "deo", "toothpaste", "trimmer", "cologne", "body wash", "hair oil", "grooming"),

        // ---- Parent-level categories without a matching subcategory ----
        "Pets" to listOf("pet", "dog", "cat food", "puppy", "kitten", "vet", "pet food", "dog food", "pedigree", "whiskas"),
        "Gifts & Donations" to listOf("gift", "gifts", "donation", "donate", "temple", "charity", "dakshina", "prasad", "birthday gift", "wedding gift", "shagun", "mandir", "dargah", "church"),
        "Family & Kids" to listOf("kids", "kid", "toys", "toy", "diaper", "baby", "school bus", "school uniform", "daycare", "creche", "pocket money"),
        "Subscriptions" to listOf("membership", "annual plan", "monthly plan", "renewal", "icloud", "google one", "chatgpt", "claude", "notion"),
        "Investments & Savings" to listOf("sip", "mutual fund", "stocks", "zerodha", "groww", "fd", "fixed deposit", "ppf", "nps", "rd", "investment", "invested", "demat", "upstox", "gold bond"),
        "Fees & Charges" to listOf("bank charges", "penalty", "late fee", "atm fee", "processing fee", "service charge", "annual fee", "gst", "fine", "convenience fee", "overdue", "interest charged"),
        "Travel & Trips" to listOf("trip", "vacation", "holiday", "travel", "outstation"),

        // ---- Income ----
        "Monthly Salary" to listOf("salary", "monthly salary", "payroll", "pay slip", "payslip", "stipend"),
        "Performance Bonus" to listOf("bonus", "incentive", "appraisal", "variable pay"),
        "Overtime Compensation" to listOf("overtime", "ot pay"),
        "Client Projects" to listOf("freelance", "client", "project payment", "invoice", "gig payment", "contract work"),
        "Consulting Fee" to listOf("consulting", "consultation fee received", "advisory"),
        "Content & Royalties" to listOf("royalty", "royalties", "adsense", "youtube income", "ad revenue", "sponsorship"),
        "Business Revenue" to listOf("sales", "business income", "shop income", "customer payment", "revenue"),
        "Stock Dividends" to listOf("dividend", "dividends"),
        "Bank Interest" to listOf("interest", "bank interest", "fd interest", "savings interest", "interest credited"),
        "Mutual Fund Gains" to listOf("mutual fund gain", "redemption", "mf redemption", "capital gain", "profit booking"),
        "Rental Income" to listOf("rent received", "rental income", "tenant", "tenant rent"),
        "Refunds & Cashback" to listOf("refund", "cashback", "cash back", "reversal", "money back", "reward points"),
        "Gifts & Rewards" to listOf("gift received", "reward", "prize", "gift money", "shagun received", "birthday money")
    )
}
