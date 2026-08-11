package com.staticum.mientreno.ui.workout

import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

class WorkoutCoach(private val tts: TextToSpeech) {
    fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}

@Composable
fun rememberWorkoutCoach(): State<WorkoutCoach?> {
    val context = LocalContext.current
    val coachState = remember { mutableStateOf<WorkoutCoach?>(null) }

    DisposableEffect(Unit) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = Locale("es", "ES")
                coachState.value = engine?.let { WorkoutCoach(it) }
            }
        }
        onDispose {
            coachState.value?.shutdown()
            coachState.value = null
        }
    }

    return coachState
}
