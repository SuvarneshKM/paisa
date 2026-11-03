package com.example.paisa

import com.example.paisa.data.local.dao.BankAccountDao
import com.example.paisa.data.local.dao.CategoryDao
import com.example.paisa.data.local.model.MonthlyTotals
import com.example.paisa.data.local.dao.TransactionDao
import com.example.paisa.data.local.entity.BankAccountEntity
import com.example.paisa.data.local.entity.CategoryEntity
import com.example.paisa.data.local.entity.TransactionEntity
import com.example.paisa.data.local.entity.TransactionSource
import com.example.paisa.data.local.entity.TransactionStatus
import com.example.paisa.data.local.entity.TransactionType
import com.example.paisa.data.local.model.AccountWithBalance
import com.example.paisa.data.local.model.TransactionWithDetails
import com.example.paisa.data.repository.ExpenseTrackerRepository
import com.example.paisa.sms.CategorySuggestionEngine
import com.example.paisa.sms.MerchantCategoryPreferences
import com.example.paisa.util.DateFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

class IncomeCalculationAndMonthBoundaryTest {

    private lateinit var fakeBankDao: TestBankAccountDao
    private lateinit var fakeCategoryDao: TestCategoryDao
    private lateinit var fakeTransactionDao: TestTransactionDao
    private lateinit var repository: ExpenseTrackerRepository
    private lateinit var suggestionEngine: CategorySuggestionEngine
    private lateinit var prefs: MerchantCategoryPreferences

    private val kolkataZone = ZoneId.of("Asia/Kolkata")
    private val utcZone = ZoneId.of("UTC")
    private val newYorkZone = ZoneId.of("America/New_York")

    @Before
    fun setUp() {
        fakeBankDao = TestBankAccountDao()
        fakeCategoryDao = TestCategoryDao()
        fakeTransactionDao = TestTransactionDao(fakeCategoryDao, fakeBankDao)

        repository = ExpenseTrackerRepository(
            bankAccountDao = fakeBankDao,
            categoryDao = fakeCategoryDao,
            transactionDao = fakeTransactionDao
        )

        prefs = MerchantCategoryPreferences.createInMemory()
        suggestionEngine = CategorySuggestionEngine.createForTesting(prefs)
    }

    // =========================================================================
    // 1. Manual Income Transactions
    // =========================================================================

    @Test
    fun manualIncome_categorizedAsIncome_countsTowardMonthlyIncome() = runBlocking {
        repository.populateInitialDataIfEmpty()
        val accId = repository.createAccount("HDFC Bank", "Savings", "7150", 125000L)
        val incomeCat = fakeCategoryDao.getByName("Income")!!

        val start = ExpenseTrackerRepository.getStartOfMonthTimestamp(2026, 11, kolkataZone)
        val end = ExpenseTrackerRepository.getEndOfMonthTimestamp(2026, 11, kolkataZone)

        // Add manual income of ₹500.00 (50,000 paise)
        val txTime = ZonedDateTime.of(LocalDate.of(2026, 11, 4), LocalTime.of(3, 25), kolkataZone)
            .toInstant().toEpochMilli()

        repository.createTransaction(
            accountId = accId,
            amountPaise = 50000L,
            type = TransactionType.CREDIT,
            categoryId = incomeCat.id,
            timestamp = txTime,
            source = TransactionSource.MANUAL,
            note = "Jobin"
        )

        val totals = repository.getMonthlyTotals(start, end)
        assertEquals(50000L, totals.totalIncomePaise)
        assertEquals(0L, totals.totalExpensePaise)
        assertEquals(1, totals.transactionCount)
    }

    @Test
    fun manualIncome_uncategorizedCredit_countsTowardMonthlyIncome() = runBlocking {
        repository.populateInitialDataIfEmpty()
        val accId = repository.createAccount("HDFC Bank", "Savings", "7150", 100000L)

        val start = ExpenseTrackerRepository.getStartOfMonthTimestamp(2026, 11, kolkataZone)
        val end = ExpenseTrackerRepository.getEndOfMonthTimestamp(2026, 11, kolkataZone)

        // Credit with null category
        repository.createTransaction(
            accountId = accId,
            amountPaise = 250000L, // ₹2,500.00
            type = TransactionType.CREDIT,
            categoryId = null,
            timestamp = start + 5000L
        )

        val totals = repository.getMonthlyTotals(start, end)
        assertEquals(250000L, totals.totalIncomePaise)
        assertEquals(0L, totals.totalExpensePaise)
    }

