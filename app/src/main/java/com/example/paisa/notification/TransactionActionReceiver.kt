package com.example.paisa.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.paisa.data.local.PaisaDatabase
import com.example.paisa.data.repository.ExpenseTrackerRepository
import com.example.paisa.sms.CategorySuggestionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TransactionActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null || intent.action != TransactionNotificationHelper.ACTION_CATEGORIZE) {
            return
        }

        val transactionId = intent.getLongExtra(TransactionNotificationHelper.EXTRA_TRANSACTION_ID, -1L)
        val categoryName = intent.getStringExtra(TransactionNotificationHelper.EXTRA_CATEGORY_NAME)
        val notificationId = intent.getIntExtra(TransactionNotificationHelper.EXTRA_NOTIFICATION_ID, -1)

        if (transactionId == -1L || categoryName.isNullOrBlank()) {
            return
        }

        // Durable asynchronous execution to withstand process death
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = PaisaDatabase.getInstance(context)
                val repository = ExpenseTrackerRepository(
                    bankAccountDao = database.bankAccountDao(),
                    categoryDao = database.categoryDao(),
                    transactionDao = database.transactionDao()
                )

                // Resolve category ID based on requested quick action name
                val categoryId = resolveCategoryId(repository, categoryName)

                // Persist category selection in Room database (sets status = CONFIRMED)
                repository.updateTransactionCategory(transactionId, categoryId)

                // Learn from confirmed manual category selection
                val tx = repository.getTransactionById(transactionId)
                if (tx != null) {
                    CategorySuggestionEngine.getInstance(context).learn(
                        merchantOrPayee = tx.note,
                        categoryName = categoryName,
                        amountPaise = tx.amountPaise
                    )
                }

                // Update or dismiss notification with confirmation
                if (notificationId != -1) {
                    TransactionNotificationHelper.updateNotificationAsCategorized(
                        context = context,
                        notificationId = notificationId,
                        categoryName = categoryName
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error categorizing transaction $transactionId: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun resolveCategoryId(
        repository: ExpenseTrackerRepository,
        categoryName: String
    ): Long {
        return when (categoryName.lowercase()) {
            "food" -> {
                // Map to existing Dining or Food
                val existing = repository.getCategoryByName("Dining")
                    ?: repository.getCategoryByName("Food")
                existing?.id ?: repository.getOrCreateCategory("Food")
            }
            "snacks" -> {
                val existing = repository.getCategoryByName("Snacks")
                    ?: repository.getCategoryByName("Dining")
                existing?.id ?: repository.getOrCreateCategory("Snacks")
            }
            "bills" -> {
                val existing = repository.getCategoryByName("Bills")
                    ?: repository.getCategoryByName("Utilities")
                existing?.id ?: repository.getOrCreateCategory("Bills")
            }
            "groceries" -> {
                val existing = repository.getCategoryByName("Groceries")
                existing?.id ?: repository.getOrCreateCategory("Groceries")
            }
            "shopping" -> {
                repository.getOrCreateCategory("Shopping")
            }
            "income" -> {
                // Map to existing Salary or Income
                val existing = repository.getCategoryByName("Salary")
                    ?: repository.getCategoryByName("Income")
                existing?.id ?: repository.getOrCreateCategory("Income")
            }
            "salary" -> {
                val existing = repository.getCategoryByName("Salary")
                    ?: repository.getCategoryByName("Income")
                existing?.id ?: repository.getOrCreateCategory("Salary")
            }
            "refund" -> {
                repository.getOrCreateCategory("Refund")
            }
            "transfer" -> {
                repository.getOrCreateCategory("Transfer")
            }
            else -> {
                val existing = repository.getCategoryByName(categoryName)
                existing?.id ?: repository.getOrCreateCategory(categoryName)
            }
        }
    }

    companion object {
        private const val TAG = "TxActionReceiver"
    }
}
