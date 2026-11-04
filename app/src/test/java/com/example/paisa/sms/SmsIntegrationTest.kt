package com.example.paisa.sms

import com.example.paisa.data.local.dao.BankAccountDao
import com.example.paisa.data.local.dao.CategoryDao
import com.example.paisa.data.local.dao.TransactionDao
import com.example.paisa.data.local.entity.BankAccountEntity
import com.example.paisa.data.local.entity.CategoryEntity
import com.example.paisa.data.local.entity.TransactionEntity
import com.example.paisa.data.local.entity.TransactionSource
import com.example.paisa.data.local.entity.TransactionStatus
import com.example.paisa.data.local.entity.TransactionType
import com.example.paisa.data.local.model.AccountWithBalance
import com.example.paisa.data.local.model.MonthlyTotals
import com.example.paisa.data.local.model.TransactionWithDetails
import com.example.paisa.data.repository.ExpenseTrackerRepository
import com.example.paisa.parser.DefaultBankSmsParser
import com.example.paisa.parser.RejectionReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SmsIntegrationTest {

    private lateinit var fakeBankAccountDao: FakeBankAccountDao
    private lateinit var fakeCategoryDao: FakeCategoryDao
    private lateinit var fakeTransactionDao: FakeTransactionDao
    private lateinit var repository: ExpenseTrackerRepository
    private lateinit var processor: SmsTransactionProcessor

    @Before
    fun setUp() {
        fakeBankAccountDao = FakeBankAccountDao()
        fakeCategoryDao = FakeCategoryDao()
        fakeTransactionDao = FakeTransactionDao()

        repository = ExpenseTrackerRepository(
            bankAccountDao = fakeBankAccountDao,
            categoryDao = fakeCategoryDao,
            transactionDao = fakeTransactionDao
        )

        processor = SmsTransactionProcessor(
            repository = repository,
            parser = DefaultBankSmsParser()
        )

        // Seed default test accounts
        runBlocking {
            fakeBankAccountDao.insert(
                BankAccountEntity(
                    id = 1L,
                    bankName = "HDFC Bank",
                    nickname = "Salary Account",
                    lastFourDigits = "4821",
                    openingBalancePaise = 5000000L,
                    reconciledBalancePaise = null,
                    isPrimary = true
                )
            )
            fakeBankAccountDao.insert(
                BankAccountEntity(
                    id = 2L,
                    bankName = "ICICI Bank",
                    nickname = "Daily Spends",
                    lastFourDigits = "9104",
                    openingBalancePaise = 2000000L,
                    reconciledBalancePaise = null,
                    isPrimary = false
                )
            )

            // Seed default test categories
            fakeCategoryDao.insert(CategoryEntity(id = 10L, name = "Dining", emoji = "☕"))
            fakeCategoryDao.insert(CategoryEntity(id = 20L, name = "Groceries", emoji = "🛒"))
            fakeCategoryDao.insert(CategoryEntity(id = 30L, name = "Transport", emoji = "🚕"))
            fakeCategoryDao.insert(CategoryEntity(id = 40L, name = "Entertainment", emoji = "🍿"))
            fakeCategoryDao.insert(CategoryEntity(id = 50L, name = "Salary", emoji = "💼"))
        }
    }

    @Test
    fun processIncomingSms_validDebitAlert_createsPendingTransactionUntilConfirmed() = runBlocking {
        val sms = "Rs 450.00 debited from A/C XX4821 on 24-Oct-26 at SWIGGY. Avail Bal: Rs 49,550.00. Ref: 429381029381"
        val outcome = processor.processIncomingSms(smsBody = sms, sender = "AD-HDFCBK")

        assertTrue(outcome is SmsProcessingOutcome.Created)
        val created = outcome as SmsProcessingOutcome.Created
        assertEquals("Salary Account", created.accountNickname)
        assertEquals(45000L, created.amountPaise)
        assertFalse(created.isReviewNeeded)
        assertEquals(listOf("Food", "Groceries"), created.suggestions)

        val savedTx = fakeTransactionDao.transactions.first { it.id == created.transactionId }
        assertEquals(1L, savedTx.accountId)
        assertEquals(45000L, savedTx.amountPaise)
        assertEquals(TransactionType.DEBIT, savedTx.type)
        assertEquals(TransactionSource.SMS, savedTx.source)
        assertEquals(TransactionStatus.PENDING, savedTx.status) // Must be PENDING until confirmed
        assertEquals("429381029381", savedTx.reference)
        assertNull(savedTx.categoryId) // Never auto-categorize without user selection
        assertEquals("SWIGGY", savedTx.note)

        // Verify account balance was reconciled to available balance (49,550.00 = 4,955,000 paise)
        val updatedAccount = fakeBankAccountDao.getById(1L)
        assertEquals(4955000L, updatedAccount?.reconciledBalancePaise)

        // Strict privacy check: Raw message is never stored
        assertFalse(savedTx.note?.contains("Avail Bal") == true)
        assertFalse(savedTx.note?.contains("A/C XX4821") == true)

        // User confirms category selection (e.g. from notification or picker)
        repository.updateTransactionCategory(savedTx.id, 10L)
        val confirmedTx = fakeTransactionDao.transactions.first { it.id == created.transactionId }
        assertEquals(TransactionStatus.CONFIRMED, confirmedTx.status)
        assertEquals(10L, confirmedTx.categoryId)
    }

    @Test
    fun processIncomingSms_validCreditAlert_createsPendingTransactionUntilConfirmed() = runBlocking {
        val sms = "Your A/C XX9104 is credited with INR 5,000.00 on 25-Oct-26 by UPI/REF987654. Bal: INR 25,000.00"
        val outcome = processor.processIncomingSms(smsBody = sms, sender = "VM-ICICIB")

        assertTrue(outcome is SmsProcessingOutcome.Created)
        val created = outcome as SmsProcessingOutcome.Created
        assertEquals("Daily Spends", created.accountNickname)
        assertEquals(500000L, created.amountPaise)
        assertFalse(created.isReviewNeeded)

        val savedTx = fakeTransactionDao.transactions.first { it.id == created.transactionId }
        assertEquals(2L, savedTx.accountId)
        assertEquals(TransactionType.CREDIT, savedTx.type)
        assertEquals(TransactionStatus.PENDING, savedTx.status)
        assertNull(savedTx.categoryId)
        assertEquals(2500000L, fakeBankAccountDao.getById(2L)?.reconciledBalancePaise)

        // Confirm category
        repository.updateTransactionCategory(savedTx.id, 50L)
        val confirmedTx = fakeTransactionDao.transactions.first { it.id == created.transactionId }
        assertEquals(TransactionStatus.CONFIRMED, confirmedTx.status)
        assertEquals(50L, confirmedTx.categoryId)
    }

    @Test
    fun processIncomingSms_duplicateSms_isDeduplicatedWithoutSecondInsertion() = runBlocking {
        val sms = "Rs 1,200.00 debited from A/C XX4821 on 24-Oct-26 at UBER. Ref: UBER999123"

        // First process
        val firstOutcome = processor.processIncomingSms(smsBody = sms, sender = "AD-HDFCBK")
        assertTrue(firstOutcome is SmsProcessingOutcome.Created)
        val initialTxCount = fakeTransactionDao.transactions.size

        // Duplicate process of same SMS
        val secondOutcome = processor.processIncomingSms(smsBody = sms, sender = "AD-HDFCBK")
        assertTrue(secondOutcome is SmsProcessingOutcome.Duplicate)

        // Transaction count in database remains unchanged
        assertEquals(initialTxCount, fakeTransactionDao.transactions.size)
    }

    @Test
    fun processIncomingSms_ambiguousAccountMatches_marksForReviewRatherThanGuessing() = runBlocking {
        // Add a second account also ending in 4821 (e.g. from an unknown/partner bank)
        fakeBankAccountDao.insert(
            BankAccountEntity(
                id = 3L,
                bankName = "Axis Bank",
                nickname = "Secondary Savings",
                lastFourDigits = "4821",
                openingBalancePaise = 1000000L,
                isPrimary = false
            )
        )

        // SMS from a generic/non-identifiable sender header
        val sms = "Rs 750.00 debited from A/C XX4821 on 25-Oct-26 at NETFLIX. Ref: NFLX4455"
        val outcome = processor.processIncomingSms(smsBody = sms, sender = "TX-ALERTS")

        assertTrue(outcome is SmsProcessingOutcome.Created)
        val created = outcome as SmsProcessingOutcome.Created
        assertTrue("Ambiguous account must flag review needed", created.isReviewNeeded)

        val savedTx = fakeTransactionDao.transactions.first { it.id == created.transactionId }
        assertEquals(TransactionStatus.REVIEW_NEEDED, savedTx.status)
        assertTrue(savedTx.note?.startsWith("[Needs Review] Ambiguous account match") == true)
        assertTrue(savedTx.note?.contains("Salary Account") == true)
        assertTrue(savedTx.note?.contains("Secondary Savings") == true)
        assertNull("Category must not be auto-assigned", savedTx.categoryId)
    }

    @Test
    fun processIncomingSms_senderEvidenceDisambiguatesSameLastFourDigits() = runBlocking {
        // Two accounts both ending in 4821
        fakeBankAccountDao.insert(
            BankAccountEntity(
                id = 3L,
                bankName = "Axis Bank",
                nickname = "Secondary Savings",
                lastFourDigits = "4821",
                openingBalancePaise = 1000000L,
                isPrimary = false
            )
        )

        // SMS clearly from HDFC Bank header "AD-HDFCBK"
        val sms = "Rs 200.00 debited from A/C XX4821 on 25-Oct-26 at ZOMATO. Ref: ZOM123"
        val outcome = processor.processIncomingSms(smsBody = sms, sender = "AD-HDFCBK")

        assertTrue(outcome is SmsProcessingOutcome.Created)
        val created = outcome as SmsProcessingOutcome.Created
        assertFalse("Sender evidence resolved ambiguity", created.isReviewNeeded)
        assertEquals("Salary Account", created.accountNickname)

        val savedTx = fakeTransactionDao.transactions.first { it.id == created.transactionId }
        assertEquals(1L, savedTx.accountId)
        assertEquals(TransactionStatus.PENDING, savedTx.status)
        assertNull(savedTx.categoryId)
    }

    @Test
    fun processIncomingSms_unmatchedAccount_createsNewAccountWithoutPollutingPrimaryAccount() = runBlocking {
        // User has primary account (Salary Account ending in 4821, id=1L).
        // Incoming SMS for an unadded account ending in 7777:
        val sms = "Rs 300.00 debited from A/C XX7777 on 25-Oct-26 at CHAI POINT. Ref: CHAI999"
        val outcome = processor.processIncomingSms(smsBody = sms, sender = "AD-HDFCBK")

        assertTrue(outcome is SmsProcessingOutcome.Created)
        val created = outcome as SmsProcessingOutcome.Created
        assertEquals("7777", created.accountLastFourDigits)
        assertEquals("HDFC •••• 7777", created.accountNickname)
        assertFalse(created.isReviewNeeded)

        val savedTx = fakeTransactionDao.transactions.first { it.id == created.transactionId }
        // Must NOT be attached to the primary account (id=1L)
        assertNotEquals(1L, savedTx.accountId)
        assertEquals(TransactionStatus.PENDING, savedTx.status)
        assertEquals("CHAI POINT", savedTx.note)

        // Verify that a new account for 7777 was created with correct bank details
        val newAccount = fakeBankAccountDao.getById(savedTx.accountId)
        assertNotNull(newAccount)
        assertEquals("7777", newAccount?.lastFourDigits)
        assertEquals("HDFC Bank", newAccount?.bankName)
        assertFalse(newAccount?.isPrimary ?: true)

        // Verify primary account was never modified
        val primaryAcc = fakeBankAccountDao.getById(1L)
        assertEquals("Salary Account", primaryAcc?.nickname)
        assertEquals(5000000L, primaryAcc?.openingBalancePaise)
    }

    @Test
    fun processIncomingSms_noAccountsInDatabase_returnsNoAccountAvailable() = runBlocking {
        fakeBankAccountDao.clear()

        val sms = "Rs 100.00 debited from A/C XX4821. Ref: 123"
        val outcome = processor.processIncomingSms(smsBody = sms, sender = "AD-HDFCBK")

        assertTrue(outcome is SmsProcessingOutcome.NoAccountAvailable)
        assertEquals("4821", (outcome as SmsProcessingOutcome.NoAccountAvailable).lastFourDigits)
        assertEquals(0, fakeTransactionDao.transactions.size)
    }

    @Test
    fun processIncomingSms_otpMessages_areRejectedAndNeverSaved() = runBlocking {
        val otpSms = "123456 is your secret OTP for transaction of INR 2,500.00 on card ending 4821. Valid for 5 mins. Do not share."
        val outcome = processor.processIncomingSms(smsBody = otpSms, sender = "AD-HDFCBK")

        assertTrue(outcome is SmsProcessingOutcome.Rejected)
        assertEquals(RejectionReason.OTP_MESSAGE, (outcome as SmsProcessingOutcome.Rejected).reason)
        assertEquals(0, fakeTransactionDao.transactions.size)
    }

    @Test
    fun processIncomingSms_promotionalMessages_areRejectedAndNeverSaved() = runBlocking {
        val promoSms = "Congratulations! You are eligible for a pre-approved personal loan of Rs 5,00,000 on your A/C XX4821. Apply now!"
        val outcome = processor.processIncomingSms(smsBody = promoSms, sender = "AD-HDFCBK")

        assertTrue(outcome is SmsProcessingOutcome.Rejected)
        assertEquals(RejectionReason.PROMOTIONAL_OR_OFFER, (outcome as SmsProcessingOutcome.Rejected).reason)
        assertEquals(0, fakeTransactionDao.transactions.size)
    }

    @Test
    fun processIncomingSms_failedTransactions_areRejectedAndNeverSaved() = runBlocking {
        val failedSms = "Transaction of INR 1,500.00 on A/C XX4821 was declined due to insufficient funds."
        val outcome = processor.processIncomingSms(smsBody = failedSms, sender = "AD-HDFCBK")

        assertTrue(outcome is SmsProcessingOutcome.Rejected)
        assertEquals(RejectionReason.TRANSACTION_FAILED_OR_DECLINED, (outcome as SmsProcessingOutcome.Rejected).reason)
        assertEquals(0, fakeTransactionDao.transactions.size)
    }

    @Test
    fun processIncomingSms_paymentCollectRequests_areRejectedAndNeverSaved() = runBlocking {
        val requestSms = "Merchant XYZ has requested Rs 850.00 from your A/C XX4821. Authorize payment in UPI app."
        val outcome = processor.processIncomingSms(smsBody = requestSms, sender = "AD-HDFCBK")

        assertTrue(outcome is SmsProcessingOutcome.Rejected)
        assertEquals(RejectionReason.PAYMENT_REQUEST, (outcome as SmsProcessingOutcome.Rejected).reason)
        assertEquals(0, fakeTransactionDao.transactions.size)
    }

    @Test
    fun processIncomingSms_balanceInquiryOnly_isRejectedAndNeverSaved() = runBlocking {
        val balanceSms = "Dear Customer, available balance in your Account XX4821 is INR 35,420.50 as of 25-Oct-26."
        val outcome = processor.processIncomingSms(smsBody = balanceSms, sender = "AD-HDFCBK")

        assertTrue(outcome is SmsProcessingOutcome.Rejected)
        assertEquals(RejectionReason.BALANCE_INQUIRY_ONLY, (outcome as SmsProcessingOutcome.Rejected).reason)
        assertEquals(0, fakeTransactionDao.transactions.size)
    }

    @Test
    fun categoryMatcher_correctlyResolvesKnownMerchants() = runBlocking {
        val categories = fakeCategoryDao.getAll()

        assertEquals(10L, CategoryMatcher.matchCategory("SWIGGY", categories))
        assertEquals(10L, CategoryMatcher.matchCategory("ZOMATO", categories))
        assertEquals(20L, CategoryMatcher.matchCategory("SWIGGY INSTAMART", categories))
        assertEquals(20L, CategoryMatcher.matchCategory("BLINKIT", categories))
        assertEquals(30L, CategoryMatcher.matchCategory("UBER INDIA", categories))
        assertEquals(40L, CategoryMatcher.matchCategory("NETFLIX", categories))
        assertEquals(50L, CategoryMatcher.matchCategory("MONTHLY SALARY", categories))
        assertNull(CategoryMatcher.matchCategory("LOCAL CORNER SHOP", categories))
    }

    @Test
    fun multipartSms_simulation_processesConcatenatedBodySafely() = runBlocking {
        // Multi-part SMS where first part contains the verb & amount, second contains account & merchant
        val part1 = "Rs 1,299.00 debited from A/C XX4821 on 25-Oct-26 at "
        val part2 = "AMAZON RETAIL. Avail Bal: Rs 42,000.00. Ref: AMZ9871"
        val fullBody = part1 + part2

        val outcome = processor.processIncomingSms(smsBody = fullBody, sender = "AD-HDFCBK")

        assertTrue(outcome is SmsProcessingOutcome.Created)
        val created = outcome as SmsProcessingOutcome.Created
        assertEquals(129900L, created.amountPaise)
        assertEquals("Salary Account", created.accountNickname)
        assertFalse(created.isReviewNeeded)
    }
}

