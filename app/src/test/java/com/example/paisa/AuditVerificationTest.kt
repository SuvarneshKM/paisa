package com.example.paisa

import com.example.paisa.data.local.PaisaDatabase
import com.example.paisa.data.local.entity.BankAccountEntity
import com.example.paisa.data.local.entity.CategoryEntity
import com.example.paisa.data.local.entity.TransactionEntity
import com.example.paisa.data.local.entity.TransactionSource
import com.example.paisa.data.local.entity.TransactionStatus
import com.example.paisa.data.local.entity.TransactionType
import com.example.paisa.data.repository.ExpenseTrackerRepository
import com.example.paisa.parser.DefaultBankSmsParser
import com.example.paisa.parser.RejectionReason
import com.example.paisa.parser.SmsParseResult
import com.example.paisa.sms.FakeBankAccountDao
import com.example.paisa.sms.FakeCategoryDao
import com.example.paisa.sms.FakeTransactionDao
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuditVerificationTest {

    private lateinit var fakeBankAccountDao: FakeBankAccountDao
    private lateinit var fakeCategoryDao: FakeCategoryDao
    private lateinit var fakeTransactionDao: FakeTransactionDao
    private lateinit var repository: ExpenseTrackerRepository
    private val parser = DefaultBankSmsParser()

    @Before
    fun setUp() {
        fakeBankAccountDao = FakeBankAccountDao()
        fakeCategoryDao = FakeCategoryDao()
        fakeTransactionDao = FakeTransactionDao(fakeCategoryDao)

        repository = ExpenseTrackerRepository(
            bankAccountDao = fakeBankAccountDao,
            categoryDao = fakeCategoryDao,
            transactionDao = fakeTransactionDao
        )
    }

    @Test
    fun audit_incomingAndOutgoingTransfers_preservesNetAssetsInvariant() = runBlocking {
        // Account 1: Salary (Opening: ₹50,000.00 = 5,000,000 paise)
        val acc1Id = repository.createAccount(
            bankName = "HDFC Bank",
            nickname = "Salary",
            lastFourDigits = "4821",
            openingBalancePaise = 5000000L
        )

        // Account 2: Spends (Opening: ₹10,000.00 = 1,000,000 paise)
        val acc2Id = repository.createAccount(
            bankName = "ICICI Bank",
            nickname = "Spends",
            lastFourDigits = "9104",
            openingBalancePaise = 1000000L
        )

        val transferCatId = repository.createCategory("Transfer", "🔄")

        // Initial net assets: 50,000 + 10,000 = ₹60,000.00 (6,000,000 paise)
        assertEquals(6000000L, repository.getTotalNetAssets())

        // Execute Transfer of ₹15,000.00 (1,500,000 paise) from Salary to Spends
        val transferAmount = 1500000L
        val timestamp = System.currentTimeMillis()

        // 1. Outgoing Transfer (Debit from Account 1)
        repository.createTransaction(
            accountId = acc1Id,
            amountPaise = transferAmount,
            type = TransactionType.DEBIT,
            categoryId = transferCatId,
            timestamp = timestamp,
            source = TransactionSource.MANUAL,
            note = "Transfer to Spends"
        )

        // 2. Incoming Transfer (Credit to Account 2)
        repository.createTransaction(
            accountId = acc2Id,
            amountPaise = transferAmount,
            type = TransactionType.CREDIT,
            categoryId = transferCatId,
            timestamp = timestamp,
            source = TransactionSource.MANUAL,
            note = "Transfer from Salary"
        )

        // Total Net Assets must remain exactly unchanged!
        assertEquals(6000000L, repository.getTotalNetAssets())

        // Verify individual transaction records
        val allTx = fakeTransactionDao.transactions
        assertEquals(2, allTx.size)
        val debitTx = allTx.first { it.type == TransactionType.DEBIT }
        val creditTx = allTx.first { it.type == TransactionType.CREDIT }

        assertEquals(acc1Id, debitTx.accountId)
        assertEquals(acc2Id, creditTx.accountId)
        assertEquals(transferAmount, debitTx.amountPaise)
        assertEquals(transferAmount, creditTx.amountPaise)
    }

    @Test
    fun audit_balanceReconciliation_overridesWithoutDeletingHistory() = runBlocking {
        val accId = repository.createAccount(
            bankName = "HDFC Bank",
            nickname = "Primary",
            lastFourDigits = "4821",
            openingBalancePaise = 2000000L // ₹20,000.00
        )

        // Add ₹5,000 credit
        repository.createTransaction(
            accountId = accId,
            amountPaise = 500000L,
            type = TransactionType.CREDIT,
            categoryId = null
        )

        // Calculated balance is ₹25,000.00
        assertEquals(1, fakeTransactionDao.transactions.size)

        // Reconcile to bank statement balance: ₹24,800.00
        repository.reconcileAccount(accId, 2480000L)
        val acc = repository.getAllAccounts().first { it.id == accId }
        assertEquals(2480000L, acc.reconciledBalancePaise)

        // Transaction history is completely preserved!
        assertEquals(1, fakeTransactionDao.transactions.size)

        // Reset reconciliation (revert to calculated balance)
        repository.reconcileAccount(accId, null)
        val resetAcc = repository.getAllAccounts().first { it.id == accId }
        assertNull(resetAcc.reconciledBalancePaise)
        assertEquals(1, fakeTransactionDao.transactions.size)
    }

    @Test
    fun audit_parserFalsePositives_strictRejection() {
        // 1. OTP message with amounts
        val otp = "Your OTP is 492019 for payment of INR 4,999.00 on card XX4821. Never share."
        val res1 = parser.parse(otp)
        assertTrue(res1 is SmsParseResult.Rejected)
        assertEquals(RejectionReason.OTP_MESSAGE, (res1 as SmsParseResult.Rejected).reason)

        // 2. Promotional pre-approved loan
        val promo = "Congratulations! You have a pre-approved personal loan of Rs 5,00,000 on account XX4821. Apply now."
        val res2 = parser.parse(promo)
        assertTrue(res2 is SmsParseResult.Rejected)
        assertEquals(RejectionReason.PROMOTIONAL_OR_OFFER, (res2 as SmsParseResult.Rejected).reason)

        // 3. Failed transaction
        val failed = "Transaction of Rs 850.00 on card XX4821 declined due to insufficient balance."
        val res3 = parser.parse(failed)
        assertTrue(res3 is SmsParseResult.Rejected)
        assertEquals(RejectionReason.TRANSACTION_FAILED_OR_DECLINED, (res3 as SmsParseResult.Rejected).reason)

        // 4. Payment collect request
        val request = "Zomato has requested payment of Rs 350.00 from your UPI account XX4821. Authorize in app."
        val res4 = parser.parse(request)
        assertTrue(res4 is SmsParseResult.Rejected)
        assertEquals(RejectionReason.PAYMENT_REQUEST, (res4 as SmsParseResult.Rejected).reason)

        // 5. Balance inquiry only
        val inquiry = "Dear Customer, available balance in your A/C XX4821 is INR 45,210.00 as on 24-Oct-26."
        val res5 = parser.parse(inquiry)
        assertTrue(res5 is SmsParseResult.Rejected)
        assertEquals(RejectionReason.BALANCE_INQUIRY_ONLY, (res5 as SmsParseResult.Rejected).reason)
    }

    @Test
    fun audit_roomMigrationStrategy_isConfigured() {
        assertNotNull(PaisaDatabase.ALL_MIGRATIONS)
        assertTrue(PaisaDatabase.ALL_MIGRATIONS.isNotEmpty())
        assertEquals("paisa_database.db", PaisaDatabase.DATABASE_NAME)
    }

    @Test
    fun audit_transfersBetweenOwnedAccounts_notCountedAsIncomeOrExpenses() = runBlocking {
        val acc1Id = repository.createAccount(
            bankName = "HDFC Bank",
            nickname = "Salary Account",
            lastFourDigits = "4821",
            openingBalancePaise = 5000000L
        )
        val acc2Id = repository.createAccount(
            bankName = "ICICI Bank",
            nickname = "Spends Account",
            lastFourDigits = "9104",
            openingBalancePaise = 1000000L
        )

        val transferCatId = repository.createCategory("Transfer", "🔄")
        val salaryCatId = repository.createCategory("Salary", "💼")
        val groceriesCatId = repository.createCategory("Groceries", "🛒")

        val now = System.currentTimeMillis()

        // 1. Legitimate Income: ₹50,000 Salary credit
        repository.createTransaction(
            accountId = acc1Id,
            amountPaise = 5000000L,
            type = TransactionType.CREDIT,
            categoryId = salaryCatId,
            timestamp = now
        )

        // 2. Legitimate Expense: ₹3,000 Groceries debit
        repository.createTransaction(
            accountId = acc2Id,
            amountPaise = 300000L,
            type = TransactionType.DEBIT,
            categoryId = groceriesCatId,
            timestamp = now
        )

        // 3. Inter-account transfer: ₹10,000 debit from acc1, ₹10,000 credit to acc2
        repository.createTransaction(
            accountId = acc1Id,
            amountPaise = 1000000L,
            type = TransactionType.DEBIT,
            categoryId = transferCatId,
            timestamp = now,
            note = "Transfer to Spends"
        )
        repository.createTransaction(
            accountId = acc2Id,
            amountPaise = 1000000L,
            type = TransactionType.CREDIT,
            categoryId = transferCatId,
            timestamp = now,
            note = "Transfer from Salary"
        )

        // Monthly totals should ONLY reflect real income (₹50,000) and real expenses (₹3,000)
        // Transfers between owned accounts MUST NOT be counted as income or expenses!
        val totals = repository.getMonthlyTotals(now - 10000L, now + 10000L)
        assertEquals(5000000L, totals.totalIncomePaise)
        assertEquals(300000L, totals.totalExpensePaise)
    }

    @Test
    fun audit_transactionValidation_requiresCategory() {
        var callbackError: String? = null
        val vm = com.example.paisa.ui.viewmodel.TransactionViewModel(repository)

        // Null categoryId must be rejected
        vm.validateAndAddTransaction(
            accountId = 1L,
            amountRupeesInput = "500",
            type = TransactionType.DEBIT,
            categoryId = null
        ) { result ->
            if (result is com.example.paisa.ui.viewmodel.TransactionValidationResult.Error) {
                callbackError = result.message
            }
        }
        assertEquals("Please select a transaction category", callbackError)

        // 0 categoryId must be rejected
        callbackError = null
        vm.validateAndAddTransaction(
            accountId = 1L,
            amountRupeesInput = "500",
            type = TransactionType.DEBIT,
            categoryId = 0L
        ) { result ->
            if (result is com.example.paisa.ui.viewmodel.TransactionValidationResult.Error) {
                callbackError = result.message
            }
        }
        assertEquals("Please select a transaction category", callbackError)

        // Missing account must be rejected
        callbackError = null
        vm.validateAndAddTransaction(
            accountId = null,
            amountRupeesInput = "500",
            type = TransactionType.DEBIT,
            categoryId = 1L
        ) { result ->
            if (result is com.example.paisa.ui.viewmodel.TransactionValidationResult.Error) {
                callbackError = result.message
            }
        }
        assertEquals("Please select a bank account", callbackError)

        // Non-positive amount must be rejected
        callbackError = null
        vm.validateAndAddTransaction(
            accountId = 1L,
            amountRupeesInput = "0",
            type = TransactionType.DEBIT,
            categoryId = 1L
        ) { result ->
            if (result is com.example.paisa.ui.viewmodel.TransactionValidationResult.Error) {
                callbackError = result.message
            }
        }
        assertEquals("Please enter a valid positive amount", callbackError)
    }

    @Test
    fun audit_privacyClaims_noDatabaseEncryptionClaimsInSettings() {
        val settingsFile = java.io.File("src/main/java/com/example/paisa/ui/components/SettingsSheet.kt").let {
            if (it.exists()) it else java.io.File("app/src/main/java/com/example/paisa/ui/components/SettingsSheet.kt")
        }
        assertTrue("SettingsSheet file must exist", settingsFile.exists())
        val content = settingsFile.readText()
        assertFalse("Settings must not claim database encryption since app uses standard Room SQLite",
            content.contains("encrypted", ignoreCase = true))
        assertTrue("Settings must truthfully state offline Room SQLite storage",
            content.contains("offline Room SQLite database", ignoreCase = true))
    }
}
