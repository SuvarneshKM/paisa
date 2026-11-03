package com.example.paisa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paisa.data.local.model.TransactionWithDetails
import com.example.paisa.model.CategoryIconRegistry
import com.example.paisa.ui.theme.BankingGreen
import com.example.paisa.ui.theme.BankingGreenDark
import com.example.paisa.ui.theme.BankingRed
import com.example.paisa.ui.theme.BankingRedDark
import com.example.paisa.util.CurrencyFormatter
import com.example.paisa.util.DateFormatter

/**
 * Premium, production-ready transaction row item.
 * Adheres to 8dp spacing grid, high contrast, non-clipping typography,
 * and restrained financial green/red amount indicators.
 */
@Composable
fun TransactionItem(
    transaction: TransactionWithDetails,
    modifier: Modifier = Modifier,
    showAccount: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()

    val amountColor = if (transaction.isCredit) {
        if (isDark) BankingGreenDark else BankingGreen
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category Icon Box (44dp with 12dp corner radius and accessible theme contrast)
        val visual = CategoryIconRegistry.getVisualForCategory(transaction.categoryName, transaction.categoryEmoji)
        val containerColor = visual.containerColor(isDark)
        val iconColor = visual.iconColor(isDark)

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(containerColor)
                .border(
                    width = 1.dp,
                    color = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = visual.iconRes),
                contentDescription = transaction.categoryName ?: "Transaction category",
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Transaction Title & Metadata Column (flexible weight, never clips)
        Column(
            modifier = Modifier.weight(1f, fill = true),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = transaction.displayTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            val subtitleText = if (showAccount) {
                "${transaction.accountNickname} • ${DateFormatter.formatTime(transaction.timestamp)}"
            } else {
                "${transaction.categoryName ?: "General"} • ${DateFormatter.formatTime(transaction.timestamp)}"
            }

            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp
                ),
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Amount & Status (right-aligned, fixed fit)
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            val formattedAmount = CurrencyFormatter.formatPaiseWithSign(
                paise = transaction.amountPaise,
                isExpense = transaction.isExpense
            )

            Text(
                text = formattedAmount,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = (-0.2).sp
                ),
                color = amountColor,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (transaction.isReviewNeeded) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(0.75.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "REVIEW",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Text(
                    text = transaction.typeLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
