package com.gmn95.mygym.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = GymGreen,
    onPrimary = GymLightBackground,
    primaryContainer = GymLightPrimaryContainer,
    onPrimaryContainer = GymLightOnPrimaryContainer,
    background = GymLightBackground,
    onBackground = GymLightOnBackground,
    surface = GymLightBackground,
    onSurface = GymLightOnBackground,
    surfaceVariant = GymLightSurfaceVariant,
    onSurfaceVariant = GymLightOnSurfaceVariant,
)

private val DarkColorScheme = darkColorScheme(
    primary = GymGreenDark,
    onPrimary = GymDarkBackground,
    primaryContainer = GymDarkPrimaryContainer,
    onPrimaryContainer = GymDarkOnPrimaryContainer,
    background = GymDarkBackground,
    onBackground = GymDarkOnBackground,
    surface = GymDarkBackground,
    onSurface = GymDarkOnBackground,
    surfaceVariant = GymDarkSurfaceVariant,
    onSurfaceVariant = GymDarkOnSurfaceVariant,
)

@Composable
fun MyGymTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content,
    )
}
