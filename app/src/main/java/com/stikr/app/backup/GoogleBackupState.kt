package com.stikr.app.backup

/** Account label is a verified Google email; null means offline mode. */
data class GoogleBackupState(
    val accountLabel: String? = null,
    val lastBackupAt: Long? = null,
    val busy: Boolean = false,
    val status: String? = null,
    val automaticBackup: Boolean = false,
    // Null until the remote check succeeds. Never enable automatic upload while unknown.
    val restoreAvailable: Boolean? = null,
)

/** A first library emission is a baseline, never an instruction to upload. */
internal class BackupChangeGate {
    private var previous: String? = null
    fun changed(fingerprint: String): Boolean {
        val changed = previous != null && previous != fingerprint
        previous = fingerprint
        return changed
    }
}
