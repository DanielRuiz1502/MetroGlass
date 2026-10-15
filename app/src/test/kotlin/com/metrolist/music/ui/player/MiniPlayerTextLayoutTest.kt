package com.metrolist.music.ui.player

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import com.metrolist.music.LocalPlayerConnection
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.metrolist.music.models.MediaMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class, qualifiers = "en-rUS")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MiniPlayerTextLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun shortArtistFitsWithAnExplicitBadgeInANarrowTextSlot() {
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalPlayerConnection provides null) {
                Box(Modifier.size(width = 90.dp, height = 48.dp)) {
                    NewMiniPlayerSongInfo(
                        mediaMetadata = MediaMetadata(
                            id = "layout-test", title = "Magnetic", duration = 180,
                            artists = listOf(MediaMetadata.Artist(null, "ILLIT")), explicit = true,
                        ),
                        onSurfaceColor = Color.White, errorColor = Color.Red,
                    )
                }
                }
            }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        val artist = compose.onNodeWithText("ILLIT").assertIsDisplayed()
        artist.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(1, layouts.size)
        val layout = layouts.single()
        assertFalse("Short artist must fit: size=${layout.size}, paragraph=${layout.multiParagraph.width}x${layout.multiParagraph.height}, constraints=${layout.layoutInput.constraints}, widthOverflow=${layout.didOverflowWidth}, heightOverflow=${layout.didOverflowHeight}", layout.hasVisualOverflow)
        val titleBounds = compose.onNodeWithText("Magnetic").getUnclippedBoundsInRoot()
        val artistBounds = artist.getUnclippedBoundsInRoot()
        assertTrue(artistBounds.top >= titleBounds.bottom)
        assertTrue(artistBounds.bottom <= 48.dp)
    }
}
