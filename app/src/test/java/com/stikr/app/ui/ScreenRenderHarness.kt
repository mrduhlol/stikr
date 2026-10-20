package com.stikr.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.stikr.app.data.StickerPack
import com.stikr.app.render.StickerCodec
import com.stikr.app.render.StickerRenderer
import org.junit.Rule
import org.robolectric.shadows.ShadowDialog
import java.io.File

/**
 * Shared plumbing for Robolectric screen renders: the compose rule, the
 * output folder, settling and capture, and the sample fixtures. Subclasses
 * carry their own Robolectric annotations and tests.
 */
abstract class ScreenRenderHarness {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    protected val out = File("build/ui-renders").apply { mkdirs() }
    protected val context: Context = ApplicationProvider.getApplicationContext()

    /** Renders and writes a fixture pack's sticker files, then returns the model. */
    protected fun packFixture(id: String = "pack-1", name: String = "My stickers", count: Int = 5): StickerPack {
        val dir = File(context.filesDir, "packs/$id").apply { mkdirs() }
        val circles = listOf(
            0xFFFFAB5E.toInt(),
            0xFF56C09E.toInt(),
            0xFF7EA6F0.toInt(),
            0xFFF07E9C.toInt(),
            0xFFD6C05C.toInt(),
        )
        val names = (0 until count).map { index ->
            val fileName = "s${index + 1}.webp"
            File(dir, fileName).writeBytes(StickerCodec.encodeWebp(sampleSticker(circles[index % circles.size])))
            fileName
        }
        return StickerPack(
            id = id,
            name = name,
            author = "stikr",
            createdAt = System.currentTimeMillis(),
            stickers = names,
        )
    }

    protected fun sampleSticker(circle: Int): Bitmap {
        val photo = Bitmap.createBitmap(600, 800, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(photo)
        canvas.drawColor(Color.rgb(242, 240, 248))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = circle
        canvas.drawCircle(300f, 260f, 170f, paint)
        paint.color = Color.rgb(66, 62, 58)
        canvas.drawRoundRect(150f, 480f, 450f, 700f, 60f, 60f, paint)
        return StickerRenderer.render(
            photo,
            StickerRenderer.Style(outline = 10, caption = "hi"),
        )
    }

    /** Lets async work (sticker files, produceState) land before capturing. */
    protected fun settle() {
        repeat(5) {
            Thread.sleep(200)
            compose.waitForIdle()
            compose.mainClock.advanceTimeBy(32)
            compose.waitForIdle()
        }
    }

    /**
     * captureToImage() needs PixelCopy, which Robolectric doesn't implement, so
     * the content view is drawn straight onto a bitmap instead.
     */
    protected fun capture(name: String, scrollTo: String? = null) {
        if (scrollTo != null) {
            compose.onNodeWithText(scrollTo).performScrollTo()
            settle()
        }
        val view: View = compose.activity.findViewById(android.R.id.content)
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File(out, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /**
     * A dialog is its own window, so the decor view is drawn straight onto a
     * bitmap. The test fails if there is no dialog or it has not been laid
     * out: a blank capture is worse than a failed test.
     */
    protected fun captureDialog(name: String) {
        val view = requireNotNull(ShadowDialog.getLatestDialog()?.window?.decorView) {
            "no dialog window to capture"
        }
        require(view.width > 0 && view.height > 0) { "dialog is not laid out yet" }
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        // The decor view is transparent; lay the dialog over a dimmed screen colour.
        bitmap.eraseColor(Color.rgb(90, 88, 84))
        view.draw(Canvas(bitmap))
        File(out, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
