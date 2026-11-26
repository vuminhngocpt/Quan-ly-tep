package com.vuminhngoc.quanlytep.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.vuminhngoc.quanlytep.model.ThemeMode

@Immutable
data class ExtendedColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val surface3: Color,
    val surfaceHover: Color,
    val text: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val border: Color,
    val borderStrong: Color,
    val accent: Color,
    val accentText: Color,
    val accentSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val success: Color,
    val warning: Color
)

val DarkExtendedColors = ExtendedColors(
    bg = Color(0xFF000000),
    surface = Color(0xFF121215),
    surface2 = Color(0xFF1A1A1F),
    surface3 = Color(0xFF24242B),
    surfaceHover = Color(0xFF222228),
    text = Color(0xFFF5F5F7),
    textSecondary = Color(0xFFA1A1AA),
    textMuted = Color(0xFF71717A),
    border = Color(0x1CFFFFFF),
    borderStrong = Color(0x38FFFFFF),
    accent = Color(0xFF0A84FF),
    accentText = Color(0xFF54A8FF),
    accentSoft = Color(0x2B0A84FF),
    danger = Color(0xFFEF4444),
    dangerSoft = Color(0x29EF4444),
    success = Color(0xFF22C55E),
    warning = Color(0xFFF59E0B)
)

val LightExtendedColors = ExtendedColors(
    bg = Color(0xFFF2F2F7),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFF7F7FA),
    surface3 = Color(0xFFECECF2),
    surfaceHover = Color(0xFFE5E5EC),
    text = Color(0xFF111827),
    textSecondary = Color(0xFF4B5563),
    textMuted = Color(0xFF6B7280),
    border = Color(0x1C0F172A),
    borderStrong = Color(0x380F172A),
    accent = Color(0xFF007AFF),
    accentText = Color(0xFF0066D6),
    accentSoft = Color(0x1F007AFF),
    danger = Color(0xFFDC2626),
    dangerSoft = Color(0x1FDC2626),
    success = Color(0xFF16A34A),
    warning = Color(0xFFD97706)
)

val LocalExtendedColors = staticCompositionLocalOf { DarkExtendedColors }

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF0A84FF),
    background = Color(0xFF000000),
    surface = Color(0xFF121215),
    surfaceVariant = Color(0xFF1A1A1F),
    onPrimary = Color.White,
    onBackground = Color(0xFFF5F5F7),
    onSurface = Color(0xFFF5F5F7),
    error = Color(0xFFEF4444)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF007AFF),
    background = Color(0xFFF2F2F7),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF7F7FA),
    onPrimary = Color.White,
    onBackground = Color(0xFF111827),
    onSurface = Color(0xFF111827),
    error = Color(0xFFDC2626)
)

@Composable
fun QuanLyTepTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.AUTO -> isSystemInDarkTheme()
    }
    val extendedColors = if (isDark) DarkExtendedColors else LightExtendedColors
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
