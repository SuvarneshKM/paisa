package com.example.paisa.notification

import com.example.paisa.data.local.entity.BankAccountEntity
import com.example.paisa.data.local.entity.CategoryEntity
import com.example.paisa.data.local.entity.TransactionEntity
import com.example.paisa.data.local.entity.TransactionType
import com.example.paisa.data.repository.ExpenseTrackerRepository
import com.example.paisa.sms.FakeBankAccountDao
import com.example.paisa.sms.FakeCategoryDao
import com.example.paisa.sms.FakeTransactionDao
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TransactionNotificationTest {

    private lateinit var fakeBankAccountDao: FakeBankAccountDao
    private lateinit var fakeCategoryDao: FakeCategoryDao
    private lateinit var fakeTransactionDao: FakeTransactionDao
    private lateinit var repository: ExpenseTrackerRepository

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

        runBlocking {
            // Seed bank account
            fakeBankAccountDao.insert(
                BankAccountEntity(
                    id = 1L,
                    bankName = "HDFC Bank",
                    nickname = "Salary Account",
                    lastFourDigits = "4821",
                    openingBalancePaise = 5000000L,
                    isPrimary = true
                )
            )

            // Seed initial categories
            fakeCategoryDao.insert(CategoryEntity(id = 10L, name = "Dining", emoji = "☕"))
            fakeCategoryDao.insert(CategoryEntity(id = 20L, name = "Groceries", emoji = "🛒"))
            fakeCategoryDao.insert(CategoryEntity(id = 30L, name = "Salary", emoji = "💼"))
            fakeCategoryDao.insert(CategoryEntity(id = 40L, name = "Shopping", emoji = "🛍️"))

            // Seed initial un-categorized debit transaction
            fakeTransactionDao.insert(
                TransactionEntity(
                    id = 101L,
                    accountId = 1L,
                    amountPaise = 75000L, // ₹750.00
                    type = TransactionType.DEBIT,
                    categoryId = null,
                    timestamp = System.currentTimeMillis(),
                    note = "Cafe Coffee Day"
                )
            )

            // Seed initial un-categorized credit transaction
            fakeTransactionDao.insert(
                TransactionEntity(
                    id = 202L,
                    accountId = 1L,
                    amountPaise = 250000L, // ₹2,500.00
                    type = TransactionType.CREDIT,
                    categoryId = null,
                    timestamp = System.currentTimeMillis(),
                    note = "Payment Received"
                )
            )
        }
    }

    @Test
    fun maskAccountDigits_masksSafelyWithoutExposingFullNumber() {
        assertEquals("•••• 4821", TransactionNotificationHelper.maskAccountDigits("4821"))
        assertEquals("•••• 0000", TransactionNotificationHelper.maskAccountDigits("0000"))
        assertEquals("•••• 9104", TransactionNotificationHelper.maskAccountDigits(" 9104 "))
        assertEquals("••••", TransactionNotificationHelper.maskAccountDigits(""))
        assertEquals("••••", TransactionNotificationHelper.maskAccountDigits("   "))
    }

    @Test
    fun getNotificationId_isPositiveAndIndependentPerTransaction() {
        val notifId1 = TransactionNotificationHelper.getNotificationId(101L)
        val notifId2 = TransactionNotificationHelper.getNotificationId(202L)
        val notifIdLarge = TransactionNotificationHelper.getNotificationId(9876543210L)

        assertTrue("Notification ID must be positive", notifId1 > 0)
        assertTrue("Notification ID must be positive", notifId2 > 0)
        assertTrue("Notification ID must be positive", notifIdLarge > 0)
        assertNotEquals(notifId1, notifId2)
    }

    @Test
    fun getActionRequestCode_generatesUniqueCodesPerActionAndTransaction() {
        val tx1 = 101L
        val tx2 = 202L

        val tx1Suggestion1 = TransactionNotificationHelper.getActionRequestCode(tx1, TransactionNotificationHelper.CODE_SUGGESTION_1)
        val tx1Suggestion2 = TransactionNotificationHelper.getActionRequestCode(tx1, TransactionNotificationHelper.CODE_SUGGESTION_2)
        val tx1More = TransactionNotificationHelper.getActionRequestCode(tx1, TransactionNotificationHelper.CODE_MORE)
        val tx1Content = TransactionNotificationHelper.getActionRequestCode(tx1, TransactionNotificationHelper.CODE_CONTENT)

        val tx2Suggestion1 = TransactionNotificationHelper.getActionRequestCode(tx2, TransactionNotificationHelper.CODE_SUGGESTION_1)
        val tx2Suggestion2 = TransactionNotificationHelper.getActionRequestCode(tx2, TransactionNotificationHelper.CODE_SUGGESTION_2)

        // All action request codes for tx1 must be distinct
        val tx1Codes = setOf(tx1Suggestion1, tx1Suggestion2, tx1More, tx1Content)
        assertEquals(4, tx1Codes.size)

        // Same action on different transactions must have distinct request codes
        assertNotEquals(tx1Suggestion1, tx2Suggestion1)
        assertNotEquals(tx1Suggestion1, tx2Suggestion2)

        // All codes must be valid positive 31-bit integers
        assertTrue(tx1Suggestion1 >= 0)
        assertTrue(tx1Suggestion2 >= 0)
        assertTrue(tx1More >= 0)
        assertTrue(tx1Content >= 0)
    }

    @Test
    fun vectorIcons_forNotificationSuggestions_areValidDrawables() {
        val categories = listOf("Snacks", "Food", "Groceries", "Shopping", "Bills", "Salary", "Refund", "Transfer", "Other")
        for (category in categories) {
            val iconRes = com.example.paisa.model.CategoryIconRegistry.getIcon(category)
            assertTrue("Icon for $category must be a valid resource ID > 0", iconRes > 0)
        }
    }

    @Test
    fun quickAction_food_mapsToExistingDiningOrFoodCategory() = runBlocking {
        // Initial transaction has no category
        assertNull(fakeTransactionDao.getById(101L)?.categoryId)

        // Simulate resolution in TransactionActionReceiver for "Food"
        val existing = repository.getCategoryByName("Dining") ?: repository.getCategoryByName("Food")
        val categoryId = existing?.id ?: repository.getOrCreateCategory("Food", "🍽️")

        assertEquals(10L, categoryId) // Found existing Dining (id 10)

        // Persist update
        val rows = repository.updateTransactionCategory(101L, categoryId)
        assertEquals(1, rows)

        val updatedTx = fakeTransactionDao.getById(101L)
        assertEquals(10L, updatedTx?.categoryId)
    }

    @Test
    fun quickAction_shopping_mapsToShoppingCategory() = runBlocking {
        val categoryId = repository.getOrCreateCategory("Shopping", "🛍️")
        assertEquals(40L, categoryId) // Matches seeded Shopping category

        val rows = repository.updateTransactionCategory(101L, categoryId)
        assertEquals(1, rows)

        val updatedTx = fakeTransactionDao.getById(101L)
        assertEquals(40L, updatedTx?.categoryId)
    }

    @Test
    fun quickAction_income_mapsToExistingSalaryOrIncomeCategory() = runBlocking {
        val existing = repository.getCategoryByName("Salary") ?: repository.getCategoryByName("Income")
        val categoryId = existing?.id ?: repository.getOrCreateCategory("Income", "💼")

        assertEquals(30L, categoryId) // Found existing Salary (id 30)

        val rows = repository.updateTransactionCategory(202L, categoryId)
        assertEquals(1, rows)

        val updatedTx = fakeTransactionDao.getById(202L)
        assertEquals(30L, updatedTx?.categoryId)
    }

    @Test
    fun quickAction_refund_createsCategoryIfAbsentAndUpdatesTransaction() = runBlocking {
        // Refund was not in initial seeded categories
        assertNull(repository.getCategoryByName("Refund"))

        val categoryId = repository.getOrCreateCategory("Refund", "↩️")
        assertTrue(categoryId > 0)

        val rows = repository.updateTransactionCategory(202L, categoryId)
        assertEquals(1, rows)

        val updatedTx = fakeTransactionDao.getById(202L)
        assertEquals(categoryId, updatedTx?.categoryId)

        val createdCat = repository.getCategoryByName("Refund")
        assertNotNull(createdCat)
        assertEquals("↩️", createdCat?.emoji)
    }

    @Test
    fun quickAction_transfer_createsCategoryIfAbsentAndUpdatesTransaction() = runBlocking {
        assertNull(repository.getCategoryByName("Transfer"))

        val categoryId = repository.getOrCreateCategory("Transfer", "🔄")
        assertTrue(categoryId > 0)

        val rows = repository.updateTransactionCategory(202L, categoryId)
        assertEquals(1, rows)

        val updatedTx = fakeTransactionDao.getById(202L)
        assertEquals(categoryId, updatedTx?.categoryId)
    }

    @Test
    fun repeatedTaps_idempotentExecutionDoesNotCorruptDatabase() = runBlocking {
        val categoryId = repository.getOrCreateCategory("Shopping", "🛍️")

        // First tap
        val rows1 = repository.updateTransactionCategory(101L, categoryId)
        assertEquals(1, rows1)
        assertEquals(categoryId, fakeTransactionDao.getById(101L)?.categoryId)

        // Second tap (repeated tap)
        val rows2 = repository.updateTransactionCategory(101L, categoryId)
        assertEquals(1, rows2)
        assertEquals(categoryId, fakeTransactionDao.getById(101L)?.categoryId)

        // Transaction count remains unchanged
        assertEquals(2, fakeTransactionDao.transactions.size)
    }

    @Test
    fun nonExistentTransaction_handledGracefullyWithoutCrashing() = runBlocking {
        val nonExistentId = 99999L
        val categoryId = 10L

        val rows = repository.updateTransactionCategory(nonExistentId, categoryId)
        assertEquals(0, rows) // 0 rows updated, no crash or exception
    }
}
