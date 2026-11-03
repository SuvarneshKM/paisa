package com.example.paisa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paisa.data.local.model.AccountWithBalance
import com.example.paisa.data.local.model.TransactionWithDetails
import com.example.paisa.data.repository.ExpenseTrackerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val totalBalancePaise: Long = 0L,
    val monthlyIncomePaise: Long = 0L,
    val monthlyExpensesPaise: Long = 0L,
    val accounts: List<AccountWithBalance> = emptyList(),
    val recentTransactions: List<TransactionWithDetails> = emptyList(),
    val isLoading: Boolean = false
)

class HomeViewModel(
    private val repository: ExpenseTrackerRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            repository.populateInitialDataIfEmpty()
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        repository.totalNetAssetsPaise,
        repository.getMonthlyTotalsFlow(),
        repository.recentTransactions,
        repository.accountsWithBalance
    ) { totalBalance, monthlyTotals, recentTxs, accounts ->
        HomeUiState(
            totalBalancePaise = totalBalance,
            monthlyIncomePaise = monthlyTotals.totalIncomePaise,
            monthlyExpensesPaise = monthlyTotals.totalExpensePaise,
            accounts = accounts,
            recentTransactions = recentTxs,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )
}
