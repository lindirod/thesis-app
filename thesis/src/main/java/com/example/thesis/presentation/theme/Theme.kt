package com.example.thesis.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ColorScheme

private val DarkColorScheme = ColorScheme(
    primary = DeepButton,
    secondary = DeepInteractive,
    tertiary = AccentGreen,
    background = DeepBg,
    surfaceContainer = DeepCard,
    outline = DeepBorder,
    onPrimary = DeepBg,
    onSecondary = DeepBg,
    onBackground = DeepTextPri,
    onSurface = DeepTextPri,
    onSurfaceVariant = DeepTextSec
)

private val LightColorScheme = ColorScheme(
    primary = SageButton,
    secondary = SageInteractive,
    tertiary = AccentGreen,
    background = SageBg,
    surfaceContainer = SageCard,
    outline = SageBorder,
    onPrimary = SageBg,
    onSecondary = SageBg,
    onBackground = SageTextPri,
    onSurface = SageTextPri,
    onSurfaceVariant = SageTextSec
)

@Composable
fun ThesisTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
