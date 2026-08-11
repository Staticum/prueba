package com.staticum.mientreno.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val state = viewModel.updateState

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("MiEntreno", style = MaterialTheme.typography.titleLarge)
            Text(
                "Versión ${viewModel.currentVersionName} (${viewModel.currentVersionCode})",
                style = MaterialTheme.typography.bodyLarge
            )

            when (state) {
                is UpdateState.Idle -> {
                    Button(onClick = { viewModel.checkForUpdate() }) {
                        Text("Buscar actualización")
                    }
                }

                is UpdateState.Checking -> {
                    Text("Buscando actualizaciones…")
                }

                is UpdateState.UpToDate -> {
                    Text("Ya tienes la última versión.")
                    OutlinedButton(onClick = { viewModel.checkForUpdate() }) {
                        Text("Volver a buscar")
                    }
                }

                is UpdateState.Available -> {
                    Text("Nueva versión disponible: ${state.info.versionName}")
                    state.info.notes?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                    Button(onClick = { viewModel.downloadAndInstall(state.info) }) {
                        Text("Descargar e instalar")
                    }
                }

                is UpdateState.Downloading -> {
                    Text("Descargando actualización…")
                }

                is UpdateState.ReadyToInstall -> {
                    LaunchedEffect(state.uri) {
                        launchInstall(context.packageName, state.uri, context)
                    }
                    Text("Abriendo instalador…")
                    OutlinedButton(onClick = { viewModel.resetToIdle() }) {
                        Text("Listo")
                    }
                }

                is UpdateState.Error -> {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = { viewModel.checkForUpdate() }) {
                        Text("Reintentar")
                    }
                }
            }
        }
    }
}

private fun launchInstall(packageName: String, uri: Uri, context: android.content.Context) {
    if (!context.packageManager.canRequestPackageInstalls()) {
        val permissionIntent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:$packageName")
        )
        context.startActivity(permissionIntent)
        return
    }
    val installIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(installIntent)
}
