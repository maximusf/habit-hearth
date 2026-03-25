package com.project.habithearth.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

private val AppBackground = Color(0xFF0A3323)
private val AppText = Color.White

@Composable
fun HabitHearthTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Force app-wide background/text colors so they stay consistent across all screens
    // (including when dynamic color is enabled).
    val forcedColorScheme =
        colorScheme.copy(
            background = AppBackground,
            onBackground = AppText,
            surface = AppBackground,
            onSurface = AppText,
            surfaceVariant = AppBackground,
            onSurfaceVariant = AppText,
            surfaceContainerLow = AppBackground,
            surfaceContainer = AppBackground,
            surfaceContainerHigh = AppBackground,
            onPrimary = AppText,
            onSecondary = AppText,
            onTertiary = AppText,
            onPrimaryContainer = AppText,
            onSecondaryContainer = AppText,
            onTertiaryContainer = AppText,
            onError = AppText,
            onErrorContainer = AppText,
        )

    MaterialTheme(
        colorScheme = forcedColorScheme,
        typography = Typography,
        content = content
    )
}