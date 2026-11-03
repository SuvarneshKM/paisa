package com.example.paisa.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = MonoWhite,
    onPrimary = MonoBlack,
    primaryContainer = MonoDarkTag,
    onPrimaryContainer = MonoWhite,
    secondary = MonoDarkTextSecondary,
    onSecondary = MonoWhite,
    secondaryContainer = MonoDarkSurfaceElevated,
    onSecondaryContainer = MonoDarkTextPrimary,
    tertiary = MonoDarkTextTertiary,
    onTertiary = MonoWhite,
    background = MonoDarkBackground,
    onBackground = MonoDarkTextPrimary,
    surface = MonoDarkSurface,
    onSurface = MonoDarkTextPrimary,
    surfaceVariant = MonoDarkSurfaceElevated,
    onSurfaceVariant = MonoDarkTextSecondary,
    outline = MonoDarkTextTertiary,
    outlineVariant = MonoDarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = MonoBlack,
    onPrimary = MonoWhite,
    primaryContainer = MonoLightTag,
    onPrimaryContainer = MonoBlack,
    secondary = MonoLightTextSecondary,
    onSecondary = MonoBlack,
    secondaryContainer = MonoLightSurfaceElevated,
    onSecondaryContainer = MonoLightTextPrimary,
    tertiary = MonoLightTextTertiary,
    onTertiary = MonoBlack,
    background = MonoLightBackground,
    onBackground = MonoLightTextPrimary,
    surface = MonoLightSurface,
    onSurface = MonoLightTextPrimary,
    surfaceVariant = MonoLightSurfaceElevated,
    onSurfaceVariant = MonoLightTextSecondary,
    outline = MonoLightTextTertiary,
    outlineVariant = MonoLightBorder
)

@Composable
fun PaisaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
