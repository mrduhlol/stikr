package com.stikr.app.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Looper
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.stikr.app.data.PackRepository
import com.stikr.app.render.StickerRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class MainViewModelTest {
    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 10_000_000_000L
        while (!condition() && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(40))
            Thread.sleep(10)
        }
        assertTrue("background work did not finish", condition())
    }

    @Test
    fun immediateSaveUsesLatestStyleWhilePreviewIsPending() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val root = File(app.filesDir, "packs").apply { deleteRecursively() }
        val repository = PackRepository(root)
        val pack = repository.createPack("Preview")
        val photo = File(app.cacheDir, "photo.png")
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.GREEN)
        photo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val model = MainViewModel(app)
        val store = ViewModelStore().apply { put("test", model) }
        try {
            model.startEditor(pack.id)
            model.setCutout(false)
            model.loadImage(Uri.fromFile(photo))
            await { model.editor.value?.ready == true }
            val oldPreview = model.editor.value!!.preview
            val style = StickerRenderer.Style(outline = 32, caption = "latest")
            repeat(100) { model.setStyle(style.copy(outline = it % 33)) }
            model.setStyle(style)
            assertTrue("preview update should be deferred", oldPreview === model.editor.value!!.preview)
            var saved = false
            model.saveSticker { saved = true }
            await { saved }
            val file = repository.getPack(pack.id)!!.stickers.single()
            val result = BitmapFactory.decodeFile(repository.stickerFile(pack.id, file)!!.path)
            assertNotNull(result)
            assertEquals(512, result.width)
            assertEquals(512, result.height)
            // An outline of 32 moves the subject's top edge from 26px to 50px.
            // At y=40 the saved sticker must have the white outline, not the old green subject.
            val pixel = result.getPixel(256, 40)
            assertTrue(Color.red(pixel) > 220 && Color.blue(pixel) > 220)
            result.recycle()
        } finally {
            store.clear()
        }
    }

    @Test
    fun exportReportsProgressUntilWhatsAppIsHandedThePack() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val root = File(app.filesDir, "packs").apply { deleteRecursively() }
        val repository = PackRepository(root)
        val pack = repository.createPack("Export")
        val sticker = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        sticker.eraseColor(Color.RED)
        repeat(3) { repository.addSticker(pack.id, com.stikr.app.render.StickerCodec.encodeWebp(sticker)) }
        sticker.recycle()
        val model = MainViewModel(app)
        val store = ViewModelStore().apply { put("test", model) }
        try {
            await { model.packs.value.singleOrNull()?.stickerCount == 3 }
            model.exportToWhatsApp(pack.id)
            assertEquals(pack.id, model.exportingPackId.value)
            await { model.exportingPackId.value == null }
            // WhatsApp isn't installed under Robolectric, so the export ends in a message.
            assertNotNull(model.message.value)
            assertNotNull(repository.getPack(pack.id)!!.trayImage)
        } finally {
            store.clear()
        }
    }
}
