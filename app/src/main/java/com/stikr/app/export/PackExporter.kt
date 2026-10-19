package com.stikr.app.export

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import com.stikr.app.data.PackRepository
import com.stikr.app.data.StickerPack
import com.stikr.app.render.StickerCodec
import com.stikr.app.render.StickerRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Hands packs to WhatsApp and single stickers to the share sheet. */
object PackExporter {

    private const val WA_ACTION = "com.whatsapp.intent.action.ENABLE_STICKER_PACK"
    private val WA_PACKAGES = listOf("com.whatsapp", "com.whatsapp.w4b")

    fun authority(context: Context): String = "${context.packageName}.stickercontentprovider"

    fun isWhatsAppInstalled(context: Context): Boolean = WA_PACKAGES.any { isInstalled(context, it) }

    /** Writes tray.webp (the 96x96 pack icon) from the first sticker, so exports are valid. */
    suspend fun ensureTray(context: Context, repo: PackRepository, pack: StickerPack): StickerPack =
        withContext(Dispatchers.IO) {
            val first = repo.stickerFile(pack.id, pack.stickers.firstOrNull().orEmpty()) ?: return@withContext pack
            val existing = pack.trayImage?.let { repo.stickerFile(pack.id, it) }
            if (existing != null && existing.length() <= StickerCodec.MAX_ICON_BYTES &&
                existing.lastModified() >= first.lastModified()
            ) return@withContext pack
            val bitmap = BitmapFactory.decodeFile(first.path) ?: return@withContext pack
            val tray = StickerRenderer.renderTray(bitmap)
            bitmap.recycle()
            val bytes = StickerCodec.encodeWebp(tray, StickerCodec.MAX_ICON_BYTES)
            tray.recycle()
            repo.setTrayImage(pack.id, bytes) ?: pack
        }

    /** Opens WhatsApp on this pack's import screen. False when WhatsApp is missing. */
    fun launchWhatsApp(context: Context, pack: StickerPack): Boolean {
        if (!isWhatsAppInstalled(context)) return false
        val base = Intent(WA_ACTION)
            .putExtra("sticker_pack_id", pack.id)
            .putExtra("sticker_pack_authority", authority(context))
            .putExtra("sticker_pack_name", pack.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        for (pkg in WA_PACKAGES) {
            if (!isInstalled(context, pkg)) continue
            try {
                context.startActivity(Intent(base).setPackage(pkg))
                return true
            } catch (_: Exception) {
                // try the next package
            }
        }
        return try {
            context.startActivity(base)
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Shares one sticker as a PNG through the share sheet. */
    suspend fun shareSticker(context: Context, repo: PackRepository, pack: StickerPack, fileName: String): Boolean =
        withContext(Dispatchers.IO) {
            val sticker = repo.stickerFile(pack.id, fileName) ?: return@withContext false
            val bitmap = BitmapFactory.decodeFile(sticker.path) ?: return@withContext false
            val dir = File(context.cacheDir, "share").apply { mkdirs() }
            val png = File(dir, sticker.nameWithoutExtension + ".png")
            png.outputStream().use { out -> bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out) }
            bitmap.recycle()
            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.files", png)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            withContext(Dispatchers.Main) {
                context.startActivity(
                    Intent.createChooser(send, "Share sticker").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
            true
        }

    private fun isInstalled(context: Context, pkg: String): Boolean = try {
        context.packageManager.getPackageInfo(pkg, 0)
        true
    } catch (_: Exception) {
        false
    }
}
