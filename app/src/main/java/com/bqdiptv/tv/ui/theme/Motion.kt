package com.bqdiptv.tv.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * Every value here is copied straight from the prototype's CSS, not
 * approximated. Cross-reference with BQDiptv-ui-redesign.html:
 *
 *   @keyframes panelInLeft  { from{opacity:0;transform:translateX(-34px)} to{opacity:1;transform:none} }   .3s
 *   @keyframes panelInRight { from{opacity:0;transform:translateX(34px)}  to{opacity:1;transform:none} }   .3s
 *   @keyframes railIn       { from{transform:translateX(-160px);opacity:.15} 60%{opacity:1} to{...} }      .36s
 *   @keyframes railOut      { from{transform:none;opacity:1} to{transform:translateX(-160px);opacity:0} }  .3s
 *   @keyframes osdUp        { from{opacity:0;transform:translateY(30px)} to{opacity:1;transform:none} }    .34s / .28s
 *   @keyframes toastUp      { from{opacity:0;transform:translateY(30px)} to{opacity:1;transform:none} }    .28s
 *   @keyframes dialogIn     { from{opacity:0;transform:scale(.9) translateY(14px)} to{...} }                .3s
 *   @keyframes fadeScale    { from{opacity:0;transform:scale(.965)} to{...} }                                .28s-.3s
 *   @keyframes rowIn        { from{opacity:0;transform:translateX(-10px)} to{...} }
 *   focus-state hover transitions (background/box-shadow/transform)                                        .18s-.2s
 */
object BqMotion {
    // cubic-bezier(.2,.7,.3,1) — the prototype's signature "settle in" ease,
    // used for every panel/OSD/dialog entrance.
    val EnterEase: Easing = CubicBezierEasing(0.2f, 0.7f, 0.3f, 1f)

    // cubic-bezier(.4,0,.7,1) — used for panel/rail exits (railOut).
    val ExitEase: Easing = CubicBezierEasing(0.4f, 0f, 0.7f, 1f)

    // cubic-bezier(.2,.72,.28,1) — the rail push-in easing, very close to
    // EnterEase but distinct in the source; kept separate for fidelity.
    val RailInEase: Easing = CubicBezierEasing(0.2f, 0.72f, 0.28f, 1f)

    // cubic-bezier(.3,.7,.3,1) — settings toggle thumb slide.
    val ToggleEase: Easing = CubicBezierEasing(0.3f, 0.7f, 0.3f, 1f)

    const val PanelDurationMs = 300          // panelInLeft/panelInRight
    const val RailInDurationMs = 360         // railIn
    const val RailOutDurationMs = 300        // railOut
    const val OsdDurationMs = 340            // osdUp (#osd)
    const val PreviewDurationMs = 340        // osdUp (#previewCard)
    const val OsdMenuDurationMs = 280        // osdUp (#osdMenu)
    const val ToastDurationMs = 280          // toastUp
    const val DialogDurationMs = 300         // dialogIn
    const val FadeScaleDurationMs = 280      // fadeScale (#search)
    const val FadeScaleSlowMs = 300          // fadeScale (#setupOverlay / dialog backdrop)
    const val RowInDurationMs = 200          // rowIn (list item stagger)
    const val HoverTransitionMs = 180        // .18s focus/hover transitions
    const val HoverTransitionSlowMs = 200    // .2s focus/hover transitions
    const val FavBlinkDurationMs = 900        // favBlink opacity fade
    const val LivePulseDurationMs = 1600      // livePulse infinite pulse

    // Slide distances, converted 1dp≈1px at the prototype's reference density.
    const val PanelSlideDp = 34
    const val RailSlideDp = 160
    const val OsdSlideDp = 30
    const val DialogSlideDp = 14
    const val RowSlideDp = 10
    const val DialogScaleFrom = 0.9f
    const val FadeScaleFrom = 0.965f
}
