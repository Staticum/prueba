package com.staticum.mientreno.ui.workout

import android.speech.tts.TextToSpeech

class WorkoutCoach(private val tts: TextToSpeech) {
    fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
