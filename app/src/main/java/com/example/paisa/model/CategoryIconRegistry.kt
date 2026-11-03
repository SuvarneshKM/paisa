package com.example.paisa.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.paisa.R

/**
 * Visual representation of a category with local VectorDrawable and accessible theme colors.
 */
data class CategoryVisual(
    val stableId: String,
    val displayName: String,
    @get:DrawableRes val iconRes: Int,
    val lightContainer: Color,
    val lightIcon: Color,
    val darkContainer: Color,
    val darkIcon: Color
) {
    fun containerColor(isDark: Boolean): Color = if (isDark) darkContainer else lightContainer
    fun iconColor(isDark: Boolean): Color = if (isDark) darkIcon else lightIcon
}

/**
 * Centralized Category Icon Registry.
 *
 * Maps stable category identities, display names, and legacy emoji fallbacks
 * to locally bundled Android Material Symbols Rounded vector drawables.
 *
 * Operates strictly offline with zero network dependency.
 */
object CategoryIconRegistry {

    const val ID_FOOD = "FOOD"
    const val ID_GROCERIES = "GROCERIES"
    const val ID_COFFEE = "COFFEE"
    const val ID_SHOPPING = "SHOPPING"
    const val ID_CLOTHES = "CLOTHES"
    const val ID_MOVIES = "MOVIES"
    const val ID_ENTERTAINMENT = "ENTERTAINMENT"
    const val ID_TRANSPORT = "TRANSPORT"
    const val ID_FUEL = "FUEL"
    const val ID_RENT = "RENT"
    const val ID_BILLS = "BILLS"
    const val ID_UTILITIES = "UTILITIES"
    const val ID_HEALTH = "HEALTH"
    const val ID_EDUCATION = "EDUCATION"
    const val ID_SALARY = "SALARY"
    const val ID_INCOME = "INCOME"
    const val ID_TRANSFER = "TRANSFER"
    const val ID_SAVINGS = "SAVINGS"
    const val ID_RECHARGE = "RECHARGE"
    const val ID_SUBSCRIPTIONS = "SUBSCRIPTIONS"
    const val ID_TRAVEL = "TRAVEL"
    const val ID_OTHER = "OTHER"

    // Fallback vector for unknown / generic categories
    @DrawableRes
    val DEFAULT_FALLBACK_ICON = R.drawable.ic_cat_category

