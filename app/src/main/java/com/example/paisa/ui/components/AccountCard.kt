package com.example.paisa.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.paisa.data.local.model.AccountWithBalance

@Composable
fun AccountCard(
    accountWithBalance: AccountWithBalance,
    onEditClick: (AccountWithBalance) -> Unit = {},
    onReconcileClick: (AccountWithBalance) -> Unit = {},
    modifier: Modifier = Modifier
) {
    RealisticBankCard(
        accountWithBalance = accountWithBalance,
        onEditClick = { onEditClick(accountWithBalance) },
        onReconcileClick = { onReconcileClick(accountWithBalance) },
        showActions = true,
        modifier = modifier.fillMaxWidth()
    )
}
