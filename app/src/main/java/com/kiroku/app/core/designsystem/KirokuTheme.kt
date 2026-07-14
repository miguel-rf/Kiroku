package com.kiroku.app.core.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors =
    lightColorScheme(
        primary = Color(0xFF006493),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFC9E6FF),
        onPrimaryContainer = Color(0xFF001E30),
        secondary = Color(0xFF4F616E),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD2E5F5),
        onSecondaryContainer = Color(0xFF0B1E29),
        tertiary = Color(0xFF65587B),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFEBDDFF),
        onTertiaryContainer = Color(0xFF201634),
        background = Color(0xFFF8F9FF),
        onBackground = Color(0xFF191C1E),
        surface = Color(0xFFF8F9FF),
        onSurface = Color(0xFF191C1E),
        surfaceVariant = Color(0xFFDDE3EA),
        onSurfaceVariant = Color(0xFF41484D),
    )

private val DarkColors =
    darkColorScheme(
        primary = Color(0xFF8DCDFF),
        onPrimary = Color(0xFF00344F),
        primaryContainer = Color(0xFF004B70),
        onPrimaryContainer = Color(0xFFC9E6FF),
        secondary = Color(0xFFB6C9D9),
        onSecondary = Color(0xFF21333E),
        secondaryContainer = Color(0xFF374955),
        onSecondaryContainer = Color(0xFFD2E5F5),
        tertiary = Color(0xFFCFC0E8),
        onTertiary = Color(0xFF362B4B),
        tertiaryContainer = Color(0xFF4D4162),
        onTertiaryContainer = Color(0xFFEBDDFF),
        background = Color(0xFF111416),
        onBackground = Color(0xFFE1E2E6),
        surface = Color(0xFF111416),
        onSurface = Color(0xFFE1E2E6),
        surfaceVariant = Color(0xFF41484D),
        onSurfaceVariant = Color(0xFFC1C7CE),
    )

@Composable
fun KirokuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> DarkColors

            else -> LightColors
        }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
