package com.kzzz3.argus.lens.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ImGreen,
    onPrimary = Color.White,
    primaryContainer = ImSuccessContainer,
    onPrimaryContainer = ImTextPrimary,
    secondary = ImBlue,
    onSecondary = Color.White,
    tertiary = ImGold,
    background = ImBackground,
    onBackground = ImTextPrimary,
    surface = ImSurface,
    onSurface = ImTextPrimary,
    surfaceVariant = ImSurfaceHighlight,
    onSurfaceVariant = ImTextSecondary,
    outline = ImBorder,
    error = ImCoral,
    onError = Color.White,
    errorContainer = ImErrorContainer,
    onErrorContainer = ImTextPrimary,
)

private val LightColorScheme = lightColorScheme(
    primary = ImGreen,
    onPrimary = Color.White,
    primaryContainer = ImSuccessContainer,
    onPrimaryContainer = ImTextPrimary,
    secondary = ImBlue,
    onSecondary = Color.White,
    tertiary = ImGold,
    background = ImBackground,
    onBackground = ImTextPrimary,
    surface = ImSurface,
    onSurface = ImTextPrimary,
    surfaceVariant = ImSurfaceHighlight,
    onSurfaceVariant = ImTextSecondary,
    outline = ImBorder,
    error = Color(0xFFB13B2F),
    onError = Color.White,
)

@Composable
fun ArguslensTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            LightColorScheme
        }
        darkTheme -> LightColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
