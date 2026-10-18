package com.metrolist.music.ui.component

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class, qualifiers = "en-rUS-w400dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LiquidGlassButtonsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun floatingButtonsHaveEqualAccessibleSizesSpacingAndOriginalCallbacks() {
        var shuffle = 0
        var recognition = 0
        compose.setContent {
            MaterialTheme {
                val haze = rememberHazeState()
                val sources = remember { mutableStateSetOf<Any>() }
                CompositionLocalProvider(
                    LocalLiquidGlassState provides haze,
                    LocalLiquidGlassSources provides sources,
                    LocalPlayerAwareWindowInsets provides WindowInsets(0),
                ) {
                    Box(Modifier.size(360.dp, 300.dp).then(
                        if (sources.isEmpty()) Modifier.hazeSource(haze) else Modifier,
                    )) {
                        Box(Modifier.fillMaxSize().liquidGlassSource().background(Color.DarkGray))
                        HideOnScrollFAB(
                            lazyListState = rememberLazyListState(), icon = R.drawable.shuffle,
                            onClick = { shuffle++ }, onRecognitionClick = { recognition++ },
                        )
                    }
                }
            }
        }
        val mic = compose.onNodeWithContentDescription("Recognize music")
        val shuffleButton = compose.onNodeWithContentDescription("Shuffle")
        mic.assertWidthIsEqualTo(56.dp).assertHeightIsEqualTo(56.dp).performClick()
        shuffleButton.assertWidthIsEqualTo(56.dp).assertHeightIsEqualTo(56.dp).performClick()
        assertEquals(1, shuffle)
        assertEquals(1, recognition)
        val top = mic.getUnclippedBoundsInRoot()
        val bottom = shuffleButton.getUnclippedBoundsInRoot()
        assertEquals(12f, (bottom.top - top.bottom).value, 0.1f)
        assertEquals(top.left, bottom.left)
        assertTrue(bottom.bottom <= 300.dp - 16.dp)
    }

    @Test fun scopedCaptureUnregistersWhenScreenLeaves() {
        var show by mutableStateOf(true)
        val sources = mutableStateSetOf<Any>()
        compose.setContent {
            CompositionLocalProvider(
                LocalLiquidGlassState provides rememberHazeState(),
                LocalLiquidGlassSources provides sources,
            ) {
                if (show) Box(Modifier.size(100.dp).liquidGlassSource())
            }
        }
        compose.runOnIdle { assertEquals(1, sources.size); show = false }
        compose.runOnIdle { assertTrue(sources.isEmpty()) }
    }

    @Test fun circularGlassClipsCornersAndBlursChangingContentBehindIt() {
        var bright by mutableStateOf(true)
        compose.setContent {
            MaterialTheme {
                val haze = rememberHazeState()
                CompositionLocalProvider(LocalLiquidGlassState provides haze) {
                    Box(Modifier.size(120.dp, 80.dp).testTag("blur_probe")) {
                        Box(Modifier.fillMaxSize().hazeSource(haze).drawBehind {
                            for (x in 0 until size.width.toInt()) {
                                drawRect(
                                    if (bright && x % 4 < 2) Color.White else Color.Black,
                                    Offset(x.toFloat(), 0f), Size(1f, size.height),
                                )
                            }
                        })
                        LiquidGlassIconButton(
                            R.drawable.mic, "Recognize music", {},
                            Modifier.align(Alignment.Center),
                        )
                    }
                }
            }
        }
        val striped = compose.onNodeWithTag("blur_probe").captureToImage().asAndroidBitmap()
        fun red(bitmap: android.graphics.Bitmap, x: Int, y: Int) = android.graphics.Color.red(bitmap.getPixel(x, y))
        // Outside the circular clip the original high-contrast stripes remain sharp.
        assertTrue(kotlin.math.abs(red(striped, 32, 12) - red(striped, 34, 12)) > 200)
        val sample = (48..71).map { red(striped, it, 24) }
        assertTrue("Glass must blur the stripes: $sample", sample.max() - sample.min() < 40)
        compose.runOnIdle { bright = false }
        compose.waitForIdle()
        val dark = compose.onNodeWithTag("blur_probe").captureToImage().asAndroidBitmap()
        assertTrue("Glass must respond to its source", sample.average() - (48..71).map { red(dark, it, 24) }.average() > 15)
    }
}
