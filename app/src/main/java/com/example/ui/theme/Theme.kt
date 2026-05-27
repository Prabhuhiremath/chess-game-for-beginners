package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CustomWoodScheme = darkColorScheme(
    primary = MDarkPrimary,
    background = MDarkBackground,
    surface = MDarkSurface,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onBackground = androidx.compose.ui.graphics.Color.White,
    onSurface = androidx.compose.ui.graphics.Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable dynamic colors for wooden theme
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CustomWoodScheme,
        typography = Typography,
        content = content
    )
}