    // =========================================================================
    // 2. Bank SMS Credits & Suggestion Engine (External Peer vs Inter-Account)
    // =========================================================================

    @Test
    fun incomingUpiCredit_fromPeer_suggestsIncomeAndOther_notTransfer() {
        val userAccounts = listOf(
            BankAccountEntity(id = 1L, bankName = "HDFC", nickname = "Primary", lastFourDigits = "7150", openingBalancePaise = 100000L)
        )

        // Incoming UPI credit from Jobin
        val suggestions = suggestionEngine.getSuggestions(
            amountPaise = 50000L,
            type = TransactionType.CREDIT,
            merchantOrPayee = "Jobin",
            accounts = userAccounts,
            currentAccountLastFour = "7150"
        )

        // Must suggest Income and Other, NOT Transfer
        assertEquals(listOf("Income", "Other"), suggestions)
        assertFalse(suggestions.contains("Transfer"))
    }

    @Test
    fun incomingCredit_matchingOwnOtherAccount_suggestsTransferAndOther() {
        val userAccounts = listOf(
            BankAccountEntity(id = 1L, bankName = "HDFC", nickname = "Salary", lastFourDigits = "7150", openingBalancePaise = 100000L),
            BankAccountEntity(id = 2L, bankName = "SBI", nickname = "Savings", lastFourDigits = "4821", openingBalancePaise = 50000L)
        )

        // Credit on 7150 referencing user's other account 4821
        val suggestions = suggestionEngine.getSuggestions(
            amountPaise = 200000L,
            type = TransactionType.CREDIT,
            merchantOrPayee = "Transfer from A/c 4821",
            accounts = userAccounts,
            currentAccountLastFour = "7150"
        )

        assertEquals(listOf("Transfer", "Other"), suggestions)
        assertTrue(suggestions.contains("Transfer"))
    }

    // =========================================================================
    // 3. Refunds and Salary Credits
    // =========================================================================

    @Test
    fun salaryCredit_suggestsSalaryAndIncome_andCountsTowardIncome() = runBlocking {
        repository.populateInitialDataIfEmpty()
        val accId = repository.createAccount("HDFC", "Salary Acc", "7150", 0L)

        // Verify suggestion
        val suggestions = suggestionEngine.getSuggestions(
            amountPaise = 8500000L,
            type = TransactionType.CREDIT,
            merchantOrPayee = "INFOSYS TECH SALARY CREDIT"
        )
        assertEquals("Salary", suggestions[0])
        assertEquals("Income", suggestions[1])

        val salaryCat = fakeCategoryDao.getByName("Salary")!!
        val start = ExpenseTrackerRepository.getStartOfMonthTimestamp(2026, 11, kolkataZone)
        val end = ExpenseTrackerRepository.getEndOfMonthTimestamp(2026, 11, kolkataZone)

        repository.createTransaction(
            accountId = accId,
            amountPaise = 8500000L,
            type = TransactionType.CREDIT,
            categoryId = salaryCat.id,
            timestamp = start + 10000L
        )

        val totals = repository.getMonthlyTotals(start, end)
        assertEquals(8500000L, totals.totalIncomePaise)
        assertEquals(0L, totals.totalExpensePaise)
    }

    @Test
    fun refundCredit_suggestsRefundAndIncome_andCountsTowardIncome() = runBlocking {
        repository.populateInitialDataIfEmpty()
        val accId = repository.createAccount("HDFC", "Primary", "7150", 500000L)

        // Verify suggestion
        val suggestions = suggestionEngine.getSuggestions(
            amountPaise = 159900L,
            type = TransactionType.CREDIT,
            merchantOrPayee = "AMAZON REFUND RET-4982"
        )
        assertEquals("Refund", suggestions[0])
        assertEquals("Income", suggestions[1])

        val refundCat = fakeCategoryDao.getByName("Refund")!!
        val start = ExpenseTrackerRepository.getStartOfMonthTimestamp(2026, 11, kolkataZone)
        val end = ExpenseTrackerRepository.getEndOfMonthTimestamp(2026, 11, kolkataZone)

        repository.createTransaction(
            accountId = accId,
            amountPaise = 159900L,
            type = TransactionType.CREDIT,
            categoryId = refundCat.id,
            timestamp = start + 20000L
        )

        val totals = repository.getMonthlyTotals(start, end)
        assertEquals(159900L, totals.totalIncomePaise)
    }

