package com.stikr.app.data

import android.content.Context
import com.stikr.app.render.StickerRenderer
import com.stikr.app.render.StickerStyleJson
import org.json.JSONObject

/**
 * Remembers the look of the last edited sticker so a run of stickers starts
 * where the previous one finished. Caption text and text-layer placement are
 * deliberately not remembered — they belong to a single sticker.
 */
class StylePrefs(context: Context) {
    private val prefs = context.getSharedPreferences("editor-style", Context.MODE_PRIVATE)

    var last: StickerRenderer.Style
        get() {
            val raw = prefs.getString("style", null) ?: return StickerRenderer.Style()
            return runCatching { StickerStyleJson.read(JSONObject(raw)) }
                .getOrDefault(StickerRenderer.Style())
        }
        set(value) {
            val reusable = value.copy(
                caption = "",
                captionX = null,
                captionY = null,
                extraCaptions = emptyList(),
            )
            prefs.edit().putString("style", StickerStyleJson.write(reusable).toString()).apply()
        }
}
