package com.stikr.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stikr.app.ui.neo.*

@Composable
fun LocalBackupPanel(busy: Boolean, onExport: () -> Unit, onRestore: () -> Unit, onDismiss: () -> Unit) {
    val p = Neo.palette
    NeoDialog(onDismiss = onDismiss, title = "Local backup", body = {
        Text("Save your packs, stickers, original photos, edits and thumbnails in one backup file.", color = p.muted)
        Spacer(Modifier.height(12.dp))
        Text("Choose Downloads or another folder on your phone. Keep the file outside Stikr so it survives uninstalling. Backups are manual; export a new file after making changes.", color = p.muted)
        Spacer(Modifier.height(20.dp))
        NeoButton("Save backup file", onExport, Modifier.fillMaxWidth(), enabled = !busy)
        Spacer(Modifier.height(12.dp))
        NeoOutlinedButton("Restore from file", onRestore, Modifier.fillMaxWidth(), enabled = !busy)
        if (busy) {
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Working…", color = p.muted)
        }
    }, actions = { NeoOutlinedButton("Close", onDismiss, Modifier.fillMaxWidth()) })
}
