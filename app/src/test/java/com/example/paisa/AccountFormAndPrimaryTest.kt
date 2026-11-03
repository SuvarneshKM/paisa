package com.example.paisa

import com.example.paisa.data.repository.ExpenseTrackerRepository
import com.example.paisa.model.BankPresets
import com.example.paisa.model.BankThemeRegistry
import com.example.paisa.sms.FakeBankAccountDao
import com.example.paisa.sms.FakeCategoryDao
import com.example.paisa.sms.FakeTransactionDao
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AccountFormAndPrimaryTest {

    private lateinit var fakeBankAccountDao: FakeBankAccountDao
    private lateinit var fakeCategoryDao: FakeCategoryDao
    private lateinit var fakeTransactionDao: FakeTransactionDao
    private lateinit var repository: ExpenseTrackerRepository

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
    fun bankPresets_allSupportedBanksArePresentAndHaveValidThemes() {
        val expectedIdentifiers = listOf(
            "SBI", "HDFC", "AXIS", "ICICI", "KOTAK",
            "FEDERAL", "CANARA", "SIB", "IDFC", "OTHER"
        )

        val actualIdentifiers = BankPresets.ALL_PRESETS.map { it.identifier }
        assertEquals(expectedIdentifiers, actualIdentifiers)

        // Verify each preset has a valid registered theme
        for (preset in BankPresets.ALL_PRESETS) {
            val theme = BankThemeRegistry.getTheme(preset.defaultThemeId)
            assertNotNull(theme)
            assertEquals(preset.defaultThemeId, theme.themeId)
            assertNotNull(theme.accentColor)
        }
    }

    @Test
    fun primaryAccount_enforcesSinglePrimaryAccountOnCreation() = runBlocking {
        // Create Account 1 as primary
        val acc1Id = repository.createAccount(
            bankName = "HDFC Bank",
            nickname = "Salary",
            lastFourDigits = "4821",
            openingBalancePaise = 5000000L,
            isPrimary = true,
            bankIdentifier = "HDFC",
            themeId = "HDFC"
        )

        val acc1 = fakeBankAccountDao.getById(acc1Id)
        assertNotNull(acc1)
        assertTrue(acc1!!.isPrimary)

        // Create Account 2 as primary -> Account 1 must lose primary status
        val acc2Id = repository.createAccount(
            bankName = "ICICI Bank",
            nickname = "Daily Spends",
            lastFourDigits = "9104",
            openingBalancePaise = 2000000L,
            isPrimary = true,
            bankIdentifier = "ICICI",
            themeId = "ICICI"
        )

        val acc1After = fakeBankAccountDao.getById(acc1Id)
        val acc2After = fakeBankAccountDao.getById(acc2Id)

        assertNotNull(acc1After)
        assertNotNull(acc2After)
        assertFalse(acc1After!!.isPrimary)
        assertTrue(acc2After!!.isPrimary)
    }

    @Test
    fun primaryAccount_enforcesSinglePrimaryAccountOnUpdate() = runBlocking {
        // Create Account 1 as non-primary
        val acc1Id = repository.createAccount(
            bankName = "HDFC Bank",
            nickname = "Salary",
            lastFourDigits = "4821",
            openingBalancePaise = 5000000L,
            isPrimary = false,
            bankIdentifier = "HDFC",
            themeId = "HDFC"
        )

        // Create Account 2 as primary
        val acc2Id = repository.createAccount(
            bankName = "ICICI Bank",
            nickname = "Daily Spends",
            lastFourDigits = "9104",
            openingBalancePaise = 2000000L,
            isPrimary = true,
            bankIdentifier = "ICICI",
            themeId = "ICICI"
        )

        assertTrue(fakeBankAccountDao.getById(acc2Id)!!.isPrimary)
        assertFalse(fakeBankAccountDao.getById(acc1Id)!!.isPrimary)

        // Update Account 1 to primary -> Account 2 must become non-primary
        val acc1 = fakeBankAccountDao.getById(acc1Id)!!
        repository.updateAccount(acc1.copy(isPrimary = true))

        assertTrue(fakeBankAccountDao.getById(acc1Id)!!.isPrimary)
        assertFalse(fakeBankAccountDao.getById(acc2Id)!!.isPrimary)
    }

    @Test
    fun primaryAccount_addingNonPrimaryDoesNotAffectExistingPrimary() = runBlocking {
        // Create Account 1 as primary
        val acc1Id = repository.createAccount(
            bankName = "HDFC Bank",
            nickname = "Salary",
            lastFourDigits = "4821",
            openingBalancePaise = 5000000L,
            isPrimary = true,
            bankIdentifier = "HDFC",
            themeId = "HDFC"
        )

        // Create Account 2 as non-primary
        val acc2Id = repository.createAccount(
            bankName = "State Bank of India",
            nickname = "Emergency",
            lastFourDigits = "6231",
            openingBalancePaise = 10000000L,
            isPrimary = false,
            bankIdentifier = "SBI",
            themeId = "SBI"
        )

        assertTrue(fakeBankAccountDao.getById(acc1Id)!!.isPrimary)
        assertFalse(fakeBankAccountDao.getById(acc2Id)!!.isPrimary)
    }
}
