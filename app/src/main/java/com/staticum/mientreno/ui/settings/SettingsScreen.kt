package com.staticum.mientreno.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.staticum.mientreno.ui.theme.AppTheme

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val state = viewModel.updateState
    val currentTheme by viewModel.selectedTheme.collectAsState()

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("MiEntreno", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Versión ${viewModel.currentVersionName} (${viewModel.currentVersionCode})",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            item {
                Text(
                    "Apariencia",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(AppTheme.entries) { theme ->
                ThemeRow(
                    theme = theme,
                    selected = theme == currentTheme,
                    onClick = { viewModel.selectTheme(theme) }
                )
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 16.dp)
                ) {
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
    }
}

@Composable
private fun ThemeRow(theme: AppTheme, selected: Boolean, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                    ColorDot(theme.previewPrimary)
                    ColorDot(theme.previewSecondary)
                    ColorDot(theme.previewTertiary)
                }
                Text(theme.displayName, modifier = Modifier.padding(start = 16.dp))
            }
            RadioButton(selected = selected, onClick = null)
        }
    }
}

@Composable
private fun ColorDot(color: androidx.compose.ui.graphics.Color) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(22.dp)
            .background(color, CircleShape)
    )
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
