package com.example.paisa.data.backup

import com.example.paisa.data.local.entity.BankAccountEntity
import com.example.paisa.data.local.entity.CategoryEntity
import com.example.paisa.data.local.entity.TransactionEntity
import com.example.paisa.data.local.entity.TransactionSource
import com.example.paisa.data.local.entity.TransactionStatus
import com.example.paisa.data.local.entity.TransactionType
import com.example.paisa.data.preferences.UserPreferences
import com.example.paisa.data.repository.ExpenseTrackerRepository
import com.example.paisa.sms.FakeBankAccountDao
import com.example.paisa.sms.FakeCategoryDao
import com.example.paisa.sms.FakeTransactionDao
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class BackupManagerTest {

    private lateinit var fakeBankAccountDao: FakeBankAccountDao
    private lateinit var fakeCategoryDao: FakeCategoryDao
    private lateinit var fakeTransactionDao: FakeTransactionDao
    private lateinit var repository: ExpenseTrackerRepository

    private val sampleAccounts = listOf(
        BankAccountEntity(
            id = 1L,
            bankName = "HDFC Bank",
            nickname = "Primary Salary",
            lastFourDigits = "4821",
            openingBalancePaise = 5000000L,
            reconciledBalancePaise = 4955000L,
            accountType = "SALARY",
            isPrimary = true,
            bankIdentifier = "HDFC",
            themeId = "HDFC",
            createdAt = 1730000000000L
        ),
        BankAccountEntity(
            id = 2L,
            bankName = "ICICI Bank",
            nickname = "Daily Spends",
            lastFourDigits = "9104",
            openingBalancePaise = 2000000L,
            reconciledBalancePaise = null,
            accountType = "SAVINGS",
            isPrimary = false,
            bankIdentifier = "ICICI",
            themeId = "ICICI",
            createdAt = 1730000000000L
        )
    )

    private val sampleCategories = listOf(
        CategoryEntity(id = 10L, name = "Dining", emoji = "🍽️", isDefault = true, displayOrder = 1),
        CategoryEntity(id = 20L, name = "Groceries", emoji = "🛒", isDefault = true, displayOrder = 2),
        CategoryEntity(id = 30L, name = "Salary", emoji = "💼", isDefault = true, displayOrder = 3)
    )

    private val sampleTransactions = listOf(
        TransactionEntity(
            id = 101L,
            accountId = 1L,
            amountPaise = 45000L,
            type = TransactionType.DEBIT,
            categoryId = 10L,
            timestamp = 1730100000000L,
            source = TransactionSource.SMS,
            status = TransactionStatus.CONFIRMED,
            reference = "429381029381",
            deduplicationKey = "DEDUP1",
            note = "SWIGGY"
        ),
        TransactionEntity(
            id = 102L,
            accountId = 1L,
            amountPaise = 7500000L,
            type = TransactionType.CREDIT,
            categoryId = 30L,
            timestamp = 1730200000000L,
            source = TransactionSource.MANUAL,
            status = TransactionStatus.CONFIRMED,
            reference = null,
            deduplicationKey = null,
            note = "Monthly Salary"
        )
    )

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
    }

    @Test
    fun exportAndParse_roundTripPreservesAllEntitiesAndFieldsAccurately() {
        val out = ByteArrayOutputStream()
        val userPrefs = UserPreferences(
            onboardingCompleted = true,
            smsDetectionEnabled = true,
            notificationsEnabled = true,
            manualOnlyMode = false,
            userConsented = true
        )

        val summary = BackupManager.exportBackup(
            outputStream = out,
            accounts = sampleAccounts,
            categories = sampleCategories,
            transactions = sampleTransactions,
            userPreferences = userPrefs
        )

        assertEquals(2, summary.accountsCount)
        assertEquals(3, summary.categoriesCount)
        assertEquals(2, summary.transactionsCount)

        val jsonBytes = out.toByteArray()
        assertTrue(jsonBytes.isNotEmpty())

        val parsed = BackupManager.parseBackup(ByteArrayInputStream(jsonBytes))

        assertEquals(BackupManager.CURRENT_BACKUP_VERSION, parsed.version)
        assertEquals(2, parsed.accounts.size)
        assertEquals(3, parsed.categories.size)
        assertEquals(2, parsed.transactions.size)

        // Verify Account fields
        val acc1 = parsed.accounts.first { it.id == 1L }
        assertEquals("HDFC Bank", acc1.bankName)
        assertEquals("Primary Salary", acc1.nickname)
        assertEquals("4821", acc1.lastFourDigits)
        assertEquals(5000000L, acc1.openingBalancePaise)
        assertEquals(4955000L, acc1.reconciledBalancePaise)
        assertTrue(acc1.isPrimary)
        assertEquals("HDFC", acc1.bankIdentifier)

        val acc2 = parsed.accounts.first { it.id == 2L }
        assertNull(acc2.reconciledBalancePaise)

        // Verify Category fields
        val cat10 = parsed.categories.first { it.id == 10L }
        assertEquals("Dining", cat10.name)
        assertEquals(1, cat10.displayOrder)

        // Verify Transaction fields
        val tx101 = parsed.transactions.first { it.id == 101L }
        assertEquals(1L, tx101.accountId)
        assertEquals(45000L, tx101.amountPaise)
        assertEquals(TransactionType.DEBIT, tx101.type)
        assertEquals(10L, tx101.categoryId)
        assertEquals("429381029381", tx101.reference)
        assertEquals("SWIGGY", tx101.note)

        val tx102 = parsed.transactions.first { it.id == 102L }
        assertEquals(TransactionType.CREDIT, tx102.type)
        assertNull(tx102.reference)
        assertNull(tx102.deduplicationKey)

        // Verify Preferences
        assertNotNull(parsed.preferences)
        assertTrue(parsed.preferences!!.smsDetectionEnabled)
        assertTrue(parsed.preferences!!.notificationsEnabled)
        assertEquals(false, parsed.preferences!!.manualOnlyMode)
    }

    @Test
    fun restoreDatabase_replacesExistingDataWithBackupData() = runBlocking {
        // Seed initial stale data
        fakeBankAccountDao.insert(
            BankAccountEntity(
                id = 99L,
                bankName = "Old Bank",
                nickname = "Stale Account",
                lastFourDigits = "0000",
                openingBalancePaise = 100L
            )
        )
        fakeCategoryDao.insert(CategoryEntity(id = 99L, name = "Stale Cat"))
        fakeTransactionDao.insert(
            TransactionEntity(
                id = 999L,
                accountId = 99L,
                amountPaise = 100L,
                type = TransactionType.DEBIT,
                timestamp = 1000L
            )
        )

        assertEquals(1, fakeBankAccountDao.accounts.size)
        assertEquals(1, fakeCategoryDao.categories.size)
        assertEquals(1, fakeTransactionDao.transactions.size)

        // Restore from sample backup
        repository.restoreDatabase(
            accounts = sampleAccounts,
            categories = sampleCategories,
            transactions = sampleTransactions
        )

        // Stale records must be gone, replaced by sample records
        assertEquals(2, fakeBankAccountDao.accounts.size)
        assertEquals(3, fakeCategoryDao.categories.size)
        assertEquals(2, fakeTransactionDao.transactions.size)

        assertNull(fakeBankAccountDao.getById(99L))
        assertNotNull(fakeBankAccountDao.getById(1L))
        assertNotNull(fakeBankAccountDao.getById(2L))
        assertNotNull(fakeTransactionDao.getById(101L))
        assertNotNull(fakeTransactionDao.getById(102L))
    }

    @Test
    fun parseBackup_emptyDataset_parsesGracefullyWithoutCrashing() {
        val out = ByteArrayOutputStream()
        BackupManager.exportBackup(
            outputStream = out,
            accounts = emptyList(),
            categories = emptyList(),
            transactions = emptyList()
        )

        val parsed = BackupManager.parseBackup(ByteArrayInputStream(out.toByteArray()))
        assertEquals(0, parsed.accounts.size)
        assertEquals(0, parsed.categories.size)
        assertEquals(0, parsed.transactions.size)
        assertNull(parsed.preferences)
    }
}
