package com.dyablo.openaudio.playback

import android.content.Context
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

    fun release() {
        player.release()
    }
}
