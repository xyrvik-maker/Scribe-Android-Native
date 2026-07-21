package com.primaloptima.scribe.util

import com.primaloptima.scribe.util.model.AppTheme
import com.primaloptima.scribe.util.model.ThemeColors

/**
 * The eight curated built-in themes. Each palette has been checked for
 * WCAG AA contrast on body text (>= 4.5:1 for `text` on `background`,
 * >= 3.0:1 for `toolbarText` on `toolbar`).
 *
 * Order matters — the first entry is used as the fallback when the
 * stored active theme id can no longer be resolved.
 */
object DefaultThemes {

    val all: List<AppTheme> = listOf(

        // ── Paper — warm off-white, writerly serif default ────────────
        AppTheme(
            id = "paper", name = "Paper", isDark = false, builtIn = true,
            colors = ThemeColors(
                background = "#f6f0e2", surface = "#efe6d1",
                text = "#241c14", mutedText = "#6b5c46", accent = "#b06520",
                border = "#dccfae", selection = "#e9c98a",
                toolbar = "#efe6d1", toolbarText = "#241c14"
            ),
            fontFamily = "serif", fontSize = 18, lineHeight = 1.7f,
            letterSpacing = 0.2f, paragraphSpacing = 14,
            paddingHorizontal = 24, paddingVertical = 20, maxWidth = 720
        ),

        // ── Ink — near-black on cream, high-contrast reading ─────────
        AppTheme(
            id = "ink", name = "Ink", isDark = false, builtIn = true,
            colors = ThemeColors(
                background = "#faf7ef", surface = "#f1ebd9",
                text = "#141210", mutedText = "#5a544a", accent = "#7a2f14",
                border = "#d8d0bb", selection = "#e4d9b7",
                toolbar = "#f1ebd9", toolbarText = "#141210"
            ),
            fontFamily = "serif", fontSize = 18, lineHeight = 1.7f,
            letterSpacing = 0.15f, paragraphSpacing = 14,
            paddingHorizontal = 26, paddingVertical = 20, maxWidth = 720
        ),

        // ── Slate — cool dark, everyday dark theme ───────────────────
        AppTheme(
            id = "slate", name = "Slate", isDark = true, builtIn = true,
            colors = ThemeColors(
                background = "#1e2228", surface = "#262b33",
                text = "#e5e8ee", mutedText = "#9aa2b0", accent = "#a4d64c",
                border = "#333a45", selection = "#3a4657",
                toolbar = "#262b33", toolbarText = "#e5e8ee"
            ),
            fontFamily = "sans", fontSize = 17, lineHeight = 1.65f,
            letterSpacing = 0f, paragraphSpacing = 12,
            paddingHorizontal = 22, paddingVertical = 18, maxWidth = 720
        ),

        // ── Midnight — deep indigo, softer than pure black ───────────
        AppTheme(
            id = "midnight", name = "Midnight", isDark = true, builtIn = true,
            colors = ThemeColors(
                background = "#10131c", surface = "#181c28",
                text = "#e7e6ee", mutedText = "#8a90a3", accent = "#7aa2f7",
                border = "#262b3a", selection = "#3d4a73",
                toolbar = "#181c28", toolbarText = "#e7e6ee"
            ),
            fontFamily = "sans", fontSize = 17, lineHeight = 1.65f,
            letterSpacing = 0f, paragraphSpacing = 12,
            paddingHorizontal = 22, paddingVertical = 18, maxWidth = 720
        ),

        // ── Sepia — writerly warm brown ──────────────────────────────
        AppTheme(
            id = "sepia", name = "Sepia", isDark = false, builtIn = true,
            colors = ThemeColors(
                background = "#f2e6cc", surface = "#e8d7b3",
                text = "#2d200f", mutedText = "#7a6a4a", accent = "#8f4a12",
                border = "#cdb98c", selection = "#d6b57a",
                toolbar = "#e8d7b3", toolbarText = "#2d200f"
            ),
            fontFamily = "serif", fontSize = 19, lineHeight = 1.75f,
            letterSpacing = 0.3f, paragraphSpacing = 16,
            paddingHorizontal = 28, paddingVertical = 22, maxWidth = 680
        ),

        // ── Forest — muted green dark ────────────────────────────────
        AppTheme(
            id = "forest", name = "Forest", isDark = true, builtIn = true,
            colors = ThemeColors(
                background = "#12211a", surface = "#1a2c22",
                text = "#e6ecdc", mutedText = "#96a58f", accent = "#8fbf6b",
                border = "#294036", selection = "#3a5544",
                toolbar = "#1a2c22", toolbarText = "#e6ecdc"
            ),
            fontFamily = "serif", fontSize = 18, lineHeight = 1.7f,
            letterSpacing = 0.1f, paragraphSpacing = 14,
            paddingHorizontal = 24, paddingVertical = 20, maxWidth = 700
        ),

        // ── Ocean — deep navy ────────────────────────────────────────
        AppTheme(
            id = "ocean", name = "Ocean", isDark = true, builtIn = true,
            colors = ThemeColors(
                background = "#0c1a2a", surface = "#132538",
                text = "#e2ecf6", mutedText = "#8fa4bc", accent = "#5cb9d6",
                border = "#22354b", selection = "#2f4e6d",
                toolbar = "#132538", toolbarText = "#e2ecf6"
            ),
            fontFamily = "sans", fontSize = 17, lineHeight = 1.7f,
            letterSpacing = 0f, paragraphSpacing = 12,
            paddingHorizontal = 22, paddingVertical = 20, maxWidth = 720
        ),

        // ── Vellum — light parchment cream, softer than Paper ────────
        AppTheme(
            id = "vellum", name = "Vellum", isDark = false, builtIn = true,
            colors = ThemeColors(
                background = "#faf5ea", surface = "#f2ead6",
                text = "#2e2822", mutedText = "#7a6f5f", accent = "#a8551f",
                border = "#e0d5ba", selection = "#ecd5a3",
                toolbar = "#f2ead6", toolbarText = "#2e2822"
            ),
            fontFamily = "serif", fontSize = 18, lineHeight = 1.75f,
            letterSpacing = 0.25f, paragraphSpacing = 15,
            paddingHorizontal = 26, paddingVertical = 22, maxWidth = 700
        )
    )

    fun findById(id: String): AppTheme = all.firstOrNull { it.id == id } ?: all.first()
}
