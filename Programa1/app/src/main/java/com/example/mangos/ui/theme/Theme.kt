package com.example.mangos.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = MangoGreen,
    onPrimary = OnMangoGreen,
    primaryContainer = MangoGreenContainer,
    onPrimaryContainer = OnMangoGreenContainer,
    secondary = MangoOrange,
    onSecondary = OnMangoOrange,
    secondaryContainer = MangoOrangeContainer,
    onSecondaryContainer = OnMangoOrangeContainer,
    tertiary = MangoYellow,
    onTertiary = OnMangoYellow,
    tertiaryContainer = MangoYellowContainer,
    onTertiaryContainer = OnMangoYellowContainer,
    background = MangoBackground,
    onBackground = OnMangoSurface,
    surface = MangoSurface,
    onSurface = OnMangoSurface,
    surfaceVariant = MangoSurfaceVariant,
    onSurfaceVariant = OnMangoSurfaceVariant,
    outline = MangoOutline,
    error = MangoError,
    onError = OnMangoError,
    errorContainer = MangoErrorContainer,
    onErrorContainer = OnMangoErrorContainer
)

@Composable
fun MangosTheme(
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

        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