    private val VISUALS: Map<String, CategoryVisual> = mapOf(
        ID_FOOD to CategoryVisual(
            stableId = ID_FOOD,
            displayName = "Food",
            iconRes = R.drawable.ic_cat_restaurant,
            lightContainer = Color(0xFFFDEEE7),
            lightIcon = Color(0xFFC04E20),
            darkContainer = Color(0xFF332018),
            darkIcon = Color(0xFFFF9566)
        ),
        ID_GROCERIES to CategoryVisual(
            stableId = ID_GROCERIES,
            displayName = "Groceries",
            iconRes = R.drawable.ic_cat_shopping_cart,
            lightContainer = Color(0xFFEBF6EC),
            lightIcon = Color(0xFF2E7D32),
            darkContainer = Color(0xFF1B2E1D),
            darkIcon = Color(0xFF81C784)
        ),
        ID_COFFEE to CategoryVisual(
            stableId = ID_COFFEE,
            displayName = "Coffee",
            iconRes = R.drawable.ic_cat_local_cafe,
            lightContainer = Color(0xFFF7EFE9),
            lightIcon = Color(0xFF795548),
            darkContainer = Color(0xFF2E241F),
            darkIcon = Color(0xFFBCAAA4)
        ),
        ID_SHOPPING to CategoryVisual(
            stableId = ID_SHOPPING,
            displayName = "Shopping",
            iconRes = R.drawable.ic_cat_shopping_bag,
            lightContainer = Color(0xFFF3EBF7),
            lightIcon = Color(0xFF7B1FA2),
            darkContainer = Color(0xFF2D1B36),
            darkIcon = Color(0xFFCE93D8)
        ),
        ID_CLOTHES to CategoryVisual(
            stableId = ID_CLOTHES,
            displayName = "Clothes",
            iconRes = R.drawable.ic_cat_checkroom,
            lightContainer = Color(0xFFEDE7F6),
            lightIcon = Color(0xFF512DA8),
            darkContainer = Color(0xFF241C38),
            darkIcon = Color(0xFFB39DDB)
        ),
        ID_MOVIES to CategoryVisual(
            stableId = ID_MOVIES,
            displayName = "Movies",
            iconRes = R.drawable.ic_cat_movie,
            lightContainer = Color(0xFFFBEBF0),
            lightIcon = Color(0xFFC2185B),
            darkContainer = Color(0xFF331622),
            darkIcon = Color(0xFFF48FB1)
        ),
        ID_ENTERTAINMENT to CategoryVisual(
            stableId = ID_ENTERTAINMENT,
            displayName = "Entertainment",
            iconRes = R.drawable.ic_cat_theater_comedy,
            lightContainer = Color(0xFFFCEEFA),
            lightIcon = Color(0xFF8E24AA),
            darkContainer = Color(0xFF311A35),
            darkIcon = Color(0xFFE1BEE7)
        ),
        ID_TRANSPORT to CategoryVisual(
            stableId = ID_TRANSPORT,
            displayName = "Transport",
            iconRes = R.drawable.ic_cat_directions_car,
            lightContainer = Color(0xFFE3F2FD),
            lightIcon = Color(0xFF1565C0),
            darkContainer = Color(0xFF162536),
            darkIcon = Color(0xFF90CAF9)
        ),
        ID_FUEL to CategoryVisual(
            stableId = ID_FUEL,
            displayName = "Fuel",
            iconRes = R.drawable.ic_cat_local_gas_station,
            lightContainer = Color(0xFFFFF3E0),
            lightIcon = Color(0xFFE65100),
            darkContainer = Color(0xFF362010),
            darkIcon = Color(0xFFFFB74D)
        ),
        ID_RENT to CategoryVisual(
            stableId = ID_RENT,
            displayName = "Rent",
            iconRes = R.drawable.ic_cat_home,
            lightContainer = Color(0xFFE0F2F1),
            lightIcon = Color(0xFF00695C),
            darkContainer = Color(0xFF132B27),
            darkIcon = Color(0xFF80CBC4)
        ),
        ID_BILLS to CategoryVisual(
            stableId = ID_BILLS,
            displayName = "Bills",
            iconRes = R.drawable.ic_cat_receipt_long,
            lightContainer = Color(0xFFECEFF1),
            lightIcon = Color(0xFF455A64),
            darkContainer = Color(0xFF21272B),
            darkIcon = Color(0xFFB0BEC5)
        ),
        ID_UTILITIES to CategoryVisual(
            stableId = ID_UTILITIES,
            displayName = "Utilities",
            iconRes = R.drawable.ic_cat_bolt,
            lightContainer = Color(0xFFFFFDE7),
            lightIcon = Color(0xFFF57F17),
            darkContainer = Color(0xFF332F10),
            darkIcon = Color(0xFFFFF59D)
        ),
        ID_HEALTH to CategoryVisual(
            stableId = ID_HEALTH,
            displayName = "Health",
            iconRes = R.drawable.ic_cat_medical_services,
            lightContainer = Color(0xFFFFEBEE),
            lightIcon = Color(0xFFC62828),
            darkContainer = Color(0xFF361819),
            darkIcon = Color(0xFFEF9A9A)
        ),
        ID_EDUCATION to CategoryVisual(
            stableId = ID_EDUCATION,
            displayName = "Education",
            iconRes = R.drawable.ic_cat_school,
            lightContainer = Color(0xFFE8EAF6),
            lightIcon = Color(0xFF283593),
            darkContainer = Color(0xFF1A1D36),
            darkIcon = Color(0xFF9FA8DA)
        ),
        ID_SALARY to CategoryVisual(
            stableId = ID_SALARY,
            displayName = "Salary",
            iconRes = R.drawable.ic_cat_work,
            lightContainer = Color(0xFFE8F5E9),
            lightIcon = Color(0xFF1B5E20),
            darkContainer = Color(0xFF152E18),
            darkIcon = Color(0xFFA5D6A7)
        ),
        ID_INCOME to CategoryVisual(
            stableId = ID_INCOME,
            displayName = "Income",
            iconRes = R.drawable.ic_cat_payments,
            lightContainer = Color(0xFFE0F7FA),
            lightIcon = Color(0xFF00838F),
            darkContainer = Color(0xFF112A2D),
            darkIcon = Color(0xFF80DEEA)
        ),
        ID_TRANSFER to CategoryVisual(
            stableId = ID_TRANSFER,
            displayName = "Transfer",
            iconRes = R.drawable.ic_cat_sync_alt,
            lightContainer = Color(0xFFEDE7F6),
            lightIcon = Color(0xFF4527A0),
            darkContainer = Color(0xFF221838),
            darkIcon = Color(0xFFB39DDB)
        ),
        ID_SAVINGS to CategoryVisual(
            stableId = ID_SAVINGS,
            displayName = "Savings",
            iconRes = R.drawable.ic_cat_savings,
            lightContainer = Color(0xFFF1F8E9),
            lightIcon = Color(0xFF33691E),
            darkContainer = Color(0xFF202E15),
            darkIcon = Color(0xFFC5E1A5)
        ),
        ID_RECHARGE to CategoryVisual(
            stableId = ID_RECHARGE,
            displayName = "Recharge",
            iconRes = R.drawable.ic_cat_smartphone,
            lightContainer = Color(0xFFE1F5FE),
            lightIcon = Color(0xFF0277BD),
            darkContainer = Color(0xFF122836),
            darkIcon = Color(0xFF81D4FA)
        ),
        ID_SUBSCRIPTIONS to CategoryVisual(
            stableId = ID_SUBSCRIPTIONS,
            displayName = "Subscriptions",
            iconRes = R.drawable.ic_cat_subscriptions,
            lightContainer = Color(0xFFFFEBEE),
            lightIcon = Color(0xFFAD1457),
            darkContainer = Color(0xFF331622),
            darkIcon = Color(0xFFF48FB1)
        ),
        ID_TRAVEL to CategoryVisual(
            stableId = ID_TRAVEL,
            displayName = "Travel",
            iconRes = R.drawable.ic_cat_flight,
            lightContainer = Color(0xFFE0F2F1),
            lightIcon = Color(0xFF00796B),
            darkContainer = Color(0xFF122B27),
            darkIcon = Color(0xFF4DB6AC)
        ),
        ID_OTHER to CategoryVisual(
            stableId = ID_OTHER,
            displayName = "Other",
            iconRes = R.drawable.ic_cat_category,
            lightContainer = Color(0xFFF5F5F7),
            lightIcon = Color(0xFF616161),
            darkContainer = Color(0xFF212124),
            darkIcon = Color(0xFF9E9E9E)
        )
    )