    // =========================================================================
    // 4. Inter-Account Transfers Excluded From Income & Expenses
    // =========================================================================

    @Test
    fun interAccountTransfer_excludedFromBothMonthlyIncomeAndExpenses() = runBlocking {
        repository.populateInitialDataIfEmpty()
        val acc1 = repository.createAccount("HDFC", "Salary", "7150", 10000000L) // ₹1,00,000
        val acc2 = repository.createAccount("ICICI", "Savings", "4821", 5000000L)  // ₹50,000
        val transferCat = fakeCategoryDao.getByName("Transfer")!!

        val start = ExpenseTrackerRepository.getStartOfMonthTimestamp(2026, 11, kolkataZone)
        val end = ExpenseTrackerRepository.getEndOfMonthTimestamp(2026, 11, kolkataZone)

        // Transfer ₹20,000 from Acc1 to Acc2
        val transferAmount = 2000000L
        val txTime = start + 50000L

        // Debit on Acc1
        repository.createTransaction(
            accountId = acc1,
            amountPaise = transferAmount,
            type = TransactionType.DEBIT,
            categoryId = transferCat.id,
            timestamp = txTime,
            note = "Transfer to ICICI 4821"
        )

        // Credit on Acc2
        repository.createTransaction(
            accountId = acc2,
            amountPaise = transferAmount,
            type = TransactionType.CREDIT,
            categoryId = transferCat.id,
            timestamp = txTime,
            note = "Transfer from HDFC 7150"
        )

        val totals = repository.getMonthlyTotals(start, end)
        // Neither income nor expense should be inflated by inter-account transfer
        assertEquals(0L, totals.totalIncomePaise)
        assertEquals(0L, totals.totalExpensePaise)
        assertEquals(2, totals.transactionCount)

        // Total net assets should remain strictly invariant: 1,00,000 + 50,000 = ₹1,50,000.00
        assertEquals(15000000L, repository.getTotalNetAssets())
    }

    // =========================================================================
    // 5. Reclassifying Ambiguous Credits Reactively Updates Income
    // =========================================================================

    @Test
    fun reclassifyingTransaction_fromTransferToIncome_updatesTotalsImmediately() = runBlocking {
        repository.populateInitialDataIfEmpty()
        val accId = repository.createAccount("HDFC", "Primary", "7150", 125000L)
        val transferCat = fakeCategoryDao.getByName("Transfer")!!
        val incomeCat = fakeCategoryDao.getByName("Income")!!

        val start = ExpenseTrackerRepository.getStartOfMonthTimestamp(2026, 11, kolkataZone)
        val end = ExpenseTrackerRepository.getEndOfMonthTimestamp(2026, 11, kolkataZone)

        // Initially classified as Transfer (the bug that occurred)
        val txId = repository.createTransaction(
            accountId = accId,
            amountPaise = 50000L, // ₹500.00
            type = TransactionType.CREDIT,
            categoryId = transferCat.id,
            timestamp = start + 10000L,
            note = "Jobin"
        )

        // When Transfer: Monthly income is ₹0.00
        var totals = repository.getMonthlyTotals(start, end)
        assertEquals(0L, totals.totalIncomePaise)

        // User changes category in UI to "Income"
        repository.updateTransactionCategory(txId, incomeCat.id)

        // Monthly income is now immediately ₹500.00
        totals = repository.getMonthlyTotals(start, end)
        assertEquals(50000L, totals.totalIncomePaise)

        // User changes to "Other"
        val otherCat = fakeCategoryDao.getByName("Other")!!
        repository.updateTransactionCategory(txId, otherCat.id)
        totals = repository.getMonthlyTotals(start, end)
        assertEquals(50000L, totals.totalIncomePaise)

        // User changes back to "Transfer"
        repository.updateTransactionCategory(txId, transferCat.id)
        totals = repository.getMonthlyTotals(start, end)
        assertEquals(0L, totals.totalIncomePaise)
    }

    // =========================================================================
    // 6. Month Boundaries & Timezone Changes
    // =========================================================================

