package com.example.paisa.model

import com.example.paisa.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryIconRegistryTest {

    @Test
    fun all22Categories_mappedToExactExpectedVectorIcons() {
        val expectedMappings = mapOf(
            "Food" to R.drawable.ic_cat_restaurant,
            "Groceries" to R.drawable.ic_cat_shopping_cart,
            "Coffee" to R.drawable.ic_cat_local_cafe,
            "Shopping" to R.drawable.ic_cat_shopping_bag,
            "Clothes" to R.drawable.ic_cat_checkroom,
            "Movies" to R.drawable.ic_cat_movie,
            "Entertainment" to R.drawable.ic_cat_theater_comedy,
            "Transport" to R.drawable.ic_cat_directions_car,
            "Fuel" to R.drawable.ic_cat_local_gas_station,
            "Rent" to R.drawable.ic_cat_home,
            "Bills" to R.drawable.ic_cat_receipt_long,
            "Utilities" to R.drawable.ic_cat_bolt,
            "Health" to R.drawable.ic_cat_medical_services,
            "Education" to R.drawable.ic_cat_school,
            "Salary" to R.drawable.ic_cat_work,
            "Income" to R.drawable.ic_cat_payments,
            "Transfer" to R.drawable.ic_cat_sync_alt,
            "Savings" to R.drawable.ic_cat_savings,
            "Recharge" to R.drawable.ic_cat_smartphone,
            "Subscriptions" to R.drawable.ic_cat_subscriptions,
            "Travel" to R.drawable.ic_cat_flight,
            "Other" to R.drawable.ic_cat_category
        )

        assertEquals(22, expectedMappings.size)

        for ((categoryName, expectedIconRes) in expectedMappings) {
            val visual = CategoryIconRegistry.getVisualForCategory(categoryName)
            assertEquals("Mismatch for category: $categoryName", expectedIconRes, visual.iconRes)
            assertNotEquals("Icon resource ID must be non-zero for $categoryName", 0, visual.iconRes)
        }
    }

    @Test
    fun categoryAliases_resolveToCorrectCategories() {
        assertEquals(R.drawable.ic_cat_restaurant, CategoryIconRegistry.getIcon("Dining"))
        assertEquals(R.drawable.ic_cat_restaurant, CategoryIconRegistry.getIcon("dinner"))
        assertEquals(R.drawable.ic_cat_restaurant, CategoryIconRegistry.getIcon("restaurant"))
        assertEquals(R.drawable.ic_cat_shopping_cart, CategoryIconRegistry.getIcon("Kirana"))
        assertEquals(R.drawable.ic_cat_shopping_cart, CategoryIconRegistry.getIcon("Supermarket"))
        assertEquals(R.drawable.ic_cat_local_cafe, CategoryIconRegistry.getIcon("Cafe"))
        assertEquals(R.drawable.ic_cat_local_cafe, CategoryIconRegistry.getIcon("Tea"))
        assertEquals(R.drawable.ic_cat_checkroom, CategoryIconRegistry.getIcon("Clothing"))
        assertEquals(R.drawable.ic_cat_checkroom, CategoryIconRegistry.getIcon("Apparel"))
        assertEquals(R.drawable.ic_cat_movie, CategoryIconRegistry.getIcon("Cinema"))
        assertEquals(R.drawable.ic_cat_movie, CategoryIconRegistry.getIcon("BookMyShow"))
        assertEquals(R.drawable.ic_cat_directions_car, CategoryIconRegistry.getIcon("Cab"))
        assertEquals(R.drawable.ic_cat_directions_car, CategoryIconRegistry.getIcon("Uber"))
        assertEquals(R.drawable.ic_cat_directions_car, CategoryIconRegistry.getIcon("Metro"))
        assertEquals(R.drawable.ic_cat_local_gas_station, CategoryIconRegistry.getIcon("Petrol"))
        assertEquals(R.drawable.ic_cat_local_gas_station, CategoryIconRegistry.getIcon("Diesel"))
        assertEquals(R.drawable.ic_cat_home, CategoryIconRegistry.getIcon("House Rent"))
        assertEquals(R.drawable.ic_cat_receipt_long, CategoryIconRegistry.getIcon("BBPS"))
        assertEquals(R.drawable.ic_cat_bolt, CategoryIconRegistry.getIcon("Electricity"))
        assertEquals(R.drawable.ic_cat_medical_services, CategoryIconRegistry.getIcon("Pharmacy"))
        assertEquals(R.drawable.ic_cat_medical_services, CategoryIconRegistry.getIcon("Fitness"))
        assertEquals(R.drawable.ic_cat_medical_services, CategoryIconRegistry.getIcon("Gym"))
        assertEquals(R.drawable.ic_cat_school, CategoryIconRegistry.getIcon("College"))
        assertEquals(R.drawable.ic_cat_work, CategoryIconRegistry.getIcon("Payroll"))
        assertEquals(R.drawable.ic_cat_payments, CategoryIconRegistry.getIcon("Freelance"))
        assertEquals(R.drawable.ic_cat_payments, CategoryIconRegistry.getIcon("Refund"))
        assertEquals(R.drawable.ic_cat_savings, CategoryIconRegistry.getIcon("Investments"))
        assertEquals(R.drawable.ic_cat_savings, CategoryIconRegistry.getIcon("Mutual Fund"))
        assertEquals(R.drawable.ic_cat_savings, CategoryIconRegistry.getIcon("Stocks"))
        assertEquals(R.drawable.ic_cat_smartphone, CategoryIconRegistry.getIcon("Mobile Recharge"))
        assertEquals(R.drawable.ic_cat_subscriptions, CategoryIconRegistry.getIcon("Netflix"))
        assertEquals(R.drawable.ic_cat_subscriptions, CategoryIconRegistry.getIcon("Spotify"))
        assertEquals(R.drawable.ic_cat_flight, CategoryIconRegistry.getIcon("Flight"))
        assertEquals(R.drawable.ic_cat_flight, CategoryIconRegistry.getIcon("Hotel"))
    }

    @Test
    fun legacyEmojiFallbacks_resolveCorrectly() {
        assertEquals(CategoryIconRegistry.ID_FOOD, CategoryIconRegistry.getStableId(null, "🍽️"))
        assertEquals(CategoryIconRegistry.ID_GROCERIES, CategoryIconRegistry.getStableId(null, "🛒"))
        assertEquals(CategoryIconRegistry.ID_COFFEE, CategoryIconRegistry.getStableId(null, "☕"))
        assertEquals(CategoryIconRegistry.ID_SHOPPING, CategoryIconRegistry.getStableId(null, "🛍️"))
        assertEquals(CategoryIconRegistry.ID_TRANSPORT, CategoryIconRegistry.getStableId(null, "🚕"))
        assertEquals(CategoryIconRegistry.ID_MOVIES, CategoryIconRegistry.getStableId(null, "🎬"))
        assertEquals(CategoryIconRegistry.ID_ENTERTAINMENT, CategoryIconRegistry.getStableId(null, "🍿"))
        assertEquals(CategoryIconRegistry.ID_HEALTH, CategoryIconRegistry.getStableId(null, "🏋️"))
        assertEquals(CategoryIconRegistry.ID_SALARY, CategoryIconRegistry.getStableId(null, "💼"))
        assertEquals(CategoryIconRegistry.ID_INCOME, CategoryIconRegistry.getStableId(null, "💻"))
        assertEquals(CategoryIconRegistry.ID_TRANSFER, CategoryIconRegistry.getStableId(null, "🔄"))
        assertEquals(CategoryIconRegistry.ID_SAVINGS, CategoryIconRegistry.getStableId(null, "📈"))
        assertEquals(CategoryIconRegistry.ID_UTILITIES, CategoryIconRegistry.getStableId(null, "⚡"))
        assertEquals(CategoryIconRegistry.ID_TRAVEL, CategoryIconRegistry.getStableId(null, "✈️"))
    }

    @Test
    fun unknownAndCustomCategories_fallbackToGenericCategoryIcon() {
        assertEquals(R.drawable.ic_cat_category, CategoryIconRegistry.getIcon("Custom Unique Category XYZ"))
        assertEquals(R.drawable.ic_cat_category, CategoryIconRegistry.getIcon(""))
        assertEquals(R.drawable.ic_cat_category, CategoryIconRegistry.getIcon(null))
        assertEquals(CategoryIconRegistry.ID_OTHER, CategoryIconRegistry.getStableId("Unknown Category 123"))
    }

    @Test
    fun getIconForTransaction_resolvesTransactionTypeFallbacks() {
        // When category is provided
        assertEquals(
            R.drawable.ic_cat_restaurant,
            CategoryIconRegistry.getIconForTransaction("Food", isCredit = false)
        )
        assertEquals(
            R.drawable.ic_cat_shopping_cart,
            CategoryIconRegistry.getIconForTransaction("Groceries", isCredit = false)
        )

        // When category is null or blank
        assertEquals(
            R.drawable.ic_cat_payments,
            CategoryIconRegistry.getIconForTransaction(null, isCredit = true)
        )
        assertEquals(
            R.drawable.ic_cat_payments,
            CategoryIconRegistry.getIconForTransaction("", isCredit = true)
        )
        assertEquals(
            R.drawable.ic_cat_category,
            CategoryIconRegistry.getIconForTransaction(null, isCredit = false)
        )
        assertEquals(
            R.drawable.ic_cat_category,
            CategoryIconRegistry.getIconForTransaction("  ", isCredit = false)
        )
    }

    @Test
    fun allCategoryVisuals_haveDistinctColorsAndValidContainerThemeColorMethods() {
        val allVisuals = CategoryIconRegistry.getAllCategoryVisuals()
        assertEquals(22, allVisuals.size)

        for (visual in allVisuals) {
            assertNotNull(visual.displayName)
            assertTrue(visual.displayName.isNotBlank())
            assertNotEquals(0, visual.iconRes)

            // Light mode colors
            val lightBg = visual.containerColor(isDark = false)
            val lightFg = visual.iconColor(isDark = false)
            assertEquals(visual.lightContainer, lightBg)
            assertEquals(visual.lightIcon, lightFg)
            assertNotEquals("Container and icon colors must differ for contrast in light mode", lightBg, lightFg)

            // Dark mode colors
            val darkBg = visual.containerColor(isDark = true)
            val darkFg = visual.iconColor(isDark = true)
            assertEquals(visual.darkContainer, darkBg)
            assertEquals(visual.darkIcon, darkFg)
            assertNotEquals("Container and icon colors must differ for contrast in dark mode", darkBg, darkFg)
        }
    }
}
