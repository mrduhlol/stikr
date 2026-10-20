package com.stikr.app.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.stikr.app.data.PackRepository
import com.stikr.app.data.StickerPack
import com.stikr.app.data.TourPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Drives the batch engine through the real view model and the real
 * file-backed repository, with fake photos and a fake cut-out.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class BatchEngineTest {

    private val app: Application = ApplicationProvider.getApplicationContext()

    @Before
    fun clearPacks() {
        File(app.filesDir, "packs").deleteRecursively()
    }

    private fun viewModel(vararg results: Boolean /* per-photo: true = loads, false = null */): MainViewModel {
        val vm = MainViewModel(app)
        var i = 0
        vm.imageLoader = { if (results[i++]) fakePhoto() else null }
        vm.subjectCutout = { it.copy(Bitmap.Config.ARGB_8888, false) } // pretend cut-out worked
        return vm
    }

    private fun fakePhoto(): Bitmap {
        val photo = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        Canvas(photo).drawCircle(32f, 32f, 24f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.RED })
        return photo
    }

    private fun uris(count: Int): List<Uri> = List(count) { Uri.parse("content://photos/$it") }

    private fun repo() = PackRepository(File(app.filesDir, "packs"))

    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 30_000_000_000L
        while (!condition() && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(40))
            Thread.sleep(10)
        }
        assertTrue("background work did not finish", condition())
    }

    private fun awaitBatch(vm: MainViewModel): BatchState {
        await { vm.batch.value?.phase == BatchPhase.DONE }
        return requireNotNull(vm.batch.value)
    }

    /**
     * Parks the batch's photo loader until the test opens the gate, so a test
     * can act on a batch that is provably still running. No wall-clock sleeps.
     */
    private class Gate {
        private val entered = CountDownLatch(1)
        private val open = CountDownLatch(1)

        /** Called by the fake loader: waits until the test lets the run continue. */
        fun hold() {
            entered.countDown()
            open.await(30, TimeUnit.SECONDS)
        }

        /** True when the run reached the loader. */
        fun reached(): Boolean = entered.await(30, TimeUnit.SECONDS)

        /** Lets the run continue. */
        fun release() = open.countDown()
    }

    @Test
    fun batchOf31SplitsInto30And1() {
        val vm = viewModel(*BooleanArray(31) { true })

        vm.startBatch("Beach trip", "Ada", uris(31))
        val state = awaitBatch(vm)

        assertEquals(2, state.created.size)
        assertEquals(listOf("Beach trip 1", "Beach trip 2"), state.created.map { it.name })
        assertEquals(listOf(30, 1), state.created.map { it.stickerCount })
        assertTrue("both packs carry the batch author", state.created.all { it.author == "Ada" })
        assertEquals(2, state.expectedPacks)
        assertEquals(31, state.saved)
        assertEquals(0, state.fellBack)
        assertEquals(0, state.failed)
        assertEquals(31, state.processed)

        val packs = repo().listPacks()
        assertEquals(2, packs.size)
        assertEquals(setOf("Beach trip 1", "Beach trip 2"), packs.map { it.name }.toSet())
    }

    @Test
    fun unreadablePhotosAreSkipped() {
        val vm = viewModel(true, false, true, true, false)

        vm.startBatch("P", "Ada", uris(5))
        val state = awaitBatch(vm)

        assertEquals(3, state.saved)
        assertEquals(2, state.failed)
        assertEquals(5, state.processed)

        val packs = repo().listPacks()
        assertEquals(1, packs.size)
        assertEquals("P", packs.first().name)
        assertEquals(3, packs.first().stickerCount)
    }

    @Test
    fun cancelDeletesTheBatchesPacks() {
        val vm = viewModel(*BooleanArray(10) { true })
        // The loader parks the run, so the cancel lands while it is under way.
        val gate = Gate()
        vm.imageLoader = {
            gate.hold()
            fakePhoto()
        }

        vm.startBatch("Trip", "Ada", uris(10))
        try {
            assertTrue("the run never reached the loader", gate.reached())
            vm.cancelBatch()
        } finally {
            gate.release()
        }

        await { vm.batch.value == null }

        assertNull("cancel clears the batch screen", vm.batch.value)
        assertTrue("cancel deletes every pack the batch made", repo().listPacks().isEmpty())
        assertEquals("Batch stopped \u2014 nothing was saved", vm.message.value)
    }

    @Test
    fun dismissAndRestartAreIgnoredWhileRunning() {
        val vm = viewModel(*BooleanArray(10) { true })
        // The loader parks the run, so it is provably still going.
        val gate = Gate()
        vm.imageLoader = {
            gate.hold()
            fakePhoto()
        }

        vm.startBatch("First", "Ada", uris(10))
        try {
            assertTrue("the run never reached the loader", gate.reached())
            vm.dismissBatch()
            assertTrue("dismissing does not abandon a running batch", vm.batch.value != null)
            vm.startBatch("Second", "Grace", uris(2))
        } finally {
            gate.release()
        }

        val state = awaitBatch(vm)
        assertEquals("First", state.title)
        assertEquals(10, state.total)
        assertEquals(10, state.saved)
        assertEquals(1, state.created.size)
        assertEquals("First", state.created.single().name)

        vm.dismissBatch()
        assertNull("dismiss clears the finished batch", vm.batch.value)
    }

    @Test
    fun repositoryWriteFailureCountsPhotosAsFailed() {
        // A file where the pack root should be makes every createPack fail.
        val packsRoot = File(app.filesDir, "packs")
        packsRoot.deleteRecursively()
        assertTrue("could not make the pack root unwritable", packsRoot.createNewFile())

        val vm = viewModel(true, true, true)
        vm.startBatch("Broken", "Ada", uris(3))
        val state = awaitBatch(vm)

        assertEquals(BatchPhase.DONE, state.phase)
        assertEquals(3, state.processed)
        assertEquals(0, state.saved)
        assertEquals(3, state.failed)
        assertTrue("a failed run creates no packs", state.created.isEmpty())
    }

    @Test
    fun singleSurvivingPackGetsThePlainTitle() {
        val loads = BooleanArray(31)
        loads[0] = true
        val vm = viewModel(*loads)

        vm.startBatch("Title", "Ada", uris(31))
        val state = awaitBatch(vm)

        assertEquals(1, state.created.size)
        assertEquals("Title", state.created.first().name)
        assertEquals(1, state.created.first().stickerCount)

        val packs = repo().listPacks()
        assertEquals(1, packs.size)
        assertEquals("Title", packs.first().name)
    }

    @Test
    fun blankTitleAndAuthorFallBack() {
        val vm = viewModel(true, true)

        vm.startBatch("  ", "  ", uris(2))
        val state = awaitBatch(vm)

        assertEquals(1, state.created.size)
        assertEquals("My stickers", state.created.first().name)
        assertEquals("stikr", state.created.first().author)

        val pack = repo().listPacks().single()
        assertEquals("My stickers", pack.name)
        assertEquals("stikr", pack.author)
    }

    @Test
    fun cutoutUnavailableKeepsFullPhoto() {
        val vm = viewModel(true, true, true)
        vm.subjectCutout = { null }

        vm.startBatch("Keepers", "Ada", uris(3))
        val state = awaitBatch(vm)

        assertEquals(3, state.saved)
        assertEquals(3, state.fellBack)
        assertEquals(0, state.failed)

        val pack = repo().listPacks().single()
        assertEquals("Keepers", pack.name)
        assertEquals(3, pack.stickerCount)
    }

    @Test
    fun aFullPackIsNotSavedIntoAndKeepsTheEditor() {
        val repository = repo()
        val pack = repository.createPack("Full")
        repeat(PackRepository.MAX_WHATSAPP_STICKERS) {
            repository.addSticker(pack.id, byteArrayOf(1, 2, 3))
        }

        val vm = MainViewModel(app)
        vm.startEditor(pack.id)
        vm.setCutout(false)
        vm.loadImage(photoUri())
        await { vm.editor.value?.ready == true }

        var saved = false
        vm.saveSticker { saved = true }
        await { vm.message.value != null }

        assertEquals("That pack is full \u2014 save into a new one", vm.message.value)
        assertTrue("nothing was saved into the full pack", !saved)
        assertNotNull("the editor keeps the photo", vm.editor.value)
        assertEquals(30, repository.getPack(pack.id)!!.stickerCount)
    }

    @Test
    fun saveStickerToNewPackNamesTheNextPackAndPublishesIt() {
        val repository = repo()
        val pack = repository.createPack("My stickers", "Ada")

        val vm = MainViewModel(app)
        vm.startEditor(pack.id)
        vm.setCutout(false)
        vm.loadImage(photoUri())
        await { vm.editor.value?.ready == true }

        var newPack: StickerPack? = null
        var published = false
        vm.saveStickerToNewPack {
            newPack = it
            // The new pack is in the list before the screen navigates to it.
            published = vm.packs.value.any { pack -> pack.id == it.id }
        }
        await { newPack != null }

        val saved = requireNotNull(newPack)
        assertTrue("the new pack is published before the callback navigates", published)
        assertEquals("My stickers 2", saved.name)
        assertEquals("Ada", saved.author)
        assertEquals(1, saved.stickerCount)
        assertNull("the editor is cleared", vm.editor.value)
        assertEquals("Saved to \u201CMy stickers 2\u201D", vm.message.value)
    }

    /**
     * The tour follows the stored preference. It lives in this file because the
     * task may only touch this test file.
     */
    @Test
    fun tourStateFollowsTheStoredPreference() {
        TourPrefs(app).done = false
        val vm = MainViewModel(app)
        assertTrue("the tour shows on a first run", vm.showTour.value)

        vm.completeTour()
        assertFalse("completing the tour hides it", vm.showTour.value)
        assertTrue("the tour is remembered", TourPrefs(app).done)

        vm.replayTour()
        assertTrue("the tour can be replayed", vm.showTour.value)

        val next = MainViewModel(app)
        assertFalse("the tour does not come back on its own", next.showTour.value)
    }

    /**
     * A failure while encoding must not leave an empty pack behind, and the
     * editor must not stay stuck on "saving".
     */
    @Test
    fun aFailedSaveIntoANewPackLeavesNothingBehind() {
        val repository = repo()
        val pack = repository.createPack("Mine")

        val vm = MainViewModel(app)
        vm.startEditor(pack.id)
        vm.setCutout(false)
        vm.loadImage(photoUri())
        await { vm.editor.value?.ready == true }
        // A recycled subject makes the render throw inside the save.
        vm.editor.value!!.subject!!.recycle()

        vm.saveStickerToNewPack { }
        await { vm.message.value != null }

        assertEquals("Couldn't save the sticker", vm.message.value)
        assertNotNull("the editor keeps the photo", vm.editor.value)
        assertFalse("the editor is not stuck saving", vm.editor.value!!.saving)
        assertEquals("no empty pack is left behind", 1, repository.listPacks().size)
    }

    /** A real photo on disk, so the editor has a subject to work with. */
    private fun photoUri(): Uri {
        val photo = File(app.cacheDir, "batch-photo.png")
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.GREEN)
        photo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return Uri.fromFile(photo)
    }
}
