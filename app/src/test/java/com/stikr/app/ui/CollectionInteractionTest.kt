package com.stikr.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import com.stikr.app.ui.theme.StikrTheme
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h880dp-xxhdpi")
class CollectionInteractionTest : ScreenRenderHarness() {
    @Test fun slideThresholdIsDeliberate() {
        assertFalse(slideConfirmed(94f,100f))
        assertTrue(slideConfirmed(95f,100f))
        assertFalse(slideConfirmed(0f,0f))
    }

    @Test fun sliderIgnoresTapAndReturnsAfterShortOrCancelledDrag() {
        var confirmed=0
        compose.setContent { StikrTheme { Column { SlideExport(onConfirm={confirmed++},modifier=Modifier.fillMaxWidth()) } } }
        val slide=compose.onNodeWithContentDescription("Slide to add to WhatsApp")
        slide.performClickWithoutSemantics()
        assertEquals(0,confirmed)
        slide.performTouchInput { swipe(Offset(20f,centerY),Offset(width*.45f,centerY)) }
        assertEquals(0,confirmed)
        slide.performTouchInput { down(Offset(20f,centerY)); moveTo(Offset(width*.6f,centerY)); cancel() }
        assertEquals(0,confirmed)
        slide.performTouchInput { swipe(Offset(10f, centerY), Offset(width - 10f, centerY)) }
        assertEquals(1,confirmed)
    }

    @Test fun accessibilityConfirmationAndBusyStateAreExplicit() {
        var confirmed=0
        val busy=androidx.compose.runtime.mutableStateOf(false)
        compose.setContent { StikrTheme { SlideExport(busy=busy.value,onConfirm={confirmed++}) } }
        val actions=compose.onNodeWithContentDescription("Slide to add to WhatsApp").fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertEquals("Confirm add to WhatsApp",actions.single().label)
        compose.runOnIdle { actions.single().action() }
        assertEquals(1,confirmed)
        compose.runOnIdle { busy.value=true }
        compose.onNodeWithContentDescription("Preparing pack for WhatsApp").assertIsNotEnabled()
    }

    @Test fun singlesExcludeStartersAndOpenCenteredExportAfterLongPress() {
        val pack=packFixture(count=3).copy(isSingles=true,starterStickers=listOf("s1.webp","s2.webp"))
        compose.setContent { StikrTheme { HomeScreen(listOf(pack),{},{},{},{_,_,_,_->},{}) } }
        settle()
        compose.onNodeWithText("1 saved").assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Single sticker").assertCountEquals(1)
        val create=compose.onNodeWithText("Create a\nsticker pack").fetchSemanticsNode().boundsInRoot
        val library=compose.onNodeWithText("Your packs").fetchSemanticsNode().boundsInRoot
        assertTrue(create.bottom<library.top)
        compose.onNodeWithContentDescription("Single sticker").performTouchInput { longClick() }
        compose.onNodeWithText("Add to WhatsApp").performClick()
        compose.onNodeWithText("Take your singles along.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Slide to add to WhatsApp").assertIsEnabled()
        captureDialog("single-centered-export")
    }

    @Test fun singleDeleteRequiresConfirmation() {
        var deleted=""
        val pack=packFixture(count=1).copy(isSingles=true)
        compose.setContent { StikrTheme { HomeScreen(listOf(pack),{},{},{},{_,_,_,_->},{},onDeleteSingle={id,file->deleted="$id/$file"}) } }
        settle()
        compose.onNodeWithContentDescription("Single sticker").performTouchInput { longClick() }
        compose.onNodeWithText("Delete").performClick()
        assertEquals("",deleted)
        compose.onNodeWithText("Keep sticker").performClick()
        assertEquals("",deleted)
        compose.onNodeWithContentDescription("Single sticker").performTouchInput { longClick() }
        compose.onNodeWithText("Delete").performClick()
        compose.onNodeWithText("Delete").performClick()
        assertEquals("pack-1/s1.webp",deleted)
    }

    private fun SemanticsNodeInteraction.performClickWithoutSemantics() = performTouchInput { click() }
    @Test fun approvedHomeSeparatesSinglesAndOpensTheirImportModalOnLongPress() {
        val singles = packFixture("singles", "Stikr", 3).copy(isSingles = true, serialNumber = 1, displayId = "48217903")
        val packs = listOf(packFixture("ghosts", "Night ghosts", 12).copy(serialNumber = 2, displayId = "48217904"), packFixture("friends", "Tiny friends", 8).copy(serialNumber = 3, displayId = "48217905"))
        var imported = false
        compose.setContent {
            StikrTheme { HomeScreen(packs = listOf(singles) + packs, onOpenPack = {}, onCreatePack = {}, onMakeSticker = {}, onStartBatch = { _, _, _, _ -> }, onShowTour = {}, onExportSingles = { imported = true }) }
        }
        settle()
        capture("collection-home-approved")
        compose.onAllNodesWithContentDescription("Single sticker").onFirst().performTouchInput { longClick() }
        compose.onNodeWithText("Add to WhatsApp").performClick()
        compose.onNodeWithText("Take your singles along.").assertIsDisplayed()
        assertFalse(imported)
        captureDialog("collection-single-import")
    }

}
