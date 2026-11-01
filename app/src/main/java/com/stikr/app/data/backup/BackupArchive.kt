package com.stikr.app.data.backup

import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Pure file helpers and validation for the private pack-only backup format. */
internal object BackupArchive {
    const val MAX_ARCHIVE_BYTES = 512L * 1024 * 1024
    const val MAX_EXPANDED_BYTES = 1024L * 1024 * 1024
    const val MAX_ENTRY_BYTES = 64L * 1024 * 1024
    const val MAX_ENTRIES = 20_000
    private const val BUFFER_SIZE = 32 * 1024

    data class Pack(val id: String, val contentVersion: Long, val files: Map<String, File>)
    data class Validated(val packs: List<Pack>, val temp: File) : AutoCloseable {
        override fun close() { temp.deleteRecursively() }
    }

    fun write(packsDir: File, destination: File) {
        destination.parentFile?.mkdirs()
        ZipOutputStream(BufferedOutputStream(FileOutputStream(destination))).use { zip ->
            packsDir.listFiles { f -> f.isDirectory && f.name.matches(Regex("pack_[A-Za-z0-9_-]+")) }
                .orEmpty().sortedBy { it.name }.forEach { dir ->
                    val meta = File(dir, "pack.json")
                    if (!meta.isFile || meta.length() > 1024 * 1024) return@forEach
                    val json = JSONObject(meta.readText(Charsets.UTF_8))
                    val names = linkedSetOf("pack.json")
                    json.optJSONArray("stickers")?.let { a ->
                        for (i in 0 until a.length()) {
                            val sticker = a.getString(i)
                            names += sticker
                            names += "$sticker.source.png"
                            names += "$sticker.edit.json"
                        }
                    }
                    listOf("trayImage", "thumbnailImage").forEach { field -> json.optString(field).takeIf { it.isNotEmpty() }?.let(names::add) }
                    val files = names.mapNotNull { name -> File(dir, name).takeIf { it.isFile } }.sortedBy { it.name }
                    files.forEach { file ->
                        zip.putNextEntry(ZipEntry("packs/${dir.name}/${file.name}"))
                        FileInputStream(file).use { input -> input.copyTo(zip, BUFFER_SIZE) }
                        zip.closeEntry()
                    }
                }
        }
    }

