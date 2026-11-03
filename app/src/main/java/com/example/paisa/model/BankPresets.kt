package com.example.paisa.model

import androidx.compose.ui.graphics.Color

enum class ChipType {
    GOLD,
    SILVER
}

enum class CardPatternType {
    WAVES,
    GRID,
    CHEVRON,
    SUN_RAYS,
    PINSTRIPES,
    CONCENTRIC,
    MINIMAL
}

data class BankPreset(
    val identifier: String,
    val displayName: String,
    val shortName: String,
    val defaultThemeId: String,
    val searchKeywords: List<String>
)

data class BankTheme(
    val themeId: String,
    val displayName: String,
    val backgroundLight: Color,
    val backgroundDark: Color,
    val textPrimaryLight: Color,
    val textPrimaryDark: Color,
    val textSecondaryLight: Color,
    val textSecondaryDark: Color,
    val accentColor: Color,
    val borderColorLight: Color,
    val borderColorDark: Color,
    val gradientEndLight: Color,
    val gradientEndDark: Color,
    val cardGradient: List<Color> = listOf(backgroundDark, gradientEndDark),
    val chipType: ChipType = ChipType.GOLD,
    val patternType: CardPatternType = CardPatternType.CONCENTRIC,
    val metallicAccentColor: Color = accentColor
)

object BankThemeRegistry {

    // Bank-specific presets
    val THEME_SBI = BankTheme(
        themeId = "SBI",
        displayName = "SBI Blue",
        backgroundLight = Color(0xFFF0F4FA),
        backgroundDark = Color(0xFF0D253F),
        textPrimaryLight = Color(0xFF0A1E35),
        textPrimaryDark = Color(0xFFFFFFFF),
        textSecondaryLight = Color(0xFF335882),
        textSecondaryDark = Color(0xFFB8D5FA),
        accentColor = Color(0xFF2A75D3),
        borderColorLight = Color(0xFFC6D8EB),
        borderColorDark = Color(0xFF1F3E60),
        gradientEndLight = Color(0xFFE2ECF7),
        gradientEndDark = Color(0xFF071727),
        cardGradient = listOf(Color(0xFF081E38), Color(0xFF103661), Color(0xFF06172C)),
        chipType = ChipType.GOLD,
        patternType = CardPatternType.WAVES,
        metallicAccentColor = Color(0xFF2879D9)
    )

    val THEME_HDFC = BankTheme(
        themeId = "HDFC",
        displayName = "HDFC Royal",
        backgroundLight = Color(0xFFF0F4FA),
        backgroundDark = Color(0xFF0A2240),
        textPrimaryLight = Color(0xFF071C35),
        textPrimaryDark = Color(0xFFFFFFFF),
        textSecondaryLight = Color(0xFF2F547D),
        textSecondaryDark = Color(0xFFBFD7ED),
        accentColor = Color(0xFF004C8F),
        borderColorLight = Color(0xFFC8D7E8),
        borderColorDark = Color(0xFF19395E),
        gradientEndLight = Color(0xFFE3EDF7),
        gradientEndDark = Color(0xFF051427),
        cardGradient = listOf(Color(0xFF06182D), Color(0xFF0E2E52), Color(0xFF041324)),
        chipType = ChipType.SILVER,
        patternType = CardPatternType.GRID,
        metallicAccentColor = Color(0xFF1D5A9C)
    )

    val THEME_AXIS = BankTheme(
        themeId = "AXIS",
        displayName = "Axis Burgundy",
        backgroundLight = Color(0xFFFAF0F4),
        backgroundDark = Color(0xFF330B1C),
        textPrimaryLight = Color(0xFF2C0816),
        textPrimaryDark = Color(0xFFFFFFFF),
        textSecondaryLight = Color(0xFF732242),
        textSecondaryDark = Color(0xFFEAC7D5),
        accentColor = Color(0xFF97144D),
        borderColorLight = Color(0xFFECC2D3),
        borderColorDark = Color(0xFF4D132A),
        gradientEndLight = Color(0xFFF3DFE7),
        gradientEndDark = Color(0xFF1F0510),
        cardGradient = listOf(Color(0xFF260614), Color(0xFF4A0A26), Color(0xFF1E0410)),
        chipType = ChipType.GOLD,
        patternType = CardPatternType.CHEVRON,
        metallicAccentColor = Color(0xFF97144D)
    )

