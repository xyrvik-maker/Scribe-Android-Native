package com.primaloptima.scribe.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.primaloptima.scribe.ScribeApp
import com.primaloptima.scribe.util.model.AppTheme

class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val themeManager = (application as ScribeApp).themeManager

    private val _themes = MutableLiveData<List<AppTheme>>()
    val themes: LiveData<List<AppTheme>> = _themes

    private val _activeTheme = MutableLiveData<AppTheme>()
    val activeTheme: LiveData<AppTheme> = _activeTheme

    init { reload() }

    fun reload() {
        _themes.value = themeManager.allThemes()
        _activeTheme.value = themeManager.activeTheme()
    }

    fun setActive(id: String) {
        themeManager.setActiveTheme(id)
        reload()
    }

    fun save(theme: AppTheme) {
        themeManager.saveCustomTheme(theme)
        reload()
    }

    fun delete(id: String) {
        themeManager.deleteCustomTheme(id)
        reload()
    }

    fun duplicate(id: String): AppTheme? {
        val copy = themeManager.duplicateTheme(id) ?: return null
        reload()
        return copy
    }

    fun generateId(): String =
        System.currentTimeMillis().toString() + Math.random().toString().takeLast(6)
}
