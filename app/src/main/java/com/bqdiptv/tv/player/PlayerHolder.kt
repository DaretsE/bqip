package com.bqdiptv.tv.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Thin wrapper around a single shared ExoPlayer instance so Compose screens
 * don't each own their own player. Mirrors the prototype's "try next source
 * on failure" behaviour via [onError].
 */
class PlayerHolder(context: Context) {

    val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build()

    private var onErrorCallback: (() -> Unit)? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                onErrorCallback?.invoke()
            }
        })
    }

    fun setOnError(callback: () -> Unit) {
        onErrorCallback = callback
    }

    fun play(streamUrl: String) {
        player.setMediaItem(MediaItem.fromUri(streamUrl))
        player.prepare()
        player.playWhenReady = true
    }

    fun release() {
        player.release()
    }
}
