package com.jx.jxdatausage.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.jx.jxdatausage.data.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = BrandBlueDark,
    onPrimary = Color(0xFF0A2A4D),
    secondary = Color(0xFFFFB4AB),
    tertiary = Color(0xFF79D9C6),
    background = AppBackgroundDark,
    surface = AppSurfaceDark,
    surfaceVariant = AppSurfaceVariantDark,
    onBackground = AppTextDark,
    onSurface = AppTextDark,
    onSurfaceVariant = Color(0xFFD3DDEB),
    outline = AppOutlineDark,
    primaryContainer = AppPrimaryContainerDark,
    onPrimaryContainer = AppOnPrimaryContainerDark
)

private val LightColorScheme = lightColorScheme(
    primary = BrandBlueLight,
    onPrimary = Color.White,
    secondary = Color(0xFF9D4A54),
    tertiary = Color(0xFF0F8B7B),
    background = AppBackgroundLight,
    surface = AppSurfaceLight,
    surfaceVariant = AppSurfaceVariantLight,
    onBackground = AppTextLight,
    onSurface = AppTextLight,
    onSurfaceVariant = Color(0xFF435467),
    outline = AppOutlineLight,
    primaryContainer = AppPrimaryContainerLight,
    onPrimaryContainer = AppOnPrimaryContainerLight
)

@Composable
fun JxDataUsageTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
