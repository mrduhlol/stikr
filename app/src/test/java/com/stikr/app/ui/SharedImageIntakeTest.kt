package com.stikr.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.stikr.app.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SharedImageIntakeTest {

    private val photo = Uri.parse("content://media/external/images/media/42")

    @Test
    fun sharedImageIsRecognisedAsStickerInput() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, photo)
        }
        assertEquals(photo, SharedImageIntake.imageUri(intent))
    }

    @Test
    fun packShareIsNotMistakenForAnImage() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, Uri.parse("content://packs/1"))
        }
        assertNull(SharedImageIntake.imageUri(intent))
    }

    @Test
    fun imageShareWithoutAStreamIsIgnored() {
        val intent = Intent(Intent.ACTION_SEND).apply { type = "image/png" }
        assertNull(SharedImageIntake.imageUri(intent))
    }

    @Test
    fun appIsOfferedAsAShareTargetForImages() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, photo)
        }
        val targets = context.packageManager.queryIntentActivities(intent, 0)
        assertTrue(targets.any { it.activityInfo.name == MainActivity::class.java.name })
    }
}
