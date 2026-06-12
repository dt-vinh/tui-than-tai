package com.phuongnn14.tuithantai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = LuckyGreen80,
    onPrimary = LuckyGreenDark,
    primaryContainer = LuckyGreen,
    onPrimaryContainer = LuckySurface,
    secondary = LuckyGold80,
    onSecondary = LuckyGreenDark,
    tertiary = LuckySky80,
    background = LuckyGreenDark,
    onBackground = LuckySurface,
    surface = ColorTokens.DarkSurface,
    onSurface = LuckySurface,
    surfaceVariant = ColorTokens.DarkSurfaceVariant,
    onSurfaceVariant = ColorTokens.DarkMuted,
    error = ColorTokens.ErrorDark,
)

private val LightColorScheme = lightColorScheme(
    primary = LuckyGreen,
    onPrimary = LuckySurface,
    primaryContainer = LuckyMint,
    onPrimaryContainer = LuckyGreenDark,
    secondary = LuckyGold,
    onSecondary = LuckyGreenDark,
    secondaryContainer = ColorTokens.GoldSoft,
    onSecondaryContainer = LuckyGreenDark,
    tertiary = ColorTokens.TealBlue,
    background = LuckyCanvas,
    onBackground = LuckyInk,
    surface = LuckySurface,
    onSurface = LuckyInk,
    surfaceVariant = ColorTokens.SurfaceVariant,
    onSurfaceVariant = LuckyMuted,
    error = LuckyExpense,
    errorContainer = LuckyExpenseSoft,
    onErrorContainer = ColorTokens.ErrorInk,
)

@Composable
fun TuiThanTaiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme && dynamicColor) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

private object ColorTokens {
    val GoldSoft = androidx.compose.ui.graphics.Color(0xFFFFF1C7)
    val TealBlue = androidx.compose.ui.graphics.Color(0xFF1D7890)
    val SurfaceVariant = androidx.compose.ui.graphics.Color(0xFFEAF0EA)
    val ErrorInk = androidx.compose.ui.graphics.Color(0xFF5D1511)
    val DarkSurface = androidx.compose.ui.graphics.Color(0xFF102C27)
    val DarkSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF1A4138)
    val DarkMuted = androidx.compose.ui.graphics.Color(0xFFC5D4CC)
    val ErrorDark = androidx.compose.ui.graphics.Color(0xFFFFB4AA)
}
