package com.stikr.app.data

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.stikr.app.render.StickerRenderer
import com.stikr.app.ui.EditorScreen
import com.stikr.app.ui.EditorState
import com.stikr.app.ui.theme.StikrTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h880dp-xxhdpi")
class RecentCaptionsTest : com.stikr.app.ui.ScreenRenderHarness() {

    private val app = ApplicationProvider.getApplicationContext<Application>()

    @Before
    fun fresh() {
        app.getSharedPreferences("recent-captions", android.content.Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun captionsAreKeptNewestFirstWithoutDuplicatesOrBlanks() {
        val store = RecentCaptions(app)
        store.record("  Hello  ")
        store.record("Bye")
        store.record("Hello")
        store.record("   ")
        assertEquals(listOf("Hello", "Bye"), store.all())
    }

    @Test
    fun theListIsBoundedToSixCaptions() {
        val store = RecentCaptions(app)
        repeat(9) { store.record("Caption $it") }
        assertEquals(
            listOf("Caption 8", "Caption 7", "Caption 6", "Caption 5", "Caption 4", "Caption 3"),
            store.all(),
        )
    }

    @Test
    fun tappingARecentCaptionFillsTheCaptionField() {
        val image = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        image.eraseColor(Color.GREEN)
        val style = mutableStateOf(StickerRenderer.Style())
        var picked = "none"
        compose.setContent {
            StikrTheme {
                EditorScreen(
                    state = EditorState(packId = "pack", source = image, subject = image, preview = image, style = style.value),
                    packName = "My stickers",
                    recentCaptions = listOf("Nice", "Wow"),
                    onPickCaption = { picked = it; style.value = style.value.copy(caption = it) },
                    onPick = {}, onCutout = {}, onStyle = { style.value = it },
                    onSave = {}, packFull = false, onSaveNew = {}, onBack = {},
                )
            }
        }
        compose.onNodeWithText("Wow").performScrollTo().performClick()
        assertEquals("picked=$picked caption=${style.value.caption}", "Wow", style.value.caption)
    }
}
