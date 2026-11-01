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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Google Drive v3 appDataFolder transport. The caller supplies an OAuth token with drive.appdata. */
class DriveBackupStore {
    suspend fun upload(accessToken: String, archive: File): Long {
        require(accessToken.isNotBlank()) { "Drive access token is required" }
        require(archive.isFile && archive.length() <= BackupArchive.MAX_ARCHIVE_BYTES) { "Backup archive is missing or too large" }
        val existing = findBackup(accessToken)
        val metadata = JSONObject().put("name", FILE_NAME).apply {
            if (existing == null) put("parents", JSONArray().put("appDataFolder"))
        }
        // Resumable sessions support libraries larger than Drive's small multipart uploads.
        val endpoint = if (existing == null) UPLOAD_ENDPOINT else "$UPLOAD_ENDPOINT/${encode(existing)}"
        val initialization = open("$endpoint?uploadType=resumable", accessToken, if (existing == null) "POST" else "PATCH")
        val session = try {
            val bytes = metadata.toString().toByteArray(Charsets.UTF_8)
            initialization.doOutput = true
            initialization.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            initialization.setRequestProperty("X-Upload-Content-Type", "application/zip")
            initialization.setRequestProperty("X-Upload-Content-Length", archive.length().toString())
            initialization.setFixedLengthStreamingMode(bytes.size)
            initialization.outputStream.use { it.write(bytes) }
            checked(initialization)
            requireNotNull(initialization.getHeaderField("Location")) { "Drive did not return an upload session" }
        } finally { initialization.disconnect() }
        val sessionUrl = URL(session)
        require(sessionUrl.protocol == "https" && sessionUrl.host == "www.googleapis.com") { "Invalid Drive upload session" }
        currentCoroutineContext().ensureActive()
        val connection = open(session, accessToken, "PUT").apply {
            doOutput = true
            setFixedLengthStreamingMode(archive.length())
            setRequestProperty("Content-Type", "application/zip")
        }
        try {
            val coroutine = currentCoroutineContext()
            BufferedOutputStream(connection.outputStream).use { output ->
                BufferedInputStream(FileInputStream(archive)).use { input ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        coroutine.ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                    }
                }
            }
            checked(connection)
            return System.currentTimeMillis()
        } finally { connection.disconnect() }
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
            instanceFollowRedirects = false
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
