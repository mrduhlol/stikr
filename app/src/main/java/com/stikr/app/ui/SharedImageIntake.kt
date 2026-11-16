package com.stikr.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Build

/**
 * Distinguishes a photo shared into Stikr from a shared sticker-pack backup.
 * Pack shares carry a zip mime type and are imported; image shares go straight
 * to the sticker editor.
 */
object SharedImageIntake {
    /** Pack backups arrive as zips; photos must never be handed to the importer. */
    fun isPackShare(intent: Intent?): Boolean {
        val action = intent?.action ?: return false
        if (action == Intent.ACTION_VIEW) return true
        if (action != Intent.ACTION_SEND) return false
        val type = intent.type ?: return false
        return !type.startsWith("image/")
    }

    fun imageUri(intent: Intent?): Uri? {
        if (intent?.action != Intent.ACTION_SEND) return null
        val type = intent.type ?: return null
        if (!type.startsWith("image/")) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
    }
}
