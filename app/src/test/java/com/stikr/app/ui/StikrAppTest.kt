package com.stikr.app.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Looper
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import com.stikr.app.audio.AppSounds
import androidx.test.core.app.ApplicationProvider
import com.stikr.app.data.PackRepository
import com.stikr.app.data.StickerPack
import com.stikr.app.data.TourPrefs
import com.stikr.app.render.StickerCodec
import com.stikr.app.ui.theme.AppearanceMode
import com.stikr.app.ui.theme.StikrTheme
import org.junit.Assert.assertEquals
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
        compose.onNodeWithText("Create a\nsticker", ignoreCase = true).assertDoesNotExist()

        viewModel.completeTour()
        settle()

        compose.onNodeWithText("Skip", ignoreCase = true).assertDoesNotExist()
        compose.onNodeWithText("Create a\nsticker", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun soundToggleReplacesSettingsAndPersistsItsChoice() {
        val viewModel = viewModel(tour = false)
        show(viewModel)
        val wasEnabled = AppSounds.enabled.value
        try {
            compose.onNodeWithContentDescription(if (wasEnabled) "Mute sounds" else "Unmute sounds").performClick()
            assertEquals(!wasEnabled, AppSounds.enabled.value)
            assertEquals(!wasEnabled, app.getSharedPreferences("app_sounds", 0).getBoolean("enabled", true))
            compose.onNodeWithContentDescription(if (wasEnabled) "Unmute sounds" else "Mute sounds").assertIsDisplayed()
            compose.onNodeWithContentDescription("Settings").assertDoesNotExist()
        } finally { if (AppSounds.enabled.value != wasEnabled) AppSounds.toggle() }
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
            compose.onNodeWithText("Create a\nsticker", ignoreCase = true).assertDoesNotExist()
        } finally {
            gate.release()
        }
    }

    // ------------------------------------------------------- the 30 cap

    @Test
    fun creatingASingleUsesStikrInsteadOfAnExistingFullLibraryPack() {
        val pack = fullPack()
        val viewModel = viewModel(tour = false)
        show(viewModel)
        awaitPacks(viewModel, pack.id)
        compose.onNodeWithText("Create a\nsticker", ignoreCase = true).performClick()
        await { viewModel.editor.value != null }
        settle()
        val destination = PackRepository(File(app.filesDir, "packs")).getPack(viewModel.editor.value!!.packId)!!
        assertTrue(destination.isSingles)
        assertEquals("Stikr", destination.name)
        assertEquals(2, destination.starterStickers.size)
        assertEquals(0, viewModel.editor.value!!.style.outline)
        assertEquals(false, viewModel.editor.value!!.style.roundedCorners)
        compose.onNodeWithText("Pack is full — 30 stickers", ignoreCase = true).assertDoesNotExist()
    }

    @Test
    fun editingAStickerInAFullPackReplacesItWithoutOpeningThePicker() {
        val pack = fullPack()
        val viewModel = viewModel(tour = false)
        show(viewModel)
        awaitPacks(viewModel, pack.id)
        compose.onNodeWithText("Full pack", ignoreCase = true).performClick()
        settle()
        compose.onNodeWithContentDescription("Sticker 1").performScrollTo().performClick()
        settle()
        compose.onNodeWithText("Edit sticker", ignoreCase = true).performClick()
        await { viewModel.editor.value?.ready == true }
        settle()
        compose.onNodeWithText("Edit sticker", ignoreCase = true).assertIsDisplayed()
        compose.onNodeWithText("Save changes", ignoreCase = true).assertIsDisplayed()
        compose.onNodeWithText("Pack is full — 30 stickers", ignoreCase = true).assertDoesNotExist()
        assertEquals(pack.id, viewModel.editor.value!!.packId)
        assertTrue(viewModel.editor.value!!.editingFileName != null)
        compose.onNodeWithText("Save changes", ignoreCase = true).performClick()
        await { viewModel.editor.value == null }
        settle()
        assertEquals(30, PackRepository(File(app.filesDir, "packs")).getPack(pack.id)!!.stickerCount)
        compose.onAllNodesWithText("Full pack", ignoreCase = true).onFirst().assertIsDisplayed()
    }

    @Test
    fun leavingPackEditDoesNotTurnTheNextStickerEditIntoABulkEdit() {
        val pack = fullPack()
        val model = viewModel(tour = false)
        show(model)
        awaitPacks(model,pack.id)
        compose.onNodeWithText("Full pack",ignoreCase=true).performClick()
        settle()
        compose.onNodeWithText("Pack edit").performClick()
        await { model.editor.value?.ready==true }
        compose.onNodeWithText("Apply to entire pack").assertIsDisplayed()
        compose.activity.onBackPressedDispatcher.onBackPressed()
        compose.onNodeWithText("Discard",ignoreCase=true).performClick()
        settle()
        compose.onNodeWithContentDescription("Sticker 1").performScrollTo().performClick()
        compose.onNodeWithText("Edit sticker",ignoreCase=true).performClick()
        await { model.editor.value?.ready==true }
        compose.onNodeWithText("Save changes",ignoreCase=true).assertIsDisplayed()
        compose.onNodeWithText("Apply to entire pack").assertDoesNotExist()
    }

    @Test
    fun savingASingleReturnsToTheLibraryAndKeepsTheFullPack() {
        val pack = fullPack()
        val viewModel = viewModel(tour = false)
        show(viewModel)
        awaitPacks(viewModel, pack.id)
        openReadyEditor(viewModel)
        compose.onNodeWithText("Save only", ignoreCase = true).performClick()
        await { viewModel.editor.value == null }
        settle()
        compose.onNodeWithText("Your packs").assertIsDisplayed()
        compose.onNodeWithText("1 saved").assertIsDisplayed()
        val repository = PackRepository(File(app.filesDir, "packs"))
        assertEquals(30, repository.getPack(pack.id)!!.userStickerCount)
        assertEquals(1, repository.listPacks().single { it.isSingles }.userStickerCount)
    }

    @Test
    fun aFailedSingleSaveKeepsThePhotoForRetry() {
        val viewModel = viewModel(tour = false)
        show(viewModel)
        openReadyEditor(viewModel)
        val destination = File(app.filesDir, "packs/${viewModel.editor.value!!.packId}")
        assertTrue(destination.setWritable(false, false))
        try {
            compose.onNodeWithText("Save only", ignoreCase = true).performClick()
            await { viewModel.message.value == "Couldn't save the sticker" }
            assertTrue(viewModel.editor.value?.ready == true)
            compose.onNodeWithText("Save only", ignoreCase = true).assertIsDisplayed()
        } finally { destination.setWritable(true, false) }
        compose.onNodeWithText("Save only", ignoreCase = true).performClick()
        await { viewModel.editor.value == null }
        settle()
        compose.onNodeWithText("Your packs").assertIsDisplayed()
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
        compose.onNodeWithText("Create a\nsticker", ignoreCase = true).performClick()
        await { viewModel.editor.value != null }
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
