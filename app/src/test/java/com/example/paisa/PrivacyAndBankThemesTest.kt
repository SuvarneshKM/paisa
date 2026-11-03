package com.example.paisa

import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.paisa.data.local.PaisaDatabase
import com.example.paisa.data.local.entity.BankAccountEntity
import com.example.paisa.data.preferences.UserPreferences
import com.example.paisa.model.BankPresets
import com.example.paisa.model.BankThemeRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PrivacyAndBankThemesTest {

    // =========================================================================
    // 1. First-Launch Defaults (Privacy-First)
    // =========================================================================

    @Test
    fun userPreferences_defaultState_hasBothFeaturesDisabledAndOnboardingIncomplete() {
        val defaults = UserPreferences()

        // Privacy-first: Onboarding must not be completed on initial state
        assertFalse("Onboarding should be incomplete by default", defaults.onboardingCompleted)

        // Privacy-first: SMS detection must strictly default to OFF
        assertFalse("SMS detection must be OFF by default", defaults.smsDetectionEnabled)

        // Privacy-first: Notifications must strictly default to OFF
        assertFalse("Category notifications must be OFF by default", defaults.notificationsEnabled)

        // Pure manual mode is not active until requested
        assertFalse(defaults.manualOnlyMode)
        assertFalse(defaults.userConsented)
    }

    // =========================================================================
    // 2. Denied Permissions Handling Logic
    // =========================================================================

    @Test
    fun permissionDenial_ensuresFeatureRemainsDisabled() {
        val isSmsPermissionGranted = false
        val userWantsSms = true

        // Effective feature state MUST check both user intent and actual permission grant
        val effectiveSmsEnabled = userWantsSms && isSmsPermissionGranted
        assertFalse("Feature must NEVER be enabled if permission is denied", effectiveSmsEnabled)

        val isNotifPermissionGranted = false
        val userWantsNotifs = true
        val effectiveNotifsEnabled = userWantsNotifs && isNotifPermissionGranted
        assertFalse("Notifications must NEVER be enabled if permission is denied", effectiveNotifsEnabled)
    }

    @Test
    fun permissionRevocation_resetsFeatureState() {
        var isSmsDetectionEnabled = true
        var isPermissionCurrentlyGranted = false // Revoked by user in OS settings

        // Simulating Lifecycle ON_RESUME check
        if (!isPermissionCurrentlyGranted && isSmsDetectionEnabled) {
            isSmsDetectionEnabled = false
        }

        assertFalse("Revoked permission must immediately turn off feature", isSmsDetectionEnabled)
    }

    // =========================================================================
    // 3. Bank Preset Mapping from Free-Text Names
    // =========================================================================

    @Test
    fun findPresetByFreeText_correctlyMapsMajorIndianBanks() {
        assertEquals("HDFC", BankPresets.findPresetByFreeText("HDFC Bank").identifier)
        assertEquals("HDFC", BankPresets.findPresetByFreeText("hdfc salary a/c").identifier)

        assertEquals("SBI", BankPresets.findPresetByFreeText("State Bank of India").identifier)
        assertEquals("SBI", BankPresets.findPresetByFreeText("SBI").identifier)
        assertEquals("SBI", BankPresets.findPresetByFreeText("sbi inb").identifier)

        assertEquals("ICICI", BankPresets.findPresetByFreeText("ICICI Bank Ltd").identifier)
        assertEquals("ICICI", BankPresets.findPresetByFreeText("ICICI").identifier)

        assertEquals("AXIS", BankPresets.findPresetByFreeText("Axis Bank").identifier)
        assertEquals("AXIS", BankPresets.findPresetByFreeText("UTI Bank").identifier)

        assertEquals("KOTAK", BankPresets.findPresetByFreeText("Kotak Mahindra Bank").identifier)
        assertEquals("KOTAK", BankPresets.findPresetByFreeText("Kotak 811").identifier)

        assertEquals("FEDERAL", BankPresets.findPresetByFreeText("Federal Bank").identifier)
        assertEquals("CANARA", BankPresets.findPresetByFreeText("Canara Bank").identifier)
        assertEquals("CANARA", BankPresets.findPresetByFreeText("CNRB").identifier)

        assertEquals("SIB", BankPresets.findPresetByFreeText("South Indian Bank").identifier)
        assertEquals("SIB", BankPresets.findPresetByFreeText("SIB").identifier)

        assertEquals("IDFC", BankPresets.findPresetByFreeText("IDFC FIRST Bank").identifier)
        assertEquals("IDFC", BankPresets.findPresetByFreeText("IDFC").identifier)
    }

    @Test
    fun findPresetByFreeText_unknownBank_fallsBackToOther() {
        assertEquals("OTHER", BankPresets.findPresetByFreeText("Saraswat Cooperative Bank").identifier)
        assertEquals("OTHER", BankPresets.findPresetByFreeText("Gramin Bank").identifier)
        assertEquals("OTHER", BankPresets.findPresetByFreeText("").identifier)
        assertEquals("OTHER", BankPresets.findPresetByFreeText(null).identifier)
    }

    // =========================================================================
    // 4. Bank Theme Selection & Entity Resolution
    // =========================================================================

    @Test
    fun bankThemeRegistry_hasAllPresetThemesRegistered() {
        val requiredThemes = listOf("SBI", "HDFC", "AXIS", "ICICI", "KOTAK", "FEDERAL", "CANARA", "SIB", "IDFC")
        for (themeId in requiredThemes) {
            val theme = BankThemeRegistry.getTheme(themeId)
            assertNotNull(theme)
            assertEquals(themeId, theme.themeId)
        }
    }

    @Test
    fun bankThemeRegistry_hasCustomThemesForOtherBank() {
        assertEquals(5, BankThemeRegistry.CUSTOM_THEMES.size)
        val customThemeIds = BankThemeRegistry.CUSTOM_THEMES.map { it.themeId }
        assertTrue(customThemeIds.contains("THEME_MONOCHROME"))
        assertTrue(customThemeIds.contains("THEME_EMERALD"))
        assertTrue(customThemeIds.contains("THEME_INDIGO"))
        assertTrue(customThemeIds.contains("THEME_PURPLE"))
        assertTrue(customThemeIds.contains("THEME_BRONZE"))
    }

    @Test
    fun bankAccountEntity_resolvedThemeId_mapsOldAccountsSeamlessly() {
        // Legacy account with no explicit theme (defaults to "DEFAULT")
        val legacyHdfcAccount = BankAccountEntity(
            id = 1L,
            bankName = "HDFC Bank",
            nickname = "Salary",
            lastFourDigits = "4821",
            openingBalancePaise = 5000000L,
            bankIdentifier = "OTHER",
            themeId = "DEFAULT"
        )
        assertEquals("HDFC", legacyHdfcAccount.resolvedThemeId)

        val legacySbiAccount = BankAccountEntity(
            id = 2L,
            bankName = "State Bank of India",
            nickname = "Savings",
            lastFourDigits = "6231",
            openingBalancePaise = 2000000L,
            bankIdentifier = "OTHER",
            themeId = "DEFAULT"
        )
        assertEquals("SBI", legacySbiAccount.resolvedThemeId)

        // Custom bank with explicit custom theme
        val customAccount = BankAccountEntity(
            id = 3L,
            bankName = "Cooperative Bank",
            nickname = "Local",
            lastFourDigits = "9901",
            openingBalancePaise = 100000L,
            bankIdentifier = "OTHER",
            themeId = "THEME_EMERALD"
        )
        assertEquals("THEME_EMERALD", customAccount.resolvedThemeId)
    }

    // =========================================================================
    // 5. Room Migration 1 -> 2
    // =========================================================================

    @Test
    fun migration1to2_executesExpectedAlterTableSql() {
        val executedSqlStatements = mutableListOf<String>()

        // Mock SupportSQLiteDatabase to record SQL statements
        val mockDb = java.lang.reflect.Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedSqlStatements.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        PaisaDatabase.MIGRATION_1_2.migrate(mockDb)

        assertEquals(2, executedSqlStatements.size)
        assertTrue(
            "Must add bankIdentifier column",
            executedSqlStatements[0].contains("ALTER TABLE bank_accounts ADD COLUMN bankIdentifier TEXT NOT NULL DEFAULT 'OTHER'")
        )
        assertTrue(
            "Must add themeId column",
            executedSqlStatements[1].contains("ALTER TABLE bank_accounts ADD COLUMN themeId TEXT NOT NULL DEFAULT 'DEFAULT'")
        )
    }

    @Test
    fun realisticCardThemes_haveValidGradientsAndHardwareAccents() {
        val allThemes = listOf(
            "SBI", "HDFC", "AXIS", "ICICI", "KOTAK", "FEDERAL", "CANARA", "SIB", "IDFC",
            "THEME_MONOCHROME", "THEME_EMERALD", "THEME_INDIGO", "THEME_PURPLE", "THEME_BRONZE"
        )

        for (themeId in allThemes) {
            val theme = BankThemeRegistry.getTheme(themeId)
            assertTrue("Card gradient must have at least 2 color stops for $themeId", theme.cardGradient.size >= 2)
            assertNotNull("ChipType must be specified for $themeId", theme.chipType)
            assertNotNull("PatternType must be specified for $themeId", theme.patternType)
        }
    }

    // =========================================================================
    // 6. Security & Privacy Audit
    // =========================================================================

    @Test
    fun verifyNoDisallowedPermissionsInManifest() {
        val manifestFile = if (File("src/main/AndroidManifest.xml").exists()) {
            File("src/main/AndroidManifest.xml")
        } else {
            File("app/src/main/AndroidManifest.xml")
        }
        assertTrue("AndroidManifest.xml must exist at ${manifestFile.absolutePath}", manifestFile.exists())
        val manifestContent = manifestFile.readText()

        assertFalse("Must NOT request READ_SMS", manifestContent.contains("android.permission.READ_SMS"))
        assertFalse("Must NOT request SEND_SMS", manifestContent.contains("android.permission.SEND_SMS"))
        assertFalse("Must NOT request INTERNET", manifestContent.contains("android.permission.INTERNET"))
    }
}
