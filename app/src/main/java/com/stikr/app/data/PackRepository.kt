package com.stikr.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import com.stikr.app.data.backup.BackupArchive
import kotlin.random.Random

/**
 * Packs live as plain files in the app's private storage:
 *
 *   packs/
 *     pack_<id>/
 *       pack.json      — name, author, created, sticker file list
 *       stk_<id>.webp  — 512x512 sticker images
 *       tray.png       — 96x96 pack icon, written when exporting
 *       *.source.png   — private original photos, not exported
 *       *.edit.json    — private editor settings
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
        val packs = dirs.mapNotNull { readPack(it) }.sortedByDescending { it.createdAt }
        packs.mapIndexed { index, pack ->
            if (pack.serialNumber > 0 && pack.displayId.isNotBlank()) pack else pack.copy(
                serialNumber = if (pack.serialNumber > 0) pack.serialNumber else packs.size - index,
                displayId = pack.displayId.ifBlank { uniqueDisplayId() },
            ).also { writePack(it) }
        }
    }

    fun getPack(id: String): StickerPack? = synchronized(lock) { readPack(File(root, id)) }

    fun createPack(name: String, author: String = DEFAULT_AUTHOR): StickerPack = synchronized(lock) {
        createPackLocked(name, author)
    }

    private fun createPackLocked(name: String, author: String, isSingles: Boolean = false): StickerPack {
        val serial = (listPacks().maxOfOrNull { it.serialNumber } ?: 0) + 1
        val pack = StickerPack(
            id = newPackId(),
            name = name.trim().ifEmpty { PackNames.random() },
            author = author,
            createdAt = System.currentTimeMillis(),
            stickers = emptyList(),
            isSingles = isSingles,
            serialNumber = serial,
            displayId = uniqueDisplayId(),
        )
        writePack(pack)
        return pack
    }

    /** Creates or returns the newest shared singles pack, rotating at WhatsApp capacity. */
    fun getOrCreateSingles(starters: List<ByteArray>): StickerPack = synchronized(lock) {
        val existing = listPacks().filter { it.isSingles }.maxByOrNull { it.serialNumber }
        if (existing != null && existing.userStickerCount < MAX_WHATSAPP_STICKERS) return@synchronized existing
        val name = if (existing == null) "Stikr" else "Stikr ${listPacks().count { it.isSingles } + 1}"
        val pack = createPackLocked(name, DEFAULT_AUTHOR, isSingles = true)
        starters.forEachIndexed { index, bytes ->
            addSticker(pack.id, bytes, fileName = "starter_$index.webp")?.let { }
        }
        readPack(File(root, pack.id)) ?: pack
    }

    fun renamePack(id: String, name: String): StickerPack? = synchronized(lock) {
        val pack = readPack(File(root, id)) ?: return null
        if (name.isBlank()) return pack
        pack.copy(name = name.trim(), contentVersion = pack.contentVersion + 1).also { writePack(it) }
    }

    fun renamePackAuthor(id: String, author: String): StickerPack? = synchronized(lock) {
        val pack = readPack(File(root, id)) ?: return null
        if (author.isBlank()) return pack
        pack.copy(author = author.trim(), contentVersion = pack.contentVersion + 1).also { writePack(it) }
    }

    fun deletePack(id: String): Boolean = synchronized(lock) {
        metadata.remove(id)
        File(root, id).deleteRecursively()
    }

    fun addSticker(packId: String, bytes: ByteArray, fileName: String = newStickerName(), source: ByteArray? = null, editData: String? = null): StickerPack? =
        synchronized(lock) {
            val pack = readPack(File(root, packId)) ?: return null
            if (pack.stickerCount >= MAX_WHATSAPP_STICKERS || pack.stickers.contains(fileName)) return null
            val file = File(root, packId).resolve(fileName)
            file.parentFile?.mkdirs()
            file.writeBytes(bytes)
            source?.let { File(file.parentFile, "$fileName.source.png").writeBytes(it) }
            editData?.let { File(file.parentFile, "$fileName.edit.json").writeText(it) }
            var updated = pack.copy(
                stickers = pack.stickers + fileName,
                starterStickers = if (fileName.startsWith("starter_") && pack.isSingles) pack.starterStickers + fileName else pack.starterStickers,
                contentVersion = pack.contentVersion + 1,
            )
            if (updated.isSingles && updated.userStickerCount >= 3 && updated.starterStickers.isNotEmpty()) {
                updated.starterStickers.forEach { starter -> File(root, packId).resolve(starter).delete() }
                updated = updated.copy(stickers = updated.stickers - updated.starterStickers.toSet(), starterStickers = emptyList(), trayImage = null, contentVersion = updated.contentVersion + 1)
            }
            writePack(updated)
            updated
        }

    /** Replaces an existing sticker in place, including in a full pack. */
    fun replaceSticker(packId: String, fileName: String, bytes: ByteArray, source: ByteArray? = null, editData: String? = null): StickerPack? =
        synchronized(lock) {
            val pack = readPack(File(root, packId)) ?: return null
            if (fileName !in pack.stickers) return null
            val dir = File(root, packId)
            val updated = pack.copy(trayImage = pack.trayImage.takeUnless { pack.stickers.firstOrNull() == fileName }, contentVersion = pack.contentVersion + 1)
            val staging = Files.createTempDirectory(dir.toPath(), "replace_").toFile()
            val changes = linkedMapOf(File(dir, fileName) to bytes)
            source?.let { changes[File(dir, "$fileName.source.png")] = it }
            editData?.let { changes[File(dir, "$fileName.edit.json")] = it.toByteArray(Charsets.UTF_8) }
            val backups = linkedMapOf<File, File?>()
            val committed = mutableListOf<File>()
            try {
                // Stage every byte and backup before touching any live editor asset.
                (changes.keys + File(dir, PACK_FILE)).forEachIndexed { index, target ->
                    check(!target.exists() || target.isFile) { "Sticker asset is not a file: ${target.name}" }
                    backups[target] = if (target.exists()) File(staging, "backup_$index").also {
                        Files.copy(target.toPath(), it.toPath())
                    } else null
                }
                val staged = changes.entries.mapIndexed { index, (target, content) ->
                    target to File(staging, "new_$index").apply { writeBytes(content) }
                }
                staged.forEach { (target, temp) ->
                    Files.move(temp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                    committed += target
                }
                // Include metadata in rollback even if its atomic move succeeds before a later failure.
                committed += File(dir, PACK_FILE)
                writePack(updated)
                updated
            } catch (failure: Exception) {
                committed.forEach { target ->
                    try {
                        val backup = backups[target]
                        if (backup == null) Files.deleteIfExists(target.toPath())
                        else Files.copy(backup.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    } catch (rollbackFailure: Exception) {
                        failure.addSuppressed(rollbackFailure)
                    }
                }
                metadata.remove(packId)
                throw failure
            } finally {
                staging.deleteRecursively()
            }
        }

    /** Private editor sources are never exposed by the WhatsApp content provider. */
    fun sourceFile(packId: String, fileName: String): File? = synchronized(lock) {
        val sticker = stickerFile(packId, fileName) ?: return null
        File(sticker.parentFile, "$fileName.source.png").takeIf { it.isFile }
    }

    fun editData(packId: String, fileName: String): String? = synchronized(lock) {
        val sticker = stickerFile(packId, fileName) ?: return null
        File(sticker.parentFile, "$fileName.edit.json").takeIf { it.isFile }?.readText()
    }

    fun deleteSticker(packId: String, fileName: String): StickerPack? = synchronized(lock) {
        val pack = readPack(File(root, packId)) ?: return null
        if (!pack.stickers.contains(fileName)) return pack
        File(root, packId).resolve(fileName).delete()
        File(root, packId).resolve("$fileName.source.png").delete()
        File(root, packId).resolve("$fileName.edit.json").delete()
        pack.copy(
            stickers = pack.stickers - fileName,
            // The generated tray represents the first sticker.
            trayImage = pack.trayImage?.takeUnless { it == fileName || pack.stickers.firstOrNull() == fileName },
            contentVersion = pack.contentVersion + 1,
        ).also { writePack(it) }
    }

    fun setPackThumbnail(packId: String, bytes: ByteArray): StickerPack? = synchronized(lock) {
        val pack = readPack(File(root, packId)) ?: return null
        File(root, packId).resolve(THUMBNAIL_FILE).writeBytes(bytes)
        pack.copy(thumbnailImage = THUMBNAIL_FILE, contentVersion = pack.contentVersion + 1).also { writePack(it) }
    }

    fun setTrayImage(packId: String, bytes: ByteArray, fileName: String = TRAY_FILE): StickerPack? =
        synchronized(lock) {
            val pack = readPack(File(root, packId)) ?: return null
            File(root, packId).resolve(fileName).writeBytes(bytes)
            pack.copy(trayImage = fileName, contentVersion = pack.contentVersion + 1).also { writePack(it) }
        }

    /** Writes a consistent pack-only snapshot. Call from an IO dispatcher. */
    fun exportBackup(destination: File): Unit = synchronized(lock) {
        val temp = File(destination.parentFile ?: root, ".${destination.name}.tmp")
        try {
            BackupArchive.write(root, temp)
            Files.move(temp.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            temp.delete()
        }
    }

    /**
     * Validates the complete archive before merging. Newer local packs win; equal
     * versions also keep local state. Returns the number of packs installed.
     */
    fun importBackup(source: File): Int = synchronized(lock) {
        root.mkdirs()
        BackupArchive.validate(source, root).use { validated ->
            val reservedDisplayIds = root.listFiles { f -> f.isDirectory }.orEmpty().mapNotNull { dir ->
                runCatching { JSONObject(File(dir, PACK_FILE).readText()).optString("displayId").takeIf { it.isNotBlank() } }.getOrNull()
            }.toMutableSet()
            val install = validated.packs.filter { incoming ->
                val local = readPack(File(root, incoming.id))
                local == null || incoming.contentVersion > local.contentVersion
            }
            if (install.isEmpty()) return@synchronized 0
            val staging = File(root, ".restore-${System.nanoTime()}").apply { mkdirs() }
            val rollback = File(staging, "rollback").apply { mkdirs() }
            val movedOld = mutableListOf<Pair<File, File>>()
            val movedNew = mutableListOf<File>()
            var rollbackComplete = true
            try {
                install.forEach { pack ->
                    val stagedDir = File(staging, "new-${pack.id}").apply { mkdirs() }
                    pack.files.forEach { (name, file) -> Files.copy(file.toPath(), File(stagedDir, name).toPath()) }
                    val metaFile = File(stagedDir, PACK_FILE)
                    val json = JSONObject(metaFile.readText())
                    var displayId = json.optString("displayId")
                    val previousDisplayId = readPack(File(root, pack.id))?.displayId
                    if (displayId.isBlank() || (displayId in reservedDisplayIds && displayId != previousDisplayId)) {
                        displayId = Random.nextInt(10_000_000, 100_000_000).toString()
                        while (displayId in reservedDisplayIds) displayId = Random.nextInt(10_000_000, 100_000_000).toString()
                        json.put("displayId", displayId)
                        metaFile.writeText(json.toString())
                    }
                    reservedDisplayIds += displayId
                }
                install.forEach { pack ->
                    val target = File(root, pack.id)
                    if (target.exists()) {
                        val backup = File(rollback, pack.id)
                        Files.move(target.toPath(), backup.toPath(), StandardCopyOption.ATOMIC_MOVE)
                        movedOld += target to backup
                    }
                    val stagedDir = File(staging, "new-${pack.id}")
                    Files.move(stagedDir.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
                    movedNew += target
                }
                metadata.clear()
                install.size
            } catch (failure: Exception) {
                movedNew.asReversed().forEach { runCatching { it.deleteRecursively() } }
                movedOld.asReversed().forEach { (target, backup) ->
                    if (backup.exists()) runCatching {
                        Files.move(backup.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
                    }.onFailure { rollbackComplete = false }
                }
                metadata.clear()
                throw failure
            } finally {
                // Keep recovery files if the filesystem also rejected rollback.
                if (rollbackComplete) staging.deleteRecursively()
            }
        }
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
                val needsDisplayIdMigration = json.optString("displayId", "").isEmpty()
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
                    isSingles = json.optBoolean("isSingles", false),
                    serialNumber = json.optInt("serialNumber", 0),
                    displayId = json.optString("displayId", "").ifEmpty { uniqueDisplayId() },
                    thumbnailImage = json.optString("thumbnailImage").takeIf { it.isNotEmpty() },
                    starterStickers = json.optJSONArray("starterStickers")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList(),
                    contentVersion = json.optLong("contentVersion", 1L),
                ).also {
                    metadata[dir.name] = CachedPack(version, it)
                    if (needsDisplayIdMigration) writePack(it)
                }
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
            .put("isSingles", pack.isSingles)
            .put("serialNumber", pack.serialNumber)
            .put("displayId", pack.displayId)
            .put("thumbnailImage", pack.thumbnailImage ?: "")
            .put("starterStickers", JSONArray(pack.starterStickers))
            .put("contentVersion", pack.contentVersion)
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

    private fun uniqueDisplayId(): String {
        val used = root.listFiles { file -> file.isDirectory }.orEmpty().mapNotNull { dir ->
            runCatching { JSONObject(File(dir, PACK_FILE).readText()).optString("displayId") }.getOrNull()
        }.toSet()
        var candidate: String
        do { candidate = Random.nextInt(10_000_000, 100_000_000).toString() } while (candidate in used)
        return candidate
    }

    companion object {
        const val PACK_FILE = "pack.json"
        const val TRAY_FILE = "tray.webp"
        const val THUMBNAIL_FILE = "thumbnail.png"
        const val DEFAULT_AUTHOR = "stikr"
        const val MIN_WHATSAPP_STICKERS = 3
        const val MAX_WHATSAPP_STICKERS = 30
    }
}
