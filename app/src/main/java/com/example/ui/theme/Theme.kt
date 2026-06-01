package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KidsColorScheme = lightColorScheme(
    primary = MPrimary,
    onPrimary = Color.White,
    secondary = Bubblegum,
    onSecondary = Color.White,
    tertiary = Sunny,
    background = MBackground,
    onBackground = DarkText,
    surface = MSurface,
    onSurface = DarkText
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false, // Keep our custom playful palette
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KidsColorScheme,
        typography = Typography,
        content = content
    )
}
