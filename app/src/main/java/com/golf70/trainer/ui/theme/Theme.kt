package com.golf70.trainer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = GolfGreen,
    onPrimary = White,
    secondary = FairwayBlue,
    tertiary = SandAccent,
    background = White,
    surface = Mist
)

private val DarkColors = darkColorScheme(
    primary = GolfGreenLight,
    secondary = FairwayBlue,
    tertiary = SandAccent,
    background = Night,
    surface = NightSurface
)

@Composable
fun Golf70Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
