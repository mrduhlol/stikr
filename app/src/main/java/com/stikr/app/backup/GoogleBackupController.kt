package com.stikr.app.backup

import android.accounts.Account
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.stikr.app.data.PackRepository
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/** Call public methods on the main thread. Own this alongside the app ViewModel and close it. */
class GoogleBackupController(context: Context, private val repository: PackRepository) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("google_backup", Context.MODE_PRIVATE)
    private val client = Identity.getAuthorizationClient(app)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val scopes = listOf(Scope(DRIVE_SCOPE), Scope("openid"), Scope("email"))
    private val mutable = MutableStateFlow(GoogleBackupState(
        accountLabel = prefs.getString("label", null),
        lastBackupAt = prefs.getLong("last", 0).takeIf { it > 0 },
        // Deliberately require opt-in each process; first reconnect must check remote data.
    ))
    val state: StateFlow<GoogleBackupState> = mutable.asStateFlow()
    private var operation: Job? = null
    private var debounce: Job? = null
    private var pending: (suspend (String) -> Unit)? = null
    private var token: String? = null
    private var closed = false
    private var disconnecting = false
    private val changes = BackupChangeGate()

    fun connect(onResolution: (PendingIntent) -> Unit) = start(onResolution) { access ->
        val email = withContext(Dispatchers.IO) { userEmail(access) }
        // A newly selected account must not inherit the old account's backup timestamp.
        val last = state.value.lastBackupAt.takeIf { state.value.accountLabel == email }
        prefs.edit().putString("label", email).putLong("last", last ?: 0L).apply()
        mutable.value = state.value.copy(accountLabel = email, lastBackupAt = last,
            automaticBackup = false, restoreAvailable = null)
        val exists = temporary { DriveBackupStore().download(access, it) }
        mutable.value = state.value.copy(restoreAvailable = exists,
            status = if (exists) "Backup found. Restore it or explicitly back up this library." else "Connected. No backup found.")
    }

    fun finishAuthorization(data: Intent?) {
        val continuation = pending ?: return
        pending = null
        if (data == null) {
            mutable.value = state.value.copy(busy = false, status = "Google authorization canceled. Offline library is available.")
            return
        }
        launchOperation {
            val result = client.getAuthorizationResultFromIntent(data)
            continuation(checkedToken(result))
        }
    }

    fun backupNow(onResolution: (PendingIntent) -> Unit) {
        if (state.value.accountLabel == null) {
            mutable.value = state.value.copy(status = "Connect Google before backing up.")
            return
        }
        start(onResolution) { upload(it) }
    }

    fun restore(onResolution: (PendingIntent) -> Unit, onRestored: () -> Unit) {
        if (state.value.accountLabel == null) return
        start(onResolution) { access ->
            val restored = temporary { file ->
                if (!DriveBackupStore().download(access, file)) false else {
                    withContext(Dispatchers.IO) { repository.importBackup(file) }
                    true
                }
            }
            mutable.value = state.value.copy(restoreAvailable = false,
                status = if (restored) "Backup restored and merged with this library." else "No backup found.")
            if (restored) onRestored()
        }
    }

    fun setAutomaticBackup(enabled: Boolean) {
        if (closed || disconnecting) return
        val allowed = enabled && state.value.accountLabel != null && state.value.restoreAvailable == false
        mutable.value = state.value.copy(automaticBackup = allowed,
            status = if (enabled && !allowed) "Connect and restore the existing backup, or explicitly back up this library first." else state.value.status)
        if (!allowed) debounce?.cancel()
    }

    fun libraryChanged(fingerprint: String) {
        if (!changes.changed(fingerprint) || !state.value.automaticBackup) return
        debounce?.cancel()
        debounce = scope.launch {
            delay(1500)
            if (state.value.automaticBackup && state.value.accountLabel != null && !state.value.busy) {
                start(null) { upload(it) }
            }
        }
    }

    fun disconnect() {
        if (closed || disconnecting) return
        disconnecting = true
        debounce?.cancel()
        pending = null
        val old = operation
        old?.cancel()
        val label = state.value.accountLabel
        mutable.value = state.value.copy(busy = true, automaticBackup = false, status = "Disconnecting…")
        operation = scope.launch {
            // Wait for blocking IO to return before reporting disconnected.
            old?.join()
            token = null
            prefs.edit().clear().apply()
            var revoked = false
            try {
                if (label != null) client.revokeAccess(RevokeAccessRequest.builder()
                    .setAccount(Account(label, "com.google")).setScopes(scopes).build()).awaitResult()
                revoked = true
            } catch (_: Exception) {
                // Local disconnect still succeeds; do not imply server revocation succeeded.
            } finally {
                mutable.value = GoogleBackupState(status = if (revoked) "Disconnected. Your library stays on this device."
                    else "Disconnected locally. Google access could not be revoked; remove access in your Google Account settings.")
                disconnecting = false
            }
        }
    }

    fun close() {
        closed = true
        pending = null
        token = null
        scope.cancel()
    }

    private fun start(resolution: ((PendingIntent) -> Unit)?, action: suspend (String) -> Unit) {
        if (closed || disconnecting || state.value.busy) return
        launchOperation {
            val request = AuthorizationRequest.builder().setRequestedScopes(scopes).apply {
                state.value.accountLabel?.let { setAccount(Account(it, "com.google")) }
            }.build()
            val result = client.authorize(request).awaitResult()
            if (result.hasResolution()) {
                if (resolution == null) {
                    mutable.value = state.value.copy(automaticBackup = false, status = "Reconnect needed. Open Google backup to continue.")
                } else {
                    pending = action
                    resolution(requireNotNull(result.pendingIntent))
                }
            } else action(checkedToken(result))
        }
    }

    private fun checkedToken(result: AuthorizationResult): String {
        require(result.grantedScopes.contains(DRIVE_SCOPE)) { "Required Google permissions were not granted." }
        return requireNotNull(result.accessToken) { "Google did not return an access token." }.also { token = it }
    }

    private fun launchOperation(block: suspend () -> Unit) {
        operation = scope.launch {
            mutable.value = state.value.copy(busy = true, status = null)
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                // Do not log exceptions or token-bearing server responses.
                if (isUnauthorized(e)) {
                    token?.let { invalid ->
                        try { client.clearToken(ClearTokenRequest.builder().setToken(invalid).build()).awaitResult() }
                        catch (_: Exception) { }
                    }
                    token = null
                    mutable.value = state.value.copy(automaticBackup = false, status = "Reconnect needed. Retry explicitly to authorize again.")
                } else {
                    val message = when ((e as? ApiException)?.statusCode) {
                        10 -> "Google setup is incomplete. Check the Android OAuth client and signing SHA-1."
                        16, 12501 -> "Google authorization canceled. Offline library is available."
                        else -> "Google backup could not complete. Check connectivity, Google Play services and project setup; your offline library is available."
                    }
                    mutable.value = state.value.copy(status = message)
                }
            } finally {
                if (!disconnecting) mutable.value = state.value.copy(busy = pending != null)
            }
        }
    }

    private suspend fun upload(access: String) {
        val timestamp = temporary { file ->
            withContext(Dispatchers.IO) { repository.exportBackup(file) }
            currentCoroutineContext().ensureActive()
            DriveBackupStore().upload(access, file)
        }
        currentCoroutineContext().ensureActive()
        prefs.edit().putLong("last", timestamp).apply()
        mutable.value = state.value.copy(lastBackupAt = timestamp, restoreAvailable = false, status = "Backup saved to Google Drive.")
    }

    private suspend fun <T> temporary(block: suspend (File) -> T): T {
        val file = File.createTempFile("google-backup-", ".zip", app.cacheDir)
        return try { block(file) } finally { file.delete() }
    }

    private fun userEmail(access: String): String {
        val connection = URL("https://www.googleapis.com/oauth2/v3/userinfo").openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Authorization", "Bearer $access")
            if (connection.responseCode != 200) throw IOException("HTTP ${connection.responseCode}")
            val json = connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
            require(json.optBoolean("email_verified") && json.optString("email").isNotBlank())
            json.getString("email")
        } finally { connection.disconnect() }
    }

    companion object {
        private const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        // Store API has no typed HTTP error contract; recognize only status markers, never display raw messages.
        internal fun isUnauthorized(error: Throwable): Boolean = generateSequence(error) { it.cause }
            .any { Regex("(?i)(?:HTTP[ :]+|status[ :]+)401\\b").containsMatchIn(it.message.orEmpty()) }
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
    addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}
