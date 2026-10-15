package com.metrolist.music.ui.component

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performClick
import com.metrolist.music.ui.screens.Screens
import dev.chrisbanes.haze.rememberHazeState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class, qualifiers = "en-rUS")
class LiquidGlassNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun searchResultsKeepSearchSelectedAndReselectionCallback() {
        var clicked: Screens? = null
        var reselection = false
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalLiquidGlassState provides rememberHazeState()) {
                    AppNavigationBar(
                        navigationItems = listOf(Screens.Home, Screens.Search, Screens.Library),
                        currentRoute = "search/something",
                        onItemClick = { screen, selected -> clicked = screen; reselection = selected },
                    )
                }
            }
        }
        compose.onNodeWithText("Search").assertIsSelected().performClick()
        assertEquals(Screens.Search, clicked)
        assertEquals(true, reselection)
        compose.onNodeWithText("Home").performClick()
        assertEquals(Screens.Home, clicked)
        assertEquals(false, reselection)
    }

    @Test fun compactNavigationHasAccessibleLabelsAndNestedLibrarySelection() {
        var clicked: Screens? = null
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalLiquidGlassState provides rememberHazeState()) {
                    AppNavigationBar(
                        navigationItems = listOf(Screens.Home, Screens.Search, Screens.Library),
                        currentRoute = "library/albums",
                        slimNav = true,
                        onItemClick = { screen, _ -> clicked = screen },
                    )
                }
            }
        }
        compose.onNodeWithContentDescription("Library").assertIsSelected()
        compose.onNodeWithContentDescription("Search").performClick()
        assertEquals(Screens.Search, clicked)
    }
    @Test fun specialTabsExposeAccessibleClickAndLongClickActions() {
        var clicks = 0
        var searchHolds = 0
        var homeHolds = 0
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalLiquidGlassState provides rememberHazeState()) {
                    AppNavigationBar(
                        navigationItems = listOf(Screens.Home, Screens.Search),
                        currentRoute = "home", onItemClick = { _, _ -> clicks++ },
                        onSearchLongClick = { searchHolds++ }, onHomeLongHold = { homeHolds++ },
                    )
                }
            }
        }
        compose.onNodeWithText("Search").performClick()
        compose.onNodeWithText("Home").performClick()
        compose.onNodeWithText("Search").performSemanticsAction(SemanticsActions.OnLongClick) { it() }
        compose.onNodeWithText("Home").performSemanticsAction(SemanticsActions.OnLongClick) { it() }
        assertEquals(2, clicks)
        assertEquals(1, searchHolds)
        assertEquals(1, homeHolds)
    }

}
