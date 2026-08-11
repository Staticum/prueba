package com.staticum.mientreno

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.staticum.mientreno.ui.navigation.MiEntrenoNavHost
import com.staticum.mientreno.ui.theme.MiEntrenoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = (application as MiEntrenoApplication).repository

        setContent {
            MiEntrenoTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MiEntrenoNavHost(repository = repository)
                }
            }
        }
    }
}
