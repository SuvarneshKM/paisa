package com.example.paisa.data.backup

import com.example.paisa.data.local.entity.BankAccountEntity
import com.example.paisa.data.local.entity.CategoryEntity
import com.example.paisa.data.local.entity.TransactionEntity
import com.example.paisa.data.local.entity.TransactionSource
import com.example.paisa.data.local.entity.TransactionStatus
import com.example.paisa.data.local.entity.TransactionType
import com.example.paisa.data.preferences.UserPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream

data class BackupSummary(
    val accountsCount: Int,
    val transactionsCount: Int,
    val categoriesCount: Int,
    val exportedAt: Long
)

data class BackupPreferences(
    val smsDetectionEnabled: Boolean,
    val notificationsEnabled: Boolean,
    val manualOnlyMode: Boolean
)

data class ParsedBackup(
    val version: Int,
    val exportedAt: Long,
    val appVersion: String,
    val accounts: List<BankAccountEntity>,
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val preferences: BackupPreferences? = null
)

/**
 * Offline, privacy-first Backup and Restore Manager.
 *
 * Encodes and decodes financial data to/from human-readable, portable JSON.
 * Operates strictly locally on-device via Storage Access Framework with zero network usage.
 */
object BackupManager {

    const val CURRENT_BACKUP_VERSION = 1
    const val CURRENT_APP_VERSION = "1.1"

    fun exportBackup(
        outputStream: OutputStream,
        accounts: List<BankAccountEntity>,
        categories: List<CategoryEntity>,
        transactions: List<TransactionEntity>,
        userPreferences: UserPreferences? = null
    ): BackupSummary {
        val root = JSONObject()
        val exportedAt = System.currentTimeMillis()

        root.put("version", CURRENT_BACKUP_VERSION)
        root.put("exportedAt", exportedAt)
        root.put("appVersion", CURRENT_APP_VERSION)
        root.put("appName", "Paisa")

        // 1. Categories
        val categoriesArray = JSONArray()
        for (cat in categories) {
            val obj = JSONObject().apply {
                put("id", cat.id)
                put("name", cat.name)
                put("emoji", cat.emoji)
                put("isDefault", cat.isDefault)
                put("displayOrder", cat.displayOrder)
            }
            categoriesArray.put(obj)
        }
        root.put("categories", categoriesArray)

        // 2. Bank Accounts
        val accountsArray = JSONArray()
        for (acc in accounts) {
            val obj = JSONObject().apply {
                put("id", acc.id)
                put("bankName", acc.bankName)
                put("nickname", acc.nickname)
                put("lastFourDigits", acc.lastFourDigits)
                put("openingBalancePaise", acc.openingBalancePaise)
                if (acc.reconciledBalancePaise != null) {
                    put("reconciledBalancePaise", acc.reconciledBalancePaise)
                } else {
                    put("reconciledBalancePaise", JSONObject.NULL)
                }
                put("accountType", acc.accountType)
                put("isPrimary", acc.isPrimary)
                put("bankIdentifier", acc.bankIdentifier)
                put("themeId", acc.themeId)
                put("createdAt", acc.createdAt)
            }
            accountsArray.put(obj)
        }
        root.put("accounts", accountsArray)

        // 3. Transactions
        val transactionsArray = JSONArray()
        for (tx in transactions) {
            val obj = JSONObject().apply {
                put("id", tx.id)
                put("accountId", tx.accountId)
                put("amountPaise", tx.amountPaise)
                put("type", tx.type.name)
                if (tx.categoryId != null) {
                    put("categoryId", tx.categoryId)
                } else {
                    put("categoryId", JSONObject.NULL)
                }
                put("timestamp", tx.timestamp)
                put("source", tx.source.name)
                put("status", tx.status.name)
                if (tx.reference != null) put("reference", tx.reference) else put("reference", JSONObject.NULL)
                if (tx.deduplicationKey != null) put("deduplicationKey", tx.deduplicationKey) else put("deduplicationKey", JSONObject.NULL)
                if (tx.note != null) put("note", tx.note) else put("note", JSONObject.NULL)
            }
            transactionsArray.put(obj)
        }
        root.put("transactions", transactionsArray)

        // 4. Preferences (optional)
        if (userPreferences != null) {
            val prefObj = JSONObject().apply {
                put("smsDetectionEnabled", userPreferences.smsDetectionEnabled)
                put("notificationsEnabled", userPreferences.notificationsEnabled)
                put("manualOnlyMode", userPreferences.manualOnlyMode)
            }
            root.put("preferences", prefObj)
        }

        val jsonString = root.toString(2)
        outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
        outputStream.flush()

        return BackupSummary(
            accountsCount = accounts.size,
            transactionsCount = transactions.size,
            categoriesCount = categories.size,
            exportedAt = exportedAt
        )
    }

