package com.example.paisa.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.paisa.data.local.entity.TransactionEntity
import com.example.paisa.data.local.model.MonthlyTotals
import com.example.paisa.data.local.model.TransactionWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun update(transaction: TransactionEntity): Int

    @Delete
    suspend fun delete(transaction: TransactionEntity): Int

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM transactions")
    suspend fun deleteAll(): Int

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    suspend fun getAll(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("UPDATE transactions SET categoryId = :categoryId, status = 'CONFIRMED' WHERE id = :transactionId")
    suspend fun updateTransactionCategory(transactionId: Long, categoryId: Long?): Int

    @Query("SELECT * FROM transactions WHERE deduplicationKey = :key LIMIT 1")
    suspend fun findByDeduplicationKey(key: String): TransactionEntity?

    @Query("""
        SELECT 
            t.id AS id,
            t.accountId AS accountId,
            t.amountPaise AS amountPaise,
            t.type AS type,
            t.categoryId AS categoryId,
            t.timestamp AS timestamp,
            t.source AS source,
            t.status AS status,
            t.reference AS reference,
            t.deduplicationKey AS deduplicationKey,
            t.note AS note,
            b.bankName AS bankName,
            b.nickname AS accountNickname,
            b.lastFourDigits AS accountLastFourDigits,
            c.name AS categoryName,
            c.emoji AS categoryEmoji
        FROM transactions t
        INNER JOIN bank_accounts b ON t.accountId = b.id
        LEFT JOIN categories c ON t.categoryId = c.id
        ORDER BY t.timestamp DESC, t.id DESC
    """)
    fun getAllTransactionsWithDetailsFlow(): Flow<List<TransactionWithDetails>>

    @Query("""
        SELECT 
            t.id AS id,
            t.accountId AS accountId,
            t.amountPaise AS amountPaise,
            t.type AS type,
            t.categoryId AS categoryId,
            t.timestamp AS timestamp,
            t.source AS source,
            t.status AS status,
            t.reference AS reference,
            t.deduplicationKey AS deduplicationKey,
            t.note AS note,
            b.bankName AS bankName,
            b.nickname AS accountNickname,
            b.lastFourDigits AS accountLastFourDigits,
            c.name AS categoryName,
            c.emoji AS categoryEmoji
        FROM transactions t
        INNER JOIN bank_accounts b ON t.accountId = b.id
        LEFT JOIN categories c ON t.categoryId = c.id
        ORDER BY t.timestamp DESC, t.id DESC
        LIMIT :limit
    """)
    fun getRecentTransactionsWithDetailsFlow(limit: Int): Flow<List<TransactionWithDetails>>

    @Query("""
        SELECT 
            t.id AS id,
            t.accountId AS accountId,
            t.amountPaise AS amountPaise,
            t.type AS type,
            t.categoryId AS categoryId,
            t.timestamp AS timestamp,
            t.source AS source,
            t.status AS status,
            t.reference AS reference,
            t.deduplicationKey AS deduplicationKey,
            t.note AS note,
            b.bankName AS bankName,
            b.nickname AS accountNickname,
            b.lastFourDigits AS accountLastFourDigits,
            c.name AS categoryName,
            c.emoji AS categoryEmoji
        FROM transactions t
        INNER JOIN bank_accounts b ON t.accountId = b.id
        LEFT JOIN categories c ON t.categoryId = c.id
        ORDER BY t.timestamp DESC, t.id DESC
        LIMIT :limit
    """)
    suspend fun getRecentTransactionsWithDetails(limit: Int): List<TransactionWithDetails>

    @Query("""
        SELECT 
            t.id AS id,
            t.accountId AS accountId,
            t.amountPaise AS amountPaise,
            t.type AS type,
            t.categoryId AS categoryId,
            t.timestamp AS timestamp,
            t.source AS source,
            t.status AS status,
            t.reference AS reference,
            t.deduplicationKey AS deduplicationKey,
            t.note AS note,
            b.bankName AS bankName,
            b.nickname AS accountNickname,
            b.lastFourDigits AS accountLastFourDigits,
            c.name AS categoryName,
            c.emoji AS categoryEmoji
        FROM transactions t
        INNER JOIN bank_accounts b ON t.accountId = b.id
        LEFT JOIN categories c ON t.categoryId = c.id
        WHERE t.accountId = :accountId
        ORDER BY t.timestamp DESC, t.id DESC
    """)
    fun getTransactionsByAccountFlow(accountId: Long): Flow<List<TransactionWithDetails>>

    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN t.type = 'CREDIT' AND t.status != 'CANCELLED' AND (c.name IS NULL OR UPPER(c.name) != 'TRANSFER') THEN t.amountPaise ELSE 0 END), 0) AS totalIncomePaise,
            COALESCE(SUM(CASE WHEN t.type = 'DEBIT' AND t.status != 'CANCELLED' AND (c.name IS NULL OR UPPER(c.name) != 'TRANSFER') THEN t.amountPaise ELSE 0 END), 0) AS totalExpensePaise,
            COUNT(CASE WHEN t.status != 'CANCELLED' THEN 1 ELSE NULL END) AS transactionCount
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        WHERE t.timestamp >= :startTimestamp AND t.timestamp <= :endTimestamp
    """)
    fun getMonthlyTotalsFlow(startTimestamp: Long, endTimestamp: Long): Flow<MonthlyTotals>

    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN t.type = 'CREDIT' AND t.status != 'CANCELLED' AND (c.name IS NULL OR UPPER(c.name) != 'TRANSFER') THEN t.amountPaise ELSE 0 END), 0) AS totalIncomePaise,
            COALESCE(SUM(CASE WHEN t.type = 'DEBIT' AND t.status != 'CANCELLED' AND (c.name IS NULL OR UPPER(c.name) != 'TRANSFER') THEN t.amountPaise ELSE 0 END), 0) AS totalExpensePaise,
            COUNT(CASE WHEN t.status != 'CANCELLED' THEN 1 ELSE NULL END) AS transactionCount
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        WHERE t.timestamp >= :startTimestamp AND t.timestamp <= :endTimestamp
    """)
    suspend fun getMonthlyTotals(startTimestamp: Long, endTimestamp: Long): MonthlyTotals

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int
}
