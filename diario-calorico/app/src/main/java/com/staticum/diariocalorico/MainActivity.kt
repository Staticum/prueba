package com.staticum.diariocalorico

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.staticum.diariocalorico.ui.navigation.DiarioCaloricoNavHost
import com.staticum.diariocalorico.ui.theme.DiarioCaloricoTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        setContent {
            DiarioCaloricoTheme {
                DiarioCaloricoNavHost()
            }
        }
    }

    /**
     * Necesaria para poder avisar cuando el reintento en segundo plano de un análisis
     * pendiente de Gemini finalmente tiene éxito. Sin este permiso (Android 13+), el
     * reintento igual ocurre, solo que sin notificación.
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