// -------------------------------------------------------------
// FAKE DAOS FOR ROBUST IN-MEMORY INTEGRATION TESTING
// -------------------------------------------------------------

class FakeBankAccountDao : BankAccountDao {
    val accounts = mutableListOf<BankAccountEntity>()

    override suspend fun insert(account: BankAccountEntity): Long {
        val id = if (account.id == 0L) (accounts.size + 1).toLong() else account.id
        val entity = account.copy(id = id)
        accounts.removeAll { it.id == id }
        accounts.add(entity)
        return id
    }

    override suspend fun insertAll(accounts: List<BankAccountEntity>): List<Long> {
        return accounts.map { insert(it) }
    }

    override suspend fun update(account: BankAccountEntity): Int {
        val idx = accounts.indexOfFirst { it.id == account.id }
        if (idx >= 0) {
            accounts[idx] = account
            return 1
        }
        return 0
    }

    override suspend fun delete(account: BankAccountEntity): Int {
        return if (accounts.removeIf { it.id == account.id }) 1 else 0
    }

    override suspend fun getById(id: Long): BankAccountEntity? {
        return accounts.firstOrNull { it.id == id }
    }

    override fun getAllAsFlow(): Flow<List<BankAccountEntity>> = emptyFlow()

    override suspend fun getAll(): List<BankAccountEntity> = accounts.toList()

