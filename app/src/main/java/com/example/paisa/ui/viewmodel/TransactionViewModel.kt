package com.example.paisa.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paisa.data.local.entity.CategoryEntity
import com.example.paisa.data.local.entity.TransactionSource
import com.example.paisa.data.local.entity.TransactionStatus
import com.example.paisa.data.local.entity.TransactionType
import com.example.paisa.data.local.model.AccountWithBalance
import com.example.paisa.data.repository.ExpenseTrackerRepository
import com.example.paisa.util.DateFormatter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class TransactionValidationResult {
    object Success : TransactionValidationResult()
    data class Error(val message: String) : TransactionValidationResult()
}

class TransactionViewModel(
    private val repository: ExpenseTrackerRepository,
    private val appContext: Context? = null
) : ViewModel() {

    val accounts: StateFlow<List<AccountWithBalance>> =
        repository.accountsWithBalance.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val categories: StateFlow<List<CategoryEntity>> =
        repository.categories.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun validateAndAddTransaction(
        accountId: Long?,
        amountRupeesInput: String,
        type: TransactionType,
        categoryId: Long?,
        timestamp: Long = System.currentTimeMillis(),
        note: String? = null,
        onComplete: (TransactionValidationResult) -> Unit
    ) {
        if (accountId == null || accountId <= 0) {
            onComplete(TransactionValidationResult.Error("Please select a bank account"))
            return
        }

        val amountRupees = amountRupeesInput.trim().toDoubleOrNull()
        if (amountRupees == null || amountRupees <= 0.0) {
            onComplete(TransactionValidationResult.Error("Please enter a valid positive amount"))
            return
        }

        if (categoryId == null || categoryId <= 0) {
            onComplete(TransactionValidationResult.Error("Please select a transaction category"))
            return
        }

        val amountPaise = (amountRupees * 100).toLong()
        val safeTimestamp = DateFormatter.normalizeTimestamp(timestamp)

        viewModelScope.launch {
            try {
                repository.createTransaction(
                    accountId = accountId,
                    amountPaise = amountPaise,
                    type = type,
                    categoryId = categoryId,
                    timestamp = safeTimestamp,
                    source = TransactionSource.MANUAL,
                    status = TransactionStatus.CONFIRMED,
                    reference = null,
                    deduplicationKey = null,
                    note = note?.trim()?.ifBlank { null }
                )
                onComplete(TransactionValidationResult.Success)
            } catch (e: Exception) {
                onComplete(TransactionValidationResult.Error("Failed to save transaction: ${e.localizedMessage}"))
            }
        }
    }

    fun validateAndUpdateTransaction(
        transactionId: Long,
        accountId: Long?,
        amountRupeesInput: String,
        type: TransactionType,
        categoryId: Long?,
        timestamp: Long? = null,
        note: String? = null,
        onComplete: (TransactionValidationResult) -> Unit
    ) {
        if (accountId == null || accountId <= 0) {
            onComplete(TransactionValidationResult.Error("Please select a bank account"))
            return
        }

        val amountRupees = amountRupeesInput.trim().toDoubleOrNull()
        if (amountRupees == null || amountRupees <= 0.0) {
            onComplete(TransactionValidationResult.Error("Please enter a valid positive amount"))
            return
        }

        if (categoryId == null || categoryId <= 0) {
            onComplete(TransactionValidationResult.Error("Please select a transaction category"))
            return
        }

        val amountPaise = (amountRupees * 100).toLong()

        viewModelScope.launch {
            try {
                val existing = repository.getTransactionById(transactionId)
                if (existing == null) {
                    onComplete(TransactionValidationResult.Error("Transaction not found"))
                    return@launch
                }

                // Preserve original timestamp if timestamp is not specified or non-positive
                val targetTimestamp = if (timestamp != null && timestamp > 0L) {
                    DateFormatter.normalizeTimestamp(timestamp)
                } else {
                    existing.timestamp
                }

                repository.updateTransaction(
                    existing.copy(
                        accountId = accountId,
                        amountPaise = amountPaise,
                        type = type,
                        categoryId = categoryId,
                        timestamp = targetTimestamp,
                        status = TransactionStatus.CONFIRMED,
                        note = note?.trim()?.ifBlank { null }
                    )
                )
                onComplete(TransactionValidationResult.Success)
            } catch (e: Exception) {
                onComplete(TransactionValidationResult.Error("Failed to update transaction: ${e.localizedMessage}"))
            }
        }
    }

    fun updateTransactionCategory(transactionId: Long, categoryId: Long?) {
        viewModelScope.launch {
            repository.updateTransactionCategory(transactionId, categoryId)
        }
    }

    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
        }
    }
}
