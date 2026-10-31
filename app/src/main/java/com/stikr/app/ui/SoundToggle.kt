package com.stikr.app.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.stikr.app.R
import com.stikr.app.audio.AppSounds
import com.stikr.app.ui.neo.Neo

@Composable
fun SoundToggle(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    remember(context.applicationContext) { AppSounds.initialize(context) }
    val enabled by AppSounds.enabled.collectAsState()
    IconButton(
        onClick = { AppSounds.toggle() },
        modifier = modifier.size(48.dp).semantics {
            stateDescription = if (enabled) "Sounds on" else "Sounds off"
        },
    ) {
        Icon(
            painter = painterResource(if (enabled) R.drawable.ic_sound_on else R.drawable.ic_sound_off),
            tint = Neo.palette.ink,
            contentDescription = if (enabled) "Mute sounds" else "Unmute sounds",
            modifier = Modifier.size(22.dp),
        )
    }
}