    override fun getAccountsWithBalanceFlow(): Flow<List<AccountWithBalance>> = emptyFlow()

    override fun getTotalNetAssetsFlow(): Flow<Long> = emptyFlow()

    override suspend fun getTotalNetAssets(): Long {
        return accounts.sumOf { it.reconciledBalancePaise ?: it.openingBalancePaise }
    }

    override suspend fun count(): Int = accounts.size

    override suspend fun updateReconciledBalance(accountId: Long, reconciledBalancePaise: Long?): Int {
        val account = getById(accountId) ?: return 0
        update(account.copy(reconciledBalancePaise = reconciledBalancePaise))
        return 1
    }

    override suspend fun deleteById(accountId: Long): Int {
        return if (accounts.removeIf { it.id == accountId }) 1 else 0
    }

    override suspend fun countTransactionsForAccount(accountId: Long): Int = 0

    override suspend fun clearOtherPrimaryAccounts(exceptAccountId: Long): Int {
        var count = 0
        for (i in accounts.indices) {
            if (accounts[i].id != exceptAccountId && accounts[i].isPrimary) {
                accounts[i] = accounts[i].copy(isPrimary = false)
                count++
            }
        }
        return count
    }

    override suspend fun clearAllPrimaryAccounts(): Int {
        var count = 0
        for (i in accounts.indices) {
            if (accounts[i].isPrimary) {
                accounts[i] = accounts[i].copy(isPrimary = false)
                count++
            }
        }
        return count
    }

