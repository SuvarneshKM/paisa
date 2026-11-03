package com.example.paisa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paisa.data.local.model.TransactionWithDetails
import com.example.paisa.data.repository.ExpenseTrackerRepository
import com.example.paisa.ui.screens.HistoryFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HistoryUiState(
    val selectedFilter: HistoryFilter = HistoryFilter.ALL,
    val transactions: List<TransactionWithDetails> = emptyList(),
    val filteredVolumePaise: Long = 0L,
    val isLoading: Boolean = false
)

class HistoryViewModel(
    private val repository: ExpenseTrackerRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(HistoryFilter.ALL)

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.allTransactions,
        _selectedFilter
    ) { transactions, filter ->
        val filtered = when (filter) {
            HistoryFilter.ALL -> transactions
            HistoryFilter.EXPENSES -> transactions.filter { it.isExpense }
            HistoryFilter.INCOME -> transactions.filter { it.isIncome }
            HistoryFilter.UNCATEGORIZED -> transactions.filter { it.isReviewNeeded }
        }
        val volume = filtered.sumOf { it.amountPaise }
        HistoryUiState(
            selectedFilter = filter,
            transactions = filtered,
            filteredVolumePaise = volume,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState(isLoading = true)
    )

    fun setFilter(filter: HistoryFilter) {
        _selectedFilter.value = filter
    }
}
