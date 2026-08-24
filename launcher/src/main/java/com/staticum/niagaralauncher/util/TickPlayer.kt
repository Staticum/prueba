package com.staticum.niagaralauncher.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.staticum.niagaralauncher.R

/** Plays a short synthesized click for scroll/index feedback, independent of the
 * system "touch sounds" setting (which only gates [android.view.View.playSoundEffect]). */
class TickPlayer(context: Context) {

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val soundId = soundPool.load(context, R.raw.tick, 1)
    @Volatile private var isLoaded = false

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (sampleId == soundId && status == 0) isLoaded = true
        }
    }

    fun play() {
        if (isLoaded) soundPool.play(soundId, 0.5f, 0.5f, 0, 0, 1f)
    }

    fun release() {
        soundPool.release()
    }
}
