package com.stikr.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import com.stikr.app.data.PackRepository
import com.stikr.app.data.StickerPack
import com.stikr.app.ui.theme.StikrTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the batch flow's surfaces — the create dialog, the progress screen
 * and the done screen — into build/ui-renders/, and drives the stop-confirm
 * interaction. Robolectric, like the other screen renders.
 *
 * The kit draws every button label in caps, so the labels are matched
 * case-insensitively.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h880dp-xxhdpi")
class BatchScreenRenderTest : ScreenRenderHarness() {

    // ------------------------------------------------------------ the dialog

    @Test
    fun batchDialogLight() {
        showCreateDialog(dark = false)
        compose.onNodeWithText("My stickers").assertIsDisplayed()
        compose.onNodeWithText("stikr").assertIsDisplayed()
        captureDialog("batch-dialog-light")
    }

    @Test
    fun batchDialogDark() {
        showCreateDialog(dark = true)
        compose.onNodeWithText("My stickers").assertIsDisplayed()
        compose.onNodeWithText("stikr").assertIsDisplayed()
        captureDialog("batch-dialog-dark")
    }

    private fun showCreateDialog(dark: Boolean) {
        compose.setContent {
            StikrTheme(darkTheme = dark) {
                CreateBatchDialog(onDismiss = {}, onConfirm = { _, _, _ -> })
            }
        }
        settle()
    }

    @Test
    fun createDialogHandsOverTitleAndAuthor() {
        var title = ""
        var author = ""
        compose.setContent {
            StikrTheme(darkTheme = false) {
                CreateBatchDialog(onDismiss = {}, onConfirm = { t, a, _ ->
                    title = t
                    author = a
                })
            }
        }
        settle()
        compose.onNodeWithText("Pick photos", ignoreCase = true).performClick()
        compose.waitForIdle()
        assertEquals("My stickers", title)
        assertEquals(PackRepository.DEFAULT_AUTHOR, author)
    }

    @Test
    fun createDialogFallsBackWhenCleared() {
        var title = ""
        var author = ""
        compose.setContent {
            StikrTheme(darkTheme = false) {
                CreateBatchDialog(onDismiss = {}, onConfirm = { t, a, _ ->
                    title = t
                    author = a
                })
            }
        }
        settle()
        compose.onNodeWithText("My stickers").performTextClearance()
        compose.onNodeWithText("stikr").performTextClearance()
        compose.onNodeWithText("Pick photos", ignoreCase = true).performClick()
        compose.waitForIdle()
        assertEquals("My stickers", title)
        assertEquals(PackRepository.DEFAULT_AUTHOR, author)
    }

    // ------------------------------------------------------------ the screens

    @Test
    fun batchRunningLight() {
        compose.setContent {
            StikrTheme(darkTheme = false) {
                BatchScreen(state = runningState(), onCancel = {}, onDone = {}, onOpenPack = {})
            }
        }
        settle()
        compose.onNodeWithText("37 of 100").assertIsDisplayed()
        capture("batch-running-light")
    }

    @Test
    fun batchDoneLight() {
        showDoneScreen(dark = false, name = "batch-done-light")
    }

    @Test
    fun batchDoneDark() {
        showDoneScreen(dark = true, name = "batch-done-dark")
    }

    private fun showDoneScreen(dark: Boolean, name: String) {
        showDone(doneState(), dark)
        compose.onNodeWithText("2 packs created", ignoreCase = true).assertIsDisplayed()
        capture(name)
    }

    private fun showDone(state: BatchState, dark: Boolean = false) {
        compose.setContent {
            StikrTheme(darkTheme = dark) {
                BatchScreen(state = state, onCancel = {}, onDone = {}, onOpenPack = {})
            }
        }
        settle()
    }

    @Test
    fun pagerSwipesAndEditOpensItsPack() {
        var opened = ""
        compose.setContent {
            StikrTheme(darkTheme = false) {
                BatchScreen(
                    state = doneState(),
                    onCancel = {},
                    onDone = {},
                    onOpenPack = { id -> opened = id },
                )
            }
        }
        settle()
        compose.onNodeWithTag("batch-pager").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithText("Pack 2 of 2 · Swipe to browse").assertIsDisplayed()
        capture("batch-done-second-pack")
        compose.onNodeWithContentDescription("Edit pack Beach trip 2").performClick()
        compose.waitForIdle()
        assertEquals("pack-b2", opened)
    }

