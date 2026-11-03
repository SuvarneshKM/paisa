package com.example.paisa.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.paisa.model.BankPresets

@Entity(tableName = "bank_accounts")
data class BankAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bankName: String,
    val nickname: String,
    val lastFourDigits: String,
    val openingBalancePaise: Long,
    val reconciledBalancePaise: Long? = null,
    val accountType: String = "SAVINGS",
    val isPrimary: Boolean = false,
    val bankIdentifier: String = "OTHER",
    val themeId: String = "DEFAULT",
    val createdAt: Long = System.currentTimeMillis()
) {
    val maskedAccountNumber: String
        get() = "•••• $lastFourDigits"

    val resolvedThemeId: String
        get() = if (themeId != "DEFAULT" && themeId.isNotBlank()) {
            themeId
        } else {
            BankPresets.findPresetByFreeText(bankName).defaultThemeId
        }
}
