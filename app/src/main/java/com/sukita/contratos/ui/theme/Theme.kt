package com.sukita.contratos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NeutralScheme = lightColorScheme(
    primary          = Color(0xFF2C2C2C),
    onPrimary        = Color.White,
    secondary        = Color(0xFF555555),
    onSecondary      = Color.White,
    background       = Color(0xFFF5F5F5),
    onBackground     = Color(0xFF1A1A1A),
    surface          = Color.White,
    onSurface        = Color(0xFF1A1A1A),
    error            = Color(0xFFB00020),
    onError          = Color.White,
)

@Composable
fun ContratosSukitaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NeutralScheme,
        content     = content
    )
}
