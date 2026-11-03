package com.example.paisa.sms

import com.example.paisa.data.local.entity.CategoryEntity

object CategoryMatcher {

    private val KEYWORD_TO_CATEGORY = mapOf(
        // Dining & Cafes
        "swiggy" to "Dining",
        "zomato" to "Dining",
        "starbucks" to "Dining",
        "mcdonalds" to "Dining",
        "mcdonald's" to "Dining",
        "kfc" to "Dining",
        "burger king" to "Dining",
        "dominos" to "Dining",
        "pizza hut" to "Dining",
        "cafe" to "Dining",
        "restaurant" to "Dining",
        "chai point" to "Dining",
        "chaayos" to "Dining",
        "eatclub" to "Dining",

        // Groceries & Essentials
        "instamart" to "Groceries",
        "blinkit" to "Groceries",
        "zepto" to "Groceries",
        "bigbasket" to "Groceries",
        "bbnow" to "Groceries",
        "dmart" to "Groceries",
        "grofers" to "Groceries",
        "supermarket" to "Groceries",
        "nature's basket" to "Groceries",
        "reliance fresh" to "Groceries",
        "more retail" to "Groceries",

        // Transport & Fuel
        "uber" to "Transport",
        "ola" to "Transport",
        "rapido" to "Transport",
        "metro" to "Transport",
        "irctc" to "Transport",
        "fuel" to "Transport",
        "petrol" to "Transport",
        "hpcl" to "Transport",
        "bpcl" to "Transport",
        "iocl" to "Transport",
        "shell" to "Transport",
        "fastag" to "Transport",

        // Entertainment & Subscriptions
        "netflix" to "Entertainment",
        "spotify" to "Entertainment",
        "hotstar" to "Entertainment",
        "prime video" to "Entertainment",
        "bookmyshow" to "Entertainment",
        "pvr" to "Entertainment",
        "inox" to "Entertainment",
        "youtube" to "Entertainment",

        // Utilities & Telecom
        "bescom" to "Utilities",
        "airtel" to "Utilities",
        "jio" to "Utilities",
        "vodafone" to "Utilities",
        "vi bill" to "Utilities",
        "act fibernet" to "Utilities",
        "wifi" to "Utilities",
        "broadband" to "Utilities",
        "electricity" to "Utilities",
        "water bill" to "Utilities",
        "gas bill" to "Utilities",

        // Fitness
        "cult.fit" to "Fitness",
        "cultfit" to "Fitness",
        "gym" to "Fitness",
        "fitness" to "Fitness",

        // Travel
        "makemytrip" to "Travel",
        "goibibo" to "Travel",
        "cleartrip" to "Travel",
        "indigo" to "Travel",
        "air india" to "Travel",
        "spicejet" to "Travel",
        "hotel" to "Travel",
        "airbnb" to "Travel",

        // Investments
        "zerodha" to "Investments",
        "groww" to "Investments",
        "upstox" to "Investments",
        "kuvera" to "Investments",
        "mutual fund" to "Investments",
        "sip" to "Investments",

        // Salary / Income
        "salary" to "Salary",
        "payroll" to "Salary"
    )

    fun matchCategoryName(merchantOrDescription: String?): String? {
        if (merchantOrDescription.isNullOrBlank()) return null
        val text = merchantOrDescription.lowercase()

        if (text.contains("instamart") || text.contains("bbnow") || text.contains("supermarket") || text.contains("grocery")) {
            return "Groceries"
        }

        for ((keyword, categoryName) in KEYWORD_TO_CATEGORY) {
            if (text.contains(keyword)) {
                return if (categoryName.equals("Dining", ignoreCase = true)) "Food" else categoryName
            }
        }
        return null
    }

    fun matchCategory(merchantOrDescription: String?, categories: List<CategoryEntity>): Long? {
        if (merchantOrDescription.isNullOrBlank() || categories.isEmpty()) return null

        val text = merchantOrDescription.lowercase()

        // Prioritize grocery-specific sub-brands (e.g. Swiggy Instamart -> Groceries)
        if (text.contains("instamart") || text.contains("bbnow") || text.contains("supermarket") || text.contains("grocery")) {
            return findCategoryIdByName("Groceries", categories)
        }

        // Iterate through keyword mapping
        for ((keyword, categoryName) in KEYWORD_TO_CATEGORY) {
            if (text.contains(keyword)) {
                val matched = findCategoryIdByName(categoryName, categories)
                if (matched != null) return matched
            }
        }

        return null
    }

    private fun findCategoryIdByName(name: String, categories: List<CategoryEntity>): Long? {
        return categories.firstOrNull { it.name.equals(name, ignoreCase = true) }?.id
    }
}
