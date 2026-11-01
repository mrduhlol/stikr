package com.stikr.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import com.stikr.app.R
import com.stikr.app.render.StickerRenderer
import com.stikr.app.ui.theme.StikrTheme
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk=[34],qualifiers="w400dp-h880dp-xxhdpi")
class ApprovedBuildTest : ScreenRenderHarness() {
    @Test fun nativeCardUsesApprovedCoverAndLargeType() {
        val pack=packFixture(name="Little reactions",count=10).copy(author="Abhishek",thumbnailImage="thumbnail.png",serialNumber=1,displayId="86483904")
        val image=BitmapFactory.decodeResource(context.resources,R.drawable.tour_preview_cover)
        File(context.filesDir,"packs/${pack.id}/thumbnail.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) }
        image.recycle()
        compose.setContent { StikrTheme { PackScreen(pack,{},{},{},{},onDeleteSticker={},onShareSticker={},onExport={}) } }
        settle()
        compose.onNodeWithText("Pack edit").assertIsDisplayed()
        compose.onNodeWithText("LVL 001").assertIsDisplayed()
        capture("approved-pack-native")
    }
    @Test fun editorPinchRotateAndSaveOnlyAreRealInteractions() {
        val image=BitmapFactory.decodeResource(context.resources,R.drawable.tour_reaction_shock)
        val style=mutableStateOf(StickerRenderer.Style(caption="No thoughts.",captionX=.5f,captionY=.5f,captionSize=60,captionFont="condensed"))
        var saves=0;var exports=0
        compose.setContent { StikrTheme {
            EditorScreen(EditorState("single",image,image,StickerRenderer.render(image,style.value),style=style.value),"Stikr",{}, {},{style.value=it},{saves++},false,{}, {},onSaveAndExport={exports++})
        } }
        val canvas=compose.onNodeWithContentDescription("Sticker preview")
        canvas.performTouchInput {
            down(0,center+Offset(-40f,0f));down(1,center+Offset(40f,0f))
            moveTo(0,center+Offset(-90f,0f),delayMillis=50)
            moveTo(1,center+Offset(90f,0f),delayMillis=50)
            up(0);up(1)
        }
        assertTrue("pinch should grow actual style",style.value.captionSize>60)
        canvas.performTouchInput {
            down(0,center+Offset(-70f,0f));down(1,center+Offset(70f,0f))
            moveTo(0,center+Offset(0f,-70f),delayMillis=50)
            moveTo(1,center+Offset(0f,70f),delayMillis=50)
            up(0);up(1)
        }
        assertTrue("two fingers should rotate freely",kotlin.math.abs(style.value.captionRotation)>20f)
        assertTrue(style.value.captionX!!.isFinite());assertTrue(style.value.captionY!!.isFinite())
        compose.onNodeWithText("Save only").performClick()
        assertEquals(1,saves);assertEquals(0,exports)
        capture("approved-editor-native")
        compose.onNodeWithText("Orbitron").performScrollTo().assertIsDisplayed()
        capture("approved-text-controls-native")
    }
}
