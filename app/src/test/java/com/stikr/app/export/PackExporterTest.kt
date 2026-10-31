package com.stikr.app.export

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.content.pm.PackageInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.stikr.app.data.PackRepository
import com.stikr.app.render.StickerCodec
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class PackExporterTest {
    private fun exportable(repository: PackRepository, name: String) = repository.createPack(name).let { pack ->
        repeat(3) { repository.addSticker(pack.id, byteArrayOf(it.toByte()), "s$it.webp") }
        requireNotNull(repository.getPack(pack.id))
    }
    @Test
    fun importIntentKeepsTheActivityResultHandshakeAndPackIdentity() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        shadowOf(context.packageManager).installPackage(PackageInfo().apply { packageName = "com.whatsapp" })
        assertTrue(PackExporter.isWhatsAppInstalled(context))
        val pack = exportable(PackRepository(File(context.cacheDir, "intent-packs")), "Friends")
        val intent = requireNotNull(PackExporter.createWhatsAppIntent(context, pack))
        assertEquals("com.whatsapp.intent.action.ENABLE_STICKER_PACK", intent.action)
        assertEquals("com.whatsapp", intent.`package`)
        assertEquals(pack.id, intent.getStringExtra("sticker_pack_id"))
        assertEquals(PackExporter.authority(context), intent.getStringExtra("sticker_pack_authority"))
        assertEquals(pack.name, intent.getStringExtra("sticker_pack_name"))
        assertEquals("Import must launch from the current Activity for a result", 0, intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun importSupportsWhatsAppBusinessAndMissingInstallations() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pack = exportable(PackRepository(File(context.cacheDir, "intent-packs")), "Friends")
        assertNull(PackExporter.createWhatsAppIntent(context, pack))
        assertTrue(!PackExporter.isWhatsAppInstalled(context))
        shadowOf(context.packageManager).installPackage(PackageInfo().apply { packageName = "com.whatsapp.w4b" })
        assertTrue(PackExporter.isWhatsAppInstalled(context))
        assertEquals("com.whatsapp.w4b", PackExporter.createWhatsAppIntent(context, pack)?.`package`)
    }

    @Test
    fun actionHandlerWorksWhenPackageMetadataIsUnavailable() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val action = "com.whatsapp.intent.action.ENABLE_STICKER_PACK"
        shadowOf(context.packageManager).addResolveInfoForIntent(Intent(action), ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = "com.whatsapp"
                name = "com.whatsapp.StickerImportActivity"
                exported = true
            }
        })
        val pack = exportable(PackRepository(File(context.cacheDir, "handler-packs")), "Friends")
        assertTrue(PackExporter.isWhatsAppInstalled(context))
        val intent = requireNotNull(PackExporter.createWhatsAppIntent(context, pack))
        assertEquals(action, intent.action)
        assertNull(intent.`package`)
        assertEquals(pack.id, intent.getStringExtra("sticker_pack_id"))
        assertEquals(0, intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun bothWhatsAppAppsOfferAChooser() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        listOf("com.whatsapp", "com.whatsapp.w4b").forEach { name ->
            shadowOf(context.packageManager).installPackage(PackageInfo().apply { packageName = name })
        }
        val pack = exportable(PackRepository(File(context.cacheDir, "intent-packs")), "Friends")
        val chooser = requireNotNull(PackExporter.createWhatsAppIntent(context, pack))
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        val target = chooser.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)!!
        assertEquals("com.whatsapp.intent.action.ENABLE_STICKER_PACK", target.action)
        assertNull(target.`package`)
        assertEquals(0, chooser.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun exportIdentityAndAuthorityStayStableForAnUpdatedPackAndRejectOverCapacity() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        shadowOf(context.packageManager).installPackage(PackageInfo().apply { packageName = "com.whatsapp" })
        val repo = PackRepository(File(context.cacheDir, "stable-packs"))
        val pack = exportable(repo, "Stable")
        val first = PackExporter.createWhatsAppIntent(context, pack)!!
        val changed = repo.addSticker(pack.id, byteArrayOf(8), "four.webp")!!
        val repeated = PackExporter.createWhatsAppIntent(context, changed)!!
        assertEquals(first.getStringExtra("sticker_pack_id"), repeated.getStringExtra("sticker_pack_id"))
        assertEquals(first.getStringExtra("sticker_pack_authority"), repeated.getStringExtra("sticker_pack_authority"))
        var rejected = false
        try {
            PackExporter.createWhatsAppIntent(context, changed.copy(stickers = changed.stickers + (0..26).map { "fake_$it.webp" }))
        } catch (_: IllegalArgumentException) { rejected = true }
        assertTrue(rejected)
    }

    @Test
    fun reusesTrayUntilTheFirstStickerIsDeleted() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = File(context.filesDir, "packs").apply { deleteRecursively() }
        val repo = PackRepository(root)
        val pack = repo.createPack("Tray")
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)
        repo.addSticker(pack.id, StickerCodec.encodeWebp(bitmap), "red.webp")
        bitmap.eraseColor(Color.BLUE)
        repo.addSticker(pack.id, StickerCodec.encodeWebp(bitmap), "blue.webp")!!
        bitmap.recycle()
        val legacy = repo.setTrayImage(pack.id, byteArrayOf(1, 2, 3), "tray.webp")!!
        val prepared = PackExporter.ensureTray(context, repo, legacy)
        assertEquals("tray.png", prepared.trayImage)
        val trayFile = repo.stickerFile(pack.id, prepared.trayImage!!)!!
        assertTrue(trayFile.readBytes().take(8).toByteArray().contentEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)))
        val modified = trayFile.lastModified()
        val again = PackExporter.ensureTray(context, repo, prepared)
        assertSame("unchanged exports must reuse the prepared pack", prepared, again)
        assertEquals(modified, trayFile.lastModified())
        val remaining = repo.deleteSticker(pack.id, "red.webp")!!
        val updated = PackExporter.ensureTray(context, repo, remaining)
        assertNotNull(updated.trayImage)
        assertTrue(trayFile.length() <= StickerCodec.MAX_ICON_BYTES)
        val tray = BitmapFactory.decodeFile(trayFile.path)
        assertEquals(96, tray.width)
        assertEquals(96, tray.height)
        assertTrue(Color.blue(tray.getPixel(48, 48)) > 220)
        assertTrue(Color.red(tray.getPixel(48, 48)) < 30)
        tray.recycle()
    }
}
