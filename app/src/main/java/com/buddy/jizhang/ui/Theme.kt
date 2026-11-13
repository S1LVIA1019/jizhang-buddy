package com.buddy.jizhang.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun JizhangTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Blue,
            onPrimary = Color.White,
            primaryContainer = HeroBg,
            onPrimaryContainer = HeroInk,
            background = PageBg,
            onBackground = TextDark,
            surface = Color.White,
            onSurface = TextDark,
            surfaceVariant = CardBg,
            onSurfaceVariant = TextGray,
            secondary = BlueLight,
            onSecondary = Color.White,
            error = OverRed
        ),
        content = content
    )
}
