package de.einkaufsliste.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF2E7D5B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6EFE2),
    onPrimaryContainer = Color(0xFF0B3B26),
    secondaryContainer = Color(0xFFE3F2EA),
    onSecondaryContainer = Color(0xFF1C2420),
    background = Color(0xFFF4F6F5),
    surface = Color(0xFFF4F6F5),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    error = Color(0xFFC62828),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF5CC493),
    onPrimary = Color(0xFF0D1F16),
    primaryContainer = Color(0xFF1F4D37),
    onPrimaryContainer = Color(0xFFD6EFE2),
    secondaryContainer = Color(0xFF23382D),
    onSecondaryContainer = Color(0xFFE6ECE9),
    background = Color(0xFF121614),
    surface = Color(0xFF121614),
    surfaceContainerLow = Color(0xFF1D2320),
    surfaceContainer = Color(0xFF1D2320),
    surfaceContainerHigh = Color(0xFF242B27),
    error = Color(0xFFEF6A6A),
)

val DoneGreen = Color(0xFF2E9D5B)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