    val THEME_ICICI = BankTheme(
        themeId = "ICICI",
        displayName = "ICICI Terracotta",
        backgroundLight = Color(0xFFFAF2EE),
        backgroundDark = Color(0xFF2E120A),
        textPrimaryLight = Color(0xFF2B0E07),
        textPrimaryDark = Color(0xFFFFFFFF),
        textSecondaryLight = Color(0xFF7A3319),
        textSecondaryDark = Color(0xFFF5CFBE),
        accentColor = Color(0xFFF37021),
        borderColorLight = Color(0xFFECC9B7),
        borderColorDark = Color(0xFF522114),
        gradientEndLight = Color(0xFFF4E3DA),
        gradientEndDark = Color(0xFF1B0904),
        cardGradient = listOf(Color(0xFF2E0E05), Color(0xFF5A200B), Color(0xFF220902)),
        chipType = ChipType.GOLD,
        patternType = CardPatternType.SUN_RAYS,
        metallicAccentColor = Color(0xFFF37021)
    )

    val THEME_KOTAK = BankTheme(
        themeId = "KOTAK",
        displayName = "Kotak Ruby",
        backgroundLight = Color(0xFFFAF0F1),
        backgroundDark = Color(0xFF300B0E),
        textPrimaryLight = Color(0xFF2B080A),
        textPrimaryDark = Color(0xFFFFFFFF),
        textSecondaryLight = Color(0xFF782227),
        textSecondaryDark = Color(0xFFF0C5C8),
        accentColor = Color(0xFFED1C24),
        borderColorLight = Color(0xFFECC4C7),
        borderColorDark = Color(0xFF52161A),
        gradientEndLight = Color(0xFFF4DFE1),
        gradientEndDark = Color(0xFF1D0507),
        cardGradient = listOf(Color(0xFF2A0609), Color(0xFF550E14), Color(0xFF1F0407)),
        chipType = ChipType.SILVER,
        patternType = CardPatternType.PINSTRIPES,
        metallicAccentColor = Color(0xFFED1C24)
    )

    val THEME_FEDERAL = BankTheme(
        themeId = "FEDERAL",
        displayName = "Federal Gold & Navy",
        backgroundLight = Color(0xFFF2F4F8),
        backgroundDark = Color(0xFF0F1E36),
        textPrimaryLight = Color(0xFF0C192E),
        textPrimaryDark = Color(0xFFFFFFFF),
        textSecondaryLight = Color(0xFF324C70),
        textSecondaryDark = Color(0xFFC4D5ED),
        accentColor = Color(0xFFFDB913),
        borderColorLight = Color(0xFFD5DFEC),
        borderColorDark = Color(0xFF1E3555),
        gradientEndLight = Color(0xFFE3E9F3),
        gradientEndDark = Color(0xFF091322),
        cardGradient = listOf(Color(0xFF0A182B), Color(0xFF162E4F), Color(0xFF081220)),
        chipType = ChipType.GOLD,
        patternType = CardPatternType.CONCENTRIC,
        metallicAccentColor = Color(0xFFFDB913)
    )

    val THEME_CANARA = BankTheme(
        themeId = "CANARA",
        displayName = "Canara Teal",
        backgroundLight = Color(0xFFEFF7F9),
        backgroundDark = Color(0xFF07262D),
        textPrimaryLight = Color(0xFF052026),
        textPrimaryDark = Color(0xFFFFFFFF),
        textSecondaryLight = Color(0xFF245763),
        textSecondaryDark = Color(0xFFBBE5EE),
        accentColor = Color(0xFF00829B),
        borderColorLight = Color(0xFFBFE0E6),
        borderColorDark = Color(0xFF133F48),
        gradientEndLight = Color(0xFFE0F0F3),
        gradientEndDark = Color(0xFF04171B),
        cardGradient = listOf(Color(0xFF051C22), Color(0xFF0B3842), Color(0xFF04161B)),
        chipType = ChipType.SILVER,
        patternType = CardPatternType.WAVES,
        metallicAccentColor = Color(0xFF0096AF)
    )

