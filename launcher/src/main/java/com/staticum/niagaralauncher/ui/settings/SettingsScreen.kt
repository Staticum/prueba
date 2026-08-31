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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.staticum.niagaralauncher.data.ScreenTintMode
import com.staticum.niagaralauncher.data.WidgetBackground
import com.staticum.niagaralauncher.ui.theme.ColorPalette
import com.staticum.niagaralauncher.update.UpdateUiState
import com.staticum.niagaralauncher.util.SoundOption
import com.staticum.niagaralauncher.widget.WidgetHostProvider

/**
 * Settings, organised into four named areas instead of one flat list of a dozen
 * equally-weighted sections in arbitrary order.
 *
 * What moved and why:
 *  - "Alcance de la ola" lived inside the *sound* section but controls the A-Z
 *    index, so it now sits with the other list/index settings.
 *  - "Apps ocultas" was the very last section, far below "Apps favoritas" and
 *    separated from it by Updates and Diagnostics; the two app pickers are now
 *    adjacent, since they're the same kind of decision.
 *  - The 11-row sound list became one row that opens its own screen.
 *  - Every action that used to be bare colored text (add widget, check for
 *    updates, share log, change default launcher…) is now a real button or a
 *    full-height row.
 */
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
    onIconSilhouetteChange: (Boolean) -> Unit,
    onFontFamilyChange: (String) -> Unit,
    onZenQuoteScrollsChange: (Boolean) -> Unit,
    onScreenTintModeChange: (ScreenTintMode) -> Unit,
    onIndexWaveOffsetChange: (Float) -> Unit,
    onHomeResetSecondsChange: (Int) -> Unit,
    onAddWidget: () -> Unit,
    onRemoveWidget: (Int) -> Unit,
    onCheckForUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onOpenFavoritesPicker: () -> Unit,
    onOpenHiddenPicker: () -> Unit,
    onOpenColorPicker: () -> Unit,
    onOpenSoundPicker: () -> Unit,
    onShareCrashLog: () -> Unit,
    onClearCrashLog: () -> Unit,
    isDefaultLauncher: Boolean,
    onChangeDefaultLauncher: () -> Unit,
    onAmbientLockChange: (Boolean) -> Unit,
    onWidgetBackgroundChange: (WidgetBackground) -> Unit,
    onFrequentsEnabledChange: (Boolean) -> Unit,
    onFrequentsCountChange: (Int) -> Unit,
    onUseSystemUsageChange: (Boolean) -> Unit,
    hasUsageAccess: Boolean,
    onOpenUsageAccess: () -> Unit,
    onClearUsageHistory: () -> Unit,
) {
    val palette = state.prefs.palette

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = palette.textPrimary)
            }
            Text(
                text = "Ajustes",
                color = palette.textPrimary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        ) {
            // ---------------------------------------------------------------- APARIENCIA
            item { SettingsSectionHeader("Apariencia", palette, first = true) }

            item {
                SettingsCard(palette) {
                    Text(
                        text = "Tema",
                        color = palette.textPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 20.dp, top = 16.dp),
                    )
                    LazyRow(
                        modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 16.dp),
                    ) {
                        items(ColorPalette.PRESETS) { p ->
                            PaletteSwatch(
                                palette = p,
                                selected = p.id == palette.id,
                                themePalette = palette,
                                onClick = { onPaletteSelected(p) },
                            )
                        }
                        item {
                            CustomPaletteSwatch(
                                selected = palette.id == ColorPalette.CUSTOM_ID,
                                currentAccent = if (palette.id == ColorPalette.CUSTOM_ID) palette.accent else null,
                                themePalette = palette,
                                onClick = onOpenColorPicker,
                            )
                        }
                    }

                    SettingsCardDivider(palette)
                    SettingsSwitchRow(
                        title = "Fondo con imagen",
                        subtitle = if (state.prefs.useWallpaper) {
                            "Activo · toca aquí para elegir otra imagen"
                        } else {
                            "Usa una foto tuya como fondo del launcher"
                        },
                        checked = state.prefs.useWallpaper,
                        palette = palette,
                        onCheckedChange = { checked ->
                            if (checked) onPickWallpaper() else onClearWallpaper()
                        },
                    )

                    SettingsCardDivider(palette)
                    SettingsSliderRow(
                        title = "Tamaño de iconos",
                        value = state.prefs.iconSizeFactor,
                        valueRange = 0.6f..1.8f,
                        palette = palette,
                        onValueChange = onIconSizeChange,
                        valueLabel = { "${(it * 100).toInt()}%" },
                    )

                    SettingsCardDivider(palette)
                    SettingsSwitchRow(
                        title = "Iconos monocromáticos",
                        subtitle = "Tiñe los iconos de apps con el color de acento",
                        checked = state.prefs.monochromeIcons,
                        palette = palette,
                        onCheckedChange = onMonochromeChange,
                    )
                    SettingsCardDivider(palette)
                    SettingsSwitchRow(
                        title = "Iconos por silueta",
                        subtitle = "Reduce cada ícono a su forma dominante en un solo color. Más radical que el monocromático: algunos íconos con degradados o poco contraste pueden salir irreconocibles",
                        checked = state.prefs.iconSilhouette,
                        palette = palette,
                        onCheckedChange = onIconSilhouetteChange,
                    )
                    SettingsCardDivider(palette)
                    Text(
                        text = "Tipografía",
                        color = palette.textSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp),
                    )
                    com.staticum.niagaralauncher.ui.theme.AppFonts.OPTIONS.forEachIndexed { index, (id, label) ->
                        SettingsRow(
                            title = label,
                            palette = palette,
                            onClick = { onFontFamilyChange(id) },
                            trailing = {
                                if (state.prefs.fontFamilyId == id) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = palette.accent)
                                }
                            },
                        )
                    }
                    SettingsCardDivider(palette)
                    SettingsSwitchRow(
                        title = "Frase zen en movimiento",
                        subtitle = "Si la apagas, la frase queda fija arriba en vez de desplazarse",
                        checked = state.prefs.zenQuoteScrolls,
                        palette = palette,
                        onCheckedChange = onZenQuoteScrollsChange,
                    )
                }
            }

            item { SettingsSectionHeader("Modo de pantalla", palette) }
            item {
                SettingsCard(palette) {
                    Text(
                        text = "Afecta toda la pantalla, widgets del sistema incluidos — distinto del tinte de iconos",
                        color = palette.textSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 18.dp),
                    ) {
                        ScreenTintOption(
                            label = "Ninguno",
                            selected = state.prefs.screenTintMode == ScreenTintMode.NONE,
                            swatchColor = palette.background,
                            palette = palette,
                            onClick = { onScreenTintModeChange(ScreenTintMode.NONE) },
                        )
                        ScreenTintOption(
                            label = "Grises",
                            selected = state.prefs.screenTintMode == ScreenTintMode.GRAYSCALE,
                            swatchColor = androidx.compose.ui.graphics.Color.Gray,
                            palette = palette,
                            onClick = { onScreenTintModeChange(ScreenTintMode.GRAYSCALE) },
                        )
                        ScreenTintOption(
                            label = "Color",
                            selected = state.prefs.screenTintMode == ScreenTintMode.COLOR,
                            swatchColor = palette.accent,
                            palette = palette,
                            onClick = { onScreenTintModeChange(ScreenTintMode.COLOR) },
                        )
                    }
                }
            }

            // ------------------------------------------------------- PANTALLA DE INICIO
            item { SettingsSectionHeader("Pantalla de inicio", palette) }
            item {
                SettingsCard(palette) {
                    SettingsNavRow(
                        title = "Apps favoritas",
                        subtitle = "Fila de accesos siempre visible, sin buscar",
                        value = "${state.prefs.favoriteAppKeys.size}",
                        palette = palette,
                        onClick = onOpenFavoritesPicker,
                    )
                    SettingsCardDivider(palette)
                    SettingsNavRow(
                        title = "Apps ocultas",
                        subtitle = "No aparecen en la lista ni en la búsqueda",
                        value = "${state.prefs.hiddenApps.size}",
                        palette = palette,
                        onClick = onOpenHiddenPicker,
                    )
                    SettingsCardDivider(palette)
                    SettingsSwitchRow(
                        title = "Pantalla de bloqueo ambiente",
                        subtitle = "Reloj a pantalla completa al abrir la app. No reemplaza el bloqueo del teléfono.",
                        checked = state.prefs.ambientLockEnabled,
                        palette = palette,
                        onCheckedChange = onAmbientLockChange,
                    )
                }
            }

            item { SettingsSectionHeader("Widgets", palette) }
            item {
                SettingsCard(palette) {
                    if (state.widgetIds.isEmpty()) {
                        Text(
                            text = "Aún no has añadido ningún widget",
                            color = palette.textSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp),
                        )
                    } else {
                        state.widgetIds.forEachIndexed { index, id ->
                            if (index > 0) SettingsCardDivider(palette)
                            WidgetRow(
                                id = id,
                                palette = palette,
                                onRemove = { onRemoveWidget(id) },
                            )
                        }
                    }
                    SettingsButton(
                        text = "Añadir widget",
                        palette = palette,
                        onClick = onAddWidget,
                        filled = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                    )

                    SettingsCardDivider(palette)
                    Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 18.dp)) {
                        Text(
                            text = "Fondo del widget",
                            color = palette.textPrimary,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = "Muchos widgets son transparentes y esperan que el launcher aporte contraste. Añade un fondo si no se leen bien sobre tu fondo de pantalla.",
                            color = palette.textSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 3.dp, bottom = 14.dp),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            WidgetBackground.entries.forEach { option ->
                                WidgetBackgroundOption(
                                    label = when (option) {
                                        WidgetBackground.NONE -> "Ninguno"
                                        WidgetBackground.SUBTLE -> "Sutil"
                                        WidgetBackground.SOLID -> "Sólido"
                                    },
                                    selected = state.prefs.widgetBackground == option,
                                    fill = when (option) {
                                        WidgetBackground.NONE -> androidx.compose.ui.graphics.Color.Transparent
                                        WidgetBackground.SUBTLE -> palette.textPrimary.copy(alpha = 0.07f)
                                        WidgetBackground.SOLID -> palette.surface
                                    },
                                    palette = palette,
                                    onClick = { onWidgetBackgroundChange(option) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }

            // ------------------------------------------------------------- INTERACCIÓN
            // ----------------------------------------------------------- FRECUENTES
            item { SettingsSectionHeader("Apps frecuentes", palette) }
            item {
                SettingsCard(palette) {
                    SettingsSwitchRow(
                        title = "Mostrar frecuentes",
                        subtitle = "Muestra arriba de la lista las apps que más usas últimamente. Todo se guarda en el teléfono, no sale del dispositivo",
                        checked = state.prefs.frequentsEnabled,
                        palette = palette,
                        onCheckedChange = onFrequentsEnabledChange,
                    )
                    if (state.prefs.frequentsEnabled) {
                        SettingsCardDivider(palette)
                        SettingsSliderRow(
                            title = "Cuántas mostrar",
                            value = state.prefs.frequentsCount.toFloat(),
                            valueRange = 4f..10f,
                            palette = palette,
                            onValueChange = { onFrequentsCountChange(it.toInt()) },
                            valueLabel = { "${it.toInt()} apps" },
                        )
                        SettingsCardDivider(palette)
                        SettingsSwitchRow(
                            title = "Usar estadísticas del sistema",
                            subtitle = if (hasUsageAccess) {
                                "Acceso concedido. Cuenta también las apps que abres desde una notificación, no solo desde MinZen"
                            } else {
                                "Requiere Acceso de uso. Sin él solo se cuentan las apps abiertas desde MinZen, así que la mensajería queda subestimada"
                            },
                            checked = state.prefs.useSystemUsageStats,
                            palette = palette,
                            onCheckedChange = onUseSystemUsageChange,
                        )
                        if (state.prefs.useSystemUsageStats && !hasUsageAccess) {
                            SettingsCardDivider(palette)
                            SettingsRow(
                                title = "Conceder Acceso de uso",
                                subtitle = "Android no permite pedirlo con un diálogo: hay que activarlo a mano",
                                palette = palette,
                                onClick = onOpenUsageAccess,
                            )
                        }
                        SettingsCardDivider(palette)
                        SettingsRow(
                            title = "Borrar historial de uso",
                            subtitle = "Vacía el contador propio de MinZen",
                            palette = palette,
                            onClick = onClearUsageHistory,
                        )
                    }
                }
            }

            item { SettingsSectionHeader("Lista de apps e índice A-Z", palette) }
            item {
                SettingsCard(palette) {
                    SettingsNavRow(
                        title = "Sonido",
                        subtitle = "Se escucha al desplazar la lista o recorrer el índice",
                        value = SoundOption.fromId(state.prefs.soundId).label,
                        palette = palette,
                        onClick = onOpenSoundPicker,
                    )
                    SettingsCardDivider(palette)
                    SettingsSliderRow(
                        title = "Alcance de la onda",
                        subtitle = "Cuánto se desplazan las letras hacia la izquierda al arrastrar el índice, para que tu dedo no las tape",
                        value = state.prefs.indexWaveOffsetDp,
                        valueRange = 0f..64f,
                        palette = palette,
                        onValueChange = onIndexWaveOffsetChange,
                        valueLabel = { "${it.toInt()} dp" },
                    )
                    SettingsCardDivider(palette)
                    SettingsSliderRow(
                        title = "Volver al inicio tras inactividad",
                        subtitle = "Si buscas, filtras por letra o te desplazas por la lista y dejas de tocar la pantalla, vuelve sola al inicio con Frecuentes",
                        value = state.prefs.homeResetSeconds.toFloat(),
                        valueRange = 5f..60f,
                        palette = palette,
                        onValueChange = { onHomeResetSecondsChange(it.toInt()) },
                        valueLabel = { "${it.toInt()} s" },
                    )
                }
            }

            // ------------------------------------------------------------------ SISTEMA
            item { SettingsSectionHeader("Sistema", palette) }
            item {
                SettingsCard(palette) {
                    SettingsRow(
                        title = "Launcher predeterminado",
                        subtitle = if (isDefaultLauncher) {
                            "MinZen es tu pantalla de inicio actual"
                        } else {
                            "Actualmente estás usando otro launcher"
                        },
                        palette = palette,
                        trailing = {
                            SettingsButton(
                                text = if (isDefaultLauncher) "Cambiar" else "Activar",
                                palette = palette,
                                onClick = onChangeDefaultLauncher,
                                filled = !isDefaultLauncher,
                            )
                        },
                    )
                    SettingsCardDivider(palette)
                    UpdateSection(
                        updateState = updateState,
                        palette = palette,
                        onCheckForUpdate = onCheckForUpdate,
                        onDownloadUpdate = onDownloadUpdate,
                        onInstallUpdate = onInstallUpdate,
                    )
                }
            }

            item { SettingsSectionHeader("Diagnóstico", palette) }
            item {
                SettingsCard(palette) {
                    Text(
                        text = "Si la app se cierra sola, comparte este registro para poder revisarlo.",
                        color = palette.textSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 18.dp),
                    ) {
                        SettingsButton(
                            text = "Compartir registro",
                            palette = palette,
                            onClick = onShareCrashLog,
                            modifier = Modifier.weight(1f),
                        )
                        SettingsButton(
                            text = "Borrar",
                            palette = palette,
                            onClick = onClearCrashLog,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            item { Box(modifier = Modifier.size(40.dp)) }
        }
    }
}

@Composable
private fun WidgetBackgroundOption(
    label: String,
    selected: Boolean,
    fill: androidx.compose.ui.graphics.Color,
    palette: ColorPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(fill)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) palette.accent else palette.textPrimary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(10.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Text(
            text = label,
            color = if (selected) palette.accent else palette.textSecondary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun ScreenTintOption(
    label: String,
    selected: Boolean,
    swatchColor: androidx.compose.ui.graphics.Color,
    palette: ColorPalette,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(swatchColor)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) palette.accent else palette.textPrimary.copy(alpha = 0.25f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = palette.accent)
            }
        }
        Text(
            text = label,
            color = if (selected) palette.accent else palette.textSecondary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 7.dp),
        )
    }
}

