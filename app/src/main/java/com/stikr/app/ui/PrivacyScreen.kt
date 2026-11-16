package com.stikr.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stikr.app.ui.neo.Neo
import com.stikr.app.ui.neo.NeoTextButton

/** A short, honest account of what stikr does with photos, in the app itself. */
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val palette = Neo.palette
    BackHandler(onBack = onBack)
    Column(
        Modifier.fillMaxSize().collectionBackground().systemBarsPadding()
            .verticalScroll(rememberScrollState()).padding(24.dp),
    ) {
        NeoTextButton(text = "Back", onClick = onBack)
        Spacer(Modifier.height(24.dp))
        Text(
            "How it works & privacy",
            style = MaterialTheme.typography.headlineLarge,
            color = palette.ink,
        )
        Spacer(Modifier.height(32.dp))
        Section("Nothing leaves your phone", "Your photos are turned into stickers on this device. stikr has no server, uploads your pictures nowhere, and asks for no account.")
        Section("About cut-out", "Background removal uses ML Kit on your device. The model is fetched by Google Play services the first time you use it; if it is unavailable, stikr uses the whole photo instead. Everything else works offline.")
        Section("Your packs stay yours", "Packs live as files in stikr's private storage. Backups are manual ZIP files you save and restore yourself — stikr never syncs them anywhere.")
        Section("No tracking", "There is no analytics, no telemetry and no advertising in stikr.")
    }
}

@Composable
private fun Section(title: String, body: String) {
    val palette = Neo.palette
    Text(title, style = MaterialTheme.typography.titleMedium, color = palette.ink)
    Spacer(Modifier.height(8.dp))
    Text(body, style = MaterialTheme.typography.bodyMedium, color = palette.muted)
    Spacer(Modifier.height(24.dp))
}
