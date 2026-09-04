package com.tsoft.audiotranscribe.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/** Reproduce un extracto [startMs, endMs] del audio original, para ayudar a identificar al hablante. */
class ClipPlayer(context: Context) {

    private val exoPlayer = ExoPlayer.Builder(context).build()
    private var boundUri: Uri? = null

    fun playClip(uri: Uri, startMs: Long, endMs: Long) {
        if (boundUri != uri) {
            exoPlayer.setMediaItem(MediaItem.fromUri(uri))
            exoPlayer.prepare()
            boundUri = uri
        }
        exoPlayer.seekTo(startMs)
        exoPlayer.play()

        exoPlayer.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (player.currentPosition >= endMs && player.isPlaying) {
                    player.pause()
                    player.removeListener(this)
                }
            }
        })
    }

    fun stop() {
        exoPlayer.stop()
    }

    fun release() {
        exoPlayer.release()
    }
}
