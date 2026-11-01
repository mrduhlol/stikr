package com.stikr.app.ui

import androidx.compose.ui.test.*
import com.stikr.app.ui.theme.StikrTheme
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h880dp-xxhdpi")
class LocalBackupPanelTest : ScreenRenderHarness() {
    @Test fun homeHasLocalBackupAndNoAccountEntry() {
        var opened = false
        compose.setContent { StikrTheme {
            HomeScreen(emptyList(), {}, {}, {}, { _, _, _, _ -> }, {}, onBackup = { opened = true })
        } }
        compose.onNodeWithContentDescription("Account and backup").assertDoesNotExist()
        compose.onNodeWithContentDescription("Local backup").performClick()
        assertTrue(opened)
    }

    @Test fun panelExportsAndRestoresWithoutAccountControls() {
        var exported = false
        var restored = false
        compose.setContent { StikrTheme {
            LocalBackupPanel(false, { exported = true }, { restored = true }, {})
        } }
        compose.onNodeWithText("Connect Google").assertDoesNotExist()
        compose.onNodeWithText("Save backup file").performClick()
        compose.onNodeWithText("Restore from file").performClick()
        assertTrue(exported)
        assertTrue(restored)
        captureDialog("local-backup-panel")
    }

    @Test fun busyPanelDisablesFileOperations() {
        compose.setContent { StikrTheme { LocalBackupPanel(true, {}, {}, {}) } }
        compose.onNodeWithText("Save backup file").assertIsNotEnabled()
        compose.onNodeWithText("Restore from file").assertIsNotEnabled()
    }
}
