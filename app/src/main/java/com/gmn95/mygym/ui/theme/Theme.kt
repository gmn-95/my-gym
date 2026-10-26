package com.gmn95.mygym.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = GymGreen,
    background = GymLightBackground,
    surface = GymLightBackground,
)

private val DarkColorScheme = darkColorScheme(
    primary = GymGreenDark,
    background = GymDarkBackground,
    surface = GymDarkBackground,
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