    @Test
    fun monthBoundaries_startAndEndOfMonth_areAccurateToTheMillisecond() {
        val startNov2026 = ExpenseTrackerRepository.getStartOfMonthTimestamp(2026, 11, kolkataZone)
        val endNov2026 = ExpenseTrackerRepository.getEndOfMonthTimestamp(2026, 11, kolkataZone)

        val zdtStart = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(startNov2026), kolkataZone)
        val zdtEnd = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(endNov2026), kolkataZone)

        assertEquals(2026, zdtStart.year)
        assertEquals(11, zdtStart.monthValue)
        assertEquals(1, zdtStart.dayOfMonth)
        assertEquals(0, zdtStart.hour)
        assertEquals(0, zdtStart.minute)
        assertEquals(0, zdtStart.second)

        assertEquals(2026, zdtEnd.year)
        assertEquals(11, zdtEnd.monthValue)
        assertEquals(30, zdtEnd.dayOfMonth) // November has 30 days
        assertEquals(23, zdtEnd.hour)
        assertEquals(59, zdtEnd.minute)
        assertEquals(59, zdtEnd.second)
    }

    @Test
    fun monthBoundaries_transactionsInDifferentMonths_areStrictlyIsolated() = runBlocking {
        repository.populateInitialDataIfEmpty()
        val accId = repository.createAccount("HDFC", "Primary", "7150", 0L)
        val incomeCat = fakeCategoryDao.getByName("Income")!!

        val startOct = ExpenseTrackerRepository.getStartOfMonthTimestamp(2026, 10, kolkataZone)
        val endOct = ExpenseTrackerRepository.getEndOfMonthTimestamp(2026, 10, kolkataZone)

        val startNov = ExpenseTrackerRepository.getStartOfMonthTimestamp(2026, 11, kolkataZone)
        val endNov = ExpenseTrackerRepository.getEndOfMonthTimestamp(2026, 11, kolkataZone)

        // Transaction in October: ₹300.00
        val octTime = ZonedDateTime.of(LocalDate.of(2026, 10, 31), LocalTime.of(23, 59, 59), kolkataZone)
            .toInstant().toEpochMilli()
        repository.createTransaction(accId, 30000L, TransactionType.CREDIT, incomeCat.id, octTime)

        // Transaction in November: ₹500.00 (The Jobin transaction: 4 Nov 2026, 3:25 AM)
        val novTime = ZonedDateTime.of(LocalDate.of(2026, 11, 4), LocalTime.of(3, 25, 0), kolkataZone)
            .toInstant().toEpochMilli()
        repository.createTransaction(accId, 50000L, TransactionType.CREDIT, incomeCat.id, novTime)

        // October totals: Only contains October transaction
        val octTotals = repository.getMonthlyTotals(startOct, endOct)
        assertEquals(30000L, octTotals.totalIncomePaise)
        assertEquals(1, octTotals.transactionCount)

        // November totals: Only contains November transaction
        val novTotals = repository.getMonthlyTotals(startNov, endNov)
        assertEquals(50000L, novTotals.totalIncomePaise)
        assertEquals(1, novTotals.transactionCount)
    }

    @Test
    fun monthBoundaries_timezoneSensitivity() {
        // Nov 1 02:00:00 IST is Oct 31 20:30:00 UTC
        val istNov1 = ZonedDateTime.of(LocalDate.of(2026, 11, 1), LocalTime.of(2, 0, 0), kolkataZone)
        val epochMillis = istNov1.toInstant().toEpochMilli()

        val startNovIst = ExpenseTrackerRepository.getStartOfMonthTimestamp(2026, 11, kolkataZone)
        val endNovIst = ExpenseTrackerRepository.getEndOfMonthTimestamp(2026, 11, kolkataZone)

        val startNovUtc = ExpenseTrackerRepository.getStartOfMonthTimestamp(2026, 11, utcZone)

        // In IST, the timestamp falls into November 2026
        assertTrue(epochMillis in startNovIst..endNovIst)
        // In UTC, the timestamp falls before November 2026 (it's Oct 31)
        assertTrue(epochMillis < startNovUtc)
    }

    // =========================================================================
    // 7. Opening Balance Separation From Monthly Income
    // =========================================================================

    @Test
    fun openingBalance_isExcludedFromMonthlyIncome_butIncludedInNetBalance() = runBlocking {
        repository.populateInitialDataIfEmpty()
        // Account with ₹1,250.00 opening balance
        val accId = repository.createAccount("HDFC", "Salary", "7150", 125000L)

        val start = ExpenseTrackerRepository.getStartOfMonthTimestamp(2026, 11, kolkataZone)
        val end = ExpenseTrackerRepository.getEndOfMonthTimestamp(2026, 11, kolkataZone)

        // With no transactions added yet:
        val totals = repository.getMonthlyTotals(start, end)
        assertEquals(0L, totals.totalIncomePaise)
        assertEquals(0L, totals.totalExpensePaise)

        // Total net assets reflects the opening balance
        assertEquals(125000L, repository.getTotalNetAssets())
    }

    // =========================================================================
    // 8. Financial Reconciliation & Exact User Scenario
    // =========================================================================

    @Test
    fun userExactScenario_financialConsistencyVerification() = runBlocking {
        // User scenario:
        // Account: HDFC ••7150
        // Opening balance: ₹1,250.00 (125,000 paise)
        // 7 Expenses totaling ₹836.00 (83,600 paise)
        // 1 Jobin Credit: ₹500.00 (50,000 paise)
        // Net balance: 125,000 - 83,600 + 50,000 = 91,400 paise (₹914.00)
        repository.populateInitialDataIfEmpty()
        val accId = repository.createAccount("HDFC Bank", "Primary", "7150", 125000L)

        val diningCat = fakeCategoryDao.getByName("Dining")!!
        val shoppingCat = fakeCategoryDao.getByName("Shopping")!!
        val incomeCat = fakeCategoryDao.getByName("Income")!!

        val startNov = ExpenseTrackerRepository.getStartOfMonthTimestamp(2026, 11, kolkataZone)
        val endNov = ExpenseTrackerRepository.getEndOfMonthTimestamp(2026, 11, kolkataZone)

        // 7 Expenses totaling ₹836.00
        val expenseAmounts = listOf(15000L, 20000L, 8600L, 12000L, 10000L, 9000L, 9000L) // Sum = 83,600 paise
        assertEquals(83600L, expenseAmounts.sum())

        for ((index, amt) in expenseAmounts.withIndex()) {
            repository.createTransaction(
                accountId = accId,
                amountPaise = amt,
                type = TransactionType.DEBIT,
                categoryId = if (index % 2 == 0) diningCat.id else shoppingCat.id,
                timestamp = startNov + (index + 1) * 3600000L
            )
        }

        // Jobin Credit of ₹500.00 (50,000 paise)
        val jobinTime = ZonedDateTime.of(LocalDate.of(2026, 11, 4), LocalTime.of(3, 25), kolkataZone)
            .toInstant().toEpochMilli()

        repository.createTransaction(
            accountId = accId,
            amountPaise = 50000L,
            type = TransactionType.CREDIT,
            categoryId = incomeCat.id,
            timestamp = jobinTime,
            note = "Jobin"
        )

        // Verify Totals
        val totals = repository.getMonthlyTotals(startNov, endNov)
        assertEquals(50000L, totals.totalIncomePaise)    // ₹500.00 Income
        assertEquals(83600L, totals.totalExpensePaise)   // ₹836.00 Expenses
        assertEquals(8, totals.transactionCount)

        // Verify Net Assets: Opening (₹1,250.00) - Expenses (₹836.00) + Income (₹500.00) = ₹914.00
        assertEquals(91400L, repository.getTotalNetAssets())
    }

    // =========================================================================
    // 9. UI Model Verification (typeLabel and isCredit)
    // =========================================================================

    @Test
    fun transactionWithDetails_typeLabel_distinguishesTransferIncomeExpense() {
        val incomeTx = TransactionWithDetails(
            id = 1L,
            accountId = 1L,
            amountPaise = 50000L,
            type = TransactionType.CREDIT,
            categoryId = 10L,
            timestamp = System.currentTimeMillis(),
            source = TransactionSource.SMS,
            status = TransactionStatus.CONFIRMED,
            reference = null,
            deduplicationKey = null,
            note = "Jobin",
            bankName = "HDFC Bank",
            accountNickname = "Primary",
            accountLastFourDigits = "7150",
            categoryName = "Income",
            categoryEmoji = null
        )
        assertTrue(incomeTx.isCredit)
        assertTrue(incomeTx.isIncome)
        assertFalse(incomeTx.isTransfer)
        assertEquals("Income", incomeTx.typeLabel)

        val transferTx = incomeTx.copy(categoryName = "Transfer")
        assertTrue(transferTx.isCredit)
        assertFalse(transferTx.isIncome)
        assertTrue(transferTx.isTransfer)
        assertEquals("Transfer", transferTx.typeLabel) // NOT Expense!

        val expenseTx = incomeTx.copy(type = TransactionType.DEBIT, categoryName = "Food")
        assertFalse(expenseTx.isCredit)
        assertFalse(expenseTx.isIncome)
        assertFalse(expenseTx.isTransfer)
        assertEquals("Expense", expenseTx.typeLabel)
    }

    @Test
    fun dateFormatter_monthYearHeader_formatsProperly() {
        val formatted = DateFormatter.formatMonthYear(
            timestamp = ZonedDateTime.of(LocalDate.of(2026, 11, 4), LocalTime.of(3, 25), kolkataZone).toInstant().toEpochMilli(),
            zoneId = kolkataZone,
            locale = Locale.ENGLISH
        )
        assertEquals("November 2026", formatted)
    }
}

