package com.metrolist.music.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

val LocalLiquidGlassState = staticCompositionLocalOf<HazeState> {
    error("Liquid glass requires the screen's shared Haze source")
}

val LocalLiquidGlassSources = staticCompositionLocalOf<SnapshotStateSet<Any>?> { null }

// Replace the whole-screen capture while a screen captures content below its glass controls.
@Composable
fun Modifier.liquidGlassSource(): Modifier {
    val sources = LocalLiquidGlassSources.current
    val key = remember { Any() }
    DisposableEffect(sources, key) {
        sources?.add(key)
        onDispose { sources?.remove(key) }
    }
    return hazeSource(LocalLiquidGlassState.current)
}

val LiquidGlassActiveColor = Color(0xFF009DFF)
val LiquidGlassContentColor = Color(0xFFF5F5F7)

private val SmokedGlassStyle = HazeBlurStyle {
    // Enable Haze's real RenderScript blur on API 26–30 as well as RenderEffect on 31+.
    blurEnabled(true)
    blurRadius(24.dp)
    noiseFactor(0f)
    backgroundColor(Color(0xFF151518))
    colorEffects(listOf(HazeColorEffect.tint(Color(0xFF202024).copy(alpha = 0.55f))))
}

private val GlassReflection = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.06f), Color.Transparent, Color.White.copy(alpha = 0.025f)),
)
private val GlassEdge = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.13f)),
)

// Selection uses the surface's captured blur without adding another blur pass.
internal val LiquidGlassSelectionFill = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.045f), Color.White.copy(alpha = 0.075f)),
)
internal val LiquidGlassSelectionEdge = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.16f)),
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

@Composable
fun LiquidGlassIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidGlassSurface(modifier.size(56.dp)) {
        Box(
            Modifier.fillMaxSize().clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = contentDescription,
                tint = LiquidGlassContentColor,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
