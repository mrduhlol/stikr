package com.stikr.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.stikr.app.ui.theme.StikrTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The first-run tour: the three pages rendered, light and dark, plus the two
 * ways out — finish on the last page, or skip from anywhere.
 *
 * The kit draws every button label in caps, so the labels are matched
 * case-insensitively.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h880dp-xxhdpi")
class TourScreenRenderTest : ScreenRenderHarness() {

    @Test
    fun tourPage1Light() {
        compose.setContent {
            StikrTheme(darkTheme = false) {
                TourOverlay(onDone = {})
            }
        }
        settle()
        capture("tour-1-light")
    }

    @Test
    fun tourPage2Light() {
        compose.setContent {
            StikrTheme(darkTheme = false) {
                TourOverlay(onDone = {})
            }
        }
        compose.onNodeWithText("Continue", ignoreCase = true).performClick()
        settle()
        capture("tour-2-light")
    }

    @Test
    fun tourPage3Light() {
        compose.setContent {
            StikrTheme(darkTheme = false) {
                TourOverlay(onDone = {})
            }
        }
        compose.onNodeWithText("Continue", ignoreCase = true).performClick()
        compose.onNodeWithText("Continue", ignoreCase = true).performClick()
        settle()
        capture("tour-3-light")
    }

    @Test
    fun tourPage1Dark() {
        compose.setContent {
            StikrTheme(darkTheme = true) {
                TourOverlay(onDone = {})
            }
        }
        settle()
        capture("tour-1-dark")
    }

    @Test
    fun tourFinishesAndCallsOnDone() {
        var done = false
        compose.setContent {
            StikrTheme(darkTheme = false) {
                TourOverlay(onDone = { done = true })
            }
        }
        compose.onNodeWithText("Continue", ignoreCase = true).performClick()
        compose.onNodeWithText("Continue", ignoreCase = true).performClick()
        assertFalse("Continue only moves the tour along", done)
        compose.onNodeWithText("Get started", ignoreCase = true).performClick()
        compose.waitForIdle()
        assertTrue(done)
    }

    @Test
    fun lastPageBackReturnsWithoutCompleting() {
        var done = false
        compose.setContent { StikrTheme { TourOverlay { done = true } } }
        compose.onNodeWithText("Continue").performClick()
        compose.onNodeWithText("Continue").performClick()
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("02 / 03").assertIsDisplayed()
        assertFalse(done)
    }

    @Test
    fun tourSkipCallsOnDone() {
        var done = false
        compose.setContent {
            StikrTheme(darkTheme = false) {
                TourOverlay(onDone = { done = true })
            }
        }
        compose.onNodeWithText("Skip", ignoreCase = true).performClick()
        compose.waitForIdle()
        assertTrue(done)
    }

    @Test
    fun tourSwallowsBack() {
        var done = false
        compose.setContent {
            StikrTheme(darkTheme = false) {
                TourOverlay(onDone = { done = true })
            }
        }
        settle()
        compose.activity.onBackPressedDispatcher.onBackPressed()
        compose.waitForIdle()
        assertFalse("Back must not lose the tour", done)
        assertFalse("Back must not leave the tour", compose.activity.isFinishing)
        compose.onNodeWithText("Turn little moments into stickers.", substring = true).assertIsDisplayed()
    }
}
