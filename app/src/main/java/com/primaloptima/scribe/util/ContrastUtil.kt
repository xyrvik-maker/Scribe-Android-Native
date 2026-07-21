package com.primaloptima.scribe.util

import android.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * WCAG contrast utilities.
 *
 * References: https://www.w3.org/TR/WCAG21/#contrast-minimum
 * - 4.5:1 required for normal body text.
 * - 3.0:1 required for large text (>= 18pt / 14pt bold).
 * - 3.0:1 required for UI component boundaries.
 */
object ContrastUtil {

    const val AA_BODY: Double = 4.5
    const val AA_LARGE: Double = 3.0

    /** Relative luminance per WCAG. Accepts an sRGB Int (0xAARRGGBB or 0xRRGGBB). */
    fun relativeLuminance(color: Int): Double {
        val r = channel(Color.red(color))
        val g = channel(Color.green(color))
        val b = channel(Color.blue(color))
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun channel(c: Int): Double {
        val s = c / 255.0
        return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }

    /** Contrast ratio between two sRGB colors. Range [1.0, 21.0]. */
    fun ratio(fg: Int, bg: Int): Double {
        val l1 = relativeLuminance(fg)
        val l2 = relativeLuminance(bg)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    fun ratio(fgHex: String, bgHex: String): Double =
        try { ratio(Color.parseColor(fgHex), Color.parseColor(bgHex)) } catch (_: Exception) { 1.0 }

    fun passesBody(fg: Int, bg: Int): Boolean = ratio(fg, bg) >= AA_BODY
    fun passesLarge(fg: Int, bg: Int): Boolean = ratio(fg, bg) >= AA_LARGE

    /**
     * Return a foreground color adjusted (lightened for dark backgrounds,
     * darkened for light backgrounds) until it reaches at least [target]
     * contrast against [bg]. Returns [fg] unchanged if already passes.
     */
    fun autoFix(fg: Int, bg: Int, target: Double = AA_BODY): Int {
        if (ratio(fg, bg) >= target) return fg
        val bgLum = relativeLuminance(bg)
        val hsl = FloatArray(3)
        val alpha = Color.alpha(fg)
        androidx.core.graphics.ColorUtils.colorToHSL(fg, hsl)
        val direction = if (bgLum < 0.5) +1f else -1f  // lift on dark, darken on light
        var l = hsl[2]
        var attempts = 0
        while (attempts < 40) {
            l = (l + direction * 0.025f).coerceIn(0f, 1f)
            hsl[2] = l
            val adjusted = androidx.core.graphics.ColorUtils.HSLToColor(hsl)
            val withAlpha = Color.argb(alpha, Color.red(adjusted), Color.green(adjusted), Color.blue(adjusted))
            if (ratio(withAlpha, bg) >= target) return withAlpha
            attempts++
            if (l <= 0f || l >= 1f) break
        }
        // Fallback: pure black or white.
        return if (bgLum < 0.5) Color.WHITE else Color.BLACK
    }

    /** Pick the better of black/white to render text on top of [bg]. */
    fun onColorFor(bg: Int): Int {
        val onWhite = ratio(Color.WHITE, bg)
        val onBlack = ratio(Color.BLACK, bg)
        return if (onWhite >= onBlack) Color.WHITE else Color.BLACK
    }
}
