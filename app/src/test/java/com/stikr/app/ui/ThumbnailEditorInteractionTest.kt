package com.stikr.app.ui

import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.semantics.SemanticsActions
import com.stikr.app.ui.theme.StikrTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h880dp-xxhdpi")
class ThumbnailEditorInteractionTest : ScreenRenderHarness() {
    @Test fun additionalFontsAndLargerSizeAreReachable() {
        compose.setContent { StikrTheme { ThumbnailEditor(onSave = {}, onBack = {}) } }
        settle()
        compose.onNodeWithText("Text", ignoreCase = true).performClick()
        compose.onNodeWithText("Oswald").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithContentDescription("Text size").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(400f) }
        compose.onNodeWithText("Size · 400").assertIsDisplayed()
        capture("thumbnail-typography-controls")
    }

    @Test fun textCoverSurvivesStateRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { StikrTheme { ThumbnailEditor(onSave = {}, onBack = {}) } }
        settle()
        compose.onNodeWithText("Text", ignoreCase = true).performClick()
        compose.onNodeWithText("Your little collection.").performTextReplacement("Weekend notes")
        restoration.emulateSavedInstanceStateRestore()
        settle()
        compose.onNodeWithText("Weekend notes").assertExists()
        capture("collection-thumbnail-text")
    }
}
