package com.bqdiptv.tv.ui.theme

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.getSystemService

enum class GraphicsQuality { HIGH, LOW }

/**
 * The prototype leans on `backdrop-filter: blur()` and multi-layer
 * `box-shadow` halos everywhere — cheap in a browser compositor, but a real
 * per-frame cost on the underpowered SoCs common in Android TV boxes/sticks
 * (RenderEffect blur is GPU-heavy, and there's no way to blur-behind before
 * API 31 at all). Rather than silently dropping fidelity, we detect capacity
 * up front and pick one of two rendering paths:
 *
 *  HIGH — real backdrop blur (API 31+) + true multi-layer glow shadows.
 *         Used on capable hardware: newer Android TV OS, or >=3GB RAM.
 *  LOW  — flat translucent panels + a single crisp accent-coloured stroke
 *         instead of a blurred glow. Visually the same palette/layout/motion,
 *         just without the two GPU-expensive effects. This mirrors the
 *         prototype's own degraded state (see `.dimrail` in the source,
 *         which drops the halo to a flat inset ring under the same idea).
 *
 * Users can override the automatic choice from Settings ("Экономичный режим").
 */
object GraphicsQualityDetector {
    fun detect(context: Context): GraphicsQuality {
        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return GraphicsQuality.LOW // no RenderEffect blur pre-API31
            val am = context.getSystemService<ActivityManager>() ?: return GraphicsQuality.LOW
            val info = ActivityManager.MemoryInfo()
            am.getMemoryInfo(info)
            val totalRamGb = info.totalMem / (1024.0 * 1024.0 * 1024.0)
            val lowRamDevice = am.isLowRamDevice
            if (!lowRamDevice && totalRamGb >= 2.5) GraphicsQuality.HIGH else GraphicsQuality.LOW
        } catch (e: Exception) {
            GraphicsQuality.LOW
        }
    }
}

val LocalGraphicsQuality = staticCompositionLocalOf { GraphicsQuality.LOW }
