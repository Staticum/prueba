package com.staticum.niagaralauncher.ui.settings

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.staticum.niagaralauncher.data.AppInfo
import com.staticum.niagaralauncher.data.LauncherPrefs
import com.staticum.niagaralauncher.ui.theme.ColorPalette

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    allApps: List<AppInfo>,
    onBack: () -> Unit,
    onPaletteSelected: (ColorPalette) -> Unit,
    onPickWallpaper: () -> Unit,
    onClearWallpaper: () -> Unit,
    onIconSizeChange: (Float) -> Unit,
    onMonochromeChange: (Boolean) -> Unit,
    onToggleHidden: (AppInfo, Boolean) -> Unit,
    onAddWidget: () -> Unit,
    onRemoveWidget: (Int) -> Unit,
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
                    state.widgetIds.forEach { id ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Widget #$id", color = palette.textPrimary)
                            Text(
                                "Quitar",
                                color = palette.accent,
                                modifier = Modifier.clickable { onRemoveWidget(id) },
                            )
                        }
                    }
                    Text(
                        text = "+ Añadir widget",
                        color = palette.accent,
                        modifier = Modifier.padding(vertical = 8.dp).clickable(onClick = onAddWidget),
                    )
                }
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