    val THEME_SIB = BankTheme(
        themeId = "SIB",
        displayName = "South Indian Maroon",
        backgroundLight = Color(0xFFFAF1F3),
        backgroundDark = Color(0xFF2E0D14),
        textPrimaryLight = Color(0xFF28070D),
        textPrimaryDark = Color(0xFFFFFFFF),
        textSecondaryLight = Color(0xFF722330),
        textSecondaryDark = Color(0xFFECC6CD),
        accentColor = Color(0xFF7D1525),
        borderColorLight = Color(0xFFEBC3CC),
        borderColorDark = Color(0xFF4C1520),
        gradientEndLight = Color(0xFFF4DFE4),
        gradientEndDark = Color(0xFF1A060B),
        cardGradient = listOf(Color(0xFF27070D), Color(0xFF4C0E1A), Color(0xFF1E0409)),
        chipType = ChipType.GOLD,
        patternType = CardPatternType.CONCENTRIC,
        metallicAccentColor = Color(0xFF8B1829)
    )

    val THEME_IDFC = BankTheme(
        themeId = "IDFC",
        displayName = "IDFC Crimson",
        backgroundLight = Color(0xFFFAF0F2),
        backgroundDark = Color(0xFF300B14),
        textPrimaryLight = Color(0xFF2A060E),
        textPrimaryDark = Color(0xFFFFFFFF),
        textSecondaryLight = Color(0xFF76202F),
        textSecondaryDark = Color(0xFFEBC7CF),
        accentColor = Color(0xFF9E1B32),
        borderColorLight = Color(0xFFEBC2CB),
        borderColorDark = Color(0xFF4E1420),
        gradientEndLight = Color(0xFFF3DEE3),
        gradientEndDark = Color(0xFF1B050B),
        cardGradient = listOf(Color(0xFF27050E), Color(0xFF4E0C1C), Color(0xFF1D040A)),
        chipType = ChipType.GOLD,
        patternType = CardPatternType.GRID,
        metallicAccentColor = Color(0xFFA31C34)
    )

    // Custom selectable themes for 'Other' bank
    val THEME_MONOCHROME = BankTheme(
        themeId = "THEME_MONOCHROME",
        displayName = "Charcoal Slate",
        backgroundLight = Color(0xFFF4F4F6),
        backgroundDark = Color(0xFF1A1A1E),
        textPrimaryLight = Color(0xFF18181B),
        textPrimaryDark = Color(0xFFFFFFFF),
        textSecondaryLight = Color(0xFF52525B),
        textSecondaryDark = Color(0xFFA1A1AA),
        accentColor = Color(0xFF71717A),
        borderColorLight = Color(0xFFE4E4E7),
        borderColorDark = Color(0xFF27272A),
        gradientEndLight = Color(0xFFE5E5E8),
        gradientEndDark = Color(0xFF121215),
        cardGradient = listOf(Color(0xFF161618), Color(0xFF26262D), Color(0xFF111113)),
        chipType = ChipType.SILVER,
        patternType = CardPatternType.MINIMAL,
        metallicAccentColor = Color(0xFFA1A1AA)
    )

    val THEME_EMERALD = BankTheme(
        themeId = "THEME_EMERALD",
        displayName = "Emerald Green",
        backgroundLight = Color(0xFFEFF8F4),
        backgroundDark = Color(0xFF0B2419),
        textPrimaryLight = Color(0xFF062B1D),
        textPrimaryDark = Color(0xFFFFFFFF),
        textSecondaryLight = Color(0xFF1E6347),
        textSecondaryDark = Color(0xFFBCE3D2),
        accentColor = Color(0xFF10B981),
        borderColorLight = Color(0xFFC7EBD9),
        borderColorDark = Color(0xFF163E2D),
        gradientEndLight = Color(0xFFDEF2EA),
        gradientEndDark = Color(0xFF061710),
        cardGradient = listOf(Color(0xFF061E14), Color(0xFF0E3B28), Color(0xFF05170F)),
        chipType = ChipType.GOLD,
        patternType = CardPatternType.WAVES,
        metallicAccentColor = Color(0xFF10B981)
    )