    @Test
    fun doneScreenBackFinishes() {
        var done = false
        compose.setContent {
            StikrTheme(darkTheme = false) {
                BatchScreen(state = doneState(), onCancel = {}, onDone = { done = true }, onOpenPack = {})
            }
        }
        settle()
        compose.activity.onBackPressedDispatcher.onBackPressed()
        compose.waitForIdle()
        assertTrue(done)
    }

    // --------------------------------------------------- the done screen's copy

    @Test
    fun onePackCreated() {
        showDone(doneState(created = listOf(packFixture("pack-b1", "Beach trip 1", 30))))
        compose.onNodeWithText("1 pack created", ignoreCase = true).assertIsDisplayed()
        compose.onNodeWithText(SUMMARY_WITH_SUFFIXES, ignoreCase = true).assertIsDisplayed()
        compose.onNodeWithText("— BEACH TRIP 1 —").assertIsDisplayed()
    }

    @Test
    fun noStickersMade() {
        showDone(doneState(created = emptyList()))
        compose.onNodeWithText("No stickers were made", ignoreCase = true).assertIsDisplayed()
        // An empty run has nothing to report: no summary, no pack rows.
        compose.onNodeWithText("photos became stickers", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Beach trip 1", ignoreCase = true).assertDoesNotExist()
    }

    @Test
    fun summaryHasNoSuffixesWhenNothingWentWrong() {
        showDone(doneState(fellBack = 0, failed = 0))
        // An exact match: a stray " · " suffix would fail this.
        compose.onNodeWithText("28 of 100 photos became stickers", ignoreCase = true).assertIsDisplayed()
        compose.onNodeWithText("kept as full photos instead", substring = true).assertDoesNotExist()
        compose.onNodeWithText("couldn't be read", substring = true).assertDoesNotExist()
    }

    @Test
    fun summaryCountsFallbacksAndFailures() {
        showDone(doneState(fellBack = 3, failed = 2))
        compose.onNodeWithText(SUMMARY_WITH_SUFFIXES, ignoreCase = true).assertIsDisplayed()
    }

    // -------------------------------------------------------- the stop confirm

    @Test
    fun stopAsksFirst() {
        var cancelled = false
        compose.setContent {
            StikrTheme(darkTheme = false) {
                BatchScreen(
                    state = runningState(),
                    onCancel = { cancelled = true },
                    onDone = {},
                    onOpenPack = {},
                )
            }
        }
        settle()

        compose.onNodeWithText("Stop", ignoreCase = true).performClick()
        compose.onNodeWithText("Nothing made so far will be kept.").assertIsDisplayed()
        assertFalse("Stop must ask before cancelling", cancelled)

        compose.onNodeWithText("Keep going", ignoreCase = true).performClick()
        compose.onNodeWithText("Nothing made so far will be kept.").assertDoesNotExist()
        assertFalse(cancelled)

        // Two "Stop" matches now: the screen's button and the dialog's.
        compose.onAllNodesWithText("Stop", ignoreCase = true).onFirst().performClick()
        compose.onAllNodesWithText("Stop", ignoreCase = true).onLast().performClick()
        assertTrue("confirming Stop cancels the batch", cancelled)
    }

    @Test
    fun backAsksToStopToo() {
        var cancelled = false
        compose.setContent {
            StikrTheme(darkTheme = false) {
                BatchScreen(
                    state = runningState(),
                    onCancel = { cancelled = true },
                    onDone = {},
                    onOpenPack = {},
                )
            }
        }
        settle()

        compose.activity.onBackPressedDispatcher.onBackPressed()
        compose.waitForIdle()
        compose.onNodeWithText("Nothing made so far will be kept.").assertIsDisplayed()
        assertFalse("Back must ask before cancelling", cancelled)
        assertFalse("Back must not leave the batch", compose.activity.isFinishing)
    }

    @Test
    fun backgroundRemovalDefaultsOffAndCanBeEnabled() {
        var enabled = false
        compose.setContent {
            StikrTheme(darkTheme = false) {
                CreateBatchDialog(onDismiss = {}, onConfirm = { _, _, remove -> enabled = remove })
            }
        }
        settle()
        compose.onNodeWithContentDescription("Remove background").assertIsOff()
        compose.onNodeWithText("Pick photos", ignoreCase = true).performClick()
        assertFalse(enabled)
        compose.onNodeWithContentDescription("Remove background").performClick()
        compose.onNodeWithText("Pick photos", ignoreCase = true).performClick()
        assertTrue(enabled)
    }

    @Test
    fun stickerSheetRequiresDeleteConfirmation() {
        var deleted = ""
        compose.setContent {
            StikrTheme(darkTheme = false) {
                BatchScreen(
                    state = doneState(created = listOf(packFixture(count = 3))),
                    onCancel = {}, onDone = {}, onOpenPack = {},
                    onDeleteSticker = { id, name -> deleted = "$id/$name" },
                )
            }
        }
        settle()
        compose.onNodeWithContentDescription("Sticker 1").performClick()
        compose.onNodeWithText("Edit sticker", ignoreCase = true).assertIsDisplayed()
        compose.onNodeWithText("Share", ignoreCase = true).assertIsDisplayed()
        captureDialog("batch-sticker-sheet")
        compose.onNodeWithText("Delete", ignoreCase = true).performClick()
        assertEquals("", deleted)
        compose.onNodeWithText("Keep", ignoreCase = true).performClick()
        assertEquals("", deleted)
        compose.onNodeWithContentDescription("Sticker 1").performClick()
        compose.onNodeWithText("Delete", ignoreCase = true).performClick()
        compose.onNodeWithText("Delete", ignoreCase = true).performClick()
        assertEquals("pack-1/s1.webp", deleted)
    }

    @Test
    fun processingNamesBackgroundRemoval() {
        compose.setContent {
            StikrTheme(darkTheme = false) {
                BatchScreen(runningState().copy(removeBackground = true), {}, {}, {})
            }
        }
        settle()
        compose.onNodeWithText("Cutting out the subject…").assertIsDisplayed()
    }

    @Test
    fun bottomExportFollowsViewedPack() {
        val packs = listOf(packFixture("first", "First", 3), packFixture("second", "Second", 3))
        var exported = ""
        compose.setContent {
            StikrTheme(darkTheme = false) {
                BatchScreen(doneState(created = packs), {}, {}, {}, onExport = { exported = it })
            }
        }
        settle()
        compose.onNodeWithContentDescription("First thumbnail").assertIsDisplayed()
        compose.onNodeWithContentDescription("Slide to add to WhatsApp").assertIsDisplayed().performTouchInput { swipe(Offset(10f, centerY), Offset(width - 10f, centerY)) }
        assertEquals("first", exported)
        compose.onNodeWithTag("batch-pager").performTouchInput { swipeLeft() }
        settle()
        compose.onNodeWithContentDescription("Second thumbnail").assertIsDisplayed()
        compose.onNodeWithContentDescription("Slide to add to WhatsApp").performTouchInput { swipe(Offset(10f, centerY), Offset(width - 10f, centerY)) }
        assertEquals("second", exported)
        capture("batch-export-second-pack")
    }

    @Test
    fun batchExportExplainsMinimumAndShowsBusyState() {
        var busy by androidx.compose.runtime.mutableStateOf(false)
        val pack = packFixture(count = 2)
        compose.setContent {
            StikrTheme(darkTheme = false) {
                BatchScreen(doneState(created = listOf(pack)), {}, {}, {},
                    exportingPackId = if (busy) "pack-1" else null)
            }
        }
        settle()
        compose.onNodeWithContentDescription("Slide to add to WhatsApp").assertIsNotEnabled()
        compose.onNodeWithText("1 more sticker — WhatsApp takes packs of 3 or more").assertIsDisplayed()
        compose.runOnIdle { busy = true }
        compose.onNodeWithContentDescription("Preparing pack for WhatsApp").assertIsDisplayed()
        compose.onNodeWithContentDescription("Slide to add to WhatsApp").assertDoesNotExist()
    }

    private fun runningState() = BatchState(
        title = "Beach trip",
        author = "stikr",
        total = 100,
        expectedPacks = 4,
        processed = 37,
        saved = 35,
        failed = 2,
    )

    private fun doneState(
        created: List<StickerPack> = listOf(
            packFixture("pack-b1", "Beach trip 1", 30),
            packFixture("pack-b2", "Beach trip 2", 10),
        ),
        saved: Int = 28,
        fellBack: Int = 3,
        failed: Int = 2,
    ) = BatchState(
        title = "Beach trip",
        author = "stikr",
        total = 100,
        expectedPacks = 4,
        processed = 30,
        saved = saved,
        fellBack = fellBack,
        failed = failed,
        created = created,
        phase = BatchPhase.DONE,
    )

    private companion object {
        /** The pinned summary line, both suffixes and all. */
        const val SUMMARY_WITH_SUFFIXES =
            "28 of 100 photos became stickers \u00B7 3 kept as full photos instead " +
                "\u00B7 2 couldn't be read"
    }
}
