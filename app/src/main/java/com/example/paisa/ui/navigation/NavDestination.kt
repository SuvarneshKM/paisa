package com.example.paisa.ui.navigation

import androidx.annotation.DrawableRes
import com.example.paisa.R

enum class NavDestination(
    val title: String,
    val topBarTitle: String,
    val topBarSubtitle: String,
    @param:DrawableRes val iconRes: Int
) {
    HOME(
        title = "Home",
        topBarTitle = "Overview",
        topBarSubtitle = "Overview",
        iconRes = R.drawable.ic_nav_home
    ),
    ACCOUNTS(
        title = "Accounts",
        topBarTitle = "Accounts",
        topBarSubtitle = "Active balances",
        iconRes = R.drawable.ic_nav_accounts
    ),
    HISTORY(
        title = "History",
        topBarTitle = "History",
        topBarSubtitle = "All transactions",
        iconRes = R.drawable.ic_nav_history
    )
}
