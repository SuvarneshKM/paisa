package com.example.paisa.sms

import android.content.Context
import com.example.paisa.data.local.entity.BankAccountEntity
import com.example.paisa.data.local.entity.TransactionType

/**
 * Deterministic offline Category Suggestion Engine.
 *
 * Implements a 5-layer ranking hierarchy:
 * 1. Previous category selections for the same merchant or UPI payee
 * 2. User-defined merchant-category preferences
 * 3. Recognized merchant keywords (via CategoryMatcher)
 * 4. Previous selections in similar amount ranges
 * 5. Default amount-based suggestions as fallback
 *
 * Rules:
 * - Merchant and user-preference matches always take priority over amount heuristics.
 * - Never auto-categorizes without user confirmation.
 * - Strictly offline: zero network requests, zero cloud AI APIs.
 * - Strictly privacy-first: never stores OTPs, full bank account numbers, or raw SMS.
 */
class CategorySuggestionEngine(
    private val preferences: MerchantCategoryPreferences = MerchantCategoryPreferences.getInstance()
) {

    /**
     * Computes the top 2 ranked category suggestions for an expense or income transaction.
     */
    fun getSuggestions(
        amountPaise: Long,
        type: TransactionType,
        merchantOrPayee: String? = null,
        accounts: List<BankAccountEntity> = emptyList(),
        currentAccountLastFour: String? = null,
        smsSnippet: String? = null
    ): List<String> {
        // --- Inter-account transfer check (applies to both DEBIT and CREDIT) ---
        if (isInterAccountTransfer(merchantOrPayee, accounts, currentAccountLastFour, smsSnippet)) {
            return listOf("Transfer", "Other")
        }

        // --- Income (Credit) Handling ---
        if (type == TransactionType.CREDIT) {
            return getIncomeSuggestions(amountPaise, merchantOrPayee, smsSnippet)
        }

        // --- Expense (Debit) Handling: 5-Layer Ranking Hierarchy ---
        val tier = AmountTier.fromPaise(amountPaise)
        val candidates = mutableListOf<String>()

        // Layer 1: Previous category selections for the same merchant or UPI payee
        val layer1Learned = preferences.getLearnedCategory(merchantOrPayee)
        if (!layer1Learned.isNullOrBlank()) {
            candidates.add(layer1Learned)
        }

        // Layer 2: User-defined merchant-category preferences
        val layer2UserPref = preferences.getUserDefinedPreference(merchantOrPayee)
        if (!layer2UserPref.isNullOrBlank()) {
            candidates.add(layer2UserPref)
        }

        // Layer 3: Recognized merchant keywords
        val layer3Keyword = CategoryMatcher.matchCategoryName(merchantOrPayee)
            ?: CategoryMatcher.matchCategoryName(smsSnippet)
        if (!layer3Keyword.isNullOrBlank()) {
            candidates.add(layer3Keyword)
        }

        // Layer 4: Previous selections in similar amount ranges
        val layer4Range = preferences.getAmountRangeCategory(tier)
        if (!layer4Range.isNullOrBlank()) {
            candidates.add(layer4Range)
        }

        // Layer 5: Default amount-based suggestions as fallback
        val layer5Defaults = getDefaultSuggestionsForTier(tier)
        candidates.addAll(layer5Defaults)

        // Deduplicate while strictly maintaining layer priority order, return top 2
        return candidates
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .take(2)
    }

    /**
     * Fallback suggestions by expense amount:
     * - Less than ₹100: Snacks, Food
     * - ₹100–₹499: Food, Groceries
     * - ₹500–₹1,999: Shopping, Food
     * - ₹2,000–₹4,999: Shopping, Bills
     * - ₹5,000 and above: Bills, Shopping
     */
    fun getDefaultSuggestionsForTier(tier: AmountTier): List<String> {
        return when (tier) {
            AmountTier.UNDER_100 -> listOf("Snacks", "Food")
            AmountTier.FROM_100_TO_499 -> listOf("Food", "Groceries")
            AmountTier.FROM_500_TO_1999 -> listOf("Shopping", "Food")
            AmountTier.FROM_2000_TO_4999 -> listOf("Shopping", "Bills")
            AmountTier.FROM_5000_ABOVE -> listOf("Bills", "Shopping")
        }
    }

    /**
     * Determines if a transaction is an inter-account transfer between user-owned accounts.
     */
    private fun isInterAccountTransfer(
        merchantOrPayee: String?,
        accounts: List<BankAccountEntity>,
        currentAccountLastFour: String?,
        smsSnippet: String?
    ): Boolean {
        val combinedText = "${merchantOrPayee.orEmpty()} ${smsSnippet.orEmpty()}".lowercase()

        // 1. Check for transfer keywords
        val transferKeywords = listOf(
            "self transfer", "fund transfer", "transferred to own", "to own account",
            "inter account", "account transfer", "transfer between"
        )
        if (transferKeywords.any { combinedText.contains(it) }) {
            return true
        }

        // 2. Check if text matches another owned account (different last four digits)
        val otherAccounts = accounts.filter {
            currentAccountLastFour == null || it.lastFourDigits != currentAccountLastFour
        }

        for (other in otherAccounts) {
            if (other.lastFourDigits.length == 4 && combinedText.contains(other.lastFourDigits)) {
                return true
            }
            if (other.nickname.length >= 3 && combinedText.contains(other.nickname.lowercase())) {
                return true
            }
        }

        return false
    }

    /**
     * Suggestions for Income (Credit) transactions:
     * Handles Salary, Refund, Transfer, Other with keyword and learned heuristic support.
     */
    private fun getIncomeSuggestions(
        amountPaise: Long,
        merchantOrPayee: String?,
        smsSnippet: String?
    ): List<String> {
        val text = "${merchantOrPayee.orEmpty()} ${smsSnippet.orEmpty()}".lowercase()
        val candidates = mutableListOf<String>()

        // 1. Check learned payee preference
        val learned = preferences.getLearnedCategory(merchantOrPayee)
        if (!learned.isNullOrBlank()) {
            candidates.add(learned)
        }

        // 2. Salary / Payroll keywords
        if (text.contains("salary") || text.contains("payroll") || text.contains("stipend") || text.contains("wages")) {
            candidates.add("Salary")
            candidates.add("Income")
        }

        // 3. Refund / Cashback keywords
        if (text.contains("refund") || text.contains("cashback") || text.contains("reversal")) {
            candidates.add("Refund")
            candidates.add("Income")
        }

        // 4. Amount heuristic for income
        if (amountPaise >= 1500000L) { // >= ₹15,000.00
            candidates.add("Salary")
            candidates.add("Income")
        } else {
            candidates.add("Income")
            candidates.add("Other")
        }

        candidates.add("Income")
        candidates.add("Other")

        return candidates
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .take(2)
    }

    /**
     * Learns from a confirmed manual category selection.
     * Guaranteed to never store OTPs, full bank account numbers, or sensitive SMS text.
     */
    fun learn(
        merchantOrPayee: String?,
        categoryName: String,
        amountPaise: Long
    ) {
        if (!preferences.isValidMerchantIdentifier(merchantOrPayee)) return

        // 1. Learn for this merchant
        preferences.saveLearnedCategory(merchantOrPayee, categoryName)

        // 2. Record selection in amount range
        val tier = AmountTier.fromPaise(amountPaise)
        preferences.recordAmountRangeSelection(tier, categoryName)
    }

    companion object {
        @Volatile
        private var INSTANCE: CategorySuggestionEngine? = null

        fun getInstance(context: Context? = null): CategorySuggestionEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CategorySuggestionEngine(
                    preferences = MerchantCategoryPreferences.getInstance(context)
                ).also { INSTANCE = it }
            }
        }

        /**
         * Creates an isolated instance for unit testing.
         */
        fun createForTesting(preferences: MerchantCategoryPreferences): CategorySuggestionEngine {
            return CategorySuggestionEngine(preferences)
        }
    }
}
