package com.stikr.app.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders stickers off-device. Images are written to build/sticker-renders/
 * so the look can be reviewed without installing the app.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "xxhdpi")
class StickerRendererTest {

    @Test
    fun defaultsAndBleedingBendChangePhotoCornersBeforeOutline() {
        val defaults = StickerRenderer.Style()
        assertEquals(0, defaults.outline)
        assertEquals(false, defaults.roundedCorners)
        assertEquals("sans", defaults.captionFont)
        assertEquals(40, defaults.captionSize)
        assertEquals(Color.TRANSPARENT, defaults.captionBackground)
        assertEquals(null, defaults.captionTextColor)
        assertEquals(false, defaults.captionBold)
        val photo = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val square = StickerRenderer.render(photo, defaults.copy(outline = 0))
        val rounded = StickerRenderer.render(photo, defaults.copy(outline = 0, roundedCorners = true))
        assertEquals(255, Color.alpha(square.getPixel(20, 20)))
        assertEquals(0, Color.alpha(rounded.getPixel(20, 20)))
        assertEquals(Color.RED, rounded.getPixel(256, 256))
        val outlined = StickerRenderer.render(photo, defaults.copy(roundedCorners = true))
        assertEquals(0, Color.alpha(outlined.getPixel(20, 20)))
        val cache = StickerRenderer.PreviewCache()
        cache.render(photo, defaults).recycle()
        val cachedRounded = cache.render(photo, defaults.copy(roundedCorners = true))
        assertTrue(outlined.sameAs(cachedRounded))
        cache.clear()
        listOf(photo, square, rounded, outlined, cachedRounded).forEach { it.recycle() }
    }

    @Test
    fun captionStyleChangesRenderedPixels() {
        val photo = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
        val style = StickerRenderer.Style(caption = "Little things")
        val base = StickerRenderer.render(photo, style)
        for (variant in listOf(style.copy(captionFont = "serif"), style.copy(captionSize = 80),
            style.copy(captionBackground = Color.MAGENTA), style.copy(captionTextColor = Color.GREEN),
            style.copy(captionBold = true))) {
            val changed = StickerRenderer.render(photo, variant)
            assertTrue("caption controls must change pixels: $variant", !base.sameAs(changed))
            changed.recycle()
        }
        base.recycle()
        photo.recycle()
    }

    private val out = File("build/sticker-renders").apply { mkdirs() }

