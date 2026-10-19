package com.stikr.app.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Draws the final 512x512 sticker: an optional outline (a dilated
 * silhouette, white or ink), the subject on top, then a caption.
 */
object StickerRenderer {

    const val SIZE = 512
    const val MAX_OUTLINE = 32
    private const val MARGIN = 18f

    enum class CaptionPosition { TOP, BOTTOM }

    /** Outline colour: classic sticker white, or the ink of the app's theme. */
    enum class OutlineColor(val color: Int) {
        WHITE(0xFFFFFFFF.toInt()),
        INK(0xFF191715.toInt()),
    }

    enum class CaptionColor(val fill: Int, val stroke: Int) {
        CREAM(0xFFFFFFFF.toInt(), 0xFF191715.toInt()),
        INK(0xFF191715.toInt(), 0xFFFFFFFF.toInt()),
        BUTTER(0xFFF8DFA0.toInt(), 0xFF191715.toInt()),
        CHERRY(0xFFAE403E.toInt(), 0xFFFFFFFF.toInt()),
    }

    data class Style(
        val outline: Int = 8,
        val outlineColor: OutlineColor = OutlineColor.WHITE,
        val caption: String = "",
        val captionPosition: CaptionPosition = CaptionPosition.BOTTOM,
        val captionColor: CaptionColor = CaptionColor.CREAM,
    )

    fun render(subject: Bitmap, style: Style): Bitmap {
        val out = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        drawSubject(canvas, subject, style)
        drawCaption(canvas, style)
        return out
    }

    /** Single-worker editor cache. Returned bitmaps never alias the cached layer. */
    class PreviewCache {
        private var subject: Bitmap? = null
        private var generation = -1
        private var outline = -1
        private var color: OutlineColor? = null
        private var layer: Bitmap? = null

        fun render(subject: Bitmap, style: Style): Bitmap {
            val width = style.outline.coerceIn(0, MAX_OUTLINE)
            if (this.subject !== subject || generation != subject.generationId ||
                outline != width || color != style.outlineColor
            ) {
                clear()
                layer = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888).also {
                    drawSubject(Canvas(it), subject, style)
                }
                this.subject = subject
                generation = subject.generationId
                outline = width
                color = style.outlineColor
            }
            val out = requireNotNull(layer).copy(Bitmap.Config.ARGB_8888, true)
            drawCaption(Canvas(out), style)
            return out
        }

        fun clear() {
            layer?.recycle()
            layer = null
            subject = null
        }
    }

    private fun drawSubject(canvas: Canvas, subject: Bitmap, style: Style) {
        val outline = style.outline.coerceIn(0, MAX_OUTLINE)

        val box = SIZE - 2f * (MARGIN + outline)
        val scale = min(box / subject.width, box / subject.height)
        val drawWidth = (subject.width * scale).toInt().coerceAtLeast(1)
        val drawHeight = (subject.height * scale).toInt().coerceAtLeast(1)
        val left = (SIZE - drawWidth) / 2f
        val top = (SIZE - drawHeight) / 2f

        val scaled = Bitmap.createScaledBitmap(subject, drawWidth, drawHeight, true)
        if (outline > 0) {
            val silhouette = silhouette(scaled, style.outlineColor.color)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val steps = 24
            for (step in 0 until steps) {
                val angle = step * 2.0 * Math.PI / steps
                canvas.drawBitmap(
                    silhouette,
                    left + (cos(angle) * outline).toFloat(),
                    top + (sin(angle) * outline).toFloat(),
                    paint,
                )
            }
            silhouette.recycle()
        }

        canvas.drawBitmap(scaled, left, top, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        if (scaled !== subject) scaled.recycle()
    }

    /** The 96x96 pack icon WhatsApp shows in the sticker tray. */
    fun renderTray(bitmap: Bitmap, size: Int = 96): Bitmap {
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val scale = min(size.toFloat() / bitmap.width, size.toFloat() / bitmap.height)
        val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
        Canvas(out).drawBitmap(
            scaled,
            (size - width) / 2f,
            (size - height) / 2f,
            Paint(Paint.FILTER_BITMAP_FLAG),
        )
        if (scaled !== bitmap) scaled.recycle()
        return out
    }

    /**
     * Crops fully transparent edges away (with a little padding), so a cut-out
     * subject fills the sticker instead of floating in the middle.
     */
    fun trimTransparent(src: Bitmap, padding: Int = 8): Bitmap {
        val width = src.width
        val height = src.height
        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1
        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                if ((pixels[row + x] ushr 24) > 8) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        if (maxX < 0) return src

        minX = (minX - padding).coerceAtLeast(0)
        minY = (minY - padding).coerceAtLeast(0)
        maxX = (maxX + padding).coerceAtMost(width - 1)
        maxY = (maxY + padding).coerceAtMost(height - 1)
        val cropWidth = maxX - minX + 1
        val cropHeight = maxY - minY + 1
        if (cropWidth == width && cropHeight == height) return src
        return Bitmap.createBitmap(src, minX, minY, cropWidth, cropHeight)
    }

    // ------------------------------------------------------------- internals

    private fun silhouette(src: Bitmap, color: Int): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        }
        Canvas(out).drawBitmap(src, 0f, 0f, paint)
        return out
    }

    private fun drawCaption(canvas: Canvas, style: Style) {
        var text: CharSequence = style.caption.trim()
        if (text.isEmpty()) return

        val maxWidth = SIZE - 2f * 22f
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        var textSize = 92f
        while (textSize > 26f) {
            paint.textSize = textSize
            if (paint.measureText(text.toString()) <= maxWidth) break
            textSize -= 2f
        }
        paint.textSize = textSize
        if (paint.measureText(text.toString()) > maxWidth) {
            text = TextUtils.ellipsize(text, paint, maxWidth, TextUtils.TruncateAt.END)
        }

        val baseline = when (style.captionPosition) {
            CaptionPosition.TOP -> textSize + 14f
            CaptionPosition.BOTTOM -> SIZE - 16f
        }

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = textSize * 0.16f
        paint.strokeJoin = Paint.Join.ROUND
        paint.color = style.captionColor.stroke
        canvas.drawText(text.toString(), SIZE / 2f, baseline, paint)

        paint.style = Paint.Style.FILL
        paint.color = style.captionColor.fill
        canvas.drawText(text.toString(), SIZE / 2f, baseline, paint)
    }
}
