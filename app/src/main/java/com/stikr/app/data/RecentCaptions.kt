package com.stikr.app.data

import android.content.Context
import org.json.JSONArray

/** A short, on-device history of captions, newest first. */
class RecentCaptions(context: Context) {
    private val prefs = context.getSharedPreferences("recent-captions", Context.MODE_PRIVATE)

    fun all(): List<String> = runCatching {
        val raw = prefs.getString("captions", null) ?: return emptyList()
        val array = JSONArray(raw)
        (0 until array.length()).map { array.optString(it) }.filter { it.isNotBlank() }
    }.getOrDefault(emptyList())

    fun record(text: String) {
        val caption = text.trim()
        if (caption.isEmpty()) return
        val updated = (listOf(caption) + all().filter { it != caption }).take(6)
        prefs.edit().putString("captions", JSONArray(updated).toString()).apply()
    }
}
