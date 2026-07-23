package ru.bqd.iptv

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.Display
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy.LoadErrorInfo

object Quality {

    fun loadControl(): LoadControl {
        val sec = Store.bufferSec.coerceIn(5, 120)
        val maxMs = sec * 1000
        val minMs = (maxMs / 2).coerceAtLeast(4000)
        val startMs = (maxMs / 6).coerceIn(1000, 4000)
        val restartMs = (maxMs / 3).coerceIn(2000, 8000)
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(minMs, maxMs, startMs, restartMs)
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(10_000, true)
            .build()
    }

    fun mediaItem(url: String): MediaItem {
        val offsetSec = Store.liveOffsetSec
        if (offsetSec <= 0) return MediaItem.fromUri(url)
        return MediaItem.Builder()
            .setUri(url)
            .setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setTargetOffsetMs(offsetSec * 1000L)
                    .setMinPlaybackSpeed(0.97f)
                    .setMaxPlaybackSpeed(1.03f)
                    .build()
            )
            .build()
    }

    private fun codecSelector(): MediaCodecSelector {
        val preferSoftware = Store.decoder == "sw"
        return MediaCodecSelector { mimeType, requiresSecure, requiresTunneling ->
            val base: List<MediaCodecInfo> =
                MediaCodecSelector.DEFAULT.getDecoderInfos(mimeType, requiresSecure, requiresTunneling)
            try {
                if (preferSoftware) base.sortedByDescending { it.softwareOnly }
                else base.sortedByDescending { it.hardwareAccelerated }
            } catch (_: Throwable) {
                base
            }
        }
    }

    fun renderersFactory(ctx: Context): RenderersFactory =
        DefaultRenderersFactory(ctx)
            .setEnableDecoderFallback(true)
            .setMediaCodecSelector(codecSelector())

    private fun timeoutsMs(): Pair<Int, Int> = when (Store.retryMode) {
        "fast" -> 6_000 to 6_000
        "persistent" -> 20_000 to 20_000
        else -> 12_000 to 12_000
    }

    private fun retryCount(): Int = when (Store.retryMode) {
        "fast" -> 2
        "persistent" -> 12
        else -> 5
    }

    private fun errorPolicy(): LoadErrorHandlingPolicy {
        val retries = retryCount()
        val step = when (Store.retryMode) {
            "fast" -> 400L
            "persistent" -> 1200L
            else -> 800L
        }
        return object : DefaultLoadErrorHandlingPolicy(retries) {
            override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorInfo): Long {
                val n: Long = loadErrorInfo.errorCount.coerceAtLeast(1).toLong()
                val delay: Long = step * n
                return if (delay > 8_000L) 8_000L else delay
            }
        }
    }

    fun mediaSourceFactory(ctx: Context): MediaSource.Factory {
        val (connectMs, readMs) = timeoutsMs()
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent("BQDiptv")
            .setConnectTimeoutMs(connectMs)
            .setReadTimeoutMs(readMs)
            .setAllowCrossProtocolRedirects(true)
            .setKeepPostFor302Redirects(true)
        val ds = DefaultDataSource.Factory(ctx, http)
        return DefaultMediaSourceFactory(ds)
            .setLoadErrorHandlingPolicy(errorPolicy())
    }

    fun applyFrameRate(act: Activity, fps: Float): Boolean {
        if (!Store.afr) return false
        if (Build.VERSION.SDK_INT < 23) return false
        if (fps <= 1f || fps > 200f) return false
        return try {
            val display: Display = act.windowManager.defaultDisplay ?: return false
            val current = display.mode ?: return false
            val modes = display.supportedModes ?: return false
            if (modes.size < 2) return false

            var best: Display.Mode? = null
            var bestErr = Float.MAX_VALUE
            for (m in modes) {
                if (m.physicalWidth != current.physicalWidth) continue
                if (m.physicalHeight != current.physicalHeight) continue
                var err = Float.MAX_VALUE
                for (k in 1..4) {
                    val target = fps * k
                    if (target > m.refreshRate + 1.5f) break
                    val e = kotlin.math.abs(m.refreshRate - target)
                    if (e < err) err = e
                }
                if (err < 0.6f && err < bestErr) { bestErr = err; best = m }
            }
            val target = best ?: return false
            if (target.modeId == current.modeId) return false

            val lp = act.window.attributes
            lp.preferredDisplayModeId = target.modeId
            act.window.attributes = lp
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun resetFrameRate(act: Activity) {
        if (Build.VERSION.SDK_INT < 23) return
        try {
            val lp = act.window.attributes
            if (lp.preferredDisplayModeId != 0) {
                lp.preferredDisplayModeId = 0
                act.window.attributes = lp
            }
        } catch (_: Throwable) { }
    }

    fun bufferLabel(): String = "${Store.bufferSec} сек"
    fun liveOffsetLabel(): String =
        if (Store.liveOffsetSec <= 0) "как в потоке" else "${Store.liveOffsetSec} сек"
    fun decoderLabel(): String =
        if (Store.decoder == "sw") "программный (SW)" else "аппаратный (HW)"
    fun retryLabel(): String = when (Store.retryMode) {
        "fast" -> "быстро сдаваться"
        "persistent" -> "упорно (слабый интернет)"
        else -> "обычно"
    }
    fun qualityLabel(): String = when (Store.quality) {
        "stable" -> "стабильность (до 720p)"
        "max" -> "максимум"
        else -> "авто"
    }

    // ДОБАВЛЕНО: отображение выбранного плеера
    fun playerEngineLabel(): String = when (Store.playerEngine) {
        "exo" -> "ExoPlayer"
        "vlc" -> "VLC"
        "mx" -> "MX Player"
        else -> "Встроенный"
    }
}
