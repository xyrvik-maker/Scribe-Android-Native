package com.primaloptima.scribe.util

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.primaloptima.scribe.ScribeApp
import com.primaloptima.scribe.util.model.AppTheme
import kotlinx.coroutines.launch

/**
 * Base class every Scribe activity extends. It:
 *
 *  1. Applies the current [AppTheme] to the decor tree in `onResume`
 *     so newly-inflated views pick up token colors.
 *  2. Subscribes to [ThemeBus] and re-applies whenever the user changes
 *     themes, so all open surfaces stay in sync without a restart.
 *
 * Activities that need custom theming can override [onThemeApplied] to
 * apply extras (e.g. colouring a custom widget) after the base pass.
 */
abstract class ThemedActivity : AppCompatActivity() {

    protected val themeManager: ThemeManager
        get() = (application as ScribeApp).themeManager

    private var lastAppliedThemeId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                ThemeBus.events.collect { theme ->
                    if (theme.id != lastAppliedThemeId) {
                        applyTheme(theme)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applyTheme(themeManager.activeTheme())
    }

    protected fun applyTheme(theme: AppTheme) {
        val bg = ThemeManager.parseColor(theme.colors.background)
        val surface = ThemeManager.parseColor(theme.colors.surface)
        val text = ThemeManager.parseColor(theme.colors.text)
        val muted = ThemeManager.parseColor(theme.colors.mutedText)
        val accent = ThemeManager.parseColor(theme.colors.accent)
        val border = ThemeManager.parseColor(theme.colors.border)
        val toolbar = ThemeManager.parseColor(theme.colors.toolbar)
        val toolbarText = ThemeManager.parseColor(theme.colors.toolbarText)

        window.decorView.setBackgroundColor(bg)
        window.statusBarColor = toolbar
        // Match status-bar icon color to the toolbar background luminance.
        val useLightIcons = ContrastUtil.relativeLuminance(toolbar) < 0.5
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            val ctrl = window.insetsController
            val flags = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            ctrl?.setSystemBarsAppearance(if (useLightIcons) 0 else flags, flags)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                if (useLightIcons) window.decorView.systemUiVisibility and
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                else window.decorView.systemUiVisibility or
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        val root = findViewById<View>(android.R.id.content) ?: return
        walkAndTint(
            root,
            Tokens(bg, surface, text, muted, accent, border, toolbar, toolbarText)
        )

        lastAppliedThemeId = theme.id
        onThemeApplied(theme)
    }

    /** Hook for subclasses to apply custom theming on top of the base. */
    protected open fun onThemeApplied(theme: AppTheme) {}

    private data class Tokens(
        val bg: Int, val surface: Int, val text: Int, val muted: Int,
        val accent: Int, val border: Int, val toolbar: Int, val toolbarText: Int
    )

    private fun walkAndTint(view: View, t: Tokens) {
        when (view) {
            is Toolbar -> {
                view.setBackgroundColor(t.toolbar)
                view.setTitleTextColor(t.toolbarText)
                view.setSubtitleTextColor(t.toolbarText)
                view.navigationIcon?.setTint(t.toolbarText)
                view.overflowIcon?.setTint(t.toolbarText)
            }
            is AppBarLayout -> view.setBackgroundColor(t.toolbar)
            is TabLayout -> {
                view.setBackgroundColor(t.toolbar)
                view.setTabTextColors(t.muted, t.accent)
                view.setSelectedTabIndicatorColor(t.accent)
            }
            is FloatingActionButton -> {
                view.backgroundTintList = android.content.res.ColorStateList.valueOf(t.accent)
                view.imageTintList = android.content.res.ColorStateList.valueOf(
                    ContrastUtil.onColorFor(t.accent)
                )
            }
            is MaterialButton -> {
                // Only tint if the button hasn't been given an explicit non-null
                // background tint by the layout.
                if (view.tag != "theme:preserve") {
                    view.setTextColor(ContrastUtil.onColorFor(t.accent))
                    view.setBackgroundColor(t.accent)
                }
            }
            is MaterialCardView -> {
                view.setCardBackgroundColor(t.surface)
                view.strokeColor = t.border
            }
            is TextInputLayout -> {
                view.boxBackgroundColor = t.surface
                view.boxStrokeColor = t.accent
                view.defaultHintTextColor = android.content.res.ColorStateList.valueOf(t.muted)
                view.hintTextColor = android.content.res.ColorStateList.valueOf(t.muted)
            }
            is TextInputEditText -> {
                view.setTextColor(t.text)
                view.setHintTextColor(t.muted)
            }
            is EditText -> {
                if (view.tag != "theme:preserve") {
                    view.setTextColor(t.text)
                    view.setHintTextColor(t.muted)
                }
            }
            is TextView -> {
                when (view.tag as? String) {
                    "theme:muted" -> view.setTextColor(t.muted)
                    "theme:accent" -> view.setTextColor(t.accent)
                    "theme:toolbar" -> view.setTextColor(t.toolbarText)
                    "theme:preserve" -> {}
                    else -> view.setTextColor(t.text)
                }
            }
            is ImageView -> {
                when (view.tag as? String) {
                    "theme:muted" -> view.setColorFilter(t.muted)
                    "theme:accent" -> view.setColorFilter(t.accent)
                    "theme:toolbar" -> view.setColorFilter(t.toolbarText)
                    "theme:text" -> view.setColorFilter(t.text)
                    "theme:preserve" -> {}
                    else -> { /* leave alone; icons often need their own tint */ }
                }
            }
        }

        // Untagged root containers get the background token so panels
        // and drawers pick up the theme.
        if (view is ViewGroup) {
            val tag = view.tag as? String
            if (tag == "theme:surface") view.setBackgroundColor(t.surface)
            for (i in 0 until view.childCount) {
                walkAndTint(view.getChildAt(i), t)
            }
        }
    }

    // Small helpers for subclasses that need on-demand token access.
    protected fun tokenBackground(): Int =
        ThemeManager.parseColor(themeManager.activeTheme().colors.background)
    protected fun tokenText(): Int =
        ThemeManager.parseColor(themeManager.activeTheme().colors.text)
    protected fun tokenAccent(): Int =
        ThemeManager.parseColor(themeManager.activeTheme().colors.accent)
    protected fun tokenSurface(): Int =
        ThemeManager.parseColor(themeManager.activeTheme().colors.surface)
    protected fun tokenMuted(): Int =
        ThemeManager.parseColor(themeManager.activeTheme().colors.mutedText)
    protected fun tokenBorder(): Int =
        ThemeManager.parseColor(themeManager.activeTheme().colors.border)

    /** Convenience: build a themed rounded rectangle drawable at radius [radius] dp. */
    protected fun themedRoundedRect(radiusDp: Float, fill: Int = tokenSurface(), stroke: Int? = null): GradientDrawable {
        val scale = resources.displayMetrics.density
        return GradientDrawable().apply {
            cornerRadius = radiusDp * scale
            setColor(fill)
            if (stroke != null) setStroke((1 * scale).toInt(), stroke)
        }
    }

    @Suppress("unused")
    protected fun themedColorDrawable(color: Int): ColorDrawable = ColorDrawable(color)

    // Keep a reference so we don't warn on unused import.
    @Suppress("unused")
    private fun quietUnused() { ViewCompat.setBackgroundTintList(window.decorView, null); Color.TRANSPARENT.hashCode() }
}
