package com.stikr.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.stikr.app.ui.neo.*

/** Optional backup presentation only. Account, progress and results always come from the host. */
@Composable
fun ProfilePanel(
    accountLabel: String?,
    lastBackupLabel: String?,
    busy: Boolean,
    status: String?,
    onConnect: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onDisconnect: () -> Unit,
    onDismiss: () -> Unit,
    automaticBackup: Boolean = false,
    onAutomaticBackup: (Boolean) -> Unit = {},
) {
    var confirmRestore by rememberSaveable { mutableStateOf(false) }
    val p = Neo.palette
    NeoDialog(
        onDismiss = onDismiss,
        title = "Account and backup",
        body = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Cloud backup is optional. Your stickers and packs work offline without an account.",
                    style = MaterialTheme.typography.bodyMedium, color = p.muted)
                if (accountLabel == null) {
                    Text("Keep a copy of your collections in your Google account.",
                        style = MaterialTheme.typography.bodyMedium, color = p.ink)
                    NeoButton("Connect Google", onConnect, Modifier.fillMaxWidth(), enabled = !busy)
                } else {
                    Text(accountLabel, style = MaterialTheme.typography.titleMedium, color = p.ink)
                    Text("Last backup: ${lastBackupLabel ?: "No backup yet"}",
                        style = MaterialTheme.typography.bodyMedium, color = p.muted)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = automaticBackup, onCheckedChange = onAutomaticBackup,
                            enabled = !busy, modifier = Modifier.semantics { contentDescription = "Automatic backup" })
                        Text("Automatic backup", style = MaterialTheme.typography.bodyMedium, color = p.ink)
                    }
                    NeoButton("Back up now", onBackup, Modifier.fillMaxWidth(), enabled = !busy)
                    NeoOutlinedButton("Restore backup", { confirmRestore = true }, Modifier.fillMaxWidth(), enabled = !busy)
                    NeoOutlinedButton("Disconnect", onDisconnect, Modifier.fillMaxWidth(), enabled = !busy)
                }
                if (busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Working…", style = MaterialTheme.typography.bodySmall, color = p.muted)
                }
                if (!status.isNullOrBlank()) Text(status, color = p.ink,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
        },
        actions = { NeoOutlinedButton("Close", onDismiss, Modifier.fillMaxWidth()) },
    )
    if (confirmRestore) ConfirmDialog(
        kicker = "",
        title = "Restore your collections?",
        body = "Missing packs will be restored. For matching packs, the newer version is kept; local data wins ties.",
        confirmLabel = "Restore",
        dismissLabel = "Cancel",
        onDismiss = { confirmRestore = false },
        onConfirm = { confirmRestore = false; onRestore() },
    )
}
