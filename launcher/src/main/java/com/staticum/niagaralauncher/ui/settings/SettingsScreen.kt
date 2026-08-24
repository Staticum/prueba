package com.staticum.niagaralauncher.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.staticum.niagaralauncher.data.AppInfo
import com.staticum.niagaralauncher.data.LauncherPrefs
import com.staticum.niagaralauncher.ui.theme.ColorPalette
import com.staticum.niagaralauncher.update.UpdateUiState
import com.staticum.niagaralauncher.widget.WidgetHostProvider

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    allApps: List<AppInfo>,
    updateState: UpdateUiState,
    onBack: () -> Unit,
    onPaletteSelected: (ColorPalette) -> Unit,
    onPickWallpaper: () -> Unit,
    onClearWallpaper: () -> Unit,
    onIconSizeChange: (Float) -> Unit,
    onMonochromeChange: (Boolean) -> Unit,
    onToggleHidden: (AppInfo, Boolean) -> Unit,
    onAddWidget: () -> Unit,
    onRemoveWidget: (Int) -> Unit,
    onMoveWidget: (Int, Int) -> Unit,
    onCheckForUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit,
) {
    val palette = state.prefs.palette

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = palette.textPrimary)
            }
            Text("Ajustes", color = palette.textPrimary, style = MaterialTheme.typography.titleLarge)
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { SectionTitle("Paleta de colores", palette.textSecondary) }
            item {
                LazyRow {
                    items(ColorPalette.entries) { p ->
                        PaletteSwatch(
                            palette = p,
                            selected = p == palette,
                            onClick = { onPaletteSelected(p) },
                        )
                    }
                }
            }

            item { SectionTitle("Fondo de pantalla", palette.textSecondary) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Usar imagen como fondo", color = palette.textPrimary)
                    Switch(
                        checked = state.prefs.useWallpaper,
                        onCheckedChange = { checked ->
                            if (checked) onPickWallpaper() else onClearWallpaper()
                        },
                    )
                }
                if (state.prefs.useWallpaper) {
                    Text(
                        text = "Toca el interruptor de nuevo para elegir otra imagen",
                        color = palette.textSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.clickable(onClick = onPickWallpaper),
                    )
                }
            }

            item { SectionTitle("Tamaño de iconos", palette.textSecondary) }
            item {
                Slider(
                    value = state.prefs.iconSizeFactor,
                    onValueChange = onIconSizeChange,
                    valueRange = 0.6f..1.8f,
                )
            }

            item { SectionTitle("Iconos monocromáticos", palette.textSecondary) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Aplicar tinte de acento a los iconos", color = palette.textPrimary)
                    Switch(checked = state.prefs.monochromeIcons, onCheckedChange = onMonochromeChange)
                }
            }

            item { SectionTitle("Widgets", palette.textSecondary) }
            item {
                Column {
                    state.widgetIds.forEachIndexed { index, id ->
                        WidgetRow(
                            id = id,
                            canMoveUp = index > 0,
                            canMoveDown = index < state.widgetIds.lastIndex,
                            textColor = palette.textPrimary,
                            accentColor = palette.accent,
                            onMoveUp = { onMoveWidget(id, -1) },
                            onMoveDown = { onMoveWidget(id, 1) },
                            onRemove = { onRemoveWidget(id) },
                        )
                    }
                    Text(
                        text = "+ Añadir widget",
                        color = palette.accent,
                        modifier = Modifier.padding(vertical = 8.dp).clickable(onClick = onAddWidget),
                    )
                }
            }

            item { SectionTitle("Apps favoritas", palette.textSecondary) }
            item {
                Text(
                    text = "Aparecen siempre visibles debajo de los widgets, sin buscarlas",
                    color = palette.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            items(allApps, key = { "fav_${it.key}" }) { app ->
                val favorite = app.key in state.prefs.favoriteAppKeys
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(app.label, color = if (favorite) palette.textPrimary else palette.textSecondary)
                    Text(
                        text = if (favorite) "Quitar" else "Agregar",
                        color = palette.accent,
                        modifier = Modifier.clickable { onToggleFavorite(app.key, !favorite) },
                    )
                }
            }

            item { SectionTitle("Actualizaciones", palette.textSecondary) }
            item {
                UpdateSection(
                    updateState = updateState,
                    accent = palette.accent,
                    textPrimary = palette.textPrimary,
                    textSecondary = palette.textSecondary,
                    onCheckForUpdate = onCheckForUpdate,
                    onDownloadUpdate = onDownloadUpdate,
                    onInstallUpdate = onInstallUpdate,
                )
            }

            item { SectionTitle("Apps ocultas", palette.textSecondary) }
            items(allApps, key = { it.key }) { app ->
                val hidden = app.key in state.prefs.hiddenApps
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(app.label, color = if (hidden) palette.textSecondary else palette.textPrimary)
                    Text(
                        text = if (hidden) "Mostrar" else "Ocultar",
                        color = palette.accent,
                        modifier = Modifier.clickable { onToggleHidden(app, !hidden) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun WidgetRow(
    id: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    textColor: androidx.compose.ui.graphics.Color,
    accentColor: androidx.compose.ui.graphics.Color,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    val providerInfo = remember(id) { WidgetHostProvider.manager(context).getAppWidgetInfo(id) }
    val label = providerInfo?.loadLabel(context.packageManager) ?: "Widget"
    val icon = remember(id, providerInfo) {
        providerInfo?.let { info -> runCatching { info.loadIcon(context, 0) }.getOrNull() }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Image(
                    bitmap = icon.toBitmap(width = 72, height = 72).asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(label, color = textColor, modifier = Modifier.padding(start = 8.dp))
        }
        Row {
            if (canMoveUp) {
                Text("▲", color = accentColor, modifier = Modifier.padding(horizontal = 8.dp).clickable(onClick = onMoveUp))
            }
            if (canMoveDown) {
                Text("▼", color = accentColor, modifier = Modifier.padding(horizontal = 8.dp).clickable(onClick = onMoveDown))
            }
            Text("Quitar", color = accentColor, modifier = Modifier.clickable(onClick = onRemove))
        }
    }
}

@Composable
private fun UpdateSection(
    updateState: UpdateUiState,
    accent: androidx.compose.ui.graphics.Color,
    textPrimary: androidx.compose.ui.graphics.Color,
    textSecondary: androidx.compose.ui.graphics.Color,
    onCheckForUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
) {
    Column {
        when (updateState) {
            is UpdateUiState.Idle -> {
                Text(
                    text = "Buscar actualización",
                    color = accent,
                    modifier = Modifier.clickable(onClick = onCheckForUpdate),
                )
            }

            is UpdateUiState.Checking -> {
                Text("Buscando actualizaciones…", color = textSecondary)
            }

            is UpdateUiState.UpToDate -> {
                Text("Ya tienes la última versión", color = textSecondary)
                Text(
                    text = "Volver a comprobar",
                    color = accent,
                    modifier = Modifier.padding(top = 4.dp).clickable(onClick = onCheckForUpdate),
                )
            }

            is UpdateUiState.Available -> {
                Text("Nueva versión disponible: ${updateState.info.versionName}", color = textPrimary)
                Text(
                    text = "Descargar e instalar",
                    color = accent,
                    modifier = Modifier.padding(top = 4.dp).clickable(onClick = onDownloadUpdate),
                )
            }

            is UpdateUiState.Downloading -> {
                Text(
                    text = "Descargando… ${(updateState.progress * 100).toInt()}%",
                    color = textPrimary,
                )
            }

            is UpdateUiState.ReadyToInstall -> {
                Text("Descarga lista", color = textPrimary)
                Text(
                    text = "Instalar ahora",
                    color = accent,
                    modifier = Modifier.padding(top = 4.dp).clickable(onClick = onInstallUpdate),
                )
            }

            is UpdateUiState.Failed -> {
                Text("Error: ${updateState.message}", color = textSecondary)
                Text(
                    text = "Reintentar",
                    color = accent,
                    modifier = Modifier.padding(top = 4.dp).clickable(onClick = onCheckForUpdate),
                )
            }
        }
    }
}

@Composable
private fun PaletteSwatch(palette: ColorPalette, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(end = 12.dp)
            .size(56.dp)
            .clip(CircleShape)
            .background(palette.background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = palette.accent)
        }
    }
}
