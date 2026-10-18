package com.metrolist.music.ui.player

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import com.metrolist.music.LocalPlayerConnection
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
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
@Config(sdk = [33], application = Application::class, qualifiers = "en-rUS-w480dp-h800dp-mdpi")
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
    @Test fun metadataAndControlsFitSmallScreensAndLargerFonts() {
        data class Case(val width: Int, val fontScale: Float, val longMetadata: Boolean, val casting: Boolean)
        var case by mutableStateOf(Case(320, 1f, false, false))
        val longTitle = "A very long song title that should scroll inside the available space"
        val longArtist = "A very long artist name with several collaborators"
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalPlayerConnection provides null,
                LocalDensity provides Density(density.density, case.fontScale),
            ) {
                MaterialTheme {
                    Row(
                        Modifier.width((case.width - 40).dp).height(64.dp)
                            .padding(MiniPlayerContentPadding).testTag("mini_player_row"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(44.dp).testTag("artwork"))
                        Spacer(Modifier.width(10.dp))
                        NewMiniPlayerSongInfo(
                            mediaMetadata = MediaMetadata(
                                id = "responsive-test", duration = 180,
                                title = if (case.longMetadata) longTitle else "Magnetic",
                                artists = listOf(MediaMetadata.Artist(null, if (case.longMetadata) longArtist else "ILLIT")),
                                explicit = true,
                            ),
                            onSurfaceColor = Color.White, errorColor = Color.Red,
                            modifier = Modifier.weight(1f).testTag("metadata"),
                        )
                        Spacer(Modifier.width(4.dp))
                        if (case.casting) {
                            Box(Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                        }
                        MiniPlayerControlsContent(true, false, false, false, true, {}, {})
                    }
                }
            }
        }
        for (width in listOf(320, 360, 412)) {
            for (scale in listOf(1f, 1.3f, 1.5f)) {
                for (longMetadata in listOf(false, true)) {
                    for (casting in listOf(false, true)) {
                        val sample = Case(width, scale, longMetadata, casting)
                        compose.runOnIdle { case = sample }
                        val artistText = if (longMetadata) longArtist else "ILLIT"
                        val titleText = if (longMetadata) longTitle else "Magnetic"
                        val artist = compose.onNodeWithText(artistText).assertIsDisplayed()
                        val title = compose.onNodeWithText(titleText).assertIsDisplayed()
                        val layouts = mutableListOf<TextLayoutResult>()
                        artist.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                        val layout = layouts.single()
                        assertEquals("$sample", 1, layout.lineCount)
                        assertTrue("Artist line fits vertically: $sample", layout.getLineBottom(0) <= layout.size.height)
                        if (longMetadata) assertTrue("Ellipsis: $sample", layout.isLineEllipsized(0))
                        else assertFalse("Short artist: $sample", layout.hasVisualOverflow)
                        val art = compose.onNodeWithTag("artwork").getUnclippedBoundsInRoot()
                        val info = compose.onNodeWithTag("metadata").getUnclippedBoundsInRoot()
                        val titleBounds = title.getUnclippedBoundsInRoot()
                        val artistBounds = artist.getUnclippedBoundsInRoot()
                        val pause = compose.onNodeWithContentDescription("Pause").getUnclippedBoundsInRoot()
                        val next = compose.onNodeWithContentDescription("Next").getUnclippedBoundsInRoot()
                        assertEquals("Artwork inset: $sample", 16f, art.left.value, 0.5f)
                        assertEquals("Artwork/text spacing: $sample", 10f, (info.left - art.right).value, 0.5f)
                        assertEquals("Artwork size: $sample", 44f, (art.right - art.left).value, 0.5f)
                        assertEquals("Vertical alignment: $sample", 32f, ((art.top + art.bottom) / 2).value, 0.5f)
                        assertEquals("Text alignment: $sample", 32f, ((info.top + info.bottom) / 2).value, 0.5f)
                        assertTrue("Title above artist: $sample", titleBounds.bottom <= artistBounds.top)
                        assertTrue("Text inside capsule: $sample", titleBounds.top >= 0.dp && artistBounds.bottom <= 64.dp)
                        assertTrue("Controls clear of text: $sample", info.right <= pause.left)
                        assertTrue("Artist clear of controls: $sample", artistBounds.right <= pause.left)
                        assertEquals("Pause touch width: $sample", 48f, (pause.right - pause.left).value, 0.5f)
                        assertEquals("Pause touch height: $sample", 48f, (pause.bottom - pause.top).value, 0.5f)
                        assertEquals("Next touch width: $sample", 48f, (next.right - next.left).value, 0.5f)
                        assertEquals("Next position: $sample", (width - 50).toFloat(), next.right.value, 0.5f)
                    }
                }
            }
        }
    }
}
