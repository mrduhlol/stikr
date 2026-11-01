package com.stikr.app.render

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ThumbnailRendererTest {
    @Test fun extremeDragsAndZoomNeverExposeOutsidePhoto() {
        for ((width, height) in listOf(4000 to 600, 600 to 4000, 1080 to 1350)) {
            for (zoom in listOf(-10f, 1f, 2f, 100f, Float.NaN)) {
                val safe = ThumbnailRenderer.clamp(width, height, ThumbnailCrop(zoom, 100f, -100f))
                assertTrue(safe.zoom in 1f..4f)
                val rect = ThumbnailRenderer.photoBounds(width, height, safe)
                assertTrue(rect.left <= 0.01f)
                assertTrue(rect.top <= 0.01f)
                assertTrue(rect.right >= 1079.99f)
                assertTrue(rect.bottom >= 1349.99f)
            }
        }
    }

    @Test fun cropExportsPortraitPngAndAppliesDrag() {
        val source = Bitmap.createBitmap(2160, 1350, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.RED)
        android.graphics.Canvas(source).drawRect(1080f, 0f, 2160f, 1350f, android.graphics.Paint().apply { color = Color.BLUE })
        val left = ThumbnailRenderer.photo(source, ThumbnailCrop(x = 100f))
        val right = ThumbnailRenderer.photo(source, ThumbnailCrop(x = -100f))
        assertEquals(1080, left.width)
        assertEquals(1350, left.height)
        assertEquals(Color.RED, left.getPixel(540, 675))
        assertEquals(Color.BLUE, right.getPixel(540, 675))
        val bytes = ThumbnailRenderer.png(left)
        val decoded = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        assertEquals(1080, decoded.width)
        assertEquals(1350, decoded.height)
        listOf(source, left, right, decoded).forEach { it.recycle() }
    }

    @Test fun largerTextAndBundledFontsChangeTheExportedPixels() {
        StickerTypography.initialize(ApplicationProvider.getApplicationContext())
        val base = ThumbnailText(text = "Hi", font = "sans", size = 240)
        val oldMaximum = ThumbnailRenderer.text(base)
        val larger = ThumbnailRenderer.text(base.copy(size = 400))
        assertFalse("new size range must affect the saved image", oldMaximum.sameAs(larger))
        for (font in listOf("handwritten", "script", "condensed")) {
            val image = ThumbnailRenderer.text(base.copy(font = font))
            assertFalse("bundled $font must render distinct glyphs", oldMaximum.sameAs(image))
            image.recycle()
        }
        oldMaximum.recycle()
        larger.recycle()
    }

    @Test fun typographyAndColorsAffectExport() {
        StickerTypography.initialize(ApplicationProvider.getApplicationContext())
        val style = ThumbnailText(text = "Tiny friends", font = "display", background = Color.MAGENTA, color = Color.GREEN)
        val normal = ThumbnailRenderer.text(style)
        assertEquals(Color.MAGENTA, normal.getPixel(0, 0))
        assertEquals(1080, normal.width)
        assertEquals(1350, normal.height)
        for (variant in listOf(style.copy(font = "serif"), style.copy(size = 60), style.copy(bold = true, italic = true), style.copy(color = Color.BLUE))) {
            val changed = ThumbnailRenderer.text(variant)
            assertFalse("style must affect actual pixels: $variant", normal.sameAs(changed))
            changed.recycle()
        }
        normal.recycle()
    }
}
