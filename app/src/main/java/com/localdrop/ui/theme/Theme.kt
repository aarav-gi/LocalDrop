package com.localdrop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LocalDropBlue = Color(0xFF3B82F6)

private val DarkColors = darkColorScheme(primary = LocalDropBlue)
private val LightColors = lightColorScheme(primary = LocalDropBlue)

@Composable
fun LocalDropTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
