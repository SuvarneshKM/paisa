package com.example.paisa.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingScreen(
    isSmsPermissionGranted: Boolean,
    isNotificationPermissionGranted: Boolean,
    onRequestSmsPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onCompleteOnboarding: (smsEnabled: Boolean, notificationsEnabled: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Both features are strictly OFF by default
    var wantsSmsDetection by remember { mutableStateOf(false) }
    var wantsNotifications by remember { mutableStateOf(false) }

    var smsPermissionRequestedOnce by remember { mutableStateOf(false) }
    var notifPermissionRequestedOnce by remember { mutableStateOf(false) }

    // Synchronize UI state with actual runtime permission result
    LaunchedEffect(isSmsPermissionGranted) {
        if (smsPermissionRequestedOnce) {
            wantsSmsDetection = isSmsPermissionGranted
        }
    }

    LaunchedEffect(isNotificationPermissionGranted) {
        if (notifPermissionRequestedOnce) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                wantsNotifications = isNotificationPermissionGranted
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
            .padding(top = 40.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // App Branding & Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "₹",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.background
                        )
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "PAISA",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Privacy-first offline expense tracker",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Privacy & Offline Guarantee Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "100% OFFLINE ARCHITECTURE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Zero internet permission. Zero cloud accounts. Paisa runs purely on your device. We never ask for full card numbers, PINs, or bank passwords.",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "OPTIONAL PRIVACY FEATURES",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Both features are disabled by default. You can opt in now or change anytime in Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Feature 1: Automatic SMS Detection
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Bank SMS Detection",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "OPTIONAL",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Reads incoming debit/credit alerts locally to record spends without manual entry. OTPs and personal chats are strictly ignored.",
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Switch(
                            checked = wantsSmsDetection && isSmsPermissionGranted,
                            onCheckedChange = { shouldEnable ->
                                if (shouldEnable) {
                                    smsPermissionRequestedOnce = true
                                    if (isSmsPermissionGranted) {
                                        wantsSmsDetection = true
                                    } else {
                                        onRequestSmsPermission()
                                    }
                                } else {
                                    wantsSmsDetection = false
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.surface,
                                checkedTrackColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    if (smsPermissionRequestedOnce && !isSmsPermissionGranted) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "SMS permission denied. Manual expense tracking remains 100% functional.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Feature 2: Category Notifications
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Category Quick Actions",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "OPTIONAL",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Shows notification buttons (Food, Shopping, etc.) right after a transaction to categorize it with one tap.",
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        val isNotifActive = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            wantsNotifications && isNotificationPermissionGranted
                        } else {
                            wantsNotifications
                        }

                        Switch(
                            checked = isNotifActive,
                            onCheckedChange = { shouldEnable ->
                                if (shouldEnable) {
                                    notifPermissionRequestedOnce = true
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        if (isNotificationPermissionGranted) {
                                            wantsNotifications = true
                                        } else {
                                            onRequestNotificationPermission()
                                        }
                                    } else {
                                        wantsNotifications = true
                                    }
                                } else {
                                    wantsNotifications = false
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.surface,
                                checkedTrackColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        notifPermissionRequestedOnce && !isNotificationPermissionGranted
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Notification permission was not granted.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Action Buttons: Continue vs Skip for now
        Column(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    val finalSms = wantsSmsDetection && isSmsPermissionGranted
                    val finalNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        wantsNotifications && isNotificationPermissionGranted
                    } else {
                        wantsNotifications
                    }
                    onCompleteOnboarding(finalSms, finalNotif)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text(
                    text = "Get Started",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    // Skip for now: leaves both features OFF
                    onCompleteOnboarding(false, false)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(
                    text = "Skip for now",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal)
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Onboarding Screen", showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    com.example.paisa.ui.theme.PaisaTheme {
        OnboardingScreen(
            isSmsPermissionGranted = false,
            isNotificationPermissionGranted = false,
            onRequestSmsPermission = {},
            onRequestNotificationPermission = {},
            onCompleteOnboarding = { _, _ -> }
        )
    }
}
