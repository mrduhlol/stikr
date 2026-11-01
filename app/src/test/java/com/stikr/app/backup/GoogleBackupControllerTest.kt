package com.stikr.app.backup

import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class GoogleBackupControllerTest {
    @Test fun initialAndRepeatedLibraryEmissionsNeverTriggerBackup() {
        val gate = BackupChangeGate()
        assertFalse(gate.changed("fresh-install"))
        assertFalse(gate.changed("fresh-install"))
        assertTrue(gate.changed("added-sticker"))
        assertFalse(gate.changed("added-sticker"))
        assertTrue(gate.changed("deleted-sticker"))
    }

    @Test fun unknownRemoteBackupAndAutomaticUploadsAreSafeByDefault() {
        val state = GoogleBackupState()
        assertNull(state.accountLabel)
        assertNull(state.restoreAvailable)
        assertFalse(state.automaticBackup)
        assertFalse(state.busy)
    }

    @Test fun unauthorizedRequiresAnHttpStatusMarkerIncludingWrappedErrors() {
        assertTrue(GoogleBackupController.isUnauthorized(IOException("HTTP 401")))
        assertTrue(GoogleBackupController.isUnauthorized(IOException("request failed", IOException("status: 401"))))
        assertFalse(GoogleBackupController.isUnauthorized(IOException("HTTP 403")))
        assertFalse(GoogleBackupController.isUnauthorized(IOException("file 40123 missing")))
    }
}