@Composable
private fun WidgetRow(
    id: Int,
    palette: ColorPalette,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    val providerInfo = remember(id) { WidgetHostProvider.manager(context).getAppWidgetInfo(id) }
    val label = providerInfo?.loadLabel(context.packageManager) ?: "Widget no disponible"
    val icon = remember(id, providerInfo) {
        providerInfo?.let { info -> runCatching { info.loadIcon(context, 0) }.getOrNull() }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Image(
                bitmap = icon.toBitmap(width = 84, height = 84).asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            text = label,
            color = palette.textPrimary,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = if (icon != null) 14.dp else 0.dp, end = 8.dp),
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Close, contentDescription = "Quitar", tint = palette.textSecondary)
        }
    }
}

@Composable
private fun UpdateSection(
    updateState: UpdateUiState,
    palette: ColorPalette,
    onCheckForUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
) {
    when (updateState) {
        is UpdateUiState.Idle -> SettingsRow(
            title = "Actualizaciones",
            subtitle = "Busca una versión más reciente",
            palette = palette,
            trailing = { SettingsButton("Buscar", palette, onCheckForUpdate) },
        )

        is UpdateUiState.Checking -> SettingsRow(
            title = "Actualizaciones",
            subtitle = "Buscando…",
            palette = palette,
        )

        is UpdateUiState.UpToDate -> SettingsRow(
            title = "Actualizaciones",
            subtitle = "Ya tienes la última versión",
            palette = palette,
            trailing = { SettingsButton("Reintentar", palette, onCheckForUpdate) },
        )

        is UpdateUiState.Available -> SettingsRow(
            title = "Actualización disponible",
            subtitle = "Versión ${updateState.info.versionName}",
            palette = palette,
            trailing = { SettingsButton("Descargar", palette, onDownloadUpdate, filled = true) },
        )

        is UpdateUiState.Downloading -> Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Descargando…", color = palette.textPrimary, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "${(updateState.progress * 100).toInt()}%",
                    color = palette.accent,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            LinearProgressIndicator(
                progress = { updateState.progress },
                color = palette.accent,
                trackColor = palette.textPrimary.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
        }

        is UpdateUiState.ReadyToInstall -> SettingsRow(
            title = "Descarga lista",
            subtitle = "Toca instalar para aplicar la actualización",
            palette = palette,
            trailing = { SettingsButton("Instalar", palette, onInstallUpdate, filled = true) },
        )

        is UpdateUiState.Failed -> SettingsRow(
            title = "Actualizaciones",
            subtitle = "Error: ${updateState.message}",
            palette = palette,
            trailing = { SettingsButton("Reintentar", palette, onCheckForUpdate) },
        )
    }
}

/** Opens the RGB/HSV picker instead of applying a fixed color directly - shows a
 * rainbow ring when nothing custom is picked yet, or a solid swatch of the user's
 * current custom accent once they've chosen one. */
@Composable
private fun CustomPaletteSwatch(
    selected: Boolean,
    currentAccent: androidx.compose.ui.graphics.Color?,
    themePalette: ColorPalette,
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(end = 14.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .then(
                    if (currentAccent != null) Modifier.background(currentAccent) else Modifier.background(rainbow),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(24.dp),
                )
            } else if (currentAccent == null) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Text(
            text = "Personalizado",
            color = if (selected) themePalette.accent else themePalette.textSecondary,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            modifier = Modifier.padding(top = 7.dp),
        )
    }
}
