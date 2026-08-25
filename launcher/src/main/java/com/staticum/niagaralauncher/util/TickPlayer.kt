package com.staticum.niagaralauncher.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/** Plays a short, user-selectable one-shot sound for scroll/index feedback,
 * independent of the system "touch sounds" setting (which only gates
 * [android.view.View.playSoundEffect]). Call [setSound] when the chosen
 * [SoundOption] changes and [setVolume] when the volume preference changes. */
class TickPlayer(private val context: Context) {

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private var loadedResId: Int = -1
    private var soundId: Int = -1
    @Volatile private var isLoaded = false
    private var volume = 0.5f

    fun setSound(option: SoundOption) {
        if (option.resId == loadedResId) return
        isLoaded = false
        loadedResId = option.resId
        if (option.resId == -1) return
        soundId = soundPool.load(context, option.resId, 1)
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (sampleId == soundId && status == 0) isLoaded = true
        }
    }

    fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
    }

    fun play() {
        if (isLoaded) soundPool.play(soundId, volume, volume, 0, 0, 1f)
    }

    fun release() {
        soundPool.release()
    }
}