    /** A stand-in photo: warm head, green body, on a light background. */
    private fun samplePhoto(width: Int = 640, height: Int = 900): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(238, 240, 250))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.rgb(255, 171, 94)
        canvas.drawCircle(width / 2f, height * 0.34f, width * 0.30f, paint)
        paint.color = Color.rgb(86, 192, 158)
        canvas.drawRoundRect(width * 0.18f, height * 0.55f, width * 0.82f, height * 0.96f, 48f, 48f, paint)
        return bitmap
    }

    @Test
    fun rendersFullStickerAt512() {
        val sticker = StickerRenderer.render(
            samplePhoto(),
            StickerRenderer.Style(
                outline = 10,
                caption = "hello!",
                captionColor = StickerRenderer.CaptionColor.CREAM,
            ),
        )

        assertEquals(StickerRenderer.SIZE, sticker.width)
        assertEquals(StickerRenderer.SIZE, sticker.height)
        // corners stay transparent: a sticker is a cut-out, not a tile
        assertEquals(0, Color.alpha(sticker.getPixel(2, 2)))
        assertEquals(0, Color.alpha(sticker.getPixel(StickerRenderer.SIZE - 3, 2)))
        save("sticker_full.png", sticker)
    }

    @Test
    fun inkOutlinePaintsTheSilhouetteInInk() {
        val sticker = StickerRenderer.render(
            samplePhoto(),
            StickerRenderer.Style(outline = 14, outlineColor = StickerRenderer.OutlineColor.INK),
        )
        // straight down from centre: image bottom -> ink outline ring -> transparent
        var outlinePixels = 0
        for (y in StickerRenderer.SIZE / 2 until StickerRenderer.SIZE) {
            val pixel = sticker.getPixel(StickerRenderer.SIZE / 2, y)
            if (pixel == StickerRenderer.OutlineColor.INK.color) outlinePixels++
        }
        assertTrue("expected an ink outline band, found $outlinePixels ink pixels", outlinePixels > 6)
        save("sticker_ink_outline.png", sticker)
    }

    @Test
    fun keepsUnderWhatsAppSizeLimit() {
        val sticker = StickerRenderer.render(
            samplePhoto(),
            StickerRenderer.Style(outline = 4, caption = "hi"),
        )
        val bytes = StickerCodec.encodeWebp(sticker)
        assertTrue(
            "sticker must stay under ${StickerCodec.MAX_STICKER_BYTES} bytes, was ${bytes.size}",
            bytes.size <= StickerCodec.MAX_STICKER_BYTES,
        )
    }

    @Test
    fun rendersStyleVariants() {
        val photo = samplePhoto()
        val variants = mapOf(
            "sticker_no_outline" to StickerRenderer.Style(outline = 0, caption = "no outline"),
            "sticker_top_caption" to StickerRenderer.Style(
                outline = 12,
                caption = "top",
                captionPosition = StickerRenderer.CaptionPosition.TOP,
                captionColor = StickerRenderer.CaptionColor.BUTTER,
            ),
            "sticker_cherry_caption" to StickerRenderer.Style(
                outline = 6,
                caption = "cherry",
                captionColor = StickerRenderer.CaptionColor.CHERRY,
            ),
            "sticker_ink_caption" to StickerRenderer.Style(
                outline = 8,
                caption = "ink",
                captionColor = StickerRenderer.CaptionColor.INK,
            ),
            "sticker_long_caption" to StickerRenderer.Style(
                outline = 6,
                caption = "a rather long caption that has to shrink",
            ),
        )
        for ((name, style) in variants) {
            val sticker = StickerRenderer.render(photo, style)
            assertEquals(StickerRenderer.SIZE, sticker.width)
            save("$name.png", sticker)
        }
    }

    @Test
    fun trayIconIs96AndUnderLimit() {
        val sticker = StickerRenderer.render(samplePhoto(), StickerRenderer.Style())
        val tray = StickerRenderer.renderTray(sticker)
        assertEquals(96, tray.width)
        assertEquals(96, tray.height)
        val bytes = StickerCodec.encodeWebp(tray, StickerCodec.MAX_ICON_BYTES)
        assertTrue(
            "tray icon must stay under ${StickerCodec.MAX_ICON_BYTES} bytes, was ${bytes.size}",
            bytes.size <= StickerCodec.MAX_ICON_BYTES,
        )
        save("tray.png", tray)
    }

    @Test
    fun trimCropsTransparentBorders() {
        val source = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(source)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.RED }
        canvas.drawCircle(200f, 200f, 60f, paint)

        val trimmed = StickerRenderer.trimTransparent(source)
        assertTrue("trimmed should be smaller: ${trimmed.width}", trimmed.width < source.width)
        assertTrue(trimmed.width >= 120)

        val empty = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        assertEquals(100, StickerRenderer.trimTransparent(empty).width)
    }

    @Test
    fun cachedPreviewsMatchFullRendersAndInvalidate() {
        val cache = StickerRenderer.PreviewCache()
        val photo = samplePhoto()
        val other = samplePhoto(900, 640)
        val styles = listOf(
            StickerRenderer.Style(caption = "hello"),
            StickerRenderer.Style(caption = "changed", captionPosition = StickerRenderer.CaptionPosition.TOP),
            StickerRenderer.Style(caption = "ink", captionColor = StickerRenderer.CaptionColor.INK),
            StickerRenderer.Style(outline = 20, outlineColor = StickerRenderer.OutlineColor.INK),
            StickerRenderer.Style(outline = 0),
        )
        for (subject in listOf(photo, other)) {
            for (style in styles) {
                val expected = StickerRenderer.render(subject, style)
                val actual = cache.render(subject, style)
                assertTrue("cached preview must match $style", expected.sameAs(actual))
                expected.recycle()
                actual.recycle()
            }
        }
        // A caller may mutate a bitmap without changing its identity.
        other.eraseColor(Color.RED)
        val expected = StickerRenderer.render(other, styles.last())
        val actual = cache.render(other, styles.last())
        assertTrue(expected.sameAs(actual))
        cache.clear()
        assertTrue("clearing the cache must not recycle a displayed preview", !actual.isRecycled)
        expected.recycle()
        actual.recycle()
        photo.recycle()
        other.recycle()
    }

    @Test
    fun measureCaptionPreviewReuse() {
        val photo = samplePhoto()
        val cache = StickerRenderer.PreviewCache()
        val styles = (0 until 20).map { StickerRenderer.Style(caption = "caption $it") }
        repeat(3) {
            StickerRenderer.render(photo, styles.first()).recycle()
            cache.render(photo, styles.first()).recycle()
        }
        val fullTimes = LongArray(3)
        val cachedTimes = LongArray(3)
        repeat(3) { round ->
            var start = System.nanoTime()
            styles.forEach { StickerRenderer.render(photo, it).recycle() }
            fullTimes[round] = System.nanoTime() - start
            start = System.nanoTime()
            styles.forEach { cache.render(photo, it).recycle() }
            cachedTimes[round] = System.nanoTime() - start
        }
        println("caption-preview: 20 renders, full median=${fullTimes.sorted()[1] / 1_000_000.0}ms, " +
            "cached median=${cachedTimes.sorted()[1] / 1_000_000.0}ms")
        cache.clear()
        photo.recycle()
    }

    private fun save(name: String, bitmap: Bitmap) {
        File(out, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
