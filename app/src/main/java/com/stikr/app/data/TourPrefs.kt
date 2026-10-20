package com.stikr.app.data

import android.content.Context

/** Remembers whether the first-run tour has been seen. */
class TourPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("stikr", Context.MODE_PRIVATE)

    var done: Boolean
        get() = prefs.getBoolean("tour_done", false)
        set(value) = prefs.edit().putBoolean("tour_done", value).apply()
}
