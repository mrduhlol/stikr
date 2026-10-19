package com.stikr.app.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.stikr.app.data.PackRepository
import com.stikr.app.render.StickerCodec
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class PackExporterTest {
    @Test
    fun reusesTrayUntilTheFirstStickerIsDeleted() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = File(context.filesDir, "packs").apply { deleteRecursively() }
        val repo = PackRepository(root)
        val pack = repo.createPack("Tray")
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)
        repo.addSticker(pack.id, StickerCodec.encodeWebp(bitmap), "red.webp")
        bitmap.eraseColor(Color.BLUE)
        val two = repo.addSticker(pack.id, StickerCodec.encodeWebp(bitmap), "blue.webp")!!
        bitmap.recycle()
        val prepared = PackExporter.ensureTray(context, repo, two)
        val trayFile = repo.stickerFile(pack.id, prepared.trayImage!!)!!
        val modified = trayFile.lastModified()
        val again = PackExporter.ensureTray(context, repo, prepared)
        assertSame("unchanged exports must reuse the prepared pack", prepared, again)
        assertEquals(modified, trayFile.lastModified())
        val remaining = repo.deleteSticker(pack.id, "red.webp")!!
        val updated = PackExporter.ensureTray(context, repo, remaining)
        assertNotNull(updated.trayImage)
        assertTrue(trayFile.length() <= StickerCodec.MAX_ICON_BYTES)
        val tray = BitmapFactory.decodeFile(trayFile.path)
        assertEquals(96, tray.width)
        assertEquals(96, tray.height)
        assertTrue(Color.blue(tray.getPixel(48, 48)) > 220)
        assertTrue(Color.red(tray.getPixel(48, 48)) < 30)
        tray.recycle()
    }
}
