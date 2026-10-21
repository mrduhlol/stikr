package com.stikr.app.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Stable preference values survive renames of display labels. */
enum class AppearanceMode(val label: String, val description: String) {
    SYSTEM("System", "Match your device appearance"),
    LIGHT("Light", "Warm paper and bright surfaces"),
    DARK("Dark", "Soft dark surfaces"),
    AMOLED("AMOLED", "Pure black background"),
}

class AppearanceSettings(context: Context) {
    private val preferences = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
    private val _mode = MutableStateFlow(
        runCatching { AppearanceMode.valueOf(preferences.getString("mode", null).orEmpty()) }
            .getOrDefault(AppearanceMode.SYSTEM),
    )
    val mode = _mode.asStateFlow()

    fun select(mode: AppearanceMode) {
        preferences.edit().putString("mode", mode.name).apply()
        _mode.value = mode
    }
}
