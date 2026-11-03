package com.example.paisa.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paisa.data.local.entity.BankAccountEntity
import com.example.paisa.data.local.model.AccountWithBalance
import com.example.paisa.data.repository.ExpenseTrackerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class AccountValidationResult {
    object Success : AccountValidationResult()
    data class Error(val message: String) : AccountValidationResult()
}

data class AccountsUiState(
    val accounts: List<AccountWithBalance> = emptyList(),
    val totalNetAssetsPaise: Long = 0L,
    val isLoading: Boolean = false
)

class AccountsViewModel(
    private val repository: ExpenseTrackerRepository,
    private val appContext: Context? = null
) : ViewModel() {

    val uiState: StateFlow<AccountsUiState> = combine(
        repository.accountsWithBalance,
        repository.totalNetAssetsPaise
    ) { accounts, totalAssets ->
        AccountsUiState(
            accounts = accounts,
            totalNetAssetsPaise = totalAssets,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountsUiState(isLoading = true)
    )

    fun validateAndSaveAccount(
        id: Long = 0L,
        bankName: String,
        nickname: String,
        lastFourDigits: String,
        openingBalanceRupeesInput: String,
        accountType: String = "SAVINGS",
        isPrimary: Boolean = false,
        bankIdentifier: String = "OTHER",
        themeId: String = "DEFAULT",
        onComplete: (AccountValidationResult) -> Unit
    ) {
        val cleanBank = bankName.trim()
        if (cleanBank.isBlank()) {
            onComplete(AccountValidationResult.Error("Bank name is required"))
            return
        }

        val cleanNickname = nickname.trim()
        if (cleanNickname.isBlank()) {
            onComplete(AccountValidationResult.Error("Account nickname is required"))
            return
        }

        val cleanLastFour = lastFourDigits.trim()
        if (cleanLastFour.length != 4 || !cleanLastFour.all { it.isDigit() }) {
            onComplete(AccountValidationResult.Error("Last four digits must be exactly 4 numeric digits"))
            return
        }

        val balanceRupees = openingBalanceRupeesInput.trim().toDoubleOrNull()
        if (balanceRupees == null || balanceRupees < 0.0) {
            onComplete(AccountValidationResult.Error("Please enter a valid opening balance (>= 0)"))
            return
        }

        val openingBalancePaise = (balanceRupees * 100).toLong()

        viewModelScope.launch {
            try {
                if (id == 0L) {
                    repository.createAccount(
                        bankName = cleanBank,
                        nickname = cleanNickname,
                        lastFourDigits = cleanLastFour,
                        openingBalancePaise = openingBalancePaise,
                        accountType = accountType,
                        isPrimary = isPrimary,
                        bankIdentifier = bankIdentifier,
                        themeId = themeId
                    )
                } else {
                    val updated = BankAccountEntity(
                        id = id,
                        bankName = cleanBank,
                        nickname = cleanNickname,
                        lastFourDigits = cleanLastFour,
                        openingBalancePaise = openingBalancePaise,
                        accountType = accountType,
                        isPrimary = isPrimary,
                        bankIdentifier = bankIdentifier,
                        themeId = themeId
                    )
                    repository.updateAccount(updated)
                }
                onComplete(AccountValidationResult.Success)
            } catch (e: Exception) {
                onComplete(AccountValidationResult.Error("Failed to save account: ${e.localizedMessage}"))
            }
        }
    }

    fun reconcileBalance(
        accountId: Long,
        reconciledRupeesInput: String?,
        onComplete: (AccountValidationResult) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (reconciledRupeesInput.isNullOrBlank()) {
                    // Reset reconciliation, fall back to calculated balance
                    repository.reconcileAccount(accountId, null)
                    onComplete(AccountValidationResult.Success)
                } else {
                    val rupees = reconciledRupeesInput.trim().toDoubleOrNull()
                    if (rupees == null) {
                        onComplete(AccountValidationResult.Error("Please enter a valid statement balance"))
                        return@launch
                    }
                    val paise = (rupees * 100).toLong()
                    repository.reconcileAccount(accountId, paise)
                    onComplete(AccountValidationResult.Success)
                }
            } catch (e: Exception) {
                onComplete(AccountValidationResult.Error("Reconciliation failed: ${e.localizedMessage}"))
            }
        }
    }

    fun getTransactionCountForAccount(accountId: Long, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val count = repository.countTransactionsForAccount(accountId)
            onResult(count)
        }
    }

    fun deleteAccount(accountId: Long, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteAccount(accountId)
            onComplete()
        }
    }
}
