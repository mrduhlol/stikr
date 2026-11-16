package com.stikr.app.data

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stikr.app.render.StickerRenderer
import com.stikr.app.ui.MainViewModel
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StylePrefsTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()

    @Before
    fun fresh() {
        app.getSharedPreferences("editor-style", Context.MODE_PRIVATE).edit().clear().commit()
        File(app.filesDir, "packs").deleteRecursively()
    }

    @Test
    fun firstSessionUsesTheDefaultLook() {
        assertEquals(StickerRenderer.Style(), StylePrefs(app).last)
    }

    @Test
    fun rememberedLookSurvivesANewSessionWithoutItsCaption() {
        StylePrefs(app).last = StickerRenderer.Style(
            outline = 14,
            caption = "do not copy me",
            captionFont = "serif",
            captionPosition = StickerRenderer.CaptionPosition.TOP,
        )
        val restored = StylePrefs(app).last
        assertEquals(14, restored.outline)
        assertEquals("serif", restored.captionFont)
        assertEquals(StickerRenderer.CaptionPosition.TOP, restored.captionPosition)
        assertEquals("", restored.caption)
    }

    @Test
    fun aNewEditorStartsWithTheRememberedLook() {
        StylePrefs(app).last = StickerRenderer.Style(outline = 20, captionFont = "mono")
        val model = MainViewModel(app)
        model.startEditor("pack")
        val style = model.editor.value!!.style
        assertEquals(20, style.outline)
        assertEquals("mono", style.captionFont)
    }
}
