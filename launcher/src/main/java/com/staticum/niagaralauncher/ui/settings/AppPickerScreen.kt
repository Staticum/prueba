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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.staticum.niagaralauncher.data.AppInfo
import com.staticum.niagaralauncher.ui.theme.ColorPalette

/** A searchable multi-select list of apps, used for both "apps favoritas" and
 * "apps ocultas" instead of embedding the full app list inline in Settings. */
@Composable
fun AppPickerScreen(
    title: String,
    palette: ColorPalette,
    allApps: List<AppInfo>,
    selectedKeys: Set<String>,
    onToggle: (AppInfo, Boolean) -> Unit,
    onBack: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(allApps, query) {
        if (query.isBlank()) allApps else allApps.filter { it.label.contains(query, ignoreCase = true) }
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
            Text(title, color = palette.textPrimary, style = MaterialTheme.typography.titleLarge)
        }

        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.textPrimary),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(palette.textPrimary),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(
                            text = "Buscar app…",
                            color = palette.textSecondary,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    inner()
                },
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filtered, key = { it.key }) { app ->
                val selected = app.key in selectedKeys
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(app, !selected) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            bitmap = app.icon.toBitmap(width = 72, height = 72).asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                        )
                        Text(
                            text = app.label,
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (selected) palette.accent else palette.surface),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
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
