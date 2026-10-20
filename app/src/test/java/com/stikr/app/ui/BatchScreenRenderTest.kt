package com.stikr.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import com.stikr.app.data.PackRepository
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
                CreateBatchDialog(onDismiss = {}, onConfirm = { _, _ -> })
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
                CreateBatchDialog(onDismiss = {}, onConfirm = { t, a ->
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
                CreateBatchDialog(onDismiss = {}, onConfirm = { t, a ->
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
        val state = doneState()
        compose.setContent {
            StikrTheme(darkTheme = dark) {
                BatchScreen(state = state, onCancel = {}, onDone = {}, onOpenPack = {})
            }
        }
        settle()
        compose.onNodeWithText("2 packs created", ignoreCase = true).assertIsDisplayed()
        capture(name)
    }

    @Test
    fun packRowOpensItsPack() {
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
        compose.onNodeWithText("Beach trip 2", ignoreCase = true).performClick()
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

    private fun runningState() = BatchState(
        title = "Beach trip",
        author = "stikr",
        total = 100,
        expectedPacks = 4,
        processed = 37,
        saved = 35,
        failed = 2,
    )

    private fun doneState() = BatchState(
        title = "Beach trip",
        author = "stikr",
        total = 100,
        expectedPacks = 4,
        processed = 30,
        saved = 28,
        fellBack = 3,
        failed = 2,
        created = listOf(
            packFixture("pack-b1", "Beach trip 1", 30),
            packFixture("pack-b2", "Beach trip 2", 10),
        ),
        phase = BatchPhase.DONE,
    )
}
