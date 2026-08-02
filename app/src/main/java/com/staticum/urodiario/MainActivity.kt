package com.staticum.urodiario

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.staticum.urodiario.ui.navigation.UroDiarioNavHost
import com.staticum.urodiario.ui.theme.UroDiarioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = (application as UroDiarioApplication).repository

        setContent {
            UroDiarioTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    UroDiarioNavHost(repository = repository)
                }
            }
        }
    }
}