    /**
     * Resolves a category name or legacy emoji to a stable category ID.
     */
    fun getStableId(name: String?, emojiFallback: String? = null): String {
        val cleanName = name?.trim()?.lowercase().orEmpty()
        val cleanEmoji = emojiFallback?.trim().orEmpty()

        return when {
            // Food
            cleanName in listOf("food", "dining", "restaurant", "food & dining", "meal", "dinner", "lunch", "snacks", "snack", "fast food", "street food", "chaat") ||
                    cleanEmoji in listOf("🍽️", "🍽", "🍕", "🍔") -> ID_FOOD

            // Groceries
            cleanName in listOf("groceries", "grocery", "supermarket", "kirana", "mart") ||
                    cleanEmoji in listOf("🛒", "🧺") -> ID_GROCERIES

            // Coffee
            cleanName in listOf("coffee", "cafe", "tea", "chai", "beverage", "drinks") ||
                    cleanEmoji in listOf("☕", "🍵", "🧋") -> ID_COFFEE

            // Shopping
            cleanName in listOf("shopping", "ecommerce", "amazon", "flipkart") ||
                    cleanEmoji in listOf("🛍️", "🛍") -> ID_SHOPPING

            // Clothes
            cleanName in listOf("clothes", "clothing", "apparel", "fashion", "myntra", "zara") ||
                    cleanEmoji in listOf("👕", "👗", "👔") -> ID_CLOTHES

            // Movies
            cleanName in listOf("movies", "movie", "cinema", "theatre", "bookmyshow", "imax") ||
                    cleanEmoji in listOf("🎬", "🎞️") -> ID_MOVIES

            // Entertainment
            cleanName in listOf("entertainment", "events", "fun", "games", "gaming") ||
                    cleanEmoji in listOf("🍿", "🎭", "🎮") -> ID_ENTERTAINMENT

            // Transport
            cleanName in listOf("transport", "travel & transport", "cab", "taxi", "uber", "ola", "auto", "metro", "bus") ||
                    cleanEmoji in listOf("🚕", "🚗", "🚙", "🚇") -> ID_TRANSPORT

            // Fuel
            cleanName in listOf("fuel", "petrol", "diesel", "cng", "gas") ||
                    cleanEmoji in listOf("⛽") -> ID_FUEL

            // Rent
            cleanName in listOf("rent", "house rent", "flat", "maintenance") ||
                    cleanEmoji in listOf("🏠", "🏡") -> ID_RENT

            // Bills
            cleanName in listOf("bills", "bill", "invoice", "receipt", "bbps") ||
                    cleanEmoji in listOf("🧾", "📄") -> ID_BILLS

            // Utilities
            cleanName in listOf("utilities", "electricity", "power", "water", "bescom", "piped gas") ||
                    cleanEmoji in listOf("⚡", "💡", "🔌") -> ID_UTILITIES

            // Health
            cleanName in listOf("health", "medical", "doctor", "pharmacy", "medicine", "hospital", "fitness", "gym", "cult.fit") ||
                    cleanEmoji in listOf("🏋️", "🏋", "💊", "🏥", "🩺") -> ID_HEALTH

            // Education
            cleanName in listOf("education", "school", "college", "tuition", "books", "course", "fees") ||
                    cleanEmoji in listOf("🎓", "📚", "🏫") -> ID_EDUCATION

            // Salary
            cleanName in listOf("salary", "payroll", "stipend", "wages") ||
                    cleanEmoji in listOf("💼") -> ID_SALARY

            // Income
            cleanName in listOf("income", "freelance", "consulting", "bonus", "interest", "refund") ||
                    cleanEmoji in listOf("💻", "↩️", "💰", "💵") -> ID_INCOME

            // Transfer
            cleanName in listOf("transfer", "inter-account", "self-transfer", "account transfer") ||
                    cleanEmoji in listOf("🔄", "↔️") -> ID_TRANSFER

            // Savings / Investments
            cleanName in listOf("savings", "investments", "investment", "mutual fund", "stocks", "sip", "fd", "gold") ||
                    cleanEmoji in listOf("📈", "🏦", "🪙") -> ID_SAVINGS

            // Recharge
            cleanName in listOf("recharge", "mobile recharge", "prepaid", "postpaid", "dth", "airtel", "jio") ||
                    cleanEmoji in listOf("📱", "📶") -> ID_RECHARGE

            // Subscriptions
            cleanName in listOf("subscriptions", "subscription", "netflix", "spotify", "prime", "youtube") ||
                    cleanEmoji in listOf("📺", "🎵") -> ID_SUBSCRIPTIONS

            // Travel
            cleanName in listOf("travel", "flight", "flights", "hotel", "holiday", "vacation", "trip", "indigo") ||
                    cleanEmoji in listOf("✈️", "✈", "🧳") -> ID_TRAVEL

            // Other fallback
            cleanName in listOf("other", "general", "misc", "miscellaneous") ||
                    cleanEmoji in listOf("🏷️", "🏷", "❓") -> ID_OTHER

            else -> ID_OTHER
        }
    }

