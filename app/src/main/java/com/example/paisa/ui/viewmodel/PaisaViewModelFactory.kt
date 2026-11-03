package com.example.paisa.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.paisa.data.local.PaisaDatabase
import com.example.paisa.data.repository.ExpenseTrackerRepository

class PaisaViewModelFactory(
    private val repository: ExpenseTrackerRepository,
    private val appContext: Context? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(repository) as T
            }
            modelClass.isAssignableFrom(AccountsViewModel::class.java) -> {
                AccountsViewModel(repository, appContext) as T
            }
            modelClass.isAssignableFrom(HistoryViewModel::class.java) -> {
                HistoryViewModel(repository) as T
            }
            modelClass.isAssignableFrom(TransactionViewModel::class.java) -> {
                TransactionViewModel(repository, appContext) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    companion object {
        fun create(context: Context): PaisaViewModelFactory {
            val database = PaisaDatabase.getInstance(context)
            val repository = ExpenseTrackerRepository(
                bankAccountDao = database.bankAccountDao(),
                categoryDao = database.categoryDao(),
                transactionDao = database.transactionDao()
            )
            return PaisaViewModelFactory(repository, context.applicationContext)
        }
    }
}
