package com.example.paisa.sms

import com.example.paisa.data.local.entity.BankAccountEntity
import com.example.paisa.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CategorySuggestionEngineTest {

    private lateinit var prefs: MerchantCategoryPreferences
    private lateinit var engine: CategorySuggestionEngine

    private val testAccounts = listOf(
        BankAccountEntity(
            id = 1L,
            bankName = "HDFC Bank",
            nickname = "Primary Salary",
            lastFourDigits = "4821",
            openingBalancePaise = 10000000L,
            isPrimary = true
        ),
        BankAccountEntity(
            id = 2L,
            bankName = "ICICI Bank",
            nickname = "Daily Spends",
            lastFourDigits = "9104",
            openingBalancePaise = 2000000L,
            isPrimary = false
        )
    )

    @Before
    fun setUp() {
        prefs = MerchantCategoryPreferences.createInMemory()
        engine = CategorySuggestionEngine.createForTesting(prefs)
    }

    // =========================================================================
    // 1. All 5 Fallback Amount Thresholds (< ₹100, ₹100–₹499, ₹500–₹1,999, ₹2,000–₹4,999, ≥ ₹5,000)
    // =========================================================================

    @Test
    fun suggestions_amountTier1_under100Rupees_suggestsSnacksAndFood() {
        // Less than ₹100 (e.g., ₹45.00 = 4,500 paise, ₹99.99 = 9,999 paise)
        val suggestions45 = engine.getSuggestions(
            amountPaise = 4500L,
            type = TransactionType.DEBIT
        )
        assertEquals(listOf("Snacks", "Food"), suggestions45)

        val suggestions99 = engine.getSuggestions(
            amountPaise = 9999L,
            type = TransactionType.DEBIT
        )
        assertEquals(listOf("Snacks", "Food"), suggestions99)
    }

    @Test
    fun suggestions_amountTier2_100to499Rupees_suggestsFoodAndGroceries() {
        // ₹100.00 (10,000 paise) to ₹499.99 (49,999 paise)
        val suggestions100 = engine.getSuggestions(
            amountPaise = 10000L,
            type = TransactionType.DEBIT
        )
        assertEquals(listOf("Food", "Groceries"), suggestions100)

        val suggestions250 = engine.getSuggestions(
            amountPaise = 25000L,
            type = TransactionType.DEBIT
        )
        assertEquals(listOf("Food", "Groceries"), suggestions250)

        val suggestions499 = engine.getSuggestions(
            amountPaise = 49999L,
            type = TransactionType.DEBIT
        )
        assertEquals(listOf("Food", "Groceries"), suggestions499)
    }

    @Test
    fun suggestions_amountTier3_500to1999Rupees_suggestsShoppingAndFood() {
        // ₹500.00 (50,000 paise) to ₹1,999.99 (199,999 paise)
        val suggestions500 = engine.getSuggestions(
            amountPaise = 50000L,
            type = TransactionType.DEBIT
        )
        assertEquals(listOf("Shopping", "Food"), suggestions500)

        val suggestions1500 = engine.getSuggestions(
            amountPaise = 150000L,
            type = TransactionType.DEBIT
        )
        assertEquals(listOf("Shopping", "Food"), suggestions1500)

        val suggestions1999 = engine.getSuggestions(
            amountPaise = 199999L,
            type = TransactionType.DEBIT
        )
        assertEquals(listOf("Shopping", "Food"), suggestions1999)
    }

    @Test
    fun suggestions_amountTier4_2000to4999Rupees_suggestsShoppingAndBills() {
        // ₹2,000.00 (200,000 paise) to ₹4,999.99 (499,999 paise)
        val suggestions2000 = engine.getSuggestions(
            amountPaise = 200000L,
            type = TransactionType.DEBIT
        )
        assertEquals(listOf("Shopping", "Bills"), suggestions2000)

        val suggestions3500 = engine.getSuggestions(
            amountPaise = 350000L,
            type = TransactionType.DEBIT
        )
        assertEquals(listOf("Shopping", "Bills"), suggestions3500)

        val suggestions4999 = engine.getSuggestions(
            amountPaise = 499999L,
            type = TransactionType.DEBIT
        )
        assertEquals(listOf("Shopping", "Bills"), suggestions4999)
    }

    @Test
    fun suggestions_amountTier5_5000AndAbove_suggestsBillsAndShopping() {
        // ₹5,000.00 (500,000 paise) and above
        val suggestions5000 = engine.getSuggestions(
            amountPaise = 500000L,
            type = TransactionType.DEBIT
        )
        assertEquals(listOf("Bills", "Shopping"), suggestions5000)

        val suggestions25000 = engine.getSuggestions(
            amountPaise = 2500000L,
            type = TransactionType.DEBIT
        )
        assertEquals(listOf("Bills", "Shopping"), suggestions25000)
    }

    // =========================================================================
    // 2. 5-Layer Ranking Hierarchy & Priority Over Amount Heuristics
    // =========================================================================

    @Test
    fun ranking_layer3_keywordMatchTakesPriorityOverAmountHeuristics() {
        // Swiggy expense of ₹6,500 (tier 5 would default to Bills, Shopping)
        // Layer 3 recognizes "swiggy" -> Food
        val suggestions = engine.getSuggestions(
            amountPaise = 650000L,
            type = TransactionType.DEBIT,
            merchantOrPayee = "SWIGGY"
        )
        assertEquals("Food", suggestions[0])
        assertEquals("Bills", suggestions[1]) // Tier 5 fallback fills second slot
    }

    @Test
    fun ranking_layer2_userDefinedPreferenceTakesPriorityOverKeywordMatch() {
        // Swiggy is naturally Food via keyword, but user explicitly defined Swiggy -> "Entertainment"
        prefs.setUserDefinedPreference("swiggy", "Entertainment")

        val suggestions = engine.getSuggestions(
            amountPaise = 45000L,
            type = TransactionType.DEBIT,
            merchantOrPayee = "SWIGGY"
        )
        assertEquals("Entertainment", suggestions[0])
        assertEquals("Food", suggestions[1]) // Layer 3 keyword match fills second slot
    }

    @Test
    fun ranking_layer1_learnedMerchantSelectionTakesHighestPriority() {
        // User previously confirmed "Groceries" for "Swiggy" (e.g. Swiggy Instamart)
        engine.learn(merchantOrPayee = "SWIGGY", categoryName = "Groceries", amountPaise = 45000L)

        // Set a user preference for another category to verify Layer 1 takes priority
        prefs.setUserDefinedPreference("swiggy", "Entertainment")

        val suggestions = engine.getSuggestions(
            amountPaise = 45000L,
            type = TransactionType.DEBIT,
            merchantOrPayee = "SWIGGY"
        )
        assertEquals("Groceries", suggestions[0])
        assertEquals("Entertainment", suggestions[1])
    }

    @Test
    fun ranking_layer4_amountRangeHistoryFillsWhenNoSpecificMerchantMatch() {
        // Learn that in the ₹500–₹1,999 range, user frequently selects "Transport"
        engine.learn(merchantOrPayee = "Local Vendor", categoryName = "Transport", amountPaise = 120000L)

        // New unknown vendor with no keyword matches in the same range
        val suggestions = engine.getSuggestions(
            amountPaise = 135000L,
            type = TransactionType.DEBIT,
            merchantOrPayee = "Unknown Store"
        )
        assertEquals("Transport", suggestions[0]) // Layer 4 learned range category
        assertEquals("Shopping", suggestions[1])  // Layer 5 default tier 3 first entry
    }

    // =========================================================================
    // 3. Learning Behavior & Privacy Protection (OTPs, Bank Accounts)
    // =========================================================================

    @Test
    fun learning_validPayeeAndUpiId_learnsSuccessfully() {
        engine.learn(merchantOrPayee = "user@okhdfcbank", categoryName = "Groceries", amountPaise = 25000L)
        assertEquals("Groceries", prefs.getLearnedCategory("user@okhdfcbank"))

        val suggestions = engine.getSuggestions(
            amountPaise = 25000L,
            type = TransactionType.DEBIT,
            merchantOrPayee = "user@okhdfcbank"
        )
        assertEquals("Groceries", suggestions[0])
    }

    @Test
    fun learning_sensitiveKeywords_strictlyRejectedAndNeverLearned() {
        // OTP messages or auth codes must never be stored
        val sensitiveStrings = listOf(
            "OTP 123456",
            "verification code 4821",
            "secret password",
            "pin auth code",
            "one time password"
        )

        for (sensitive in sensitiveStrings) {
            engine.learn(merchantOrPayee = sensitive, categoryName = "Food", amountPaise = 50000L)
            assertFalse(prefs.isValidMerchantIdentifier(sensitive))
            assertEquals(null, prefs.getLearnedCategory(sensitive))
        }
    }

    @Test
    fun learning_numericOtpAndFullAccountNumbers_strictlyRejected() {
        val rejectedStrings = listOf(
            "849201",             // 6-digit OTP
            "12345678",           // 8-digit OTP / account
            "A/C 123456789012",   // Full bank account number
            "Account 9876543210"  // Full account number
        )

        for (item in rejectedStrings) {
            engine.learn(merchantOrPayee = item, categoryName = "Food", amountPaise = 50000L)
            assertFalse(prefs.isValidMerchantIdentifier(item))
            assertEquals(null, prefs.getLearnedCategory(item))
        }
    }

    // =========================================================================
    // 4. Income (Credit) Transactions Handling
    // =========================================================================

    @Test
    fun income_salaryKeywords_suggestsSalaryAndIncome() {
        val suggestions = engine.getSuggestions(
            amountPaise = 9500000L, // ₹95,000.00
            type = TransactionType.CREDIT,
            merchantOrPayee = "COMPANY PAYROLL SALARY OCT"
        )
        assertEquals("Salary", suggestions[0])
        assertEquals("Income", suggestions[1])
    }

    @Test
    fun income_refundKeywords_suggestsRefundAndIncome() {
        val suggestions = engine.getSuggestions(
            amountPaise = 45000L, // ₹450.00
            type = TransactionType.CREDIT,
            merchantOrPayee = "AMAZON REFUND REVERSAL"
        )
        assertEquals("Refund", suggestions[0])
        assertEquals("Income", suggestions[1])
    }

    @Test
    fun income_largeAmountWithoutKeywords_defaultsToSalaryAndIncome() {
        val suggestions = engine.getSuggestions(
            amountPaise = 5000000L, // ₹50,000.00
            type = TransactionType.CREDIT,
            merchantOrPayee = "Direct Credit"
        )
        assertEquals("Salary", suggestions[0])
        assertEquals("Income", suggestions[1])
    }

    @Test
    fun income_smallAmountWithoutKeywords_defaultsToRefundAndIncome() {
        val suggestions = engine.getSuggestions(
            amountPaise = 30000L, // ₹300.00
            type = TransactionType.CREDIT,
            merchantOrPayee = "Cashback Credit"
        )
        assertEquals("Refund", suggestions[0])
        assertEquals("Income", suggestions[1])
    }

    // =========================================================================
    // 5. Inter-Account Transfers Between Owned Accounts
    // =========================================================================

    @Test
    fun interAccountTransfer_byTargetAccountLastFourDigits_suggestsTransferAndOther() {
        // User has Account 1 (4821) and Account 2 (9104).
        // Debit from Account 1 with merchant/reference mentioning 9104:
        val suggestionsDebit = engine.getSuggestions(
            amountPaise = 1000000L,
            type = TransactionType.DEBIT,
            merchantOrPayee = "Transfer to A/c 9104",
            accounts = testAccounts,
            currentAccountLastFour = "4821"
        )
        assertEquals(listOf("Transfer", "Other"), suggestionsDebit)

        // Credit to Account 2 mentioning 4821:
        val suggestionsCredit = engine.getSuggestions(
            amountPaise = 1000000L,
            type = TransactionType.CREDIT,
            merchantOrPayee = "Fund received from 4821",
            accounts = testAccounts,
            currentAccountLastFour = "9104"
        )
        assertEquals(listOf("Transfer", "Other"), suggestionsCredit)
    }

    @Test
    fun interAccountTransfer_byAccountNickname_suggestsTransferAndOther() {
        val suggestions = engine.getSuggestions(
            amountPaise = 500000L,
            type = TransactionType.DEBIT,
            merchantOrPayee = "Sent to Daily Spends",
            accounts = testAccounts,
            currentAccountLastFour = "4821"
        )
        assertEquals(listOf("Transfer", "Other"), suggestions)
    }

    @Test
    fun interAccountTransfer_bySelfTransferKeywords_suggestsTransferAndOther() {
        val suggestions = engine.getSuggestions(
            amountPaise = 250000L,
            type = TransactionType.DEBIT,
            merchantOrPayee = "Self transfer via UPI",
            accounts = testAccounts,
            currentAccountLastFour = "4821"
        )
        assertEquals(listOf("Transfer", "Other"), suggestions)
    }

    // =========================================================================
    // 6. Deduplication and Stability
    // =========================================================================

    @Test
    fun suggestions_alwaysReturnsExactlyTwoDistinctCategories() {
        val tiers = listOf(5000L, 25000L, 100000L, 300000L, 800000L)
        for (amount in tiers) {
            val suggestions = engine.getSuggestions(amountPaise = amount, type = TransactionType.DEBIT)
            assertEquals(2, suggestions.size)
            assertTrue(suggestions[0] != suggestions[1])
        }
    }
}
