package com.stikr.app.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
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
        val outline: Int = 0,
        val outlineColor: OutlineColor = OutlineColor.WHITE,
        val caption: String = "",
        val captionPosition: CaptionPosition = CaptionPosition.BOTTOM,
        val captionColor: CaptionColor = CaptionColor.CREAM,
        val roundedCorners: Boolean = false,
        val captionFont: String = "sans",
        val captionSize: Int = 40,
        val captionBackground: Int = android.graphics.Color.TRANSPARENT,
        val captionTextColor: Int? = null,
        val captionBold: Boolean = false,
        val captionItalic: Boolean = false,
        val captionUnderline: Boolean = false,
        val captionStrike: Boolean = false,
        val captionX: Float? = null,
        val captionY: Float? = null,
        val captionRotation: Float = 0f,
        val captionAlign: String = "center",
        val extraCaptions: List<Style> = emptyList(),
    )

    fun render(subject: Bitmap, style: Style): Bitmap {
        val out = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        drawSubject(canvas, subject, style)
        drawCaption(canvas, style)
        style.extraCaptions.take(19).forEach { drawCaption(canvas, it) }
        return out
    }

    /** Single-worker editor cache. Returned bitmaps never alias the cached layer. */
    class PreviewCache {
        private var subject: Bitmap? = null
        private var generation = -1
        private var outline = -1
        private var color: OutlineColor? = null
        private var layer: Bitmap? = null
        private var roundedCorners = false

        fun render(subject: Bitmap, style: Style): Bitmap {
            val width = style.outline.coerceIn(0, MAX_OUTLINE)
            if (this.subject !== subject || generation != subject.generationId ||
                outline != width || color != style.outlineColor || roundedCorners != style.roundedCorners
            ) {
                clear()
                layer = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888).also {
                    drawSubject(Canvas(it), subject, style)
                }
                this.subject = subject
                generation = subject.generationId
                outline = width
                color = style.outlineColor
                roundedCorners = style.roundedCorners
            }
            val out = requireNotNull(layer).copy(Bitmap.Config.ARGB_8888, true)
            drawCaption(Canvas(out), style)
            style.extraCaptions.take(19).forEach { drawCaption(Canvas(out), it) }
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

        val resized = Bitmap.createScaledBitmap(subject, drawWidth, drawHeight, true)
        val scaled = if (style.roundedCorners) {
            Bitmap.createBitmap(drawWidth, drawHeight, Bitmap.Config.ARGB_8888).also { rounded ->
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = android.graphics.BitmapShader(resized, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP)
                }
                val radius = min(drawWidth, drawHeight) * 0.09f
                Canvas(rounded).drawRoundRect(0f, 0f, drawWidth.toFloat(), drawHeight.toFloat(), radius, radius, paint)
                if (resized !== subject) resized.recycle()
            }
        } else resized
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
        val text = style.caption.take(500)
        if (text.isBlank()) return
        val free = style.captionX != null || style.captionY != null
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            typeface = StickerTypography.typeface(style.captionFont, style.captionBold, style.captionItalic)
            textSize = style.captionSize.coerceIn(if (free) 8 else 16, if (free) 320 else 170).toFloat()
            isUnderlineText = style.captionUnderline
            isStrikeThruText = style.captionStrike
        }
        val lines = text.split('\n')
        if (!free) while (paint.textSize > 16f && lines.maxOf { paint.measureText(it) } > SIZE - 44f) paint.textSize -= 2f
        val width = lines.maxOf { paint.measureText(it) }
        val fm = paint.fontMetrics
        val lineHeight = (fm.descent - fm.ascent) * 1.1f
        val height = lineHeight * lines.size
        val defaultY = if (style.captionPosition == CaptionPosition.TOP) (height / 2f + 14f) / SIZE else (SIZE - 16f - height / 2f) / SIZE
        val x = (style.captionX ?: .5f).let { if (it.isFinite()) it.coerceIn(0f, 1f) else .5f } * SIZE
        val y = (style.captionY ?: defaultY).let { if (it.isFinite()) it.coerceIn(0f, 1f) else .78f } * SIZE
        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(style.captionRotation.takeIf { it.isFinite() } ?: 0f)
        if (android.graphics.Color.alpha(style.captionBackground) > 0) {
            val padX = paint.textSize * .18f
            val padY = paint.textSize * .12f
            canvas.drawRoundRect(-width/2 - padX, -height/2 - padY, width/2 + padX, height/2 + padY,
                paint.textSize * .1f, paint.textSize * .1f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = style.captionBackground })
        }
        paint.textAlign = when (style.captionAlign) { "left" -> Paint.Align.LEFT; "right" -> Paint.Align.RIGHT; else -> Paint.Align.CENTER }
        val tx = when (paint.textAlign) { Paint.Align.LEFT -> -width/2; Paint.Align.RIGHT -> width/2; else -> 0f }
        lines.forEachIndexed { index, line ->
            val baseline = -height / 2f + index * lineHeight + (lineHeight - fm.descent - fm.ascent) / 2f
            // Preserve the original outlined caption appearance for legacy edits only.
            if (!free) {
                paint.style = Paint.Style.STROKE; paint.strokeWidth = paint.textSize * .16f
                paint.strokeJoin = Paint.Join.ROUND; paint.color = style.captionColor.stroke
                canvas.drawText(line, tx, baseline, paint)
            }
            paint.style = Paint.Style.FILL; paint.color = style.captionTextColor ?: style.captionColor.fill
            canvas.drawText(line, tx, baseline, paint)
        }
        canvas.restore()
    }
}
