package com.stikr.app.render

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.stikr.app.R

/** Shared Android typefaces so editor previews and exported bitmaps use identical glyphs. */
object StickerTypography {
    val fonts = listOf("sans", "serif", "mono", "display")
    @Volatile private var display: Typeface? = null

    fun initialize(context: Context) {
        if (display == null) display = ResourcesCompat.getFont(context, R.font.anton_regular)
    }

    fun typeface(font: String, bold: Boolean = false, italic: Boolean = false): Typeface {
        val base = when (font) {
            "serif" -> Typeface.SERIF
            "mono" -> Typeface.MONOSPACE
            "display" -> display ?: Typeface.create("sans-serif-condensed", Typeface.NORMAL)
            else -> Typeface.SANS_SERIF
        }
        val style = (if (bold) Typeface.BOLD else 0) or (if (italic) Typeface.ITALIC else 0)
        return Typeface.create(base, style)
    }
}
