package com.staticum.niagaralauncher.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.staticum.niagaralauncher.ui.theme.ColorPalette
import com.staticum.niagaralauncher.util.SoundOption
import com.staticum.niagaralauncher.util.TickPlayer
import kotlinx.coroutines.delay

/**
 * The sound library as its own screen.
 *
 * These eleven options used to be rendered inline in Settings, which meant a third
 * of the whole settings screen was one radio list you scroll past every time. It's
 * now a single summary row that opens this, matching how favorite/hidden apps
 * already work - so Settings scans as a short list of topics again.
 */
@Composable
fun SoundPickerScreen(
    palette: ColorPalette,
    selectedId: String,
    volume: Float,
    onSelect: (String) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val previewPlayer = remember { TickPlayer(context) }
    DisposableEffect(Unit) { onDispose { previewPlayer.release() } }
    LaunchedEffect(volume) { previewPlayer.setVolume(volume) }

    var previewRequest by remember { mutableIntStateOf(0) }
    var previewOption by remember { mutableStateOf(SoundOption.fromId(selectedId)) }
    LaunchedEffect(previewRequest) {
        if (previewRequest == 0) return@LaunchedEffect
        previewPlayer.setSound(previewOption)
        delay(80)
        previewPlayer.play()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(horizontal = 16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = palette.textPrimary)
            }
            Text("Sonido", color = palette.textPrimary, style = MaterialTheme.typography.titleLarge)
        }

        SettingsCard(palette) {
            SettingsSliderRow(
                title = "Volumen",
                value = volume,
                valueRange = 0f..1f,
                palette = palette,
                onValueChange = onVolumeChange,
                valueLabel = { "${(it * 100).toInt()}%" },
            )
        }

        Text(
            text = "Toca un sonido para escucharlo y seleccionarlo",
            color = palette.textSecondary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 10.dp),
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(SoundOption.entries.toList(), key = { it.id }) { option ->
                val selected = option.id == selectedId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                        .background(
                            if (selected) palette.accent.copy(alpha = 0.14f) else cardSurface(palette),
                        )
                        .clickable {
                            onSelect(option.id)
                            previewOption = option
                            previewRequest++
                        }
                        .heightIn(min = 56.dp)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = option.label,
                        color = if (selected) palette.accent else palette.textPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(palette.accent),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Seleccionado",
                                tint = palette.background,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