    val THEME_INDIGO = BankTheme(
        themeId = "THEME_INDIGO",
        displayName = "Deep Indigo",
        backgroundLight = Color(0xFFF1F3FB),
        backgroundDark = Color(0xFF14193E),
        textPrimaryLight = Color(0xFF0F1435),
        textPrimaryDark = Color(0xFFFFFFFF),
        textSecondaryLight = Color(0xFF384382),
        textSecondaryDark = Color(0xFFC6CCF2),
        accentColor = Color(0xFF6366F1),
        borderColorLight = Color(0xFFD3D8F5),
        borderColorDark = Color(0xFF222A5C),
        gradientEndLight = Color(0xFFE2E6F8),
        gradientEndDark = Color(0xFF0C0F26),
        cardGradient = listOf(Color(0xFF0F1332), Color(0xFF1B2358), Color(0xFF0C0E26)),
        chipType = ChipType.SILVER,
        patternType = CardPatternType.GRID,
        metallicAccentColor = Color(0xFF6366F1)
    )

    val THEME_PURPLE = BankTheme(
        themeId = "THEME_PURPLE",
        displayName = "Royal Purple",
        backgroundLight = Color(0xFFF6F0FA),
        backgroundDark = Color(0xFF260F38),
        textPrimaryLight = Color(0xFF200930),
        textPrimaryDark = Color(0xFFFFFFFF),
        textSecondaryLight = Color(0xFF5D2782),
        textSecondaryDark = Color(0xFFDEC3F5),
        accentColor = Color(0xFFA855F7),
        borderColorLight = Color(0xFFE4CDF2),
        borderColorDark = Color(0xFF3C1B55),
        gradientEndLight = Color(0xFFEBDFF5),
        gradientEndDark = Color(0xFF170822),
        cardGradient = listOf(Color(0xFF1E082B), Color(0xFF3E1256), Color(0xFF170522)),
        chipType = ChipType.SILVER,
        patternType = CardPatternType.CONCENTRIC,
        metallicAccentColor = Color(0xFFA855F7)
    )

    val THEME_BRONZE = BankTheme(
        themeId = "THEME_BRONZE",
        displayName = "Warm Bronze",
        backgroundLight = Color(0xFFFAF4EF),
        backgroundDark = Color(0xFF2B1B10),
        textPrimaryLight = Color(0xFF29170B),
        textPrimaryDark = Color(0xFFFFFFFF),
        textSecondaryLight = Color(0xFF6E4324),
        textSecondaryDark = Color(0xFFF2D1BA),
        accentColor = Color(0xFFD97706),
        borderColorLight = Color(0xFFEBD4C1),
        borderColorDark = Color(0xFF452C1B),
        gradientEndLight = Color(0xFFF3E8DF),
        gradientEndDark = Color(0xFF1A1009),
        cardGradient = listOf(Color(0xFF26150A), Color(0xFF4C2A15), Color(0xFF1C0E07)),
        chipType = ChipType.GOLD,
        patternType = CardPatternType.SUN_RAYS,
        metallicAccentColor = Color(0xFFD97706)
    )

    val CUSTOM_THEMES = listOf(
        THEME_MONOCHROME,
        THEME_EMERALD,
        THEME_INDIGO,
        THEME_PURPLE,
        THEME_BRONZE
    )

    private val ALL_THEMES_MAP = mapOf(
        "SBI" to THEME_SBI,
        "HDFC" to THEME_HDFC,
        "AXIS" to THEME_AXIS,
        "ICICI" to THEME_ICICI,
        "KOTAK" to THEME_KOTAK,
        "FEDERAL" to THEME_FEDERAL,
        "CANARA" to THEME_CANARA,
        "SIB" to THEME_SIB,
        "IDFC" to THEME_IDFC,
        "THEME_MONOCHROME" to THEME_MONOCHROME,
        "THEME_EMERALD" to THEME_EMERALD,
        "THEME_INDIGO" to THEME_INDIGO,
        "THEME_PURPLE" to THEME_PURPLE,
        "THEME_BRONZE" to THEME_BRONZE,
        "DEFAULT" to THEME_MONOCHROME
    )

    fun getTheme(themeId: String?): BankTheme {
        return ALL_THEMES_MAP[themeId?.uppercase()] ?: THEME_MONOCHROME
    }
}

object BankPresets {

