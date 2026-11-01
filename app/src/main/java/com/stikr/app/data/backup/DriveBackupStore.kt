package com.stikr.app.data.backup

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

/** Google Drive v3 appDataFolder transport. The caller supplies an OAuth token with drive.appdata. */
class DriveBackupStore {
    suspend fun upload(accessToken: String, archive: File): Long {
        require(accessToken.isNotBlank()) { "Drive access token is required" }
        require(archive.isFile && archive.length() <= BackupArchive.MAX_ARCHIVE_BYTES) { "Backup archive is missing or too large" }
        val existing = findBackup(accessToken)
        val metadata = JSONObject().put("name", FILE_NAME).apply {
            if (existing == null) put("parents", JSONArray().put("appDataFolder"))
        }
        val boundary = "stikr-${UUID.randomUUID()}"
        val url = if (existing == null) "$UPLOAD_ENDPOINT?uploadType=multipart" else "$UPLOAD_ENDPOINT/${encode(existing)}?uploadType=multipart"
        val connection = open(url, accessToken, if (existing == null) "POST" else "PATCH").apply {
            doOutput = true
            setChunkedStreamingMode(BUFFER_SIZE)
            setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
        }
        try {
            BufferedOutputStream(connection.outputStream).use { out ->
                out.write("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n".toByteArray())
                out.write(metadata.toString().toByteArray(Charsets.UTF_8))
                out.write("\r\n--$boundary\r\nContent-Type: application/zip\r\n\r\n".toByteArray())
                BufferedInputStream(FileInputStream(archive)).use { input -> input.copyTo(out, BUFFER_SIZE) }
                out.write("\r\n--$boundary--\r\n".toByteArray())
            }
            checked(connection)
            return System.currentTimeMillis()
        } finally {
            connection.disconnect()
        }
    }

    /** Returns false when this Drive account has no saved backup. */
    suspend fun download(accessToken: String, destination: File): Boolean {
        require(accessToken.isNotBlank()) { "Drive access token is required" }
        val id = findBackup(accessToken) ?: return false
        val parent = destination.parentFile ?: File(".")
        parent.mkdirs()
        val temp = File.createTempFile(".stikr-drive-", ".download", parent)
        val connection = open("$API_ENDPOINT/${encode(id)}?alt=media", accessToken, "GET")
        try {
            checked(connection)
            var total = 0L
            BufferedInputStream(connection.inputStream).use { input ->
                BufferedOutputStream(FileOutputStream(temp)).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= BackupArchive.MAX_ARCHIVE_BYTES) { "Drive backup exceeds the size limit" }
                        output.write(buffer, 0, count)
                    }
                }
            }
            require(total > 0L) { "Drive returned an empty backup" }
            Files.move(temp.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            return true
        } finally {
            connection.disconnect()
            temp.delete()
        }
    }

    private fun findBackup(token: String): String? {
        val query = "name='$FILE_NAME' and trashed=false"
        val url = "$API_ENDPOINT?spaces=appDataFolder&pageSize=10&fields=files(id,name)&q=${encode(query)}"
        val connection = open(url, token, "GET")
        try {
            checked(connection)
            val bytes = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= MAX_RESPONSE_BYTES) { "Drive response is too large" }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            return JSONObject(String(bytes, Charsets.UTF_8)).optJSONArray("files")
                ?.optJSONObject(0)?.optString("id")?.takeIf { it.isNotBlank() }
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String, token: String, method: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
        }

    private fun checked(connection: HttpURLConnection) {
        val status = connection.responseCode
        if (status !in 200..299) {
            val detail = runCatching {
                connection.errorStream?.use { input ->
                    val buffer = ByteArray(2048)
                    val count = input.read(buffer).coerceAtLeast(0)
                    String(buffer, 0, count, Charsets.UTF_8).take(1000)
                }
            }.getOrNull().orEmpty()
            throw java.io.IOException("Google Drive request failed (HTTP $status)${if (detail.isBlank()) "" else ": $detail"}")
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    companion object {
        const val FILE_NAME = "stikr-backup.zip"
        const val SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        private const val API_ENDPOINT = "https://www.googleapis.com/drive/v3/files"
        private const val UPLOAD_ENDPOINT = "https://www.googleapis.com/upload/drive/v3/files"
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 60_000
        private const val MAX_RESPONSE_BYTES = 64 * 1024
        private const val BUFFER_SIZE = 32 * 1024
    }
}
