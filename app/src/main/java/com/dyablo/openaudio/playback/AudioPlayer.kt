package com.dyablo.openaudio.playback

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.dyablo.openaudio.data.Track

class AudioPlayer(context: Context) {
    private val player = ExoPlayer.Builder(context).build()

    fun play(track: Track) {
        player.setMediaItem(MediaItem.fromUri(track.uri))
        player.prepare()
        player.play()
    }

    fun playUrl(url: String) {
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun resume() {
        player.play()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    fun currentPositionMs(): Long = player.currentPosition.coerceAtLeast(0L)

    fun durationMs(): Long {
        val duration = player.duration
        return if (duration == C.TIME_UNSET || duration < 0) 0L else duration
    }

    fun release() {
        player.release()
    }
}
