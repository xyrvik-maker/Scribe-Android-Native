package com.primaloptima.scribe.util

import com.primaloptima.scribe.util.model.AppTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Broadcast bus for theme changes. `ThemeManager.setActiveTheme(id)`
 * emits the newly-active [AppTheme] here. `ThemedActivity` and any
 * theme-aware adapter/fragment collects and re-tints without an activity
 * restart.
 *
 * Replay = 1 so a subscriber that connects after the emission still
 * receives the current theme.
 */
object ThemeBus {

    private val _events: MutableSharedFlow<AppTheme> =
        MutableSharedFlow(replay = 1, extraBufferCapacity = 4)

    val events: SharedFlow<AppTheme> = _events.asSharedFlow()

    /** Fire-and-forget emission from any thread. */
    fun emit(theme: AppTheme) {
        _events.tryEmit(theme)
    }
}
