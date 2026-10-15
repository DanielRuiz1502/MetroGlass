package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

val LocalLiquidGlassState = staticCompositionLocalOf<HazeState> {
    error("Liquid glass requires the screen's shared Haze source")
}

val LiquidGlassActiveColor = Color(0xFF009DFF)
val LiquidGlassContentColor = Color(0xFFF5F5F7)

private val SmokedGlassStyle = HazeBlurStyle {
    // Enable Haze's real RenderScript blur on API 26–30 as well as RenderEffect on 31+.
    blurEnabled(true)
    blurRadius(24.dp)
    noiseFactor(0f)
    backgroundColor(Color(0xFF151518))
    colorEffects(listOf(HazeColorEffect.tint(Color(0xFF202024).copy(alpha = 0.58f))))
}

private val GlassReflection = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.06f), Color.Transparent, Color.White.copy(alpha = 0.025f)),
)
private val GlassEdge = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.13f)),
)

@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .hazeBlur(
                input = HazeInput.Sources(LocalLiquidGlassState.current),
                style = SmokedGlassStyle,
                performanceMode = HazePerformanceMode.Default,
            )
            .background(GlassReflection)
            .border(0.5.dp, GlassEdge, CircleShape),
        content = content,
    )
}
