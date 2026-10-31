package com.stikr.app.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.ByteArrayOutputStream
import kotlin.math.max

/** Crop offsets are fractions of the frame, independent of device density and preview size. */
data class ThumbnailCrop(val zoom: Float = 1f, val x: Float = 0f, val y: Float = 0f)

data class ThumbnailText(
    val text: String = "Your little collection.",
    val font: String = "display",
    val size: Int = 120,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val background: Int = 0xFFBDA1ED.toInt(),
    val color: Int = 0xFF211A2C.toInt(),
)

object ThumbnailRenderer {
    const val WIDTH = 1080
    const val HEIGHT = 1350

    fun clamp(sourceWidth: Int, sourceHeight: Int, crop: ThumbnailCrop): ThumbnailCrop {
        require(sourceWidth > 0 && sourceHeight > 0)
        val zoom = crop.zoom.takeIf { it.isFinite() }?.coerceIn(1f, 4f) ?: 1f
        val scale = max(WIDTH.toFloat() / sourceWidth, HEIGHT.toFloat() / sourceHeight) * zoom
        val maxX = max(0f, (sourceWidth * scale / WIDTH - 1f) / 2f)
        val maxY = max(0f, (sourceHeight * scale / HEIGHT - 1f) / 2f)
        return ThumbnailCrop(zoom, (if (crop.x.isFinite()) crop.x else 0f).coerceIn(-maxX, maxX),
            (if (crop.y.isFinite()) crop.y else 0f).coerceIn(-maxY, maxY))
    }

    fun photoBounds(sourceWidth: Int, sourceHeight: Int, crop: ThumbnailCrop): RectF {
        val safe = clamp(sourceWidth, sourceHeight, crop)
        val scale = max(WIDTH.toFloat() / sourceWidth, HEIGHT.toFloat() / sourceHeight) * safe.zoom
        val w = sourceWidth * scale
        val h = sourceHeight * scale
        val left = (WIDTH - w) / 2f + safe.x * WIDTH
        val top = (HEIGHT - h) / 2f + safe.y * HEIGHT
        return RectF(left, top, left + w, top + h)
    }

    fun drawPhoto(canvas: Canvas, source: Bitmap, crop: ThumbnailCrop) {
        canvas.drawBitmap(source, null, photoBounds(source.width, source.height, crop), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    }

    fun photo(source: Bitmap, crop: ThumbnailCrop): Bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888).also {
        drawPhoto(Canvas(it), source, crop)
    }

    fun text(style: ThumbnailText): Bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888).also {
        drawText(Canvas(it), style)
    }

    fun drawText(canvas: Canvas, style: ThumbnailText) {
        canvas.drawColor(style.background)
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.color
            typeface = StickerTypography.typeface(style.font, style.bold, style.italic)
            textSize = style.size.coerceIn(32, 240).toFloat()
        }
        fun layout() = StaticLayout.Builder.obtain(style.text.take(500), 0, style.text.take(500).length, paint, WIDTH - 144)
            .setAlignment(Layout.Alignment.ALIGN_CENTER).setIncludePad(false).setLineSpacing(0f, 1.08f).build()
        var text = layout()
        while (text.height > HEIGHT - 160 && paint.textSize > 20f) {
            paint.textSize -= 2f
            text = layout()
        }
        canvas.save()
        canvas.translate(72f, (HEIGHT - text.height) / 2f)
        text.draw(canvas)
        canvas.restore()
    }

    fun png(bitmap: Bitmap): ByteArray = ByteArrayOutputStream().use {
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) { "Could not encode thumbnail" }
        it.toByteArray()
    }
}
