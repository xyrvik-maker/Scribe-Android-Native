package com.primaloptima.scribe.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.primaloptima.scribe.util.ThemeManager
import com.primaloptima.scribe.util.model.AppTheme

fun parseComposeColor(hex: String, fallback: Color = Color.Black): Color {
    return try {
        Color(ThemeManager.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}

@Composable
fun ScribeComposeTheme(
    appTheme: AppTheme? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (appTheme != null) {
        val bg = parseComposeColor(appTheme.colors.background, Color(0xFFF8F6F0))
        val surface = parseComposeColor(appTheme.colors.surface, Color.White)
        val text = parseComposeColor(appTheme.colors.text, Color(0xFF1C1B1F))
        val accent = parseComposeColor(appTheme.colors.accent, Color(0xFF6750A4))
        lightColorScheme(
            primary = accent,
            background = bg,
            surface = surface,
            onBackground = text,
            onSurface = text,
            surfaceContainer = surface
        )
    } else if (darkTheme) {
        darkColorScheme()
    } else {
        lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
