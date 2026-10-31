package com.stikr.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stikr.app.ui.theme.AppearanceMode
import com.stikr.app.ui.theme.AppearanceSettings
import com.stikr.app.ui.StikrApp
import com.stikr.app.ui.theme.StikrTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.stikr.app.audio.AppSounds.initialize(applicationContext)
        com.stikr.app.render.StickerTypography.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            val settings = remember { AppearanceSettings(applicationContext) }
            val mode by settings.mode.collectAsStateWithLifecycle()
            val dark = mode == AppearanceMode.DARK || mode == AppearanceMode.AMOLED ||
                (mode == AppearanceMode.SYSTEM && isSystemInDarkTheme())
            SideEffect {
                val bars = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            }
            StikrTheme(darkTheme = dark, amoled = mode == AppearanceMode.AMOLED) {
                StikrApp(appearanceMode = mode, onAppearanceChange = settings::select)
            }
        }
    }
}
