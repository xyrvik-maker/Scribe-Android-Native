package com.primaloptima.scribe.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.primaloptima.scribe.util.DefaultThemes
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
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val resolvedTheme = appTheme ?: try {
        ThemeManager(context).activeTheme()
    } catch (_: Exception) {
        DefaultThemes.all.first()
    }

    val bg = parseComposeColor(resolvedTheme.colors.background, Color(0xFFFAFAF7))
    val surface = parseComposeColor(resolvedTheme.colors.surface, Color.White)
    val text = parseComposeColor(resolvedTheme.colors.text, Color(0xFF1A1A1A))
    val mutedText = parseComposeColor(resolvedTheme.colors.mutedText, text)
    val accentIcons = parseComposeColor(resolvedTheme.colors.accent, Color(0xFF333333))
    val border = parseComposeColor(resolvedTheme.colors.border, Color(0xFFE0E0D8))
    val surfaceVariant = parseComposeColor(resolvedTheme.colors.surface, surface)

    val isLight = !resolvedTheme.isDark
    val onPrimaryColor = if (ThemeManager.isColorDark(ThemeManager.parseColor(resolvedTheme.colors.accent))) Color.White else Color.Black

    val rawColorScheme: ColorScheme = if (isLight) {
        lightColorScheme(
            primary = accentIcons,
            onPrimary = onPrimaryColor,
            primaryContainer = surface,
            onPrimaryContainer = text,
            secondary = accentIcons,
            onSecondary = onPrimaryColor,
            background = bg,
            onBackground = text,
            surface = surface,
            onSurface = text,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = text,
            surfaceContainer = surface,
            surfaceContainerHigh = surface,
            surfaceContainerLow = bg,
            outline = border,
            outlineVariant = border
        )
    } else {
        darkColorScheme(
            primary = accentIcons,
            onPrimary = onPrimaryColor,
            primaryContainer = surface,
            onPrimaryContainer = text,
            secondary = accentIcons,
            onSecondary = onPrimaryColor,
            background = bg,
            onBackground = text,
            surface = surface,
            onSurface = text,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = text,
            surfaceContainer = surface,
            surfaceContainerHigh = surface,
            surfaceContainerLow = bg,
            outline = border,
            outlineVariant = border
        )
    }

    // Step 6: Smooth Theme Transition Animation (400ms duration)
    val duration = 400
    val animSpec = tween<Color>(durationMillis = duration)

    val animPrimary by animateColorAsState(rawColorScheme.primary, animSpec, label = "primary")
    val animOnPrimary by animateColorAsState(rawColorScheme.onPrimary, animSpec, label = "onPrimary")
    val animBg by animateColorAsState(rawColorScheme.background, animSpec, label = "bg")
    val animOnBg by animateColorAsState(rawColorScheme.onBackground, animSpec, label = "onBg")
    val animSurface by animateColorAsState(rawColorScheme.surface, animSpec, label = "surface")
    val animOnSurface by animateColorAsState(rawColorScheme.onSurface, animSpec, label = "onSurface")
    val animSurfaceVariant by animateColorAsState(rawColorScheme.surfaceVariant, animSpec, label = "surfaceVariant")
    val animOnSurfaceVariant by animateColorAsState(rawColorScheme.onSurfaceVariant, animSpec, label = "onSurfaceVariant")
    val animOutline by animateColorAsState(rawColorScheme.outline, animSpec, label = "outline")

    val animatedColorScheme = rawColorScheme.copy(
        primary = animPrimary,
        onPrimary = animOnPrimary,
        primaryContainer = animSurface,
        onPrimaryContainer = animOnSurface,
        secondary = animPrimary,
        onSecondary = animOnPrimary,
        background = animBg,
        onBackground = animOnBg,
        surface = animSurface,
        onSurface = animOnSurface,
        surfaceVariant = animSurfaceVariant,
        onSurfaceVariant = animOnSurfaceVariant,
        surfaceContainer = animSurface,
        surfaceContainerHigh = animSurface,
        surfaceContainerLow = animBg,
        outline = animOutline,
        outlineVariant = animOutline
    )

    // Step 2: System bar color & icon adaptation using Accompanist SystemUiController
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setSystemBarsColor(
            color = animSurface,
            darkIcons = isLight
        )
    }

    MaterialTheme(
        colorScheme = animatedColorScheme,
        content = {
            val bgUri = resolvedTheme.backgroundImageUri
            val bgOpacity = resolvedTheme.backgroundImageOpacity ?: 0.35f

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(animatedColorScheme.background)
            ) {
                if (!bgUri.isNullOrEmpty()) {
                    AsyncImage(
                        model = bgUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = bgOpacity))
                    )
                }

                content()
            }
        }
    )
}
