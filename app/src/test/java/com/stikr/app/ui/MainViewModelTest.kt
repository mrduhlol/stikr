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
            val style = StickerRenderer.Style(
                outline = 32, caption = "latest", roundedCorners = true,
                captionFont = "serif", captionSize = 48,
                captionBackground = Color.MAGENTA, captionTextColor = Color.YELLOW, captionBold = true,
            )
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
            model.editSticker(pack.id, file)
            await { model.editor.value?.ready == true }
            val restored = model.editor.value!!.style
            assertEquals(style.roundedCorners, restored.roundedCorners)
            assertEquals(style.captionFont, restored.captionFont)
            assertEquals(style.captionSize, restored.captionSize)
            assertEquals(style.captionBackground, restored.captionBackground)
            assertEquals(style.captionTextColor, restored.captionTextColor)
            assertEquals(style.captionBold, restored.captionBold)
        } finally {
            store.clear()
        }
    }

    @Test
    fun packEditUpdatesEveryStickerInPlaceAndKeepsOriginals() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val root = File(app.filesDir, "packs").apply { deleteRecursively() }
        val repo = PackRepository(root)
        val pack = repo.createPack("Whole pack")
        val photo = Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
        val source = java.io.ByteArrayOutputStream().use { out -> photo.compress(Bitmap.CompressFormat.PNG,100,out);out.toByteArray() }
        val bytes = com.stikr.app.render.StickerCodec.encodeWebp(photo)
        repo.addSticker(pack.id,bytes,fileName="one.webp",source=source)
        repo.addSticker(pack.id,bytes,fileName="two.webp",source=source)
        photo.recycle()
        val before = repo.getPack(pack.id)!!
        val model = MainViewModel(app)
        val store = ViewModelStore().apply { put("bulk",model) }
        try {
            model.editSticker(pack.id,"one.webp")
            await { model.editor.value?.ready == true }
            val style = StickerRenderer.Style(caption="Together",captionX=.4f,captionY=.6f,captionRotation=25f,captionItalic=true)
            model.setStyle(style)
            var done=false
            model.applyPackStyle { done=true }
            await { done }
            val after=repo.getPack(pack.id)!!
            assertEquals(before.stickers,after.stickers)
            assertTrue(after.contentVersion>before.contentVersion)
            for(name in after.stickers) {
                assertTrue(repo.sourceFile(pack.id,name)!!.readBytes().contentEquals(source))
                assertEquals(style,com.stikr.app.render.StickerStyleJson.read(org.json.JSONObject(repo.editData(pack.id,name)!!)))
            }
        } finally { store.clear() }
    }

    @Test
    fun singleStickerEditorReusesTheSharedPackAndCreatesStartersOnlyOnce() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val root = File(app.filesDir, "packs").apply { deleteRecursively() }
        val repository = PackRepository(root)
        val model = MainViewModel(app)
        val store = ViewModelStore().apply { put("singles", model) }
        try {
            var firstId: String? = null
            model.startSingleSticker { firstId = it }
            await { firstId != null && model.editor.value?.packId == firstId }
            val first = repository.getPack(firstId!!)!!
            assertTrue(first.isSingles)
            assertEquals("Stikr", first.name)
            assertEquals(2, first.stickerCount)
            assertEquals(0, first.userStickerCount)
            var secondId: String? = null
            model.startSingleSticker { secondId = it }
            await { secondId != null && model.editor.value?.packId == secondId }
            assertEquals(firstId, secondId)
            assertEquals(2, repository.getPack(firstId!!)!!.stickerCount)
        } finally { store.clear() }
    }

    @Test
    fun importsThirtyPhotosAndEditsAnExistingStickerInPlaceWithAutoCutoutOff() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val root = File(app.filesDir, "packs").apply { deleteRecursively() }
        val repository = PackRepository(root)
        val pack = repository.createPack("Thirty")
        val model = MainViewModel(app)
        val store = ViewModelStore().apply { put("import", model) }
        model.imageLoader = {
            Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
        }
        model.subjectCutout = { error("Auto cutout must be opt-in") }
        try {
            model.importPhotos(pack.id, (1..31).map { Uri.parse("content://photos/$it") })
            assertNotNull(model.importing.value)
            await { model.importing.value == null }
            val imported = repository.getPack(pack.id)!!
            assertEquals(30, imported.stickerCount)
            assertNotNull(repository.sourceFile(pack.id, imported.stickers.first()))
            model.editSticker(pack.id, imported.stickers.first())
            await { model.editor.value?.ready == true }
            assertEquals(false, model.editor.value!!.cutoutEnabled)
            model.setStyle(StickerRenderer.Style(caption = "edited"))
            var saved = false
            model.saveSticker { saved = true }
            await { saved }
            val edited = repository.getPack(pack.id)!!
            assertEquals(imported.stickers, edited.stickers)
            assertEquals(30, edited.stickerCount)
            model.editSticker(pack.id, imported.stickers.first())
            await { model.editor.value?.ready == true }
            assertEquals("edited", model.editor.value!!.style.caption)
            assertEquals(32, model.editor.value!!.source!!.width)
            assertEquals(false, model.editor.value!!.cutoutEnabled)
        } finally { store.clear() }
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
    @Test
    fun thumbnailPersistenceCompletesBeforeNavigationAndReportsFailureForRetry() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val root = File(app.filesDir, "packs").apply { deleteRecursively() }
        val repository = PackRepository(root)
        val pack = repository.createPack("Cover")
        val model = MainViewModel(app)
        val store = ViewModelStore().apply { put("cover", model) }
        try {
            var result: Boolean? = null
            model.setPackThumbnail(pack.id, byteArrayOf(1, 2, 3)) { saved ->
                assertNotNull(repository.getPack(pack.id)!!.thumbnailImage)
                result = saved
            }
            await { result != null }
            assertEquals(true, result)
            result = null
            model.setPackThumbnail("missing-pack", byteArrayOf(4)) { result = it }
            await { result != null }
            assertEquals(false, result)
            assertTrue(model.message.value!!.contains("Your edits are still here"))
        } finally { store.clear() }
    }

}
