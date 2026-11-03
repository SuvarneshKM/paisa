package com.example.paisa.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.paisa.data.local.model.AccountWithBalance

/**
 * Reusable bank account card composable delegating to the realistic physical bank card renderer.
 */
@Composable
fun BankAccountCard(
    accountWithBalance: AccountWithBalance,
    onClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    onReconcileClick: (() -> Unit)? = null,
    isCarouselCard: Boolean = false,
    modifier: Modifier = Modifier
) {
    val sizeModifier = if (isCarouselCard) {
        Modifier.width(310.dp)
    } else {
        Modifier.fillMaxWidth()
    }

    RealisticBankCard(
        accountWithBalance = accountWithBalance,
        onClick = onClick,
        onEditClick = onEditClick,
        onReconcileClick = onReconcileClick,
        showActions = !isCarouselCard && (onEditClick != null || onReconcileClick != null),
        modifier = modifier.then(sizeModifier)
    )
}
