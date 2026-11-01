package com.stikr.app.render

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk=[34])
class MovableTextTest {
    private fun bounds(style:StickerRenderer.Style):Rect {
        val source=Bitmap.createBitmap(32,32,Bitmap.Config.ARGB_8888)
        val result=StickerRenderer.render(source,style)
        var minX=512;var minY=512;var maxX=0;var maxY=0
        for(y in 0 until 512) for(x in 0 until 512) if(Color.alpha(result.getPixel(x,y))>20) {
            minX=minOf(minX,x);minY=minOf(minY,y);maxX=maxOf(maxX,x);maxY=maxOf(maxY,y)
        }
        source.recycle();result.recycle()
        return Rect(minX,minY,maxX,maxY)
    }
    @Test fun textMovesScalesAndRotatesInSavedPixels() {
        val style=StickerRenderer.Style(caption="HELLO",captionX=.5f,captionY=.5f,captionSize=40,captionTextColor=Color.WHITE)
        val base=bounds(style)
        val moved=bounds(style.copy(captionX=.7f,captionY=.3f))
        assertEquals(102,moved.centerX()-base.centerX(),2)
        assertEquals(-102,moved.centerY()-base.centerY(),2)
        val bigger=bounds(style.copy(captionSize=80))
        assertTrue(bigger.width()>base.width()*1.8)
        val rotated=bounds(style.copy(captionRotation=90f))
        assertEquals(base.width(),rotated.height(),3)
        assertEquals(base.height(),rotated.width(),3)
        assertEquals(base,bounds(style.copy(captionRotation=360f)))
    }
    @Test fun transformationsAndFormattingSurviveJsonRoundTrip() {
        val style=StickerRenderer.Style(caption="first",captionX=.3f,captionY=.8f,captionRotation=725f,
            captionItalic=true,captionUnderline=true,captionStrike=true,captionAlign="right",captionBackground=Color.BLUE,
            extraCaptions=listOf(StickerRenderer.Style(caption="second",captionFont="lobster",captionX=.7f,captionY=.2f)))
        assertEquals(style,StickerStyleJson.read(StickerStyleJson.write(style)))
        val old=StickerStyleJson.read(JSONObject("{\"caption\":\"old\",\"outline\":3}"))
        assertNull(old.captionX);assertEquals(3,old.outline);assertTrue(old.extraCaptions.isEmpty())
    }
    @Test fun twelveOfflineFontsAndBackgroundsAffectOutput() {
        StickerTypography.initialize(ApplicationProvider.getApplicationContext())
        assertEquals(12,StickerTypography.fonts.size)
        val base=StickerRenderer.Style(caption="Example",captionX=.5f,captionY=.5f,captionSize=55)
        val noBg=bounds(base)
        val bg=bounds(base.copy(captionBackground=Color.RED))
        assertTrue(bg.width()>noBg.width())
        assertTrue(bg.height()>noBg.height())
        val source=Bitmap.createBitmap(32,32,Bitmap.Config.ARGB_8888)
        val sans=StickerRenderer.render(source,base)
        for(font in StickerTypography.fonts.drop(1)) {
            val image=StickerRenderer.render(source,base.copy(captionFont=font))
            assertFalse("Font $font must have distinct glyphs",image.sameAs(sans));image.recycle()
        }
        source.recycle();sans.recycle()
    }
    private fun assertEquals(expected:Int,actual:Int,tolerance:Int) { assertTrue("$expected versus $actual",kotlin.math.abs(expected-actual)<=tolerance) }
}
