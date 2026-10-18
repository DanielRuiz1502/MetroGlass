package com.metrolist.music.ui.component

import android.app.Application
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.metrolist.music.ui.screens.Screens
import dev.chrisbanes.haze.rememberHazeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Pointer input is the contract under test, rather than a simulated pressed/dragged appearance.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class, qualifiers = "en-rUS-w700dp-h800dp-mdpi")
class LiquidGlassDragTest {
    @get:Rule val compose = createComposeRule()
    private var route by mutableStateOf("home")
    private var width by mutableStateOf(360.dp)
    private var slim by mutableStateOf(false)
    private var calls = 0
    private var searchHolds = 0
    private var homeHolds = 0

    private fun content(rtl: Boolean = false) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalLiquidGlassState provides rememberHazeState(),
                    LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                ) {
                    AppNavigationBar(
                        listOf(Screens.Home, Screens.Search, Screens.Library), route,
                        onItemClick = { screen, _ -> calls++; route = screen.route },
                        modifier = Modifier.width(width), slimNav = slim,
                        onSearchLongClick = { searchHolds++ }, onHomeLongHold = { homeHolds++ },
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    private fun finishAnimation() {
        repeat(2) {
            compose.mainClock.advanceTimeByFrame()
            compose.waitForIdle()
        }
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
    }

    private fun startDrag(toFraction: Float) {
        compose.onNodeWithTag("navigation_glass_bar").performTouchInput {
            down(Offset(width / 6f, height / 2f))
            moveTo(Offset(width * toFraction, height / 2f))
        }
        compose.mainClock.advanceTimeByFrame()
    }

    @Test fun followsFingerButNavigatesOnlyAfterReleaseAndSettling() {
        content()
        val start = compose.onNodeWithTag("navigation_glass_indicator").getUnclippedBoundsInRoot()
        startDrag(0.8f)
        assertEquals(0, calls)
        compose.onNodeWithText("Home").assertIsSelected()
        val dragged = compose.onNodeWithTag("navigation_glass_indicator").getUnclippedBoundsInRoot()
        assertTrue(dragged.left > start.left + 100.dp)
        compose.onNodeWithTag("navigation_glass_bar").performTouchInput { up() }
        assertEquals(0, calls)
        finishAnimation()
        compose.onNodeWithText("Library").assertIsSelected()
        assertEquals(1, calls)
        assertEquals(0, homeHolds)
    }

    @Test fun cancellationReturnsToTheActiveTabWithoutNavigation() {
        content()
        val start = compose.onNodeWithTag("navigation_glass_indicator").getUnclippedBoundsInRoot()
        startDrag(0.8f)
        compose.onNodeWithTag("navigation_glass_bar").performTouchInput { cancel() }
        finishAnimation()
        assertEquals(0, calls)
        assertEquals(start, compose.onNodeWithTag("navigation_glass_indicator").getUnclippedBoundsInRoot())
    }

    @Test fun returningBeforeReleaseKeepsTheOriginalTab() {
        content()
        startDrag(0.8f)
        compose.onNodeWithTag("navigation_glass_bar").performTouchInput {
            moveTo(Offset(width / 6f, height / 2f)); up()
        }
        finishAnimation()
        assertEquals(0, calls)
        compose.onNodeWithText("Home").assertIsSelected()
    }

    @Test fun aTapInterruptsPendingDragNavigationImmediately() {
        content()
        startDrag(0.8f)
        compose.onNodeWithTag("navigation_glass_bar").performTouchInput { up() }
        compose.onNodeWithText("Search").performClick()
        assertEquals("search_input", route)
        assertEquals(1, calls)
        finishAnimation()
        assertEquals("search_input", route)
        assertEquals(1, calls)
    }

    @Test fun physicalTapsAndSpecialHoldsKeepTheirOriginalActions() {
        content()
        compose.onNodeWithText("Search").performTouchInput { down(center); up() }
        finishAnimation()
        assertEquals("search_input", route)
        assertEquals(1, calls)
        compose.onNodeWithText("Search").performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(700)
        compose.onNodeWithText("Search").performTouchInput { up() }
        finishAnimation()
        assertEquals(1, searchHolds)
        assertEquals(1, calls)
        compose.onNodeWithText("Home").performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(15_100)
        compose.onNodeWithText("Home").performTouchInput { up() }
        finishAnimation()
        assertEquals(1, homeHolds)
        assertEquals(1, calls)
    }

    @Test fun marginsStayAtEightDpForEveryTabAtBothSizesAndHeights() {
        content()
        for (barWidth in listOf(320.dp, 600.dp)) {
            for (compact in listOf(false, true)) {
                for (destination in listOf("home", "search_input", "library")) {
                    compose.runOnIdle { width = barWidth; slim = compact; route = destination }
                    finishAnimation()
                    val bar = compose.onNodeWithTag("navigation_glass_bar").getUnclippedBoundsInRoot()
                    val bubble = compose.onNodeWithTag("navigation_glass_indicator").getUnclippedBoundsInRoot()
                    val index = listOf("home", "search_input", "library").indexOf(destination)
                    assertEquals(8f, (bubble.top - bar.top).value, 0.1f)
                    assertEquals(8f, (bar.bottom - bubble.bottom).value, 0.1f)
                    assertEquals(8f, (bubble.left - bar.left - (bar.right - bar.left) / 3 * index).value, 1f)
                    assertTrue(bubble.left >= bar.left + 8.dp && bubble.right <= bar.right - 8.dp)
                }
            }
        }
    }

    @Test fun rtlDraggingUsesPhysicalDirectionAndClampsInsideTheBar() {
        content(rtl = true)
        compose.onNodeWithTag("navigation_glass_bar").performTouchInput {
            down(Offset(width * 5f / 6f, height / 2f))
            moveTo(Offset(0f, height / 2f))
        }
        compose.mainClock.advanceTimeByFrame()
        val bar = compose.onNodeWithTag("navigation_glass_bar").getUnclippedBoundsInRoot()
        val bubble = compose.onNodeWithTag("navigation_glass_indicator").getUnclippedBoundsInRoot()
        assertTrue(bubble.left >= bar.left + 8.dp && bubble.right <= bar.right - 8.dp)
        assertEquals(0, calls)
        compose.onNodeWithTag("navigation_glass_bar").performTouchInput { up() }
        finishAnimation()
        assertEquals("library", route)
        assertEquals(1, calls)
    }
}
