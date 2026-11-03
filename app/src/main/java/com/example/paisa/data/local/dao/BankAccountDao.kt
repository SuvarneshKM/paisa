package com.example.paisa.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.paisa.data.local.entity.BankAccountEntity
import com.example.paisa.data.local.model.AccountWithBalance
import kotlinx.coroutines.flow.Flow

@Dao
interface BankAccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: BankAccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<BankAccountEntity>): List<Long>

    @Update
    suspend fun update(account: BankAccountEntity): Int

    @Delete
    suspend fun delete(account: BankAccountEntity): Int

    @Query("DELETE FROM bank_accounts")
    suspend fun deleteAll(): Int

    @Query("SELECT * FROM bank_accounts WHERE id = :id")
    suspend fun getById(id: Long): BankAccountEntity?

    @Query("SELECT * FROM bank_accounts ORDER BY isPrimary DESC, nickname ASC")
    fun getAllAsFlow(): Flow<List<BankAccountEntity>>

    @Query("SELECT * FROM bank_accounts ORDER BY isPrimary DESC, nickname ASC")
    suspend fun getAll(): List<BankAccountEntity>

    @Query("""
        SELECT 
            b.id, b.bankName, b.nickname, b.lastFourDigits, b.openingBalancePaise, 
            b.reconciledBalancePaise, b.accountType, b.isPrimary, b.bankIdentifier, b.themeId, b.createdAt,
            COALESCE(
                b.reconciledBalancePaise,
                b.openingBalancePaise + COALESCE(
                    (SELECT SUM(CASE WHEN t.type = 'CREDIT' THEN t.amountPaise ELSE -t.amountPaise END)
                     FROM transactions t
                     WHERE t.accountId = b.id AND t.status != 'CANCELLED'), 0
                )
            ) AS currentBalancePaise,
            (b.openingBalancePaise + COALESCE(
                (SELECT SUM(CASE WHEN t.type = 'CREDIT' THEN t.amountPaise ELSE -t.amountPaise END)
                 FROM transactions t
                 WHERE t.accountId = b.id AND t.status != 'CANCELLED'), 0
            )) AS calculatedBalancePaise
        FROM bank_accounts b
        ORDER BY b.isPrimary DESC, b.nickname ASC
    """)
    fun getAccountsWithBalanceFlow(): Flow<List<AccountWithBalance>>

    @Query("""
        SELECT COALESCE(SUM(
            COALESCE(
                b.reconciledBalancePaise,
                b.openingBalancePaise + COALESCE(
                    (SELECT SUM(CASE WHEN t.type = 'CREDIT' THEN t.amountPaise ELSE -t.amountPaise END)
                     FROM transactions t
                     WHERE t.accountId = b.id AND t.status != 'CANCELLED'), 0
                )
            )
        ), 0)
        FROM bank_accounts b
    """)
    fun getTotalNetAssetsFlow(): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(
            COALESCE(
                b.reconciledBalancePaise,
                b.openingBalancePaise + COALESCE(
                    (SELECT SUM(CASE WHEN t.type = 'CREDIT' THEN t.amountPaise ELSE -t.amountPaise END)
                     FROM transactions t
                     WHERE t.accountId = b.id AND t.status != 'CANCELLED'), 0
                )
            )
        ), 0)
        FROM bank_accounts b
    """)
    suspend fun getTotalNetAssets(): Long

    @Query("SELECT COUNT(*) FROM bank_accounts")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :accountId")
    suspend fun countTransactionsForAccount(accountId: Long): Int

    @Query("UPDATE bank_accounts SET reconciledBalancePaise = :reconciledBalancePaise WHERE id = :accountId")
    suspend fun updateReconciledBalance(accountId: Long, reconciledBalancePaise: Long?): Int

    @Query("DELETE FROM bank_accounts WHERE id = :accountId")
    suspend fun deleteById(accountId: Long): Int

    @Query("UPDATE bank_accounts SET isPrimary = 0 WHERE id != :exceptAccountId")
    suspend fun clearOtherPrimaryAccounts(exceptAccountId: Long): Int

    @Query("UPDATE bank_accounts SET isPrimary = 0")
    suspend fun clearAllPrimaryAccounts(): Int
}
