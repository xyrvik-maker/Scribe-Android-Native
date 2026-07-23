package com.primaloptima.scribe.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val resolvedTheme = remember(appTheme) {
        appTheme ?: try { ThemeManager(context).activeTheme() } catch (_: Exception) { null }
    }

    val colorScheme = if (resolvedTheme != null) {
        val bg = parseComposeColor(resolvedTheme.colors.background, Color(0xFFF8F6F0))
        val surface = parseComposeColor(resolvedTheme.colors.surface, Color.White)
        val text = parseComposeColor(resolvedTheme.colors.text, Color(0xFF1C1B1F))
        val mutedText = parseComposeColor(resolvedTheme.colors.mutedText, Color(0xFF707070))
        val accent = parseComposeColor(resolvedTheme.colors.accent, Color(0xFF6750A4))
        val border = parseComposeColor(resolvedTheme.colors.border, Color(0xFFCCCCCC))

        val isDark = resolvedTheme.isDark || ThemeManager.isColorDark(ThemeManager.parseColor(resolvedTheme.colors.background))
        val onAccent = if (ThemeManager.isColorDark(ThemeManager.parseColor(resolvedTheme.colors.accent))) Color.White else Color.Black

        if (isDark) {
            darkColorScheme(
                primary = accent,
                onPrimary = onAccent,
                primaryContainer = surface,
                onPrimaryContainer = text,
                secondary = accent,
                onSecondary = onAccent,
                background = bg,
                onBackground = text,
                surface = surface,
                onSurface = text,
                surfaceVariant = surface,
                onSurfaceVariant = mutedText,
                surfaceContainer = surface,
                outline = border,
                outlineVariant = border
            )
        } else {
            lightColorScheme(
                primary = accent,
                onPrimary = onAccent,
                primaryContainer = surface,
                onPrimaryContainer = text,
                secondary = accent,
                onSecondary = onAccent,
                background = bg,
                onBackground = text,
                surface = surface,
                onSurface = text,
                surfaceVariant = surface,
                onSurfaceVariant = mutedText,
                surfaceContainer = surface,
                outline = border,
                outlineVariant = border
            )
        }
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