// =============================================================================
// In-Memory Test DAOs Implementing the Exact Behavior of Room Database
// =============================================================================

class TestBankAccountDao : BankAccountDao {
    val accounts = mutableListOf<BankAccountEntity>()
    var transactionDaoRef: TestTransactionDao? = null

    override suspend fun insert(account: BankAccountEntity): Long {
        val id = if (account.id == 0L) (accounts.size + 1).toLong() else account.id
        val entity = account.copy(id = id)
        accounts.removeAll { it.id == id }
        accounts.add(entity)
        return id
    }

    override suspend fun insertAll(accounts: List<BankAccountEntity>): List<Long> = accounts.map { insert(it) }

    override suspend fun update(account: BankAccountEntity): Int {
        val idx = accounts.indexOfFirst { it.id == account.id }
        if (idx >= 0) {
            accounts[idx] = account
            return 1
        }
        return 0
    }

    override suspend fun delete(account: BankAccountEntity): Int = if (accounts.removeIf { it.id == account.id }) 1 else 0

    override suspend fun deleteById(accountId: Long): Int = if (accounts.removeIf { it.id == accountId }) 1 else 0

    override suspend fun getById(id: Long): BankAccountEntity? = accounts.firstOrNull { it.id == id }

    suspend fun getPrimary(): BankAccountEntity? = accounts.firstOrNull { it.isPrimary }

