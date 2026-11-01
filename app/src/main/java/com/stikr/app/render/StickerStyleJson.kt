package com.stikr.app.render

import org.json.JSONArray
import org.json.JSONObject

/** Optional fields preserve older edits; nested layers are bounded to one level. */
object StickerStyleJson {
    fun read(json: JSONObject, layers: Boolean = true): StickerRenderer.Style = StickerRenderer.Style(
        outline = json.optInt("outline", 0),
        outlineColor = runCatching { StickerRenderer.OutlineColor.valueOf(json.optString("outlineColor")) }.getOrDefault(StickerRenderer.OutlineColor.WHITE),
        caption = json.optString("caption"),
        captionPosition = runCatching { StickerRenderer.CaptionPosition.valueOf(json.optString("captionPosition")) }.getOrDefault(StickerRenderer.CaptionPosition.BOTTOM),
        captionColor = runCatching { StickerRenderer.CaptionColor.valueOf(json.optString("captionColor")) }.getOrDefault(StickerRenderer.CaptionColor.CREAM),
        captionFont = json.optString("captionFont", "sans"), captionSize = json.optInt("captionSize", 40),
        captionBackground = json.optInt("captionBackground", 0),
        captionTextColor = if (json.has("captionTextColor") && !json.isNull("captionTextColor")) json.optInt("captionTextColor") else null,
        captionBold = json.optBoolean("captionBold"), captionItalic = json.optBoolean("captionItalic"),
        captionUnderline = json.optBoolean("captionUnderline"), captionStrike = json.optBoolean("captionStrike"),
        captionX = coordinate(json, "captionX"), captionY = coordinate(json, "captionY"),
        captionRotation = json.optDouble("captionRotation", 0.0).toFloat().takeIf { it.isFinite() } ?: 0f,
        captionAlign = json.optString("captionAlign", "center"), roundedCorners = json.optBoolean("roundedCorners"),
        extraCaptions = if (layers) json.optJSONArray("extraCaptions")?.let { a ->
            (0 until minOf(a.length(), 19)).mapNotNull { a.optJSONObject(it)?.let { read(it, false) } }
        }.orEmpty() else emptyList(),
    )

    private fun coordinate(json: JSONObject, key: String): Float? =
        if (!json.has(key) || json.isNull(key)) null else json.optDouble(key).toFloat().takeIf { it.isFinite() }?.coerceIn(0f, 1f)

    fun write(style: StickerRenderer.Style, json: JSONObject = JSONObject(), layers: Boolean = true): JSONObject = json
        .put("outline", style.outline).put("outlineColor", style.outlineColor.name)
        .put("caption", style.caption).put("captionPosition", style.captionPosition.name).put("captionColor", style.captionColor.name)
        .put("captionFont", style.captionFont).put("captionSize", style.captionSize).put("captionBackground", style.captionBackground)
        .put("captionTextColor", style.captionTextColor ?: JSONObject.NULL).put("captionBold", style.captionBold)
        .put("captionItalic", style.captionItalic).put("captionUnderline", style.captionUnderline).put("captionStrike", style.captionStrike)
        .put("captionX", style.captionX ?: JSONObject.NULL).put("captionY", style.captionY ?: JSONObject.NULL)
        .put("captionRotation", style.captionRotation).put("captionAlign", style.captionAlign).put("roundedCorners", style.roundedCorners)
        .put("extraCaptions", JSONArray().apply { if (layers) style.extraCaptions.take(19).forEach { put(write(it, layers = false)) } })
}
