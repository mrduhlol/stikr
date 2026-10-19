package com.stikr.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import kotlin.random.Random

/**
 * Packs live as plain files in the app's private storage:
 *
 *   packs/
 *     pack_<id>/
 *       pack.json      — name, author, created, sticker file list
 *       stk_<id>.webp  — 512x512 sticker images
 *       tray.webp      — 96x96 pack icon, written when exporting
 *
 * Plain files and JSON keep the app dependency-free and the data inspectable.
 */
class PackRepository(private val root: File) {

    private val lock = Any()
    private data class Stamp(val modified: FileTime, val size: Long, val fileKey: Any?)
    private data class CachedPack(val stamp: Stamp, val pack: StickerPack)
    private val metadata = mutableMapOf<String, CachedPack>()

    fun listPacks(): List<StickerPack> = synchronized(lock) {
        val dirs = root.listFiles { file -> file.isDirectory }.orEmpty()
        metadata.keys.retainAll(dirs.map { it.name }.toSet())
        dirs.mapNotNull { readPack(it) }.sortedByDescending { it.createdAt }
    }

    fun getPack(id: String): StickerPack? = synchronized(lock) { readPack(File(root, id)) }

    fun createPack(name: String, author: String = DEFAULT_AUTHOR): StickerPack = synchronized(lock) {
        val pack = StickerPack(
            id = newPackId(),
            name = name.trim().ifEmpty { "Untitled pack" },
            author = author,
            createdAt = System.currentTimeMillis(),
            stickers = emptyList(),
        )
        writePack(pack)
        pack
    }

    fun renamePack(id: String, name: String): StickerPack? = synchronized(lock) {
        val pack = readPack(File(root, id)) ?: return null
        if (name.isBlank()) return pack
        pack.copy(name = name.trim()).also { writePack(it) }
    }

    fun deletePack(id: String): Boolean = synchronized(lock) {
        metadata.remove(id)
        File(root, id).deleteRecursively()
    }

    fun addSticker(packId: String, bytes: ByteArray, fileName: String = newStickerName()): StickerPack? =
        synchronized(lock) {
            val pack = readPack(File(root, packId)) ?: return null
            val file = File(root, packId).resolve(fileName)
            file.parentFile?.mkdirs()
            file.writeBytes(bytes)
            pack.copy(stickers = pack.stickers + fileName).also { writePack(it) }
        }

    fun deleteSticker(packId: String, fileName: String): StickerPack? = synchronized(lock) {
        val pack = readPack(File(root, packId)) ?: return null
        if (!pack.stickers.contains(fileName)) return pack
        File(root, packId).resolve(fileName).delete()
        pack.copy(
            stickers = pack.stickers - fileName,
            // The generated tray represents the first sticker.
            trayImage = pack.trayImage?.takeUnless { it == fileName || pack.stickers.firstOrNull() == fileName },
        ).also { writePack(it) }
    }

    fun setTrayImage(packId: String, bytes: ByteArray, fileName: String = TRAY_FILE): StickerPack? =
        synchronized(lock) {
            val pack = readPack(File(root, packId)) ?: return null
            File(root, packId).resolve(fileName).writeBytes(bytes)
            pack.copy(trayImage = fileName).also { writePack(it) }
        }

    /**
     * Resolves a sticker file for serving, but only when the pack still lists it.
     * This keeps the exported content provider from reading arbitrary paths.
     */
    fun stickerFile(packId: String, fileName: String): File? = synchronized(lock) {
        if (fileName.isBlank() || fileName.contains('/') || fileName.contains("..")) return null
        val pack = readPack(File(root, packId), filterMissingFiles = false) ?: return null
        if (!pack.stickers.contains(fileName) && pack.trayImage != fileName) return null
        File(root, packId).resolve(fileName).takeIf { it.isFile }
    }

    // ------------------------------------------------------------- internals

    private fun stamp(file: File): Stamp {
        val attributes = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
        return Stamp(attributes.lastModifiedTime(), attributes.size(), attributes.fileKey())
    }

    private fun readPack(dir: File, filterMissingFiles: Boolean = true): StickerPack? {
        val meta = File(dir, PACK_FILE)
        if (!meta.isFile) {
            metadata.remove(dir.name)
            return null
        }
        return try {
            // Atomic replacement changes fileKey even for same-length writes in one clock tick.
            val version = stamp(meta)
            val cached = metadata[dir.name]?.takeIf { it.stamp == version }
            val pack = cached?.pack ?: run {
                val json = JSONObject(meta.readText())
                StickerPack(
                    id = json.getString("id"),
                    name = json.optString("name", "Untitled pack"),
                    author = json.optString("author", DEFAULT_AUTHOR),
                    createdAt = json.optLong("createdAt", 0L),
                    stickers = json.optJSONArray("stickers")?.let { array ->
                        (0 until array.length()).mapNotNull { index ->
                            array.optString(index).takeIf { it.isNotEmpty() }
                        }
                    } ?: emptyList(),
                    trayImage = json.optString("trayImage").takeIf { it.isNotEmpty() },
                ).also { metadata[dir.name] = CachedPack(version, it) }
            }
            // Serving one file only needs to stat that file, not every asset in the pack.
            if (!filterMissingFiles) return pack
            // Files may disappear independently of metadata, including through another instance.
            val stickers = pack.stickers.filter { File(dir, it).isFile }
            val tray = pack.trayImage?.takeIf { File(dir, it).isFile }
            if (stickers == pack.stickers && tray == pack.trayImage) pack
            else pack.copy(stickers = stickers, trayImage = tray)
        } catch (_: Exception) {
            metadata.remove(dir.name)
            null // an unreadable pack.json is skipped rather than crashing the list
        }
    }

    private fun writePack(pack: StickerPack) {
        val dir = File(root, pack.id).apply { mkdirs() }
        val json = JSONObject()
            .put("id", pack.id)
            .put("name", pack.name)
            .put("author", pack.author)
            .put("createdAt", pack.createdAt)
            .put("stickers", JSONArray(pack.stickers))
            .put("trayImage", pack.trayImage ?: "")
        val target = File(dir, PACK_FILE)
        val temp = File(dir, "$PACK_FILE.tmp")
        temp.writeText(json.toString())
        Files.move(temp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        metadata[pack.id] = CachedPack(stamp(target), pack)
    }

    private fun newPackId(): String =
        "pack_" + System.currentTimeMillis().toString(36) + "_" + Random.nextInt(0xFFFF).toString(16)

    private fun newStickerName(): String =
        "stk_" + System.currentTimeMillis().toString(36) + "_" + Random.nextInt(0xFFFF).toString(16) + ".webp"

    companion object {
        const val PACK_FILE = "pack.json"
        const val TRAY_FILE = "tray.webp"
        const val DEFAULT_AUTHOR = "stikr"
        const val MIN_WHATSAPP_STICKERS = 3
        const val MAX_WHATSAPP_STICKERS = 30
    }
}
