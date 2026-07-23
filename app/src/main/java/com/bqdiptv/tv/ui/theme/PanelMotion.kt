package com.bqdiptv.tv.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

enum class SlideDirection { FROM_LEFT, FROM_RIGHT, FROM_BOTTOM }

/**
 * `@keyframes panelInLeft{from{opacity:0;transform:translateX(-34px)}to{opacity:1;transform:none}}`
 * `@keyframes panelInRight{from{opacity:0;transform:translateX(34px)}to{opacity:1;transform:none}}`
 * `@keyframes osdUp{from{opacity:0;transform:translateY(30px)}to{opacity:1;transform:none}}`
 * duration .3s (.34s for OSD), easing cubic-bezier(.2,.7,.3,1). The prototype
 * never defines a distinct exit for these — it just removes the node — so
 * exit here is a quick fade rather than an invented reverse-slide.
 */
@Composable
fun SlidePanel(
    visible: Boolean,
    direction: SlideDirection,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val panelPx = with(density) { BqMotion.PanelSlideDp.dp.roundToPx() }
    val osdPx = with(density) { BqMotion.OsdSlideDp.dp.roundToPx() }

    val enter = when (direction) {
        SlideDirection.FROM_LEFT -> slideInHorizontally(
            animationSpec = tween(BqMotion.PanelDurationMs, easing = BqMotion.EnterEase),
            initialOffsetX = { -panelPx }
        ) + fadeIn(tween(BqMotion.PanelDurationMs, easing = BqMotion.EnterEase))
        SlideDirection.FROM_RIGHT -> slideInHorizontally(
            animationSpec = tween(BqMotion.PanelDurationMs, easing = BqMotion.EnterEase),
            initialOffsetX = { panelPx }
        ) + fadeIn(tween(BqMotion.PanelDurationMs, easing = BqMotion.EnterEase))
        SlideDirection.FROM_BOTTOM -> slideInVertically(
            animationSpec = tween(BqMotion.OsdDurationMs, easing = BqMotion.EnterEase),
            initialOffsetY = { osdPx }
        ) + fadeIn(tween(BqMotion.OsdDurationMs, easing = BqMotion.EnterEase))
    }
    val exit = fadeOut(tween(BqMotion.HoverTransitionSlowMs))

    AnimatedVisibility(visible = visible, enter = enter, exit = exit, modifier = modifier) {
        content()
    }
}

/**
 * `@keyframes railIn{from{transform:translateX(-160px);opacity:.15}60%{opacity:1}to{transform:none;opacity:1}}`
 * duration .36s, easing cubic-bezier(.2,.72,.28,1) — used when the channel
 * browser slides out from behind the collapsed left-menu rail.
 * `@keyframes railOut{...}` duration .3s, easing cubic-bezier(.4,0,.7,1).
 */
@Composable
fun RailPanel(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val railPx = with(density) { BqMotion.RailSlideDp.dp.roundToPx() }

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            animationSpec = tween(BqMotion.RailInDurationMs, easing = BqMotion.RailInEase),
            initialOffsetX = { -railPx }
        ) + fadeIn(tween((BqMotion.RailInDurationMs * 0.6).toInt())),
        exit = slideOutHorizontally(
            animationSpec = tween(BqMotion.RailOutDurationMs, easing = BqMotion.ExitEase),
            targetOffsetX = { -railPx }
        ) + fadeOut(tween(BqMotion.RailOutDurationMs, easing = BqMotion.ExitEase)),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * `@keyframes fadeScale{from{opacity:0;transform:scale(.965)}to{opacity:1;transform:none}}`
 * Used by #search and modal-style full-screen overlays.
 */
@Composable
fun FadeScalePanel(
    visible: Boolean,
    modifier: Modifier = Modifier,
    durationMs: Int = BqMotion.FadeScaleDurationMs,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            animationSpec = tween(durationMs, easing = BqMotion.EnterEase),
            initialScale = BqMotion.FadeScaleFrom
        ) + fadeIn(tween(durationMs, easing = BqMotion.EnterEase)),
        exit = fadeOut(tween(BqMotion.HoverTransitionSlowMs)),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * `@keyframes dialogIn{from{opacity:0;transform:scale(.9) translateY(14px)}to{opacity:1;transform:none}}`
 */
@Composable
fun DialogPanel(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val dialogPx = with(density) { BqMotion.DialogSlideDp.dp.roundToPx() }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            animationSpec = tween(BqMotion.DialogDurationMs, easing = BqMotion.EnterEase),
            initialScale = BqMotion.DialogScaleFrom
        ) + slideInVertically(
            animationSpec = tween(BqMotion.DialogDurationMs, easing = BqMotion.EnterEase),
            initialOffsetY = { dialogPx }
        ) + fadeIn(tween(BqMotion.DialogDurationMs, easing = BqMotion.EnterEase)),
        exit = scaleOut(tween(BqMotion.HoverTransitionSlowMs)) + fadeOut(tween(BqMotion.HoverTransitionSlowMs)),
        modifier = modifier
    ) {
        content()
    }
}
