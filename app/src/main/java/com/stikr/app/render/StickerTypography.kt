package com.stikr.app.render

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.stikr.app.R

/** Shared fonts keep editing previews and exported bitmaps identical, including offline. */
object StickerTypography {
    val fonts = listOf("sans", "serif", "mono", "display", "handwritten", "script", "condensed", "bebas", "lobster", "playfair", "orbitron", "righteous")
    private val names = listOf("Sans", "Serif", "Mono", "Anton", "Caveat", "Pacifico", "Oswald", "Bebas Neue", "Lobster", "Playfair Display", "Orbitron", "Righteous")
    @Volatile private var bundled: Map<String, Typeface> = emptyMap()

    fun label(font: String): String = names.getOrElse(fonts.indexOf(font)) { "Sans" }

    fun initialize(context: Context) {
        if (bundled.isEmpty()) {
            bundled = mapOf(
                "display" to R.font.anton_regular,
                "handwritten" to R.font.caveat_regular,
                "script" to R.font.pacifico_regular,
                "condensed" to R.font.oswald_regular,
                "bebas" to R.font.bebasneue_regular,
                "lobster" to R.font.lobster_regular,
                "playfair" to R.font.playfairdisplay_regular,
                "orbitron" to R.font.orbitron_regular,
                "righteous" to R.font.righteous_regular,
            ).mapValues { (_, resource) -> ResourcesCompat.getFont(context, resource) ?: Typeface.SANS_SERIF }
        }
    }

    fun typeface(font: String, bold: Boolean = false, italic: Boolean = false): Typeface {
        val base = bundled[font] ?: when (font) {
            "serif" -> Typeface.SERIF
            "mono" -> Typeface.MONOSPACE
            else -> Typeface.SANS_SERIF
        }
        val style = (if (bold) Typeface.BOLD else 0) or (if (italic) Typeface.ITALIC else 0)
        return Typeface.create(base, style)
    }
}
