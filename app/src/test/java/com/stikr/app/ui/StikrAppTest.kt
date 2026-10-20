package com.stikr.app.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.stikr.app.data.PackRepository
import com.stikr.app.data.StickerPack
import com.stikr.app.data.TourPrefs
import com.stikr.app.render.StickerCodec
import com.stikr.app.ui.theme.StikrTheme
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
 * StikrApp is the one place where the tour, the batch and the ordinary
 * screens meet, so the order they appear in — and the full-pack bar in the
 * editor — are pinned here.
 *
 * The kit draws every button label in caps, so the labels are matched
 * case-insensitively.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h880dp-xxhdpi")
class StikrAppTest : ScreenRenderHarness() {

    private val app: Application = ApplicationProvider.getApplicationContext()

    @Before
    fun freshInstall() {
        File(app.filesDir, "packs").deleteRecursively()
        // No tour has been seen: this is a first run.
        TourPrefs(app).done = false
    }

    // ------------------------------------------------------------ the tour

    @Test
    fun theTourShowsOnAFreshInstallAndLeavesWhenItIsCompleted() {
        val viewModel = MainViewModel(app)
        show(viewModel)

        // The tour is over the home screen, not instead of a screen.
        compose.onNodeWithText("Skip", ignoreCase = true).assertIsDisplayed()
        compose.onNodeWithText("Make a sticker", ignoreCase = true).assertDoesNotExist()

        viewModel.completeTour()
        settle()

        compose.onNodeWithText("Skip", ignoreCase = true).assertDoesNotExist()
        compose.onNodeWithText("Make a sticker", ignoreCase = true).assertIsDisplayed()
    }

    // ---------------------------------------------------------- the batch

    @Test
    fun theTourHasPriorityOverARunningBatch() {
        val viewModel = MainViewModel(app)
        val gate = startRunningBatch(viewModel)
        show(viewModel)
        try {
            assertTrue("the run never reached the loader", gate.reached())
            // The tour is on top, so the batch screen is not shown.
            compose.onNodeWithText("Skip", ignoreCase = true).assertIsDisplayed()
            compose.onNodeWithText("Stop", ignoreCase = true).assertDoesNotExist()
        } finally {
            gate.release()
        }
    }

    @Test
    fun theBatchTakesOverOnceTheTourIsDone() {
        TourPrefs(app).done = true
        val viewModel = MainViewModel(app)
        val gate = startRunningBatch(viewModel)
        show(viewModel)
        try {
            assertTrue("the run never reached the loader", gate.reached())
            // No tour: the batch screen is up and the home screen is not.
            compose.onNodeWithText("Stop", ignoreCase = true).assertIsDisplayed()
            compose.onNodeWithText("Skip", ignoreCase = true).assertDoesNotExist()
            compose.onNodeWithText("Make a sticker", ignoreCase = true).assertDoesNotExist()
        } finally {
            gate.release()
        }
    }

    // ------------------------------------------------------- the 30 cap

    @Test
    fun theEditorOfAFullPackOffersANewPackInsteadOfTheSaveButton() {
        val pack = fullPack()
        val viewModel = viewModel(tour = false)
        show(viewModel)
        awaitPacks(viewModel, pack.id)

        compose.onNodeWithText("Make a sticker", ignoreCase = true).performClick()
        settle()

        compose.onNodeWithText("Pack is full — 30 stickers", ignoreCase = true).assertIsDisplayed()
        compose.onNodeWithText("Save into a new pack", ignoreCase = true).assertIsDisplayed()
        // The ordinary save button is gone.
        compose.onNodeWithText("Add to", substring = true, ignoreCase = true).assertDoesNotExist()
    }

    @Test
    fun savingIntoANewPackOpensTheNewPackAndNotTheFullOne() {
        val pack = fullPack()
        val viewModel = viewModel(tour = false)
        show(viewModel)
        awaitPacks(viewModel, pack.id)
        openReadyEditor(viewModel)

        compose.onNodeWithText("Save into a new pack", ignoreCase = true).performClick()
        await { viewModel.packs.value.any { it.name == "Full pack 2" } }
        settle()

        // The pack that was just made is opened, not the full one we left.
        compose.onNodeWithText("Full pack 2", ignoreCase = true).assertIsDisplayed()
        compose.onNodeWithText("Full pack", ignoreCase = true).assertDoesNotExist()
    }