    override suspend fun getAll(): List<BankAccountEntity> = accounts.toList()

    override fun getAllAsFlow(): Flow<List<BankAccountEntity>> = emptyFlow()

    override fun getAccountsWithBalanceFlow(): Flow<List<AccountWithBalance>> = emptyFlow()

    suspend fun getAccountsWithBalance(): List<AccountWithBalance> {
        return accounts.map { acc ->
            val txs = transactionDaoRef?.transactions?.filter { it.accountId == acc.id && it.status != TransactionStatus.CANCELLED } ?: emptyList()
            val netDelta = txs.sumOf { if (it.type == TransactionType.CREDIT) it.amountPaise else -it.amountPaise }
            val calculated = acc.openingBalancePaise + netDelta
            val current = acc.reconciledBalancePaise ?: calculated
            AccountWithBalance(acc, current, calculated)
        }
    }

    override fun getTotalNetAssetsFlow(): Flow<Long> = emptyFlow()

    override suspend fun getTotalNetAssets(): Long {
        return getAccountsWithBalance().sumOf { it.currentBalancePaise }
    }

    override suspend fun count(): Int = accounts.size

    override suspend fun countTransactionsForAccount(accountId: Long): Int =
        transactionDaoRef?.transactions?.count { it.accountId == accountId } ?: 0

    override suspend fun updateReconciledBalance(accountId: Long, reconciledBalancePaise: Long?): Int {
        val acc = getById(accountId) ?: return 0
        update(acc.copy(reconciledBalancePaise = reconciledBalancePaise))
        return 1
    }

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
}

class TestCategoryDao : CategoryDao {
    val categories = mutableListOf<CategoryEntity>()

    override suspend fun insert(category: CategoryEntity): Long {
        val id = if (category.id == 0L) (categories.size + 1).toLong() else category.id
        val entity = category.copy(id = id)
        categories.removeAll { it.id == id }
        categories.add(entity)
        return id
    }

    override suspend fun insertAll(categories: List<CategoryEntity>): List<Long> = categories.map { insert(it) }

