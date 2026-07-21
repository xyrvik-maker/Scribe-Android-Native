package com.primaloptima.scribe.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.primaloptima.scribe.util.model.AppTheme
import com.primaloptima.scribe.util.model.ThemeColors

/**
 * Extract a themed palette from a user-picked background image.
 *
 * The heuristic:
 *  - `accent`     ← vibrant swatch (falls back to lightVibrant, dominant)
 *  - `surface`    ← muted swatch (falls back to lightMuted, dominant)
 *  - `background` ← darkMuted for dark themes, lightMuted for light
 *  - `text`       ← on-color computed via ContrastUtil.autoFix so body
 *                   text is AA-compliant against `background`
 *
 * Downsizes the bitmap to <= 128 px on the longest edge before palette
 * extraction so we don't OOM on huge photos.
 */
object PaletteExtractor {

    private const val MAX_EDGE = 128

    fun extract(context: Context, uri: Uri, base: AppTheme): AppTheme? {
        val bitmap = decodeSampled(context, uri) ?: return null
        val palette = Palette.from(bitmap).maximumColorCount(24).generate()

        val vibrant = palette.vibrantSwatch ?: palette.lightVibrantSwatch
            ?: palette.dominantSwatch
        val muted = palette.mutedSwatch ?: palette.lightMutedSwatch
            ?: palette.dominantSwatch
        val darkMuted = palette.darkMutedSwatch ?: palette.darkVibrantSwatch
            ?: palette.dominantSwatch
        val lightMuted = palette.lightMutedSwatch ?: palette.lightVibrantSwatch
            ?: palette.dominantSwatch

        val accentInt = vibrant?.rgb ?: return null
        val isDark = base.isDark
        val backgroundInt = if (isDark) {
            darkMuted?.rgb ?: shift(accentInt, lDelta = -0.35f)
        } else {
            lightMuted?.rgb ?: shift(accentInt, lDelta = +0.4f)
        }
        val surfaceInt = if (isDark) {
            shift(backgroundInt, lDelta = +0.06f)
        } else {
            shift(backgroundInt, lDelta = -0.04f)
        }
        val textInt = ContrastUtil.autoFix(
            fg = if (isDark) 0xFFECECEC.toInt() else 0xFF141210.toInt(),
            bg = backgroundInt,
            target = ContrastUtil.AA_BODY
        )
        val mutedTextInt = ContrastUtil.autoFix(
            fg = ColorUtils.blendARGB(textInt, backgroundInt, 0.45f),
            bg = backgroundInt,
            target = ContrastUtil.AA_LARGE
        )
        val borderInt = if (isDark) shift(surfaceInt, lDelta = +0.05f)
                        else shift(surfaceInt, lDelta = -0.06f)
        val selectionInt = ColorUtils.blendARGB(accentInt, backgroundInt, 0.55f)

        return base.copy(
            colors = ThemeColors(
                background = hex(backgroundInt),
                surface = hex(surfaceInt),
                text = hex(textInt),
                mutedText = hex(mutedTextInt),
                accent = hex(accentInt),
                border = hex(borderInt),
                selection = hex(selectionInt),
                toolbar = hex(surfaceInt),
                toolbarText = hex(textInt)
            )
        )
    }

    private fun decodeSampled(context: Context, uri: Uri): Bitmap? = try {
        if (Build.VERSION.SDK_INT >= 28) {
            val src = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                val w = info.size.width
                val h = info.size.height
                val longest = maxOf(w, h)
                if (longest > MAX_EDGE) {
                    val scale = MAX_EDGE.toFloat() / longest
                    decoder.setTargetSize((w * scale).toInt().coerceAtLeast(1),
                                          (h * scale).toInt().coerceAtLeast(1))
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = false
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { s ->
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(s, null, boundsOpts)
                val longest = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
                var sample = 1
                while (longest / sample > MAX_EDGE) sample *= 2
                context.contentResolver.openInputStream(uri)?.use { s2 ->
                    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                    BitmapFactory.decodeStream(s2, null, opts)
                } ?: null
            }
        }
    } catch (_: Exception) { null }

    private fun shift(color: Int, lDelta: Float): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        hsl[2] = (hsl[2] + lDelta).coerceIn(0f, 1f)
        return ColorUtils.HSLToColor(hsl)
    }

    private fun hex(color: Int): String =
        String.format("#%06X", 0xFFFFFF and color)
}
