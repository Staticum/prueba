package com.staticum.diariocalorico

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.staticum.diariocalorico.ui.navigation.DiarioCaloricoNavHost
import com.staticum.diariocalorico.ui.theme.DiarioCaloricoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DiarioCaloricoTheme {
                DiarioCaloricoNavHost()
            }
        }
    }
}
