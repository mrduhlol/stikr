package com.stikr.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.stikr.app.ui.theme.AppearanceMode
import com.stikr.app.ui.theme.StikrTheme
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h880dp-xxhdpi")
class PrivacyScreenTest : ScreenRenderHarness() {

    @Test
    fun settingsOffersAPrivacyEntry() {
        var opened = 0
        compose.setContent {
            StikrTheme {
                SettingsScreen(
                    mode = AppearanceMode.SYSTEM,
                    onSelect = {},
                    onBack = {},
                    onPrivacy = { opened++ },
                )
            }
        }
        compose.onNodeWithText("How it works & privacy", substring = true, ignoreCase = true).performClick()
        assertEquals(1, opened)
    }

    @Test
    fun privacyScreenExplainsOnDeviceProcessingAndGoesBack() {
        var back = 0
        compose.setContent { StikrTheme { PrivacyScreen(onBack = { back++ }) } }
        compose.onNodeWithText("Nothing leaves your phone", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Back", ignoreCase = true).performClick()
        assertEquals(1, back)
    }
}
