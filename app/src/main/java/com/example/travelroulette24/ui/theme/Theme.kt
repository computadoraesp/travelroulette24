package com.example.travelroulette24.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = OceanPrimaryContainer,
    onPrimary = OceanOnPrimaryContainer,
    primaryContainer = OceanPrimary,
    onPrimaryContainer = OceanOnPrimary,
    secondary = CoralSecondaryContainer,
    onSecondary = CoralOnSecondaryContainer,
    secondaryContainer = CoralSecondary,
    onSecondaryContainer = CoralOnSecondary,
    tertiary = EmeraldTertiaryContainer,
    onTertiary = EmeraldOnTertiaryContainer,
    background = DarkSurface,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = OceanPrimary,
    onPrimary = OceanOnPrimary,
    primaryContainer = OceanPrimaryContainer,
    onPrimaryContainer = OceanOnPrimaryContainer,
    secondary = CoralSecondary,
    onSecondary = CoralOnSecondary,
    secondaryContainer = CoralSecondaryContainer,
    onSecondaryContainer = CoralOnSecondaryContainer,
    tertiary = EmeraldTertiary,
    onTertiary = EmeraldOnTertiary,
    background = LightSurface,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant
)

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    COLORBLIND_OKABE_ITO
}

private val OkabeItoColorScheme = lightColorScheme(
    primary = OkabeBlue,
    onPrimary = OkabeOnBlue,
    primaryContainer = OkabeSkyBlueContainer,
    onPrimaryContainer = OkabeOnSkyBlueContainer,
    secondary = OkabeOrange,
    onSecondary = OkabeOnOrange,
    secondaryContainer = OkabeYellowContainer,
    onSecondaryContainer = OkabeOnYellowContainer,
    tertiary = OkabeBluishGreen,
    onTertiary = OkabeOnBluishGreen,
    tertiaryContainer = OkabeBluishGreenContainer,
    onTertiaryContainer = OkabeOnBluishGreenContainer,
    error = OkabeVermillion,
    onError = OkabeOnVermillion,
    errorContainer = OkabeVermillionContainer,
    onErrorContainer = OkabeOnVermillionContainer,
    background = OkabeSurface,
    onBackground = OkabeOnSurface,
    surface = OkabeSurface,
    onSurface = OkabeOnSurface,
    surfaceVariant = OkabeSurfaceVariant,
    onSurfaceVariant = OkabeOnSurfaceVariant,
    outline = OkabePurpleOutline
)

@Composable
fun TravelRoulette24Theme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        AppThemeMode.COLORBLIND_OKABE_ITO -> OkabeItoColorScheme
        AppThemeMode.DARK -> DarkColorScheme
        AppThemeMode.LIGHT -> LightColorScheme
        AppThemeMode.SYSTEM -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else if (darkTheme) {
                DarkColorScheme
            } else {
                LightColorScheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}