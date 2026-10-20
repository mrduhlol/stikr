package com.stikr.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TourPrefsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test fun defaultsToNotDone() { assertFalse(TourPrefs(context).done) }
    @Test fun donePersists() {
        TourPrefs(context).done = true
        assertTrue(TourPrefs(context).done)
    }
}
