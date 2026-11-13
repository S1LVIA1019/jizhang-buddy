package com.buddy.jizhang.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Blue = Color(0xFF0B57F0)
private val BlueLight = Color(0xFF5E9CFF)

@Composable
fun JizhangTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Blue,
            onPrimary = Color.White,
            background = Color(0xFFF4F6FA),
            surface = Color.White,
            secondary = BlueLight,
            onSecondary = Color.White
        ),
        content = content
    )
}
