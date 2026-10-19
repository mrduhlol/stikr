package com.stikr.app.export

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.stikr.app.data.PackRepository
import com.stikr.app.data.StickerPack
import java.io.File
import java.io.FileNotFoundException

/**
 * Serves packs to WhatsApp. When the user taps "Add to WhatsApp", WhatsApp
 * queries this provider for pack metadata and the sticker list, then reads the
 * sticker and tray files through it. Paths and column names are fixed by
 * WhatsApp's sticker pack contract (github.com/WhatsApp/stickers):
 *
 *   metadata                         every pack
 *   metadata/<pack id>               one pack
 *   stickers/<pack id>               that pack's sticker files and emojis
 *   stickers_asset/<pack id>/<file>  a sticker or the tray icon
 */
class StickerPackContentProvider : ContentProvider() {

    private val repo: PackRepository by lazy {
        PackRepository(File(requireNotNull(context).filesDir, "packs"))
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val segments = uri.pathSegments
        return when {
            segments.firstOrNull() == METADATA && segments.size == 1 -> metadataCursor(repo.listPacks())
            segments.firstOrNull() == METADATA && segments.size == 2 ->
                metadataCursor(listOfNotNull(repo.getPack(segments[1])))
            segments.firstOrNull() == STICKERS && segments.size == 2 -> stickersCursor(repo.getPack(segments[1]))
            else -> MatrixCursor(PACK_COLUMNS)
        }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw FileNotFoundException("stikr only serves read-only stickers")
        val segments = uri.pathSegments
        val file: File? = if (segments.size == 3 && segments[0] == STICKERS_ASSET) {
            repo.stickerFile(segments[1], segments[2])
        } else {
            null
        }
        if (file == null || !file.isFile) throw FileNotFoundException("No such sticker: $uri")
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri): String? {
        val segments = uri.pathSegments
        val authority = uri.authority
        return when (segments.firstOrNull()) {
            METADATA -> if (segments.size == 1) {
                "vnd.android.cursor.dir/vnd.$authority.$METADATA"
            } else {
                "vnd.android.cursor.item/vnd.$authority.$METADATA"
            }
            STICKERS -> "vnd.android.cursor.dir/vnd.$authority.$STICKERS"
            STICKERS_ASSET -> "image/webp"
            else -> null
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    // ------------------------------------------------------------- internals

    private fun metadataCursor(packs: List<StickerPack>): Cursor =
        MatrixCursor(PACK_COLUMNS).apply { packs.forEach { addRow(packRow(it)) } }

    /** One metadata row, in [PACK_COLUMNS] order. */
    private fun packRow(pack: StickerPack): Array<Any?> = arrayOf(
        pack.id,
        pack.name.take(MAX_TEXT_LENGTH),
        pack.author.take(MAX_TEXT_LENGTH),
        pack.trayImage ?: PackRepository.TRAY_FILE,
        "", // android_play_store_link
        "", // ios_app_download_link
        "", // publisher_email
        "", // publisher_website
        "", // privacy_policy_website
        "", // license_agreement_website
        imageDataVersion(pack),
        0, // whatsapp_will_not_cache_stickers
        0, // animated_sticker_pack
    )

    private fun stickersCursor(pack: StickerPack?): Cursor {
        val cursor = MatrixCursor(STICKER_COLUMNS)
        pack?.stickers?.take(PackRepository.MAX_WHATSAPP_STICKERS)?.forEach { fileName ->
            cursor.addRow(arrayOf<Any?>(fileName, DEFAULT_EMOJIS.joinToString(","), null))
        }
        return cursor
    }

    /**
     * WhatsApp caches pack images until this string changes, so it tracks the
     * sticker list and the tray file. The tray is rewritten under the same name.
     */
    private fun imageDataVersion(pack: StickerPack): String {
        val tray = pack.trayImage?.let { repo.stickerFile(pack.id, it) }?.lastModified() ?: 0L
        val stickers = pack.stickers.take(PackRepository.MAX_WHATSAPP_STICKERS)
        return (stickers.hashCode().toLong() * 31 + tray).toULong().toString(36)
    }

    companion object {
        private const val METADATA = "metadata"
        private const val STICKERS = "stickers"
        private const val STICKERS_ASSET = "stickers_asset"
        private const val MAX_TEXT_LENGTH = 128
        private val DEFAULT_EMOJIS = listOf("🙂") // 🙂
        private val PACK_COLUMNS = arrayOf(
            "sticker_pack_identifier",
            "sticker_pack_name",
            "sticker_pack_publisher",
            "sticker_pack_icon",
            "android_play_store_link",
            "ios_app_download_link",
            "sticker_pack_publisher_email",
            "sticker_pack_publisher_website",
            "sticker_pack_privacy_policy_website",
            "sticker_pack_license_agreement_website",
            "image_data_version",
            "whatsapp_will_not_cache_stickers",
            "animated_sticker_pack",
        )
        private val STICKER_COLUMNS = arrayOf(
            "sticker_file_name",
            "sticker_emoji",
            "sticker_accessibility_text",
        )
    }
}
