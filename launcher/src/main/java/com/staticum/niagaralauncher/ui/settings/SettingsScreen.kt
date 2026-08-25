package com.staticum.niagaralauncher.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.staticum.niagaralauncher.data.LauncherPrefs
import com.staticum.niagaralauncher.data.ScreenTintMode
import com.staticum.niagaralauncher.ui.theme.ColorPalette
import com.staticum.niagaralauncher.update.UpdateUiState
import com.staticum.niagaralauncher.util.SoundOption
import com.staticum.niagaralauncher.util.TickPlayer
import com.staticum.niagaralauncher.widget.WidgetHostProvider

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    updateState: UpdateUiState,
    onBack: () -> Unit,
    onPaletteSelected: (ColorPalette) -> Unit,
    onPickWallpaper: () -> Unit,
    onClearWallpaper: () -> Unit,
    onIconSizeChange: (Float) -> Unit,
    onMonochromeChange: (Boolean) -> Unit,
    onScreenTintModeChange: (ScreenTintMode) -> Unit,
    onSoundSelected: (String) -> Unit,
    onSoundVolumeChange: (Float) -> Unit,
    onIndexWaveOffsetChange: (Float) -> Unit,
    onAddWidget: () -> Unit,
    onRemoveWidget: (Int) -> Unit,
    onMoveWidget: (Int, Int) -> Unit,
    onCheckForUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onOpenFavoritesPicker: () -> Unit,
    onOpenHiddenPicker: () -> Unit,
    onOpenColorPicker: () -> Unit,
    onShareCrashLog: () -> Unit,
    onClearCrashLog: () -> Unit,
    isDefaultLauncher: Boolean,
    onChangeDefaultLauncher: () -> Unit,
    onAmbientLockChange: (Boolean) -> Unit,
) {
    val palette = state.prefs.palette
    val context = LocalContext.current
    val previewPlayer = remember { TickPlayer(context) }
    DisposableEffect(Unit) { onDispose { previewPlayer.release() } }
    LaunchedEffect(state.prefs.soundVolume) { previewPlayer.setVolume(state.prefs.soundVolume) }
    var previewRequest by remember { mutableStateOf(0) }
    var previewOption by remember { mutableStateOf(SoundOption.DEFAULT) }
    LaunchedEffect(previewRequest) {
        if (previewRequest == 0) return@LaunchedEffect
        previewPlayer.setSound(previewOption)
        kotlinx.coroutines.delay(80)
        previewPlayer.play()
    }

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
            item { SectionTitle("Paleta de colores", palette.textSecondary, first = true) }
            item {
                LazyRow {
                    items(ColorPalette.PRESETS) { p ->
                        PaletteSwatch(
                            palette = p,
                            selected = p.id == palette.id,
                            onClick = { onPaletteSelected(p) },
                        )
                    }
                    item {
                        CustomPaletteSwatch(
                            selected = palette.id == ColorPalette.CUSTOM_ID,
                            currentAccent = if (palette.id == ColorPalette.CUSTOM_ID) palette.accent else null,
                            onClick = onOpenColorPicker,
                        )
                    }
                }
            }

            item { SectionTitle("Launcher predeterminado", palette.textSecondary) }
            item {
                Column {
                    Text(
                        text = if (isDefaultLauncher) {
                            "MinZen es tu launcher predeterminado"
                        } else {
                            "MinZen no es tu launcher predeterminado"
                        },
                        color = palette.textPrimary,
                    )
                    Text(
                        text = if (isDefaultLauncher) {
                            "Cambiar launcher predeterminado"
                        } else {
                            "Hacer predeterminado"
                        },
                        color = palette.accent,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clickable(onClick = onChangeDefaultLauncher),
                    )
                }
            }

            item { SectionTitle("Pantalla de bloqueo", palette.textSecondary) }
            item {
                Column {
                    Text(
                        text = "Muestra un reloj grande al abrir o volver a la app, antes de tus apps y widgets. No reemplaza el bloqueo real del teléfono (PIN/patrón/huella), que sigue activo aparte.",
                        color = palette.textSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Activar pantalla de bloqueo ambiente", color = palette.textPrimary)
                        Switch(checked = state.prefs.ambientLockEnabled, onCheckedChange = onAmbientLockChange)
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

            item { SectionTitle("Sonido de scroll / índice A-Z", palette.textSecondary) }
            item {
                Column {
                    SoundOption.entries.forEach { option ->
                        val selected = option.id == state.prefs.soundId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSoundSelected(option.id)
                                    previewOption = option
                                    previewRequest++
                                }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = option.label,
                                color = if (selected) palette.accent else palette.textPrimary,
                            )
                            if (selected) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = palette.accent)
                            }
                        }
                    }
                    Text(
                        text = "Volumen",
                        color = palette.textSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Slider(
                        value = state.prefs.soundVolume,
                        onValueChange = onSoundVolumeChange,
                        valueRange = 0f..1f,
                    )
                    Text(
                        text = "Alcance de la ola hacia la izquierda",
                        color = palette.textSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Slider(
                        value = state.prefs.indexWaveOffsetDp,
                        onValueChange = onIndexWaveOffsetChange,
                        valueRange = 0f..64f,
                    )
                }
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

            item { SectionTitle("Modo de pantalla", palette.textSecondary) }
            item {
                Column {
                    Text(
                        text = "Distinto del tinte de íconos: afecta toda la pantalla, widgets incluidos",
                        color = palette.textSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ScreenTintOption(
                            label = "Ninguno",
                            selected = state.prefs.screenTintMode == ScreenTintMode.NONE,
                            swatchColor = palette.background,
                            textColor = palette.textPrimary,
                            accentColor = palette.accent,
                            onClick = { onScreenTintModeChange(ScreenTintMode.NONE) },
                        )
                        ScreenTintOption(
                            label = "Grises",
                            selected = state.prefs.screenTintMode == ScreenTintMode.GRAYSCALE,
                            swatchColor = androidx.compose.ui.graphics.Color.Gray,
                            textColor = palette.textPrimary,
                            accentColor = palette.accent,
                            onClick = { onScreenTintModeChange(ScreenTintMode.GRAYSCALE) },
                        )
                        ScreenTintOption(
                            label = "Color",
                            selected = state.prefs.screenTintMode == ScreenTintMode.COLOR,
                            swatchColor = palette.accent,
                            textColor = palette.textPrimary,
                            accentColor = palette.accent,
                            onClick = { onScreenTintModeChange(ScreenTintMode.COLOR) },
                        )
                    }
                    if (state.prefs.screenTintMode == ScreenTintMode.COLOR) {
                        Text(
                            text = "Usa el color de acento de la paleta elegida arriba - cambia la paleta para cambiar el tono",
                            color = palette.textSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
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
                PickerSummaryRow(
                    description = "Aparecen siempre visibles debajo de los widgets, sin buscarlas",
                    countLabel = "${state.prefs.favoriteAppKeys.size} elegidas",
                    textColor = palette.textPrimary,
                    secondaryColor = palette.textSecondary,
                    accentColor = palette.accent,
                    onClick = onOpenFavoritesPicker,
                )
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

            item { SectionTitle("Diagnóstico (etapa de pruebas)", palette.textSecondary) }
            item {
                Column {
                    Text(
                        text = "Si la app se cierra sola, comparte este registro para poder revisarlo",
                        color = palette.textSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Compartir registro de errores",
                            color = palette.accent,
                            modifier = Modifier.clickable(onClick = onShareCrashLog),
                        )
                        Text(
                            text = "Borrar registro",
                            color = palette.accent,
                            modifier = Modifier.clickable(onClick = onClearCrashLog),
                        )
                    }
                }
            }

            item { SectionTitle("Apps ocultas", palette.textSecondary) }
            item {
                PickerSummaryRow(
                    description = "No aparecen en la lista principal ni en la búsqueda",
                    countLabel = "${state.prefs.hiddenApps.size} ocultas",
                    textColor = palette.textPrimary,
                    secondaryColor = palette.textSecondary,
                    accentColor = palette.accent,
                    onClick = onOpenHiddenPicker,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    first: Boolean = false,
) {
    Column {
        if (!first) {
            androidx.compose.material3.HorizontalDivider(
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.padding(top = 20.dp),
            )
        }
        Text(
            text = text.uppercase(),
            color = color,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = if (first) 4.dp else 16.dp, bottom = 10.dp),
        )
    }
}

@Composable
private fun PickerSummaryRow(
    description: String,
    countLabel: String,
    textColor: androidx.compose.ui.graphics.Color,
    secondaryColor: androidx.compose.ui.graphics.Color,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Column {
        Text(
            text = description,
            color = secondaryColor,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(countLabel, color = textColor)
            Text("Elegir", color = accentColor)
        }
    }
}

@Composable
private fun ScreenTintOption(
    label: String,
    selected: Boolean,
    swatchColor: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(swatchColor)
                .then(
                    if (selected) {
                        Modifier.border(2.dp, accentColor, CircleShape)
                    } else {
                        Modifier.border(1.dp, textColor.copy(alpha = 0.3f), CircleShape)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = accentColor)
            }
        }
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
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

/** Opens the RGB/HSV picker instead of applying a fixed color directly - shows a
 * rainbow ring when nothing custom is picked yet, or a solid swatch of the user's
 * current custom accent once they've chosen one. */
@Composable
private fun CustomPaletteSwatch(
    selected: Boolean,
    currentAccent: androidx.compose.ui.graphics.Color?,
    onClick: () -> Unit,
) {
    val rainbow = remember {
        androidx.compose.ui.graphics.Brush.sweepGradient(
            listOf(
                androidx.compose.ui.graphics.Color.Red,
                androidx.compose.ui.graphics.Color.Yellow,
                androidx.compose.ui.graphics.Color.Green,
                androidx.compose.ui.graphics.Color.Cyan,
                androidx.compose.ui.graphics.Color.Blue,
                androidx.compose.ui.graphics.Color.Magenta,
                androidx.compose.ui.graphics.Color.Red,
            ),
        )
    }
    Box(
        modifier = Modifier
            .padding(end = 12.dp)
            .size(56.dp)
            .clip(CircleShape)
            .then(
                if (currentAccent != null) Modifier.background(currentAccent) else Modifier.background(rainbow),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White)
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