    val PRESET_SBI = BankPreset(
        identifier = "SBI",
        displayName = "State Bank of India (SBI)",
        shortName = "SBI",
        defaultThemeId = "SBI",
        searchKeywords = listOf("sbi", "state bank", "state bank of india", "state")
    )

    val PRESET_HDFC = BankPreset(
        identifier = "HDFC",
        displayName = "HDFC Bank",
        shortName = "HDFC",
        defaultThemeId = "HDFC",
        searchKeywords = listOf("hdfc", "hdfc bank")
    )

    val PRESET_AXIS = BankPreset(
        identifier = "AXIS",
        displayName = "Axis Bank",
        shortName = "Axis",
        defaultThemeId = "AXIS",
        searchKeywords = listOf("axis", "axis bank", "uti")
    )

    val PRESET_ICICI = BankPreset(
        identifier = "ICICI",
        displayName = "ICICI Bank",
        shortName = "ICICI",
        defaultThemeId = "ICICI",
        searchKeywords = listOf("icici", "icici bank")
    )

    val PRESET_KOTAK = BankPreset(
        identifier = "KOTAK",
        displayName = "Kotak Mahindra Bank",
        shortName = "Kotak",
        defaultThemeId = "KOTAK",
        searchKeywords = listOf("kotak", "kotak mahindra", "kotak bank")
    )

    val PRESET_FEDERAL = BankPreset(
        identifier = "FEDERAL",
        displayName = "Federal Bank",
        shortName = "Federal",
        defaultThemeId = "FEDERAL",
        searchKeywords = listOf("federal", "federal bank")
    )

    val PRESET_CANARA = BankPreset(
        identifier = "CANARA",
        displayName = "Canara Bank",
        shortName = "Canara",
        defaultThemeId = "CANARA",
        searchKeywords = listOf("canara", "canara bank", "cnrb")
    )

    val PRESET_SIB = BankPreset(
        identifier = "SIB",
        displayName = "South Indian Bank",
        shortName = "South Indian Bank",
        defaultThemeId = "SIB",
        searchKeywords = listOf("south indian bank", "south indian", "sib")
    )

    val PRESET_IDFC = BankPreset(
        identifier = "IDFC",
        displayName = "IDFC FIRST Bank",
        shortName = "IDFC FIRST",
        defaultThemeId = "IDFC",
        searchKeywords = listOf("idfc", "idfc first", "idfc first bank", "idfc bank")
    )

    val PRESET_OTHER = BankPreset(
        identifier = "OTHER",
        displayName = "Other Bank",
        shortName = "Other",
        defaultThemeId = "THEME_MONOCHROME",
        searchKeywords = listOf("other", "custom", "cooperative", "rural")
    )

    val ALL_PRESETS: List<BankPreset> = listOf(
        PRESET_SBI,
        PRESET_HDFC,
        PRESET_AXIS,
        PRESET_ICICI,
        PRESET_KOTAK,
        PRESET_FEDERAL,
        PRESET_CANARA,
        PRESET_SIB,
        PRESET_IDFC,
        PRESET_OTHER
    )

    fun getPreset(identifier: String?): BankPreset {
        val idClean = identifier?.trim()?.uppercase() ?: return PRESET_OTHER
        return ALL_PRESETS.firstOrNull { it.identifier == idClean } ?: PRESET_OTHER
    }

    /**
     * Maps legacy free-text bank names to presets without deleting existing accounts.
     */
    fun findPresetByFreeText(bankNameText: String?): BankPreset {
        if (bankNameText.isNullOrBlank()) return PRESET_OTHER
        val upper = bankNameText.trim().uppercase()

        return when {
            upper.contains("SBI") || upper.contains("STATE BANK") -> PRESET_SBI
            upper.contains("HDFC") -> PRESET_HDFC
            upper.contains("AXIS") || upper.contains("UTI") -> PRESET_AXIS
            upper.contains("ICICI") -> PRESET_ICICI
            upper.contains("KOTAK") -> PRESET_KOTAK
            upper.contains("FEDERAL") -> PRESET_FEDERAL
            upper.contains("CANARA") || upper.contains("CNRB") -> PRESET_CANARA
            upper.contains("SOUTH INDIAN") || upper.contains("SIB") -> PRESET_SIB
            upper.contains("IDFC") -> PRESET_IDFC
            else -> PRESET_OTHER
        }
    }
}