    override suspend fun deleteAll(): Int {
        val size = accounts.size
        accounts.clear()
        return size
    }

    fun clear() {
        accounts.clear()
    }
}

class FakeCategoryDao : CategoryDao {
    val categories = mutableListOf<CategoryEntity>()

    override suspend fun insert(category: CategoryEntity): Long {
        val id = if (category.id == 0L) (categories.size + 1).toLong() else category.id
        val entity = category.copy(id = id)
        categories.removeAll { it.id == id }
        categories.add(entity)
        return id
    }

    override suspend fun insertAll(categories: List<CategoryEntity>): List<Long> {
        return categories.map { insert(it) }
    }

    override suspend fun update(category: CategoryEntity): Int {
        val idx = categories.indexOfFirst { it.id == category.id }
        if (idx >= 0) {
            categories[idx] = category
            return 1
        }
        return 0
    }

    override suspend fun delete(category: CategoryEntity): Int {
        return if (categories.removeIf { it.id == category.id }) 1 else 0
    }

    override suspend fun deleteAll(): Int {
        val size = categories.size
        categories.clear()
        return size
    }

    override suspend fun getById(id: Long): CategoryEntity? {
        return categories.firstOrNull { it.id == id }
    }

    override suspend fun getByName(name: String): CategoryEntity? {
        return categories.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    override fun getAllAsFlow(): Flow<List<CategoryEntity>> = emptyFlow()

    override suspend fun getAll(): List<CategoryEntity> = categories.toList()

    override suspend fun count(): Int = categories.size
}

class FakeTransactionDao(private val categoryDao: CategoryDao? = null) : TransactionDao {
    val transactions = mutableListOf<TransactionEntity>()

