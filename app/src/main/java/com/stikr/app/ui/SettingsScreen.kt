package com.stikr.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.stikr.app.ui.neo.Neo
import com.stikr.app.ui.neo.NeoTextButton
import com.stikr.app.ui.theme.AppearanceMode

@Composable
fun SettingsScreen(mode: AppearanceMode, onSelect: (AppearanceMode) -> Unit, onBack: () -> Unit) {
    val palette = Neo.palette
    BackHandler(onBack = onBack)
    Column(
        Modifier.fillMaxSize().background(palette.paper).systemBarsPadding()
            .verticalScroll(rememberScrollState()).padding(24.dp),
    ) {
        NeoTextButton(text = "Back", onClick = onBack)
        Spacer(Modifier.height(24.dp))
        Text("Settings", style = MaterialTheme.typography.headlineLarge, color = palette.ink)
        Spacer(Modifier.height(32.dp))
        Text("Appearance", style = MaterialTheme.typography.titleMedium, color = palette.ink)
        Text("Choose how stikr looks on this device.", style = MaterialTheme.typography.bodyMedium, color = palette.muted)
        Spacer(Modifier.height(16.dp))
        Column(Modifier.selectableGroup()) {
            AppearanceMode.entries.forEach { option ->
                Row(
                    Modifier.fillMaxWidth().selectable(
                        selected = mode == option, role = Role.RadioButton,
                        onClick = { onSelect(option) },
                    ).padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = mode == option, onClick = null)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(option.label, style = MaterialTheme.typography.titleMedium, color = palette.ink)
                        Text(option.description, style = MaterialTheme.typography.bodyMedium, color = palette.muted)
                    }
                }
            }
        }
    }
}
