package com.example.paisa.data.repository

import androidx.room.withTransaction
import com.example.paisa.data.local.PaisaDatabase
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import com.example.paisa.util.DateFormatter

class ExpenseTrackerRepository(
    private val bankAccountDao: BankAccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val database: PaisaDatabase? = null
) {

    val accountsWithBalance: Flow<List<AccountWithBalance>> =
        bankAccountDao.getAccountsWithBalanceFlow()

    val totalNetAssetsPaise: Flow<Long> =
        bankAccountDao.getTotalNetAssetsFlow()

    val allTransactions: Flow<List<TransactionWithDetails>> =
        transactionDao.getAllTransactionsWithDetailsFlow()

    val recentTransactions: Flow<List<TransactionWithDetails>> =
        transactionDao.getRecentTransactionsWithDetailsFlow(limit = 5)

    val categories: Flow<List<CategoryEntity>> =
        categoryDao.getAllAsFlow()

    fun getMonthlyTotalsFlow(
        startTimestamp: Long = getStartOfMonthTimestamp(),
        endTimestamp: Long = getEndOfMonthTimestamp()
    ): Flow<MonthlyTotals> {
        return transactionDao.getMonthlyTotalsFlow(startTimestamp, endTimestamp)
    }

    suspend fun getTotalNetAssets(): Long = withContext(Dispatchers.IO) {
        bankAccountDao.getTotalNetAssets()
    }

    suspend fun getMonthlyTotals(
        startTimestamp: Long = getStartOfMonthTimestamp(),
        endTimestamp: Long = getEndOfMonthTimestamp()
    ): MonthlyTotals = withContext(Dispatchers.IO) {
        transactionDao.getMonthlyTotals(startTimestamp, endTimestamp)
    }

    suspend fun getRecentTransactions(limit: Int = 1): List<TransactionWithDetails> = withContext(Dispatchers.IO) {
        transactionDao.getRecentTransactionsWithDetails(limit)
    }

    suspend fun getAllAccounts(): List<BankAccountEntity> = withContext(Dispatchers.IO) {
        bankAccountDao.getAll()
    }

    suspend fun getAllCategories(): List<CategoryEntity> = withContext(Dispatchers.IO) {
        categoryDao.getAll()
    }

    suspend fun createAccount(
        bankName: String,
        nickname: String,
        lastFourDigits: String,
        openingBalancePaise: Long,
        reconciledBalancePaise: Long? = null,
        accountType: String = "SAVINGS",
        isPrimary: Boolean = false,
        bankIdentifier: String = "OTHER",
        themeId: String = "DEFAULT"
    ): Long = withContext(Dispatchers.IO) {
        if (isPrimary) {
            bankAccountDao.clearAllPrimaryAccounts()
        }
        val account = BankAccountEntity(
            bankName = bankName,
            nickname = nickname,
            lastFourDigits = lastFourDigits,
            openingBalancePaise = openingBalancePaise,
            reconciledBalancePaise = reconciledBalancePaise,
            accountType = accountType,
            isPrimary = isPrimary,
            bankIdentifier = bankIdentifier,
            themeId = themeId
        )
        bankAccountDao.insert(account)
    }

    suspend fun updateAccount(account: BankAccountEntity): Int = withContext(Dispatchers.IO) {
        if (account.isPrimary) {
            bankAccountDao.clearOtherPrimaryAccounts(account.id)
        }
        bankAccountDao.update(account)
    }

    suspend fun deleteAccount(accountId: Long): Int = withContext(Dispatchers.IO) {
        bankAccountDao.deleteById(accountId)
    }

    suspend fun countTransactionsForAccount(accountId: Long): Int = withContext(Dispatchers.IO) {
        bankAccountDao.countTransactionsForAccount(accountId)
    }

    suspend fun reconcileAccount(accountId: Long, reconciledBalancePaise: Long?): Int = withContext(Dispatchers.IO) {
        bankAccountDao.updateReconciledBalance(accountId, reconciledBalancePaise)
    }

    suspend fun deleteTransaction(transactionId: Long): Int = withContext(Dispatchers.IO) {
        transactionDao.deleteById(transactionId)
    }

    suspend fun createTransaction(
        accountId: Long,
        amountPaise: Long,
        type: TransactionType,
        categoryId: Long?,
        timestamp: Long = System.currentTimeMillis(),
        source: TransactionSource = TransactionSource.MANUAL,
        status: TransactionStatus = TransactionStatus.CONFIRMED,
        reference: String? = null,
        deduplicationKey: String? = null,
        note: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val safeTimestamp = DateFormatter.normalizeTimestamp(timestamp)
        val transaction = TransactionEntity(
            accountId = accountId,
            amountPaise = amountPaise,
            type = type,
            categoryId = categoryId,
            timestamp = safeTimestamp,
            source = source,
            status = status,
            reference = reference,
            deduplicationKey = deduplicationKey,
            note = note
        )
        transactionDao.insert(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity): Int = withContext(Dispatchers.IO) {
        val existing = transactionDao.getById(transaction.id)
        val safeTimestamp = if (transaction.timestamp > 0L) {
            DateFormatter.normalizeTimestamp(transaction.timestamp)
        } else {
            existing?.timestamp ?: System.currentTimeMillis()
        }
        transactionDao.update(transaction.copy(timestamp = safeTimestamp))
    }

    suspend fun findByDeduplicationKey(deduplicationKey: String): TransactionEntity? =
        withContext(Dispatchers.IO) {
            transactionDao.findByDeduplicationKey(deduplicationKey)
        }

    suspend fun getTransactionById(transactionId: Long): TransactionEntity? =
        withContext(Dispatchers.IO) {
            transactionDao.getById(transactionId)
        }

    suspend fun updateTransactionCategory(transactionId: Long, categoryId: Long?): Int =
        withContext(Dispatchers.IO) {
            transactionDao.updateTransactionCategory(transactionId, categoryId)
        }

    suspend fun getCategoryByName(name: String): CategoryEntity? =
        withContext(Dispatchers.IO) {
            categoryDao.getByName(name)
        }

    suspend fun getOrCreateCategory(name: String, emoji: String = ""): Long =
        withContext(Dispatchers.IO) {
            val existing = categoryDao.getByName(name)
            if (existing != null) {
                existing.id
            } else {
                createCategory(name = name, emoji = emoji)
            }
        }

    suspend fun createCategory(
        name: String,
        emoji: String = "",
        isDefault: Boolean = false,
        displayOrder: Int = 0
    ): Long = withContext(Dispatchers.IO) {
        val category = CategoryEntity(
            name = name,
            emoji = emoji,
            isDefault = isDefault,
            displayOrder = displayOrder
        )
        categoryDao.insert(category)
    }

    /**
     * Seeds initial database with default categories if categories are fresh and unpopulated.
     * No bank accounts or transactions are created, ensuring the app starts with a clean slate.
     */
    suspend fun populateInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val defaultCategories = listOf(
            CategoryEntity(name = "Groceries", emoji = "", isDefault = true, displayOrder = 1),
            CategoryEntity(name = "Dining", emoji = "", isDefault = true, displayOrder = 2),
            CategoryEntity(name = "Income", emoji = "", isDefault = true, displayOrder = 3),
            CategoryEntity(name = "Salary", emoji = "", isDefault = true, displayOrder = 4),
            CategoryEntity(name = "Shopping", emoji = "", isDefault = true, displayOrder = 5),
            CategoryEntity(name = "Transport", emoji = "", isDefault = true, displayOrder = 6),
            CategoryEntity(name = "Entertainment", emoji = "", isDefault = true, displayOrder = 7),
            CategoryEntity(name = "Fitness", emoji = "", isDefault = true, displayOrder = 8),
            CategoryEntity(name = "Freelance", emoji = "", isDefault = true, displayOrder = 9),
            CategoryEntity(name = "Utilities", emoji = "", isDefault = true, displayOrder = 10),
            CategoryEntity(name = "Travel", emoji = "", isDefault = true, displayOrder = 11),
            CategoryEntity(name = "Investments", emoji = "", isDefault = true, displayOrder = 12),
            CategoryEntity(name = "Refund", emoji = "", isDefault = true, displayOrder = 13),
            CategoryEntity(name = "Transfer", emoji = "", isDefault = true, displayOrder = 14),
            CategoryEntity(name = "Other", emoji = "", isDefault = true, displayOrder = 15)
        )

        if (categoryDao.count() == 0) {
            categoryDao.insertAll(defaultCategories)
            return@withContext
        }

        // Ensure key categories like Income and Refund exist even if app was initialized previously
        val existingNames = categoryDao.getAll().map { it.name.trim().lowercase() }.toSet()
        val toInsert = mutableListOf<CategoryEntity>()
        var nextOrder = (categoryDao.getAll().maxOfOrNull { it.displayOrder } ?: 12) + 1

        val essentialCategories = listOf(
            CategoryEntity(name = "Income", emoji = "", isDefault = true, displayOrder = 3),
            CategoryEntity(name = "Refund", emoji = "", isDefault = true, displayOrder = 13),
            CategoryEntity(name = "Other", emoji = "", isDefault = true, displayOrder = 15)
        )

        for (essential in essentialCategories) {
            if (essential.name.lowercase() !in existingNames) {
                toInsert.add(essential.copy(displayOrder = nextOrder++))
            }
        }
        if (toInsert.isNotEmpty()) {
            categoryDao.insertAll(toInsert)
        }
    }

    /**
     * Clears all bank accounts and transactions, leaving categories intact.
     */
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        if (database != null) {
            database.withTransaction {
                transactionDao.deleteAll()
                bankAccountDao.deleteAll()
            }
        } else {
            transactionDao.deleteAll()
            bankAccountDao.deleteAll()
        }
    }

    suspend fun getAllTransactionsRaw(): List<TransactionEntity> = withContext(Dispatchers.IO) {
        transactionDao.getAll()
    }

    suspend fun restoreDatabase(
        accounts: List<BankAccountEntity>,
        categories: List<CategoryEntity>,
        transactions: List<TransactionEntity>
    ) = withContext(Dispatchers.IO) {
        if (database != null) {
            database.withTransaction {
                transactionDao.deleteAll()
                bankAccountDao.deleteAll()
                categoryDao.deleteAll()

                categoryDao.insertAll(categories)
                bankAccountDao.insertAll(accounts)
                transactionDao.insertAll(transactions)
            }
        } else {
            transactionDao.deleteAll()
            bankAccountDao.deleteAll()
            categoryDao.deleteAll()

            categoryDao.insertAll(categories)
            bankAccountDao.insertAll(accounts)
            transactionDao.insertAll(transactions)
        }
    }

    companion object {
        fun getStartOfMonthTimestamp(
            zoneId: ZoneId = ZoneId.systemDefault()
        ): Long {
            val now = ZonedDateTime.now(zoneId)
            return now.toLocalDate()
                .withDayOfMonth(1)
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()
        }

        fun getEndOfMonthTimestamp(
            zoneId: ZoneId = ZoneId.systemDefault()
        ): Long {
            val now = ZonedDateTime.now(zoneId)
            val lastDay = now.toLocalDate().lengthOfMonth()
            return now.toLocalDate()
                .withDayOfMonth(lastDay)
                .atTime(LocalTime.MAX)
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli()
        }

        fun getStartOfMonthTimestamp(
            year: Int,
            month: Int,
            zoneId: ZoneId = ZoneId.systemDefault()
        ): Long {
            return LocalDate.of(year, month, 1)
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()
        }

        fun getEndOfMonthTimestamp(
            year: Int,
            month: Int,
            zoneId: ZoneId = ZoneId.systemDefault()
        ): Long {
            val date = LocalDate.of(year, month, 1)
            val lastDay = date.lengthOfMonth()
            return date.withDayOfMonth(lastDay)
                .atTime(LocalTime.MAX)
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli()
        }
    }
}