    override suspend fun insert(transaction: TransactionEntity): Long {
        val id = if (transaction.id == 0L) (transactions.size + 1).toLong() else transaction.id
        val entity = transaction.copy(id = id)
        transactions.removeAll { it.id == id }
        transactions.add(entity)
        return id
    }

    override suspend fun insertAll(transactions: List<TransactionEntity>): List<Long> {
        return transactions.map { insert(it) }
    }

    override suspend fun update(transaction: TransactionEntity): Int {
        val idx = transactions.indexOfFirst { it.id == transaction.id }
        if (idx >= 0) {
            transactions[idx] = transaction
            return 1
        }
        return 0
    }

    override suspend fun delete(transaction: TransactionEntity): Int {
        return if (transactions.removeIf { it.id == transaction.id }) 1 else 0
    }

    override suspend fun deleteById(id: Long): Int {
        return if (transactions.removeIf { it.id == id }) 1 else 0
    }

    override suspend fun getById(id: Long): TransactionEntity? {
        return transactions.firstOrNull { it.id == id }
    }

    override suspend fun getAll(): List<TransactionEntity> = transactions.toList()

    override suspend fun deleteAll(): Int {
        val size = transactions.size
        transactions.clear()
        return size
    }

    override suspend fun updateTransactionCategory(transactionId: Long, categoryId: Long?): Int {
        val idx = transactions.indexOfFirst { it.id == transactionId }
        if (idx >= 0) {
            transactions[idx] = transactions[idx].copy(
                categoryId = categoryId,
                status = TransactionStatus.CONFIRMED
            )
            return 1
        }
        return 0
    }

