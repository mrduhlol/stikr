package com.stikr.app.ui

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import com.stikr.app.render.StickerRenderer
import com.stikr.app.ui.theme.StikrTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h880dp-xxhdpi")
class EditorControlsInteractionTest : ScreenRenderHarness() {
    @Test fun unifiedCaptionPalettePreservesClassicColorsAndStroke() {
        val image = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val style = mutableStateOf(StickerRenderer.Style(caption = "Hi", captionTextColor = android.graphics.Color.MAGENTA))
        compose.setContent {
            StikrTheme {
                EditorScreen(
                    state = EditorState(packId = "pack", source = image, subject = image, preview = image, style = style.value),
                    packName = "My stickers", onPick = {}, onCutout = {}, onStyle = { style.value = it },
                    onSave = {}, packFull = false, onSaveNew = {}, onBack = {},
                )
            }
        }
        compose.onAllNodesWithContentDescription("Cherry")[0].performScrollTo().performClick()
        assertEquals(StickerRenderer.CaptionColor.CHERRY, style.value.captionColor)
        assertNull(style.value.captionTextColor)
        compose.onNodeWithText("Text color").assertExists()
        compose.onNodeWithText("POSITION").performScrollTo().assertIsDisplayed()
        capture("editor-unified-caption-colors")
    }
}
