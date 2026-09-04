package com.tsoft.audiotranscribe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.tsoft.audiotranscribe.player.ClipPlayer
import com.tsoft.audiotranscribe.ui.MainScreen
import com.tsoft.audiotranscribe.ui.TranscribeViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TranscribeViewModel by viewModels()
    private lateinit var clipPlayer: ClipPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clipPlayer = ClipPlayer(applicationContext)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(viewModel = viewModel, clipPlayer = clipPlayer)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        clipPlayer.release()
    }
}