    override suspend fun findByDeduplicationKey(key: String): TransactionEntity? {
        return transactions.firstOrNull { it.deduplicationKey == key }
    }

    override fun getAllTransactionsWithDetailsFlow(): Flow<List<TransactionWithDetails>> = emptyFlow()

    override fun getRecentTransactionsWithDetailsFlow(limit: Int): Flow<List<TransactionWithDetails>> = emptyFlow()

    override suspend fun getRecentTransactionsWithDetails(limit: Int): List<TransactionWithDetails> {
        return transactions.sortedByDescending { it.timestamp }.take(limit).map { tx ->
            TransactionWithDetails(
                id = tx.id,
                accountId = tx.accountId,
                amountPaise = tx.amountPaise,
                type = tx.type,
                categoryId = tx.categoryId,
                timestamp = tx.timestamp,
                source = tx.source,
                status = tx.status,
                reference = tx.reference,
                deduplicationKey = tx.deduplicationKey,
                note = tx.note,
                bankName = "Test Bank",
                accountNickname = "Test Account",
                accountLastFourDigits = "1234",
                categoryName = null,
                categoryEmoji = null
            )
        }
    }

    override fun getTransactionsByAccountFlow(accountId: Long): Flow<List<TransactionWithDetails>> = emptyFlow()

    override fun getMonthlyTotalsFlow(startTimestamp: Long, endTimestamp: Long): Flow<MonthlyTotals> = emptyFlow()

    override suspend fun getMonthlyTotals(startTimestamp: Long, endTimestamp: Long): MonthlyTotals {
        val filtered = transactions.filter { it.timestamp in startTimestamp..endTimestamp && it.status != TransactionStatus.CANCELLED }
        val income = filtered.filter { tx ->
            if (tx.type != TransactionType.CREDIT) return@filter false
            val catName = tx.categoryId?.let { id -> runBlocking { categoryDao?.getAll() }?.firstOrNull { it.id == id }?.name }
            !catName.equals("Transfer", ignoreCase = true)
        }.sumOf { it.amountPaise }
        val expenses = filtered.filter { tx ->
            if (tx.type != TransactionType.DEBIT) return@filter false
            val catName = tx.categoryId?.let { id -> runBlocking { categoryDao?.getAll() }?.firstOrNull { it.id == id }?.name }
            !catName.equals("Transfer", ignoreCase = true)
        }.sumOf { it.amountPaise }
        return MonthlyTotals(
            totalIncomePaise = income,
            totalExpensePaise = expenses,
            transactionCount = filtered.size
        )
    }

    override suspend fun count(): Int = transactions.size
}
