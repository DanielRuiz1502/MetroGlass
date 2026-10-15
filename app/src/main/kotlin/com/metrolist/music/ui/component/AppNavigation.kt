/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.metrolist.music.constants.GlassHorizontalMargin
import com.metrolist.music.constants.GlassBottomSpacing
import com.metrolist.music.constants.NavigationBarHeight
import com.metrolist.music.constants.SlimNavBarHeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.metrolist.music.ui.screens.Screens
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

// The indicator shares the bar's captured blur instead of running a third blur pass.
private val NavigationBubbleFill = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.065f), Color.White.copy(alpha = 0.10f)),
)
private val NavigationBubbleEdge = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.32f), Color.White.copy(alpha = 0.07f), Color.White.copy(alpha = 0.18f)),
)

@Stable
private fun isRouteSelected(currentRoute: String?, screenRoute: String, navigationItems: List<Screens>): Boolean {
    if (currentRoute == null) return false
    if (currentRoute == screenRoute) return true
    if (navigationItems.any { it.route == screenRoute } &&
        currentRoute.startsWith("$screenRoute/")) return true

    // Fix: match the route template, not the resolved route
    if (screenRoute == "search_input" &&
        (currentRoute.startsWith("search/") || currentRoute == "search/{query}")) return true

    return false
}

@Composable
fun AppNavigationRail(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null,
    onHomeLongHold: (() -> Unit)? = null,
) {
    val containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current

    NavigationRail(
        modifier = modifier,
        containerColor = containerColor
    ) {
        Spacer(modifier = Modifier.weight(1f))

        navigationItems.forEach { screen ->
            val isSelected = remember(currentRoute, screen.route) {
                isRouteSelected(currentRoute, screen.route, navigationItems)
            }
            val currentIsSelected by rememberUpdatedState(isSelected)
            val iconRes = remember(isSelected, screen) {
                if (isSelected) screen.iconIdActive else screen.iconIdInactive
            }

            val isSearchItem = screen == Screens.Search && onSearchLongClick != null
            val isHomeHoldItem = screen == Screens.Home && onHomeLongHold != null
            val interactionSource = remember { MutableInteractionSource() }

            // Long press detection using InteractionSource
            if (isSearchItem || isHomeHoldItem) {
                LaunchedEffect(interactionSource) {
                    var isLongClick = false
                    interactionSource.interactions.collectLatest { interaction ->
                        when (interaction) {
                            is PressInteraction.Press -> {
                                isLongClick = false
                                delay(if (isHomeHoldItem) 15_000L else viewConfiguration.longPressTimeoutMillis)
                                isLongClick = true
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (isHomeHoldItem) onHomeLongHold.invoke() else onSearchLongClick?.invoke()
                            }
                            is PressInteraction.Release -> {
                                if (!isLongClick) {
                                    onItemClick(screen, currentIsSelected)
                                }
                            }
                            is PressInteraction.Cancel -> {
                                isLongClick = false
                            }
                        }
                    }
                }
            }

            NavigationRailItem(
                selected = isSelected,
                onClick = {
                    if (!isSearchItem && !isHomeHoldItem) {
                        onItemClick(screen, currentIsSelected)
                    }
                    // Long presses are handled via InteractionSource
                },
                interactionSource = interactionSource,
                icon = {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = stringResource(screen.titleId)
                    )
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun AppNavigationBar(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    slimNav: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null,
    onHomeLongHold: (() -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current

    Box(modifier = modifier.padding(horizontal = GlassHorizontalMargin).padding(bottom = GlassBottomSpacing)) {
        LiquidGlassSurface(Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
            BoxWithConstraints(
                Modifier.fillMaxWidth().height(if (slimNav) SlimNavBarHeight else NavigationBarHeight),
            ) {
                val selectedIndex = navigationItems.indexOfFirst {
                    isRouteSelected(currentRoute, it.route, navigationItems)
                }
                val indicatorPosition = animateFloatAsState(
                    targetValue = selectedIndex.coerceAtLeast(0).toFloat(),
                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                    label = "glassNavigationIndicator",
                )
                if (selectedIndex >= 0) {
                    val slotWidth = maxWidth / navigationItems.size
                    Box(
                        Modifier
                            .offset { IntOffset((slotWidth.toPx() * indicatorPosition.value).roundToInt(), 0) }
                            .width(slotWidth)
                            .fillMaxHeight()
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                            .clip(CircleShape)
                            .background(NavigationBubbleFill)
                            .border(0.5.dp, NavigationBubbleEdge, CircleShape)
                            .testTag("navigation_glass_indicator"),
                    )
                }
                Row(
                    modifier = Modifier.matchParentSize().selectableGroup(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    navigationItems.forEach { screen ->
                        val isSelected = remember(currentRoute, screen.route) {
                            isRouteSelected(currentRoute, screen.route, navigationItems)
                        }
                        val currentIsSelected by rememberUpdatedState(isSelected)
                        val iconRes = remember(isSelected, screen) {
                            if (isSelected) screen.iconIdActive else screen.iconIdInactive
                        }

                        val isSearchItem = screen == Screens.Search && onSearchLongClick != null
                        val isHomeHoldItem = screen == Screens.Home && onHomeLongHold != null
                        val interactionSource = remember { MutableInteractionSource() }

                        // Long press detection using InteractionSource
                        if (isSearchItem || isHomeHoldItem) {
                            LaunchedEffect(interactionSource) {
                                var isLongClick = false
                                interactionSource.interactions.collectLatest { interaction ->
                                    when (interaction) {
                                        is PressInteraction.Press -> {
                                            isLongClick = false
                                            delay(if (isHomeHoldItem) 15_000L else viewConfiguration.longPressTimeoutMillis)
                                            isLongClick = true
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            if (isHomeHoldItem) onHomeLongHold.invoke() else onSearchLongClick?.invoke()
                                        }
                                        is PressInteraction.Release -> {
                                            if (!isLongClick) {
                                                onItemClick(screen, currentIsSelected)
                                            }
                                        }
                                        is PressInteraction.Cancel -> {
                                            isLongClick = false
                                        }
                                    }
                                }
                            }
                        }

                        val itemColor by animateColorAsState(
                            targetValue = if (isSelected) LiquidGlassActiveColor else LiquidGlassContentColor,
                            animationSpec = tween(180),
                            label = "navigationColor",
                        )
                        Column(
                            modifier = Modifier.weight(1f).fillMaxSize()
                                .padding(horizontal = 4.dp, vertical = 8.dp)
                                .clip(CircleShape)
                                .semantics {
                                    if (isSearchItem || isHomeHoldItem) {
                                        onClick { onItemClick(screen, currentIsSelected); true }
                                        onLongClick {
                                            if (isHomeHoldItem) onHomeLongHold?.invoke() else onSearchLongClick?.invoke()
                                            true
                                        }
                                    }
                                }
                                .selectable(
                                    selected = isSelected,
                                    role = Role.Tab,
                                    interactionSource = interactionSource,
                                    indication = androidx.compose.foundation.LocalIndication.current,
                                    onClick = {
                                        if (!isSearchItem && !isHomeHoldItem) onItemClick(screen, currentIsSelected)
                                    },
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                painter = painterResource(iconRes),
                                contentDescription = if (slimNav) stringResource(screen.titleId) else null,
                                tint = itemColor,
                                modifier = Modifier.size(30.dp),
                            )
                            if (!slimNav) {
                                Text(
                                    text = stringResource(screen.titleId),
                                    color = itemColor,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 3.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
