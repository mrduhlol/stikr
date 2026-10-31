package com.stikr.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
class PackScreenActionTest : ScreenRenderHarness() {
    @Test
    fun thumbnailAndBottomExportWorkWithoutCachedWhatsAppDetection() {
        var exported = false
        val pack = packFixture(count = 3)
        compose.setContent {
            StikrTheme(darkTheme = false) {
                PackScreen(pack, {}, {}, {}, {}, onDeleteSticker = {}, onShareSticker = {},
                    onExport = { exported = true })
            }
        }
        settle()
        val thumbnail = compose.onNodeWithContentDescription("My stickers thumbnail").assertIsDisplayed()
        val action = compose.onNodeWithText("Add to WhatsApp", ignoreCase = true).assertIsDisplayed().assertIsEnabled()
        assertTrue(action.fetchSemanticsNode().boundsInRoot.top > thumbnail.fetchSemanticsNode().boundsInRoot.bottom)
        action.performClick()
        assertTrue(exported)
        compose.onNodeWithContentDescription("Sticker 1").assertIsDisplayed()
        capture("pack-thumbnail-bottom-export")
    }

    @Test
    fun exportRequiresThreeToThirtyStickers() {
        var count by androidx.compose.runtime.mutableStateOf(2)
        compose.setContent {
            StikrTheme(darkTheme = false) {
                androidx.compose.foundation.layout.Column { WhatsAppPackAction(count, false, {}) }
            }
        }
        compose.onNodeWithText("Add to WhatsApp", ignoreCase = true).assertIsNotEnabled()
        compose.onNodeWithText("1 more sticker — WhatsApp takes packs of 3 or more").assertIsDisplayed()
        compose.runOnIdle { count = 30 }
        compose.onNodeWithText("Add to WhatsApp", ignoreCase = true).assertIsEnabled()
        compose.runOnIdle { count = 31 }
        compose.onNodeWithText("Add to WhatsApp", ignoreCase = true).assertIsNotEnabled()
        compose.onNodeWithText("WhatsApp takes packs of 3–30 stickers. Remove 1 to continue.").assertIsDisplayed()
    }
}
