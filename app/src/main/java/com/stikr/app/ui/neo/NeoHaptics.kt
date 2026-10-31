package com.stikr.app.ui.neo

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import com.stikr.app.audio.AppSounds
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/** How a control answers a tap under the finger. */
enum class NeoHaptic { None, Tap, Confirm, Reject }

/**
 * Haptics through the view, so they follow the system "touch feedback"
 * setting. Confirm/Reject use the API 30 constants and fall back to a key
 * click / long-press buzz below that.
 */
@Composable
fun rememberHaptics(): (NeoHaptic) -> Unit {
    val view = LocalView.current
    return remember(view) { { kind -> view.neoHaptic(kind) } }
}

fun View.neoHaptic(kind: NeoHaptic) {
    if (kind == NeoHaptic.Tap || kind == NeoHaptic.Confirm) {
        AppSounds.initialize(context)
        AppSounds.click()
    }
    val constant = when (kind) {
        NeoHaptic.None -> return
        NeoHaptic.Tap -> HapticFeedbackConstants.KEYBOARD_TAP
        NeoHaptic.Confirm ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
            else HapticFeedbackConstants.VIRTUAL_KEY
        NeoHaptic.Reject ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT
            else HapticFeedbackConstants.LONG_PRESS
    }
    performHapticFeedback(constant)
}
