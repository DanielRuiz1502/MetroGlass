package com.metrolist.music.ui.player

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class, qualifiers = "en-rUS")
class MiniPlayerControlsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun playbackStateUpdatesPausePlayAndNextCallbacks() {
        var playing by mutableStateOf(true)
        var playbackClicks = 0
        var nextClicks = 0
        compose.setContent {
            MaterialTheme {
                MiniPlayerControlsContent(
                    isPlaying = playing, hasEnded = false, isGuest = false, isMuted = false,
                    canSkipNext = true,
                    onPlayPause = { playbackClicks++; playing = !playing },
                    onNext = { nextClicks++ },
                )
            }
        }
        compose.onNodeWithContentDescription("Pause").performClick()
        compose.onNodeWithContentDescription("Play").performClick()
        compose.onNodeWithContentDescription("Next").performClick()
        assertEquals(2, playbackClicks)
        assertEquals(1, nextClicks)
    }

    @Test fun guestsRetainMuteAndCannotSkip() {
        var muteClicks = 0
        compose.setContent {
            MaterialTheme {
                MiniPlayerControlsContent(
                    isPlaying = true, hasEnded = false, isGuest = true, isMuted = true,
                    canSkipNext = true, onPlayPause = { muteClicks++ }, onNext = {},
                )
            }
        }
        compose.onNodeWithContentDescription("Next").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Unmute").performClick()
        assertEquals(1, muteClicks)
    }

    @Test fun endedTrackCanRestartAndUnavailableNextStaysDisabled() {
        var restartClicks = 0
        compose.setContent {
            MaterialTheme {
                MiniPlayerControlsContent(
                    isPlaying = false, hasEnded = true, isGuest = false, isMuted = false,
                    canSkipNext = false, onPlayPause = { restartClicks++ }, onNext = {},
                )
            }
        }
        compose.onNodeWithContentDescription("Next").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Play").performClick()
        assertEquals(1, restartClicks)
    }
}
