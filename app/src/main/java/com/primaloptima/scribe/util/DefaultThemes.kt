package com.primaloptima.scribe.util

import com.primaloptima.scribe.util.model.AppTheme
import com.primaloptima.scribe.util.model.ThemeColors

object DefaultThemes {

    val all: List<AppTheme> = listOf(
        AppTheme(
            id = "obsidian", name = "Obsidian", isDark = true, builtIn = true,
            colors = ThemeColors(
                background = "#121214", surface = "#1C1C20",
                text = "#FFFFFF", mutedText = "#A0A0B0", accent = "#A2E048",
                border = "#2A2A32", selection = "#334422",
                toolbar = "#18181C", toolbarText = "#FFFFFF"
            ),
            fontFamily = "sans", fontSize = 17, lineHeight = 1.65f,
            letterSpacing = 0f, paragraphSpacing = 12,
            paddingHorizontal = 22, paddingVertical = 18, maxWidth = 720
        ),
        AppTheme(
            id = "paper", name = "Paper", isDark = false, builtIn = true,
            colors = ThemeColors(
                background = "#f5efe4", surface = "#ede5d4",
                text = "#2a2622", mutedText = "#7a6f5d", accent = "#a8651e",
                border = "#d8cfb9", selection = "#e8c89c",
                toolbar = "#ede5d4", toolbarText = "#2a2622"
            ),
            fontFamily = "serif", fontSize = 18, lineHeight = 1.7f,
            letterSpacing = 0.2f, paragraphSpacing = 14,
            paddingHorizontal = 24, paddingVertical = 20, maxWidth = 720
        ),
        AppTheme(
            id = "midnight", name = "Midnight Blue", isDark = true, builtIn = true,
            colors = ThemeColors(
                background = "#0F172A", surface = "#1E293B",
                text = "#F8FAFC", mutedText = "#94A3B8", accent = "#38BDF8",
                border = "#334155", selection = "#1E3A5F",
                toolbar = "#1E293B", toolbarText = "#F8FAFC"
            ),
            fontFamily = "sans", fontSize = 17, lineHeight = 1.65f,
            letterSpacing = 0f, paragraphSpacing = 12,
            paddingHorizontal = 22, paddingVertical = 18, maxWidth = 720
        ),
        AppTheme(
            id = "sepia", name = "Sepia", isDark = false, builtIn = true,
            colors = ThemeColors(
                background = "#efe2c6", surface = "#e6d6b3",
                text = "#3b2e1c", mutedText = "#8a7551", accent = "#9c5a16",
                border = "#cfbc94", selection = "#d4b881",
                toolbar = "#e6d6b3", toolbarText = "#3b2e1c"
            ),
            fontFamily = "serif", fontSize = 19, lineHeight = 1.75f,
            letterSpacing = 0.3f, paragraphSpacing = 16,
            paddingHorizontal = 28, paddingVertical = 22, maxWidth = 680
        ),
        AppTheme(
            id = "typewriter", name = "Typewriter", isDark = false, builtIn = true,
            colors = ThemeColors(
                background = "#fafaf7", surface = "#f0f0ec",
                text = "#1a1a1a", mutedText = "#6b6b6b", accent = "#1a1a1a",
                border = "#dcdcd8", selection = "#cfcfc8",
                toolbar = "#f0f0ec", toolbarText = "#1a1a1a"
            ),
            fontFamily = "mono", fontSize = 16, lineHeight = 1.8f,
            letterSpacing = 0f, paragraphSpacing = 14,
            paddingHorizontal = 22, paddingVertical = 20, maxWidth = 680
        ),
        AppTheme(
            id = "focus", name = "Focus", isDark = true, builtIn = true,
            colors = ThemeColors(
                background = "#1a1a1a", surface = "#222222",
                text = "#d4d4d4", mutedText = "#777777", accent = "#A2E048",
                border = "#2e2e2e", selection = "#3a3a3a",
                toolbar = "#222222", toolbarText = "#d4d4d4"
            ),
            fontFamily = "mono", fontSize = 17, lineHeight = 1.7f,
            letterSpacing = 0f, paragraphSpacing = 14,
            paddingHorizontal = 26, paddingVertical = 22, maxWidth = 700
        )
    )

    fun findById(id: String): AppTheme = all.firstOrNull { it.id == id } ?: all.first()
}
