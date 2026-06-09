package com.example.thesis.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = DeepButton,
    secondary = DeepInteractive,
    tertiary = AccentGreen,
    background = DeepBg,
    surface = DeepCard,
    outline = DeepBorder,
    onPrimary = DeepBg,
    onSecondary = DeepBg,
    onBackground = DeepTextPri,
    onSurface = DeepTextPri,
    onSurfaceVariant = DeepTextSec
)

private val LightColorScheme = lightColorScheme(
    primary = SageButton,
    secondary = SageInteractive,
    tertiary = AccentGreen,
    background = SageBg,
    surface = SageCard,
    outline = SageBorder,
    onPrimary = SageBg,
    onSecondary = SageBg,
    onBackground = SageTextPri,
    onSurface = SageTextPri,
    onSurfaceVariant = SageTextSec
)

@Composable
fun ThesisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
