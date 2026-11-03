package com.example.paisa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paisa.R
import com.example.paisa.data.local.entity.BankAccountEntity
import com.example.paisa.data.local.entity.TransactionSource
import com.example.paisa.data.local.entity.TransactionStatus
import com.example.paisa.data.local.entity.TransactionType
import com.example.paisa.data.local.model.AccountWithBalance
import com.example.paisa.data.local.model.TransactionWithDetails
import com.example.paisa.ui.components.RealisticBankCard
import com.example.paisa.ui.components.TransactionItem
import com.example.paisa.ui.theme.BankingGreen
import com.example.paisa.ui.theme.BankingGreenBorderDark
import com.example.paisa.ui.theme.BankingGreenBorderLight
import com.example.paisa.ui.theme.BankingGreenContainerDark
import com.example.paisa.ui.theme.BankingGreenContainerLight
import com.example.paisa.ui.theme.BankingGreenDark
import com.example.paisa.ui.theme.BankingRed
import com.example.paisa.ui.theme.BankingRedBorderDark
import com.example.paisa.ui.theme.BankingRedBorderLight
import com.example.paisa.ui.theme.BankingRedContainerDark
import com.example.paisa.ui.theme.BankingRedContainerLight
import com.example.paisa.ui.theme.BankingRedDark
import com.example.paisa.ui.theme.PaisaTheme
import com.example.paisa.ui.viewmodel.HomeUiState
import com.example.paisa.util.CurrencyFormatter

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onNavigateToHistory: () -> Unit,
    onTransactionClick: (TransactionWithDetails) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp)
    ) {
        item {
            // Premium Total Balance Hero Card
            TotalBalanceCard(
                totalBalancePaise = uiState.totalBalancePaise,
                monthlyIncomePaise = uiState.monthlyIncomePaise,
                monthlyExpensesPaise = uiState.monthlyExpensesPaise
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Horizontal Bank Account Carousel
        if (uiState.accounts.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Accounts",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${uiState.accounts.size} ACTIVE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                letterSpacing = 0.8.sp
                            ),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val carouselState = rememberLazyListState()
                LazyRow(
                    state = carouselState,
                    flingBehavior = rememberSnapFlingBehavior(lazyListState = carouselState),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(
                        items = uiState.accounts,
                        key = { it.account.id }
                    ) { accountWithBalance ->
                        RealisticBankCard(
                            accountWithBalance = accountWithBalance,
                            modifier = Modifier.width(310.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Accounts",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .padding(vertical = 24.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No bank accounts added",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add your bank accounts in the Accounts tab to track your balances",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Recent Transactions Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "See all",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    ),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onNavigateToHistory() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            val pendingReviewCount = uiState.recentTransactions.count { it.isReviewNeeded }
            if (pendingReviewCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                        .clickable { onNavigateToHistory() }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$pendingReviewCount transaction${if (pendingReviewCount > 1) "s" else ""} need review",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Review →",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Recent Transactions List
        if (uiState.recentTransactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .padding(vertical = 36.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No transactions yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap the + button below to log your first transaction",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        } else {
            itemsIndexed(
                items = uiState.recentTransactions,
                key = { _, tx -> tx.id }
            ) { index, transaction ->
                TransactionItem(
                    transaction = transaction,
                    onClick = { onTransactionClick(transaction) }
                )
                if (index < uiState.recentTransactions.size - 1) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Premium Total Net Balance Card with Income & Expense breakdown pills.
 */
@Composable
private fun TotalBalanceCard(
    totalBalancePaise: Long,
    monthlyIncomePaise: Long,
    monthlyExpensesPaise: Long,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    val incomeBg = if (isDark) BankingGreenContainerDark else BankingGreenContainerLight
    val incomeBorder = if (isDark) BankingGreenBorderDark else BankingGreenBorderLight
    val incomeColor = if (isDark) BankingGreenDark else BankingGreen

    val expenseBg = if (isDark) BankingRedContainerDark else BankingRedContainerLight
    val expenseBorder = if (isDark) BankingRedBorderDark else BankingRedBorderLight
    val expenseColor = if (isDark) BankingRedDark else BankingRed

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(20.dp)
    ) {
        Column {
            // Label
            Text(
                text = "TOTAL NET BALANCE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp
                ),
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Hero Balance Amount
            Text(
                text = CurrencyFormatter.formatPaise(totalBalancePaise),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Monthly Income and Expense Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Monthly Income
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(incomeBg)
                        .border(1.dp, incomeBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_arrow_income),
                                contentDescription = "Income",
                                tint = incomeColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "INCOME",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = incomeColor
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = CurrencyFormatter.formatPaiseWithSign(monthlyIncomePaise, isExpense = false),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = incomeColor,
                            maxLines = 1
                        )
                    }
                }

                // Monthly Expenses
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(expenseBg)
                        .border(1.dp, expenseBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_arrow_expense),
                                contentDescription = "Expense",
                                tint = expenseColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "EXPENSES",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = expenseColor
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = CurrencyFormatter.formatPaiseWithSign(monthlyExpensesPaise, isExpense = true),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = expenseColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Home Screen", showBackground = true)
@Composable
fun HomeScreenPreview() {
    PaisaTheme {
        HomeScreen(
            uiState = HomeUiState(
                totalBalancePaise = 24500000L,
                monthlyIncomePaise = 8500000L,
                monthlyExpensesPaise = 3240000L,
                accounts = listOf(
                    AccountWithBalance(
                        account = BankAccountEntity(
                            id = 1L,
                            bankName = "HDFC Bank",
                            nickname = "Primary Salary",
                            lastFourDigits = "4821",
                            openingBalancePaise = 15000000L,
                            bankIdentifier = "HDFC",
                            themeId = "HDFC",
                            isPrimary = true
                        ),
                        currentBalancePaise = 18500000L,
                        calculatedBalancePaise = 18500000L
                    )
                ),
                recentTransactions = emptyList()
            ),
            onNavigateToHistory = {}
        )
    }
}
