package com.example.paisa.sms

import android.content.Context
import android.content.SharedPreferences

/**
 * Amount tiers for expense classification heuristics and range learning.
 */
enum class AmountTier(val label: String) {
    UNDER_100("< ₹100"),
    FROM_100_TO_499("₹100–₹499"),
    FROM_500_TO_1999("₹500–₹1,999"),
    FROM_2000_TO_4999("₹2,000–₹4,999"),
    FROM_5000_ABOVE("≥ ₹5,000");

    companion object {
        fun fromPaise(paise: Long): AmountTier {
            return when {
                paise < 10000L -> UNDER_100       // < ₹100.00
                paise < 50000L -> FROM_100_TO_499  // ₹100.00 to ₹499.99
                paise < 200000L -> FROM_500_TO_1999 // ₹500.00 to ₹1,999.99
                paise < 500000L -> FROM_2000_TO_4999 // ₹2,000.00 to ₹4,999.99
                else -> FROM_5000_ABOVE            // ≥ ₹5,000.00
            }
        }
    }
}

/**
 * Privacy-first offline storage for learned merchant categories and user-defined preferences.
 *
 * Guarantees:
 * - Offline only (zero network access, zero external APIs)
 * - Never stores or learns from OTPs, verification codes, bank account numbers, or raw SMS bodies
 * - Only learns from confirmed manual category selections
 */
class MerchantCategoryPreferences private constructor(
    private val prefs: SharedPreferences? = null
) {
    // In-memory caches for fast lookups and for isolated unit tests without Android Context
    private val inMemoryLearned = mutableMapOf<String, String>()
    private val inMemoryUserPrefs = mutableMapOf<String, String>()
    private val inMemoryRangeCounts = mutableMapOf<AmountTier, MutableMap<String, Int>>()

    fun isValidMerchantIdentifier(merchant: String?): Boolean {
        if (merchant.isNullOrBlank()) return false
        val trimmed = merchant.trim()
        if (trimmed.length < 2) return false

        val lower = trimmed.lowercase()

        // 1. Sensitive security keywords
        val sensitiveKeywords = listOf(
            "otp", "verification", "secret", "password", "pin",
            "cvv", "passcode", "auth code", "one time password"
        )
        if (sensitiveKeywords.any { lower.contains(it) }) {
            return false
        }

        // 2. Reject if the merchant text is purely numeric with 5 or more digits (likely OTP or account digits)
        if (trimmed.all { it.isDigit() } && trimmed.length >= 5) {
            return false
        }

        // 3. Reject full bank account strings (e.g. "A/c 1234567890", "account 987654321")
        if (lower.contains("a/c") || lower.contains("account") || lower.contains("acct")) {
            val digitCount = trimmed.count { it.isDigit() }
            if (digitCount >= 6) return false
        }

        // 4. Reject long consecutive digits anywhere (> 6 consecutive digits without @)
        if (!lower.contains("@") && Regex("\\b\\d{6,}\\b").containsMatchIn(trimmed)) {
            return false
        }

        return true
    }

    fun normalizeMerchantKey(merchant: String): String {
        return merchant.trim()
            .lowercase()
            .replace(Regex("^[^a-z0-9@]+|[^a-z0-9@]+$"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Layer 1: Retrieves previously learned category for this exact merchant or UPI payee.
     */
    fun getLearnedCategory(merchant: String?): String? {
        if (!isValidMerchantIdentifier(merchant)) return null
        val key = normalizeMerchantKey(merchant!!)
        inMemoryLearned[key]?.let { return it }
        return prefs?.getString(PREF_PREFIX_LEARNED + key, null)?.also {
            inMemoryLearned[key] = it
        }
    }

    /**
     * Saves a confirmed manual category selection for a merchant.
     */
    fun saveLearnedCategory(merchant: String?, categoryName: String) {
        if (!isValidMerchantIdentifier(merchant)) return
        val key = normalizeMerchantKey(merchant!!)
        inMemoryLearned[key] = categoryName
        prefs?.edit()?.putString(PREF_PREFIX_LEARNED + key, categoryName)?.apply()
    }

    /**
     * Layer 2: Retrieves user-defined category preference for this merchant.
     */
    fun getUserDefinedPreference(merchant: String?): String? {
        if (!isValidMerchantIdentifier(merchant)) return null
        val key = normalizeMerchantKey(merchant!!)
        inMemoryUserPrefs[key]?.let { return it }
        return prefs?.getString(PREF_PREFIX_USER_RULE + key, null)?.also {
            inMemoryUserPrefs[key] = it
        }
    }

    /**
     * Saves an explicit user-defined merchant category preference.
     */
    fun setUserDefinedPreference(merchant: String?, categoryName: String) {
        if (!isValidMerchantIdentifier(merchant)) return
        val key = normalizeMerchantKey(merchant!!)
        inMemoryUserPrefs[key] = categoryName
        prefs?.edit()?.putString(PREF_PREFIX_USER_RULE + key, categoryName)?.apply()
    }

    /**
     * Layer 4: Retrieves the most frequently confirmed category in this amount range.
     */
    fun getAmountRangeCategory(tier: AmountTier): String? {
        val tierCounts = inMemoryRangeCounts[tier]
        if (!tierCounts.isNullOrEmpty()) {
            return tierCounts.maxByOrNull { it.value }?.key
        }

        val stored = prefs?.getString(PREF_PREFIX_RANGE_TOP + tier.name, null)
        return stored
    }

    /**
     * Records a category selection into the amount range frequency table.
     */
    fun recordAmountRangeSelection(tier: AmountTier, categoryName: String) {
        val tierCounts = inMemoryRangeCounts.getOrPut(tier) { mutableMapOf() }
        val newCount = (tierCounts[categoryName] ?: 0) + 1
        tierCounts[categoryName] = newCount

        val topCategory = tierCounts.maxByOrNull { it.value }?.key ?: categoryName
        prefs?.edit()?.putString(PREF_PREFIX_RANGE_TOP + tier.name, topCategory)?.apply()
    }

    fun clearAll() {
        inMemoryLearned.clear()
        inMemoryUserPrefs.clear()
        inMemoryRangeCounts.clear()
        prefs?.edit()?.clear()?.apply()
    }

    companion object {
        private const val PREFS_NAME = "paisa_merchant_categories"
        private const val PREF_PREFIX_LEARNED = "learned_"
        private const val PREF_PREFIX_USER_RULE = "user_rule_"
        private const val PREF_PREFIX_RANGE_TOP = "range_top_"

        @Volatile
        private var INSTANCE: MerchantCategoryPreferences? = null

        fun getInstance(context: Context? = null): MerchantCategoryPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MerchantCategoryPreferences(
                    prefs = context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ).also { INSTANCE = it }
            }
        }

        /**
         * Creates an isolated in-memory instance for testing without Android dependencies.
         */
        fun createInMemory(): MerchantCategoryPreferences {
            return MerchantCategoryPreferences(null)
        }
    }
}