    fun parseBackup(inputStream: InputStream): ParsedBackup {
        val jsonText = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(jsonText)

        val version = root.optInt("version", 1)
        val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())
        val appVersion = root.optString("appVersion", "1.0")

        // Parse Categories
        val categories = mutableListOf<CategoryEntity>()
        if (root.has("categories")) {
            val arr = root.getJSONArray("categories")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                categories.add(
                    CategoryEntity(
                        id = obj.optLong("id", 0L),
                        name = obj.getString("name"),
                        emoji = obj.optString("emoji", ""),
                        isDefault = obj.optBoolean("isDefault", false),
                        displayOrder = obj.optInt("displayOrder", i + 1)
                    )
                )
            }
        }

        // Parse Accounts
        val accounts = mutableListOf<BankAccountEntity>()
        if (root.has("accounts")) {
            val arr = root.getJSONArray("accounts")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val reconciled = if (obj.isNull("reconciledBalancePaise")) null else obj.optLong("reconciledBalancePaise")
                accounts.add(
                    BankAccountEntity(
                        id = obj.optLong("id", 0L),
                        bankName = obj.getString("bankName"),
                        nickname = obj.getString("nickname"),
                        lastFourDigits = obj.getString("lastFourDigits"),
                        openingBalancePaise = obj.getLong("openingBalancePaise"),
                        reconciledBalancePaise = reconciled,
                        accountType = obj.optString("accountType", "SAVINGS"),
                        isPrimary = obj.optBoolean("isPrimary", false),
                        bankIdentifier = obj.optString("bankIdentifier", "OTHER"),
                        themeId = obj.optString("themeId", "DEFAULT"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        // Parse Transactions
        val transactions = mutableListOf<TransactionEntity>()
        if (root.has("transactions")) {
            val arr = root.getJSONArray("transactions")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val catId = if (obj.isNull("categoryId")) null else obj.optLong("categoryId")
                val ref = if (obj.isNull("reference")) null else obj.optString("reference")
                val dedup = if (obj.isNull("deduplicationKey")) null else obj.optString("deduplicationKey")
                val note = if (obj.isNull("note")) null else obj.optString("note")

                val typeStr = obj.optString("type", "DEBIT")
                val type = try {
                    TransactionType.valueOf(typeStr)
                } catch (e: Exception) {
                    TransactionType.DEBIT
                }

                val sourceStr = obj.optString("source", "MANUAL")
                val source = try {
                    TransactionSource.valueOf(sourceStr)
                } catch (e: Exception) {
                    TransactionSource.MANUAL
                }

                val statusStr = obj.optString("status", "CONFIRMED")
                val status = try {
                    TransactionStatus.valueOf(statusStr)
                } catch (e: Exception) {
                    TransactionStatus.CONFIRMED
                }

                transactions.add(
                    TransactionEntity(
                        id = obj.optLong("id", 0L),
                        accountId = obj.getLong("accountId"),
                        amountPaise = obj.getLong("amountPaise"),
                        type = type,
                        categoryId = catId,
                        timestamp = obj.getLong("timestamp"),
                        source = source,
                        status = status,
                        reference = ref,
                        deduplicationKey = dedup,
                        note = note
                    )
                )
            }
        }

        // Parse Preferences
        var preferences: BackupPreferences? = null
        if (root.has("preferences")) {
            val prefObj = root.getJSONObject("preferences")
            preferences = BackupPreferences(
                smsDetectionEnabled = prefObj.optBoolean("smsDetectionEnabled", false),
                notificationsEnabled = prefObj.optBoolean("notificationsEnabled", prefObj.optBoolean("categoryNotificationsEnabled", false)),
                manualOnlyMode = prefObj.optBoolean("manualOnlyMode", false)
            )
        }

        return ParsedBackup(
            version = version,
            exportedAt = exportedAt,
            appVersion = appVersion,
            accounts = accounts,
            categories = categories,
            transactions = transactions,
            preferences = preferences
        )
    }
}
