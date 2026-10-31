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
import com.stikr.app.ui.theme.StikrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders whole screens off-device into build/ui-renders/ so the theme can be
 * reviewed without installing the app. A contact sheet, not an assertion suite.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h880dp-xxhdpi")
class ScreenRenderTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val out = File("build/ui-renders").apply { mkdirs() }
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun homeEmptyLight() {
        compose.setContent {
            StikrTheme(darkTheme = false) {
                HomeScreen(
                    packs = emptyList(),
                    onOpenPack = {},
                    onCreatePack = {},
                    onMakeSticker = {},
                    onStartBatch = { _, _, _, _ -> },
                    onShowTour = {},
                )
            }
        }
        settle()
        capture("home-empty-light")
    }

    @Test
    fun homePacksLight() {
        val packs = listOf(packFixture("pack-2", "Faces"), packFixture("pack-1", "My stickers"))
        compose.setContent {
            StikrTheme(darkTheme = false) {
                HomeScreen(
                    packs = packs,
                    onOpenPack = {},
                    onCreatePack = {},
                    onMakeSticker = {},
                    onStartBatch = { _, _, _, _ -> },
                    onShowTour = {},
                )
            }
        }
        settle()
        capture("home-packs-light")
    }

    @Test
    fun homePacksDark() {
        val packs = listOf(packFixture("pack-2", "Faces"), packFixture("pack-1", "My stickers"))
        compose.setContent {
            StikrTheme(darkTheme = true) {
                HomeScreen(
                    packs = packs,
                    onOpenPack = {},
                    onCreatePack = {},
                    onMakeSticker = {},
                    onStartBatch = { _, _, _, _ -> },
                    onShowTour = {},
                )
            }
        }
        settle()
        capture("home-packs-dark")
    }

    @Test
    fun homePacksAmoled() {
        val packs = listOf(packFixture("pack-2", "Faces"), packFixture("pack-1", "My stickers"))
        compose.setContent {
            StikrTheme(darkTheme = true, amoled = true) {
                HomeScreen(
                    packs = packs, onOpenPack = {}, onCreatePack = {}, onMakeSticker = {},
                    onStartBatch = { _, _, _, _ -> }, onShowTour = {},
                )
            }
        }
        settle()
        capture("home-packs-amoled")
    }

    @Test
    fun packScreenLight() {
        val pack = packFixture("pack-1", "My stickers")
        compose.setContent {
            StikrTheme(darkTheme = false) {
                PackScreen(
                    pack = pack,
                    onBack = {},
                    onRename = {},
                    onDelete = {},
                    onAddSticker = {},
                    onDeleteSticker = {},
                    onShareSticker = {},
                    onExport = {},
                )
            }
        }
        settle()
        capture("pack-light")
    }

    @Test
    fun packScreenDark() {
        val pack = packFixture("pack-1", "My stickers")
        compose.setContent {
            StikrTheme(darkTheme = true) {
                PackScreen(
                    pack = pack,
                    onBack = {},
                    onRename = {},
                    onDelete = {},
                    onAddSticker = {},
                    onDeleteSticker = {},
                    onShareSticker = {},
                    onExport = {},
                )
            }
        }
        settle()
        capture("pack-dark")
    }

    @Test
    fun editorScreenLight() {
        editorScreen(dark = false)
        capture("editor-light")
    }

    @Test
    fun editorBottomLight() {
        editorScreen(dark = false)
        capture("editor-light-bottom", scrollTo = "BOTTOM")
    }

    @Test
    fun editorScreenDark() {
        editorScreen(dark = true)
        capture("editor-dark")
    }

    @Test
    fun editorBottomDark() {
        editorScreen(dark = true)
        capture("editor-dark-bottom", scrollTo = "BOTTOM")
    }

    @Test
    fun editorFullLight() {
        editorScreen(dark = false, packFull = true)
        capture("editor-full-light")
    }

    private fun editorScreen(
        dark: Boolean,
        packFull: Boolean = false,
        transform: (EditorState) -> EditorState = { it },
    ) {
        val subject = sampleSubject(0xFFFFAB5E.toInt())
        val style = StickerRenderer.Style(outline = 14, caption = "hello", captionColor = StickerRenderer.CaptionColor.CHERRY)
        val state = transform(
            EditorState(
                packId = "pack-1",
                source = subject,
                subject = subject,
                preview = StickerRenderer.render(subject, style),
                style = style,
            ),
        )
        compose.setContent {
            StikrTheme(darkTheme = dark) {
                EditorScreen(
                    state = state,
                    packName = "My stickers",
                    onPick = {},
                    onCutout = {},
                    onStyle = {},
                    onSave = {},
                    onSaveNew = {},
                    packFull = packFull,
                    onBack = {},
                )
            }
        }
        settle()
    }

    @Test
    fun packScreenWhatsAppLight() {
        val packageManager = context.packageManager
        val whatsApp = android.content.pm.PackageInfo().apply { packageName = "com.whatsapp" }
        org.robolectric.Shadows.shadowOf(packageManager).installPackage(whatsApp)

        val pack = packFixture("pack-1", "My stickers")
        compose.setContent {
            StikrTheme(darkTheme = false) {
                PackScreen(
                    pack = pack,
                    onBack = {},
                    onRename = {},
                    onDelete = {},
                    onAddSticker = {},
                    onDeleteSticker = {},
                    onShareSticker = {},
                    onExport = {},
                )
            }
        }
        settle()
        capture("pack-whatsapp-light")
    }

    @Test
    fun newPackDialogLight() {
        compose.setContent {
            StikrTheme(darkTheme = false) {
                NameDialog(
                    title = "New pack",
                    confirmLabel = "Create",
                    initial = "",
                    onDismiss = {},
                    onConfirm = {},
                )
            }
        }
        settle()
        captureDialog("dialog-light")
    }

    @Test
    fun newPackDialogDark() {
        compose.setContent {
            StikrTheme(darkTheme = true) {
                NameDialog(title = "Rename pack", confirmLabel = "Save", initial = "Faces", onDismiss = {}, onConfirm = {})
            }
        }
        settle()
        captureDialog("dialog-dark")
    }

    @Test
    fun confirmDialogLight() = confirmDialog(dark = false, name = "dialog-confirm-light")

    @Test
    fun confirmDialogDark() = confirmDialog(dark = true, name = "dialog-confirm-dark")

    private fun confirmDialog(dark: Boolean, name: String) {
        compose.setContent {
            StikrTheme(darkTheme = dark) {
                ConfirmDialog(
                    kicker = "Can\u2019t be undone",
                    title = "Delete this pack?",
                    body = "\u201CFaces\u201D and its 5 stickers will be removed from stikr. " +
                        "Copies already imported into WhatsApp are not affected.",
                    confirmLabel = "Delete",
                    dismissLabel = "Cancel",
                    onDismiss = {},
                    onConfirm = {},
                )
            }
        }
        settle()
        captureDialog(name)
    }

    @Test
    fun homeEmptyDark() {
        compose.setContent {
            StikrTheme(darkTheme = true) {
                HomeScreen(
                    packs = emptyList(),
                    onOpenPack = {},
                    onCreatePack = {},
                    onMakeSticker = {},
                    onStartBatch = { _, _, _, _ -> },
                    onShowTour = {},
                )
            }
        }
        settle()
        capture("home-empty-dark")
    }

    @Test
    fun packEmptyLight() {
        packScreen(dark = false, pack = packFixture("pack-empty", "Empty", count = 0))
        capture("pack-empty-light")
    }

    @Test
    fun packEmptyDark() {
        packScreen(dark = true, pack = packFixture("pack-empty", "Empty", count = 0))
        capture("pack-empty-dark")
    }

    @Test
    fun packScreenWhatsAppDark() {
        installWhatsApp()
        packScreen(dark = true, pack = packFixture("pack-1", "My stickers"))
        capture("pack-whatsapp-dark")
    }

    @Test
    fun packFewWhatsAppDark() {
        installWhatsApp()
        packScreen(dark = true, pack = packFixture("pack-few", "Dogs", count = 2))
        capture("pack-few-whatsapp-dark")
    }

    @Test
    fun packExportingLight() {
        installWhatsApp()
        packScreen(dark = false, pack = packFixture("pack-1", "My stickers"), exporting = true)
        capture("pack-exporting-light")
    }

    @Test
    fun editorEmptyLight() {
        editorScreen(dark = false) { EditorState(packId = "pack-1") }
        capture("editor-empty-light")
    }

    @Test
    fun editorLoadingLight() {
        editorScreen(dark = false) { it.copy(loading = true, status = "Cutting out the subject\u2026") }
        capture("editor-loading-light")
    }

    @Test
    fun editorLoadingDark() {
        editorScreen(dark = true) { it.copy(loading = true, status = "Cutting out the subject\u2026") }
        capture("editor-loading-dark")
    }

    @Test
    fun editorSavingDark() {
        editorScreen(dark = true) { it.copy(saving = true) }
        capture("editor-saving-dark")
    }

    private fun installWhatsApp() {
        val whatsApp = android.content.pm.PackageInfo().apply { packageName = "com.whatsapp" }
        org.robolectric.Shadows.shadowOf(context.packageManager).installPackage(whatsApp)
    }

    private fun packScreen(dark: Boolean, pack: StickerPack, exporting: Boolean = false) {
        compose.setContent {
            StikrTheme(darkTheme = dark) {
                PackScreen(
                    pack = pack,
                    onBack = {},
                    onRename = {},
                    onDelete = {},
                    onAddSticker = {},
                    onDeleteSticker = {},
                    onShareSticker = {},
                    onExport = {},
                    exporting = exporting,
                )
            }
        }
        settle()
    }

    private fun captureDialog(name: String) {
        val dialog = org.robolectric.shadows.ShadowDialog.getLatestDialog()
        val view = dialog.window?.decorView
        if (view != null && view.width > 0 && view.height > 0) {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            // The decor view is transparent; lay the dialog over a dimmed screen colour.
            bitmap.eraseColor(Color.rgb(90, 88, 84))
            view.draw(Canvas(bitmap))
            File(out, "$name.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    // ------------------------------------------------------------- fixtures

    /** A cut-out subject on transparency, the way ML Kit hands it to the renderer. */
    private fun sampleSubject(circle: Int): Bitmap {
        val subject = Bitmap.createBitmap(600, 760, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(subject)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.rgb(66, 62, 58)
        canvas.drawRoundRect(110f, 440f, 490f, 760f, 150f, 150f, paint)
        paint.color = circle
        canvas.drawCircle(300f, 240f, 200f, paint)
        return subject
    }

    private fun sampleSticker(circle: Int, caption: String = ""): Bitmap =
        StickerRenderer.render(sampleSubject(circle), StickerRenderer.Style(outline = 14, caption = caption))

    private fun packFixture(id: String = "pack-1", name: String = "My stickers", count: Int = 5): StickerPack {
        val dir = File(context.filesDir, "packs/$id").apply { mkdirs() }
        val circles = listOf(
            0xFFFFAB5E.toInt(),
            0xFF56C09E.toInt(),
            0xFF7EA6F0.toInt(),
            0xFFF07E9C.toInt(),
            0xFFD6C05C.toInt(),
        )
        val captions = listOf("hi!", "", "lol", "", "yes")
        val names = circles.take(count).mapIndexed { index, circle ->
            val fileName = "s${index + 1}.webp"
            File(dir, fileName).writeBytes(StickerCodec.encodeWebp(sampleSticker(circle, captions[index])))
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

    // ------------------------------------------------------------- plumbing

    /** Lets async work (sticker files, produceState) land before capturing. */
    private fun settle() {
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
    private fun capture(name: String, scrollTo: String? = null) {
        if (scrollTo != null) {
            compose.onNodeWithText(scrollTo).performScrollTo()
            settle()
        }
        val view: View = compose.activity.findViewById(android.R.id.content)
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File(out, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
