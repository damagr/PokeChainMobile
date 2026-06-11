package com.pokechain.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1A73E8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E3FD),
    secondary = Color(0xFF5F6368),
    surface = Color(0xFFFAFAFA),
    background = Color(0xFFF5F5F5),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color(0xFF003B8F),
    primaryContainer = Color(0xFF004BA0),
    secondary = Color(0xFF9AA0A6),
    surface = Color(0xFF1E1E1E),
    background = Color(0xFF121212),
)

@Composable
fun PokeChainTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
