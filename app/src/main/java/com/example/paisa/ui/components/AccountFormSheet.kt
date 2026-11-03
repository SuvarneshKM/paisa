package com.example.paisa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paisa.data.local.model.AccountWithBalance
import com.example.paisa.model.BankPreset
import com.example.paisa.model.BankPresets
import com.example.paisa.model.BankThemeRegistry
import com.example.paisa.ui.viewmodel.AccountValidationResult

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AccountFormSheet(
    sheetState: SheetState,
    accountToEdit: AccountWithBalance? = null,
    onDismissRequest: () -> Unit,
    onSave: (
        id: Long,
        bankName: String,
        nickname: String,
        lastFourDigits: String,
        openingBalanceInput: String,
        accountType: String,
        isPrimary: Boolean,
        bankIdentifier: String,
        themeId: String,
        onComplete: (AccountValidationResult) -> Unit
    ) -> Unit,
    onDeleteRequest: (accountId: Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isEditMode = accountToEdit != null

    val initialPreset = remember(accountToEdit) {
        if (accountToEdit != null) {
            if (accountToEdit.account.bankIdentifier != "OTHER" && accountToEdit.account.bankIdentifier.isNotBlank()) {
                BankPresets.getPreset(accountToEdit.account.bankIdentifier)
            } else {
                BankPresets.findPresetByFreeText(accountToEdit.account.bankName)
            }
        } else {
            BankPresets.PRESET_HDFC
        }
    }

    var selectedPreset by remember(accountToEdit) { mutableStateOf(initialPreset) }
    var customBankName by remember(accountToEdit) {
        mutableStateOf(
            if (accountToEdit != null && initialPreset == BankPresets.PRESET_OTHER) {
                accountToEdit.account.bankName
            } else ""
        )
    }
    var selectedThemeId by remember(accountToEdit) {
        mutableStateOf(accountToEdit?.account?.resolvedThemeId ?: initialPreset.defaultThemeId)
    }

    var nickname by remember(accountToEdit) { mutableStateOf(accountToEdit?.account?.nickname ?: "") }
    var lastFourDigits by remember(accountToEdit) { mutableStateOf(accountToEdit?.account?.lastFourDigits ?: "") }
    var openingBalanceInput by remember(accountToEdit) {
        mutableStateOf(
            if (accountToEdit != null) {
                (accountToEdit.account.openingBalancePaise / 100.0).toString()
            } else {
                ""
            }
        )
    }
    var selectedType by remember(accountToEdit) { mutableStateOf(accountToEdit?.account?.accountType ?: "SAVINGS") }
    var isPrimary by remember(accountToEdit) { mutableStateOf(accountToEdit?.account?.isPrimary ?: false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val accountTypes = listOf("SAVINGS", "SALARY", "CURRENT", "CREDIT_CARD")

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant)
                    .width(36.dp)
                    .height(4.dp)
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header
            Text(
                text = if (isEditMode) "Edit Bank Account" else "Add Bank Account",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isEditMode) "Update bank theme, nickname, or balance" else "Select bank and enter account details",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live Card Preview
            val previewBankName = if (selectedPreset == BankPresets.PRESET_OTHER) {
                if (customBankName.isNotBlank()) customBankName else "Custom Bank"
            } else {
                selectedPreset.displayName
            }
            val previewPaise = (openingBalanceInput.toDoubleOrNull()?.let { (it * 100).toLong() }) ?: 0L

            Text(
                text = "CARD PREVIEW",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            RealisticBankCard(
                bankName = previewBankName,
                accountNickname = if (nickname.isNotBlank()) nickname else "Account Nickname",
                lastFourDigits = if (lastFourDigits.isNotBlank()) lastFourDigits else "••••",
                balancePaise = previewPaise,
                themeId = selectedThemeId,
                accountType = selectedType,
                isPrimary = isPrimary,
                isReconciled = accountToEdit?.account?.reconciledBalancePaise != null,
                showActions = false,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Bank Selector
            Text(
                text = "SELECT BANK",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Responsive FlowRow for direct bank badge selection
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BankPresets.ALL_PRESETS.forEach { preset ->
                    val isSelected = selectedPreset.identifier == preset.identifier
                    val presetTheme = BankThemeRegistry.getTheme(preset.defaultThemeId)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                selectedPreset = preset
                                selectedThemeId = preset.defaultThemeId
                                errorMessage = null
                            }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(presetTheme.accentColor)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = preset.shortName,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.5.sp
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.surface
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Custom / Other Bank Details
            if (selectedPreset == BankPresets.PRESET_OTHER) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "CUSTOM BANK NAME",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = customBankName,
                    onValueChange = {
                        customBankName = it
                        errorMessage = null
                    },
                    placeholder = { Text("e.g. Bank of Baroda, IndusInd, PNB") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "SELECT COLOR THEME",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BankThemeRegistry.CUSTOM_THEMES.forEach { customTheme ->
                        val isSelected = selectedThemeId == customTheme.themeId
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedThemeId = customTheme.themeId }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(customTheme.accentColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = customTheme.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.surface
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Account Nickname Input
            Text(
                text = "ACCOUNT NICKNAME",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = nickname,
                onValueChange = {
                    nickname = it
                    errorMessage = null
                },
                placeholder = { Text("e.g. Primary Salary, Spends, Savings") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Last 4 Digits & Opening Balance in Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Last 4 Digits
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LAST 4 DIGITS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = lastFourDigits,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                lastFourDigits = it
                                errorMessage = null
                            }
                        },
                        placeholder = { Text("4821") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }

                // Opening Balance in ₹
                Column(modifier = Modifier.weight(1.3f)) {
                    Text(
                        text = "OPENING BALANCE (₹)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = openingBalanceInput,
                        onValueChange = {
                            openingBalanceInput = it
                            errorMessage = null
                        },
                        placeholder = { Text("0.00") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Account Type Selector
            Text(
                text = "ACCOUNT TYPE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                accountTypes.forEach { type ->
                    val isSelected = selectedType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedType = type }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Is Primary Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .toggleable(
                        value = isPrimary,
                        onValueChange = { isPrimary = it },
                        role = Role.Switch
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Set as Primary Account",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Default account for manual & detected spends",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Switch(
                    checked = isPrimary,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.surface,
                        checkedTrackColor = MaterialTheme.colorScheme.onSurface,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Button(
                onClick = {
                    val finalBankName = if (selectedPreset == BankPresets.PRESET_OTHER) {
                        customBankName.trim()
                    } else {
                        selectedPreset.displayName
                    }

                    if (finalBankName.isBlank()) {
                        errorMessage = "Please enter bank name"
                        return@Button
                    }

                    onSave(
                        accountToEdit?.account?.id ?: 0L,
                        finalBankName,
                        nickname,
                        lastFourDigits,
                        openingBalanceInput,
                        selectedType,
                        isPrimary,
                        selectedPreset.identifier,
                        selectedThemeId
                    ) { result ->
                        when (result) {
                            is AccountValidationResult.Success -> onDismissRequest()
                            is AccountValidationResult.Error -> errorMessage = result.message
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text(
                    text = if (isEditMode) "Save Changes" else "Create Account",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            if (accountToEdit != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { onDeleteRequest(accountToEdit.account.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                ) {
                    Text(
                        text = "Delete Account",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal)
                    )
                }
            }
        }
    }
}
