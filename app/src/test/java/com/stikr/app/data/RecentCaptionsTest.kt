package com.stikr.app.data

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.stikr.app.render.StickerRenderer
import com.stikr.app.ui.MainViewModel
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecentCaptionsTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()

    @Before
    fun fresh() {
        app.getSharedPreferences("recent-captions", android.content.Context.MODE_PRIVATE).edit().clear().commit()
        File(app.filesDir, "packs").deleteRecursively()
    }

    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 10_000_000_000L
        while (!condition() && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(40))
            Thread.sleep(10)
        }
        org.junit.Assert.assertTrue("background work did not finish", condition())
    }

    @Test
    fun captionsAreKeptNewestFirstWithoutDuplicatesOrBlanks() {
        val store = RecentCaptions(app)
        store.record("  Hello  ")
        store.record("Bye")
        store.record("Hello")
        store.record("   ")
        assertEquals(listOf("Hello", "Bye"), store.all())
    }

    @Test
    fun theListIsBoundedToSixCaptions() {
        val store = RecentCaptions(app)
        repeat(9) { store.record("Caption $it") }
        assertEquals(
            listOf("Caption 8", "Caption 7", "Caption 6", "Caption 5", "Caption 4", "Caption 3"),
            store.all(),
        )
    }

    @Test
    fun savingACaptionedStickerMakesTheCaptionReusable() {
        val repository = PackRepository(File(app.filesDir, "packs"))
        val pack = repository.createPack("Recent")
        val photo = File(app.cacheDir, "recent-photo.png")
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.GREEN)
        photo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()

        val model = MainViewModel(app)
        model.startEditor(pack.id)
        model.loadImage(Uri.fromFile(photo))
        await { model.editor.value?.ready == true }
        model.setStyle(StickerRenderer.Style(caption = "Saved caption"))
        var saved = false
        model.saveSticker { saved = true }
        await { saved }
        await { model.captions.value == listOf("Saved caption") }
        assertEquals(listOf("Saved caption"), model.captions.value)
    }
}
