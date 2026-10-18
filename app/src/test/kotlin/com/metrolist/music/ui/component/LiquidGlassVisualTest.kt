package com.metrolist.music.ui.component

import android.app.Application
import com.metrolist.music.R
import com.metrolist.music.LocalPlayerConnection
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.player.MiniPlayerControlsContent
import com.metrolist.music.ui.player.MiniPlayerContentPadding
import com.metrolist.music.ui.player.NewMiniPlayerSongInfo
import com.metrolist.music.ui.screens.Screens
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// Native Compose component renders for review; these are not screenshots of the installed APK.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class, qualifiers = "en-rUS-w400dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LiquidGlassVisualTest {
    @get:Rule val compose = createComposeRule()

    @Test fun recordGlassIndicatorAndArtistLayout() {
        var route by mutableStateOf("home")
        val backdrops = listOf(
            "smoked" to listOf(Color(0xFF633E49), Color(0xFF163840), Color(0xFF312447)),
            "light" to listOf(Color.White, Color(0xFFECEFF1), Color.White),
            "dark" to listOf(Color.Black, Color(0xFF181818), Color.Black),
            "colorful" to listOf(Color(0xFFFF1744), Color(0xFF00C853), Color(0xFF2979FF)),
        )
        var backdrop by mutableStateOf(backdrops.first().second)
        compose.setContent {
            MaterialTheme {
                val haze = rememberHazeState()
                CompositionLocalProvider(LocalLiquidGlassState provides haze, LocalPlayerConnection provides null) {
                    Box(Modifier.width(360.dp).height(320.dp).background(Color.Black).testTag("glass_review")) {
                        Box(Modifier.fillMaxSize().hazeSource(haze).background(
                            Brush.horizontalGradient(backdrop),
                        ))
                        Column(
                            Modifier.align(Alignment.TopEnd).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            LiquidGlassIconButton(R.drawable.mic, "Recognize music", {})
                            LiquidGlassIconButton(R.drawable.shuffle, "Shuffle", {})
                        }
                        Column(Modifier.align(Alignment.BottomCenter), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LiquidGlassSurface(Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(64.dp)) {
                                Row(Modifier.fillMaxSize().padding(MiniPlayerContentPadding), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFF455A64)))
                                    Spacer(Modifier.width(10.dp))
                                    NewMiniPlayerSongInfo(
                                        mediaMetadata = MediaMetadata("visual-test", "Magnetic", listOf(MediaMetadata.Artist(null, "ILLIT")), 180),
                                        onSurfaceColor = Color.White, errorColor = Color.Red,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    MiniPlayerControlsContent(true, false, false, false, true, {}, {})
                                }
                            }
                            AppNavigationBar(
                                navigationItems = listOf(Screens.Home, Screens.Search, Screens.Library),
                                currentRoute = route, onItemClick = { screen, _ -> route = screen.route },
                            )
                        }
                    }
                }
            }
        }
        val output = File(System.getProperty("metroglass.visualOutputDir") ?: "build/outputs/liquid-glass-review")
        output.mkdirs()
        for ((name, colors) in backdrops) {
            for (destination in listOf("home", "search_input", "library")) {
                compose.runOnIdle { route = destination; backdrop = colors }
                compose.waitForIdle()
                val suffix = if (name == "smoked") destination else "$name-$destination"
                val file = File(output, "compose-components-$suffix.png")
                file.outputStream().use {
                    assertTrue(compose.onNodeWithTag("glass_review").captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it))
                }
                assertTrue(file.isFile && file.length() > 0)
            }
        }
    }
}
