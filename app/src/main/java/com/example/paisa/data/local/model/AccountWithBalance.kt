package com.example.paisa.data.local.model

import androidx.room.Embedded
import com.example.paisa.data.local.entity.BankAccountEntity

data class AccountWithBalance(
    @Embedded val account: BankAccountEntity,
    val currentBalancePaise: Long,
    val calculatedBalancePaise: Long
)