    /**
     * Retrieves the complete visual definition for a category name (or emoji fallback).
     */
    fun getVisualForCategory(categoryName: String?, emojiFallback: String? = null): CategoryVisual {
        val stableId = getStableId(categoryName, emojiFallback)
        return VISUALS[stableId] ?: VISUALS.getValue(ID_OTHER)
    }

    /**
     * Returns the local VectorDrawable resource ID for a category.
     */
    @DrawableRes
    fun getIcon(categoryName: String?, emojiFallback: String? = null): Int {
        return getVisualForCategory(categoryName, emojiFallback).iconRes
    }

    /**
     * Resolves the appropriate vector icon for a transaction row, taking into account
     * its category name and credit/debit type as fallback.
     */
    @DrawableRes
    fun getIconForTransaction(categoryName: String?, isCredit: Boolean): Int {
        if (!categoryName.isNullOrBlank()) {
            return getIcon(categoryName)
        }
        return if (isCredit) {
            VISUALS.getValue(ID_INCOME).iconRes
        } else {
            VISUALS.getValue(ID_OTHER).iconRes
        }
    }

    /**
     * Returns all default category visuals for selection lists and pickers.
     */
    fun getAllCategoryVisuals(): List<CategoryVisual> {
        return VISUALS.values.toList()
    }
}