    override suspend fun update(category: CategoryEntity): Int {
        val idx = categories.indexOfFirst { it.id == category.id }
        if (idx >= 0) {
            categories[idx] = category
            return 1
        }
        return 0
    }

    override suspend fun delete(category: CategoryEntity): Int = if (categories.removeIf { it.id == category.id }) 1 else 0

    override suspend fun deleteAll(): Int {
        val s = categories.size
        categories.clear()
        return s
    }

    override suspend fun getById(id: Long): CategoryEntity? = categories.firstOrNull { it.id == id }

    override suspend fun getByName(name: String): CategoryEntity? =
        categories.firstOrNull { it.name.trim().equals(name.trim(), ignoreCase = true) }

    override fun getAllAsFlow(): Flow<List<CategoryEntity>> = emptyFlow()

    override suspend fun getAll(): List<CategoryEntity> = categories.toList()

    override suspend fun count(): Int = categories.size
}

class TestTransactionDao(
    private val categoryDao: CategoryDao,
    private val bankAccountDao: TestBankAccountDao
) : TransactionDao {

    init {
        bankAccountDao.transactionDaoRef = this
    }

    val transactions = mutableListOf<TransactionEntity>()

    override suspend fun insert(transaction: TransactionEntity): Long {
        val id = if (transaction.id == 0L) (transactions.size + 1).toLong() else transaction.id
        val entity = transaction.copy(id = id)
        transactions.removeAll { it.id == id }
        transactions.add(entity)
        return id
    }

    override suspend fun insertAll(transactions: List<TransactionEntity>): List<Long> = transactions.map { insert(it) }

    override suspend fun update(transaction: TransactionEntity): Int {
        val idx = transactions.indexOfFirst { it.id == transaction.id }
        if (idx >= 0) {
            transactions[idx] = transaction
            return 1
        }
        return 0
    }

    override suspend fun delete(transaction: TransactionEntity): Int = if (transactions.removeIf { it.id == transaction.id }) 1 else 0

    override suspend fun deleteById(id: Long): Int = if (transactions.removeIf { it.id == id }) 1 else 0

    override suspend fun getById(id: Long): TransactionEntity? = transactions.firstOrNull { it.id == id }

    override suspend fun getAll(): List<TransactionEntity> = transactions.toList()

    override suspend fun deleteAll(): Int {
        val s = transactions.size
        transactions.clear()
        return s
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

    override suspend fun findByDeduplicationKey(key: String): TransactionEntity? =
        transactions.firstOrNull { it.deduplicationKey == key }

    override fun getAllTransactionsWithDetailsFlow(): Flow<List<TransactionWithDetails>> = emptyFlow()

    override fun getRecentTransactionsWithDetailsFlow(limit: Int): Flow<List<TransactionWithDetails>> = emptyFlow()

    override suspend fun getRecentTransactionsWithDetails(limit: Int): List<TransactionWithDetails> = emptyList()

    override fun getTransactionsByAccountFlow(accountId: Long): Flow<List<TransactionWithDetails>> = emptyFlow()

    override fun getMonthlyTotalsFlow(startTimestamp: Long, endTimestamp: Long): Flow<MonthlyTotals> = emptyFlow<MonthlyTotals>()

    override suspend fun getMonthlyTotals(startTimestamp: Long, endTimestamp: Long): MonthlyTotals {
        val filtered = transactions.filter {
            it.timestamp in startTimestamp..endTimestamp && it.status != TransactionStatus.CANCELLED
        }
        val income = filtered.filter { tx ->
            if (tx.type != TransactionType.CREDIT) return@filter false
            val catName = tx.categoryId?.let { id -> runBlocking { categoryDao.getById(id) }?.name }
            catName == null || !catName.equals("Transfer", ignoreCase = true)
        }.sumOf { it.amountPaise }

        val expenses = filtered.filter { tx ->
            if (tx.type != TransactionType.DEBIT) return@filter false
            val catName = tx.categoryId?.let { id -> runBlocking { categoryDao.getById(id) }?.name }
            catName == null || !catName.equals("Transfer", ignoreCase = true)
        }.sumOf { it.amountPaise }

        return MonthlyTotals(
            totalIncomePaise = income,
            totalExpensePaise = expenses,
            transactionCount = filtered.size
        )
    }

    override suspend fun count(): Int = transactions.size
}
