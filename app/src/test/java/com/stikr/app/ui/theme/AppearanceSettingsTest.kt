package com.stikr.app.ui.theme

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppearanceSettingsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before fun reset() {
        context.getSharedPreferences("appearance", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun selectionSurvivesRecreation() {
        AppearanceMode.entries.forEach { mode ->
            AppearanceSettings(context).select(mode)
            assertEquals(mode, AppearanceSettings(context).mode.value)
        }
    }

    @Test fun unknownPreferenceUsesSystemTheme() {
        context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
            .edit().putString("mode", "removed-mode").commit()
        assertEquals(AppearanceMode.SYSTEM, AppearanceSettings(context).mode.value)
    }
}