    /** Expands to private staging, then validates every path, JSON record and referenced asset. */
    fun validate(source: File, tempParent: File): Validated {
        require(source.isFile && source.length() <= MAX_ARCHIVE_BYTES) { "Backup archive is missing or too large" }
        rejectSymlinks(source)
        val temp = File.createTempFile("backup-restore-", ".tmp", tempParent).also { it.delete(); it.mkdir() }
        try {
            val seen = mutableSetOf<String>()
            var total = 0L
            var count = 0
            ZipInputStream(BufferedInputStream(FileInputStream(source))).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    require(++count <= MAX_ENTRIES) { "Backup has too many entries" }
                    require(!entry.isDirectory && safeEntry(entry.name)) { "Invalid backup path" }
                    require(seen.add(entry.name)) { "Duplicate backup entry" }
                    val path = entry.name.removePrefix("packs/")
                    val slash = path.indexOf('/')
                    val packId = path.substring(0, slash)
                    val filename = path.substring(slash + 1)
                    require(packId.matches(Regex("pack_[A-Za-z0-9_-]+")) && isKnownFile(filename)) { "Unknown backup entry" }
                    val out = File(File(temp, packId), filename)
                    out.parentFile.mkdirs()
                    var entryBytes = 0L
                    FileOutputStream(out).use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        while (true) {
                            val read = zip.read(buffer)
                            if (read < 0) break
                            entryBytes += read
                            total += read
                            require(entryBytes <= MAX_ENTRY_BYTES && total <= MAX_EXPANDED_BYTES) { "Backup expands beyond the size limit" }
                            output.write(buffer, 0, read)
                        }
                    }
                    zip.closeEntry()
                }
            }
            val packs = temp.listFiles().orEmpty().sortedBy { it.name }.map { dir -> validatePack(dir) }
            require(packs.isNotEmpty()) { "Backup contains no packs" }
            return Validated(packs, temp)
        } catch (failure: Exception) {
            temp.deleteRecursively()
            throw failure
        }
    }

    private fun validatePack(dir: File): Pack {
        val meta = File(dir, "pack.json")
        require(meta.isFile && meta.length() <= 1024 * 1024) { "Pack metadata missing or too large" }
        val json = JSONObject(meta.readText(Charsets.UTF_8))
        val id = json.getString("id")
        require(id == dir.name && id.matches(Regex("pack_[A-Za-z0-9_-]+"))) { "Pack identity mismatch" }
        val stickers = json.optJSONArray("stickers") ?: org.json.JSONArray()
        require(stickers.length() <= 30) { "Pack has too many stickers" }
        val refs = mutableSetOf("pack.json")
        val stickerNames = mutableSetOf<String>()
        for (i in 0 until stickers.length()) {
            val name = stickers.getString(i)
            require(validAssetName(name) && name.endsWith(".webp") && stickerNames.add(name)) { "Invalid sticker list" }
            require(File(dir, name).isFile) { "Sticker asset is missing" }
            refs += name
            listOf("$name.source.png", "$name.edit.json").forEach { sidecar ->
                if (File(dir, sidecar).exists()) refs += sidecar
            }
            File(dir, "$name.edit.json").takeIf { it.isFile }?.let {
                require(it.length() <= 1024 * 1024)
                JSONObject(it.readText(Charsets.UTF_8))
            }
        }
        for (field in listOf("trayImage", "thumbnailImage")) {
            val name = json.optString(field).takeIf { it.isNotEmpty() } ?: continue
            require(validAssetName(name)) { "Invalid pack image reference" }
            require(File(dir, name).isFile) { "Pack image is missing" }
            refs += name
        }
        json.optJSONArray("starterStickers")?.let { a ->
            for (i in 0 until a.length()) require(a.getString(i) in stickerNames) { "Invalid starter sticker reference" }
        }
        val actual = dir.listFiles().orEmpty()
        require(actual.all { it.isFile && it.name in refs }) { "Archive contains an unknown pack asset" }
        return Pack(id, json.optLong("contentVersion", 1L), actual.associate { it.name to it })
    }

    private fun safeEntry(name: String): Boolean =
        !name.startsWith('/') && !name.contains('\\') && !name.split('/').any { it.isEmpty() || it == "." || it == ".." } &&
            name.startsWith("packs/") && name.count { it == '/' } == 2

    /** Java's ZipEntry omits Unix mode bits, so inspect central-directory attributes directly. */
    private fun rejectSymlinks(source: File) {
        RandomAccessFile(source, "r").use { file ->
            val tailSize = minOf(file.length(), 65_557L).toInt()
            val tail = ByteArray(tailSize)
            file.seek(file.length() - tailSize)
            file.readFully(tail)
            var eocd = -1
            for (i in tail.size - 22 downTo 0) {
                if (u32(tail, i) == 0x06054b50L) { eocd = i; break }
            }
            require(eocd >= 0) { "Invalid ZIP directory" }
            val entryCount = u16(tail, eocd + 10)
            val directoryOffset = u32(tail, eocd + 16)
            require(entryCount <= MAX_ENTRIES && directoryOffset < file.length()) { "Invalid ZIP directory" }
            file.seek(directoryOffset)
            repeat(entryCount) {
                val header = ByteArray(46)
                file.readFully(header)
                require(u32(header, 0) == 0x02014b50L) { "Invalid ZIP directory entry" }
                val madeBy = u16(header, 4) ushr 8
                val external = u32(header, 38)
                val mode = (external ushr 16).toInt()
                require(madeBy != 3 || mode and 0xF000 != 0xA000) { "ZIP symlinks are not allowed" }
                val nameLength = u16(header, 28)
                val extraLength = u16(header, 30)
                val commentLength = u16(header, 32)
                file.seek(file.filePointer + nameLength + extraLength + commentLength)
            }
        }
    }

    private fun u16(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xff) or ((bytes[offset + 1].toInt() and 0xff) shl 8)

    private fun u32(bytes: ByteArray, offset: Int): Long =
        u16(bytes, offset).toLong() or (u16(bytes, offset + 2).toLong() shl 16)

    private fun validAssetName(name: String): Boolean = name.isNotEmpty() && name.length <= 180 &&
        name.matches(Regex("[A-Za-z0-9_.-]+")) && name != "." && name != ".."

    private fun isKnownFile(name: String): Boolean = name == "pack.json" || name == "thumbnail.png" ||
        name == "tray.png" || name == "tray.webp" ||
        name.matches(Regex("[A-Za-z0-9_.-]+\\.webp")) ||
        name.matches(Regex("[A-Za-z0-9_.-]+\\.webp\\.source\\.png")) ||
        name.matches(Regex("[A-Za-z0-9_.-]+\\.webp\\.edit\\.json"))
}
