package com.stikr.app.ui

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class StickerImageTest {
    @get:Rule val temp = TemporaryFolder()

    private fun write(file: File, color: Int) {
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test
    fun samplesToDisplaySizeAndReusesDecodedPixels() {
        val file = temp.newFile("sticker.png")
        write(file, Color.RED)
        val first = StickerThumbnails.load(file, 96)!!
        assertEquals(128, first.width)
        assertEquals(128, first.height)
        assertSame(first, StickerThumbnails.load(file, 110))
        val large = StickerThumbnails.load(file, 300)!!
        assertEquals(512, large.width)
        assertTrue(first.allocationByteCount <= large.allocationByteCount / 16)
        assertEquals(Color.RED, first.getPixel(0, 0))
    }

    @Test
    fun replacedAndDeletedFilesDoNotReturnStaleThumbnails() {
        val file = temp.newFile("sticker.png")
        write(file, Color.RED)
        val first = StickerThumbnails.load(file, 100)!!
        val replacement = temp.newFile("replacement.png")
        write(replacement, Color.BLUE)
        Files.move(replacement.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        val updated = StickerThumbnails.load(file, 100)!!
        assertNotSame(first, updated)
        assertEquals(Color.BLUE, updated.getPixel(0, 0))
        assertEquals(Color.RED, first.getPixel(0, 0))
        file.delete()
        assertNull(StickerThumbnails.load(file, 100))
    }

    @Test
    fun samplingNeverUndershootsTheRequestedSize() {
        assertEquals(512, StickerThumbnails.sampleSize(0))
        assertEquals(4, StickerThumbnails.sampleSize(96))
        assertEquals(2, StickerThumbnails.sampleSize(256))
        assertEquals(1, StickerThumbnails.sampleSize(257))
        assertEquals(1, StickerThumbnails.sampleSize(1024))
    }
}
