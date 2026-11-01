package com.stikr.app.ui

import androidx.compose.ui.test.*
import com.stikr.app.ui.theme.StikrTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h880dp-xxhdpi")
class ProfilePanelTest : ScreenRenderHarness() {
    @Test
    fun homeExposesAccountEntry() {
        var opened = false
        compose.setContent {
            StikrTheme {
                HomeScreen(emptyList(), {}, {}, {}, { _, _, _, _ -> }, {},
                    onProfile = { opened = true })
            }
        }
        compose.onNodeWithContentDescription("Account and backup").performClick()
        assertTrue(opened)
    }

    @Test
    fun disconnectedOffersOptionalConnectionAndShowsHostStatus() {
        var connected = false
        compose.setContent {
            StikrTheme {
                ProfilePanel(null, null, false, "Google backup setup is not ready",
                    { connected = true }, {}, {}, {}, {})
            }
        }
        compose.onNodeWithText("Cloud backup is optional.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Google backup setup is not ready").assertIsDisplayed()
        compose.onNodeWithText("Back up now").assertDoesNotExist()
        compose.onNodeWithText("Connect Google").performClick()
        assertTrue(connected)
    }

    @Test
    fun restoreRequiresConfirmationAndCancelKeepsLocalState() {
        var restores = 0
        compose.setContent {
            StikrTheme {
                ProfilePanel("person@example.com", "Today at 10:30", false, null,
                    {}, {}, { restores++ }, {}, {})
            }
        }
        compose.onNodeWithText("Last backup: Today at 10:30").assertIsDisplayed()
        compose.onNodeWithText("Restore backup").performClick()
        assertEquals(0, restores)
        compose.onNodeWithText("Your local packs stay in place.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(0, restores)
        compose.onNodeWithText("Restore backup").performClick()
        compose.onNodeWithText("Restore", substring = false).performClick()
        assertEquals(1, restores)
    }

    @Test
    fun busyStateDisablesCloudMutations() {
        compose.setContent {
            StikrTheme {
                ProfilePanel("person@example.com", null, true, "Uploading…",
                    {}, {}, {}, {}, {})
            }
        }
        compose.onNodeWithText("Back up now").assertIsNotEnabled()
        compose.onNodeWithText("Restore backup").assertIsNotEnabled()
        compose.onNodeWithText("Disconnect").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Automatic backup").assertIsNotEnabled()
    }

    @Test
    fun automaticBackupIsOffUntilExplicitlyEnabled() {
        var enabled = false
        compose.setContent {
            StikrTheme {
                ProfilePanel("person@example.com", null, false, null, {}, {}, {}, {}, {},
                    onAutomaticBackup = { enabled = it })
            }
        }
        compose.onNodeWithContentDescription("Automatic backup").assertIsOff().performClick()
        assertTrue(enabled)
    }
}
