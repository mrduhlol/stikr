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

    fun isWhatsAppInstalled(context: Context): Boolean = availablePackages(context).isNotEmpty() ||
        resolvesWhatsAppAction(context)

    /** Writes a PNG tray icon (96x96), matching the WhatsApp asset contract. */
    suspend fun ensureTray(context: Context, repo: PackRepository, pack: StickerPack): StickerPack =
        withContext(Dispatchers.IO) {
            val first = repo.stickerFile(pack.id, pack.stickers.firstOrNull().orEmpty()) ?: return@withContext pack
            val existing = pack.trayImage?.let { repo.stickerFile(pack.id, it) }
            if (existing != null && existing.extension == "png" && existing.length() in 1..StickerCodec.MAX_ICON_BYTES &&
                existing.lastModified() >= first.lastModified()
            ) return@withContext pack
            val bitmap = BitmapFactory.decodeFile(first.path) ?: return@withContext pack
            val tray = StickerRenderer.renderTray(bitmap)
            bitmap.recycle()
            val bytes = StickerCodec.encodePng(tray)
            tray.recycle()
            check(bytes.size <= StickerCodec.MAX_ICON_BYTES) { "Tray icon exceeds WhatsApp size limit" }
            repo.setTrayImage(pack.id, bytes, "tray.png") ?: pack
        }

    /**
     * Launch this from an Activity using StartActivityForResult. WhatsApp uses the
     * calling Activity to identify the source app and returns import errors in
     * `validation_error`; an Application-context NEW_TASK launch loses both.
     */
    fun createWhatsAppIntent(context: Context, pack: StickerPack): Intent? {
        val installed = availablePackages(context)
        val hasActionHandler = resolvesWhatsAppAction(context)
        if (installed.isEmpty() && !hasActionHandler) return null
        val intent = Intent(WA_ACTION)
            .putExtra("sticker_pack_id", pack.id)
            .putExtra("sticker_pack_authority", authority(context))
            .putExtra("sticker_pack_name", pack.name.take(128))
        return if (installed.size == 1) {
            intent.setPackage(installed.single())
        } else if (installed.isEmpty()) {
            // Some compatible handlers do not expose package metadata to us.
            // Keep the action implicit so Android can route it to that handler.
            intent
        } else {
            Intent.createChooser(intent, "Add to WhatsApp")
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

    private fun availablePackages(context: Context): List<String> =
        WA_PACKAGES.filter { isInstalled(context, it) }

    private fun resolvesWhatsAppAction(context: Context): Boolean = try {
        context.packageManager.queryIntentActivities(Intent(WA_ACTION), 0).isNotEmpty()
    } catch (_: Exception) {
        false
    }
}