    @Test
    fun aFailedSaveIntoANewPackKeepsTheEditorOpenAndLetsItBeLeft() {
        val pack = fullPack()
        val viewModel = viewModel(tour = false)
        show(viewModel)
        awaitPacks(viewModel, pack.id)
        openReadyEditor(viewModel)

        // The packs folder can't be written to, so the new pack can't be made.
        val packsRoot = File(app.filesDir, "packs")
        assertTrue("could not make the packs folder read-only", packsRoot.setWritable(false, false))
        try {
            compose.onNodeWithText("Save into a new pack", ignoreCase = true).performClick()
            await { viewModel.message.value == "Couldn't save the sticker" }
            settle()

            // The save failed: the editor keeps the photo, and the user is not
            // locked out of it.
            compose.onNodeWithText("Couldn't save the sticker").assertIsDisplayed()
            compose.onNodeWithText("New sticker", ignoreCase = true).assertIsDisplayed()
            compose.onNodeWithText("Pack is full — 30 stickers", ignoreCase = true).assertIsDisplayed()
        } finally {
            packsRoot.setWritable(true, false)
        }

        // The editor can still be left, and the app lands on the pack.
        compose.onNodeWithContentDescription("Back").performClick()
        settle()
        compose.onNodeWithText("Discard", ignoreCase = true).performClick()
        settle()
        compose.onNodeWithText("New sticker", ignoreCase = true).assertDoesNotExist()
        compose.onNodeWithText("Full pack", ignoreCase = true).assertIsDisplayed()
    }

    // ----------------------------------------------------------- plumbing

    /**
     * A view model for a fresh install: with [tour] the first-run tour is
     * waiting, without it the home screen is what the user sees.
     */
    private fun viewModel(tour: Boolean): MainViewModel {
        TourPrefs(app).done = !tour
        return MainViewModel(app)
    }

    private fun show(viewModel: MainViewModel) {
        compose.setContent {
            StikrTheme(darkTheme = false) {
                StikrApp(viewModel = viewModel)
            }
        }
        settle()
    }

    /** Starts a batch whose loader parks, so the run is provably still going. */
    private fun startRunningBatch(viewModel: MainViewModel): Gate {
        val gate = Gate()
        viewModel.imageLoader = {
            gate.hold()
            null
        }
        viewModel.startBatch("Trip", "Ada", listOf(Uri.parse("content://photos/1")))
        return gate
    }

    /** A pack with as many stickers as WhatsApp takes. */
    private fun fullPack(): StickerPack {
        val repository = PackRepository(File(app.filesDir, "packs"))
        val pack = repository.createPack("Full pack", PackRepository.DEFAULT_AUTHOR)
        val bytes = StickerCodec.encodeWebp(sampleSticker(0xFFFFAB5E.toInt()))
        repeat(PackRepository.MAX_WHATSAPP_STICKERS) {
            repository.addSticker(pack.id, bytes)
        }
        return pack
    }

    /** Opens the editor for the first pack, with a photo loaded and ready to save. */
    private fun openReadyEditor(viewModel: MainViewModel) {
        compose.onNodeWithText("Make a sticker", ignoreCase = true).performClick()
        settle()
        viewModel.setCutout(false)
        viewModel.loadImage(photoUri())
        await { viewModel.editor.value?.ready == true }
        settle()
    }

    private fun awaitPacks(viewModel: MainViewModel, packId: String) {
        await { viewModel.packs.value.any { it.id == packId } }
        settle()
    }

    /** Waits for background work to land; no wall-clock sleeps in the assertions. */
    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 30_000_000_000L
        while (!condition() && System.nanoTime() < deadline) {
            // The view model works on the main looper: its posted work and its
            // timers have to run before the state moves on.
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            compose.waitForIdle()
            Thread.sleep(20)
        }
        assertTrue("background work did not finish", condition())
    }

    /** A photo on disk, so the editor can be loaded without the system picker. */
    private fun photoUri(): Uri {
        val photo = File(app.cacheDir, "stikr-photo.png")
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.GREEN)
        photo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return Uri.fromFile(photo)
    }

    /**
     * Parks the batch's photo loader until the test opens the gate, so a test
     * can act on a batch that is provably still running.
     */
    private class Gate {
        private val entered = CountDownLatch(1)
        private val open = CountDownLatch(1)

        fun hold() {
            entered.countDown()
            open.await(30, TimeUnit.SECONDS)
        }

        fun reached(): Boolean = entered.await(30, TimeUnit.SECONDS)

        fun release() = open.countDown()
    }
}
