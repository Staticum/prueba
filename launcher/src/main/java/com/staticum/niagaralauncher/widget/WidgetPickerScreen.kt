package com.staticum.niagaralauncher.widget

import android.appwidget.AppWidgetProviderInfo
import android.graphics.drawable.Drawable
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.staticum.niagaralauncher.ui.theme.ColorPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One provider with everything already resolved off the main thread.
 *
 * The old screen called `loadLabel`/`loadIcon` from inside composition, once per
 * visible row - each of those is a PackageManager round trip, on a list that is
 * routinely 60-150 entries long.
 */
private data class WidgetChoice(
    val info: AppWidgetProviderInfo,
    val key: String,
    val label: String,
    val appLabel: String,
    val preview: ImageBitmap?,
    val icon: ImageBitmap?,
    val sizeLabel: String,
)

/** Android's standard cell formula: a widget declaring 250dp wide occupies 4 cells. */
private fun cellsFor(sizeDp: Int): Int = ((sizeDp - 30) / 70 + 1).coerceAtLeast(1)

private fun Drawable.toImageBitmapOrNull(maxPx: Int): ImageBitmap? = runCatching {
    val w = intrinsicWidth.takeIf { it > 0 } ?: maxPx
    val h = intrinsicHeight.takeIf { it > 0 } ?: maxPx
    val scale = minOf(maxPx.toFloat() / w, maxPx.toFloat() / h, 1f)
    toBitmap(
        width = (w * scale).toInt().coerceAtLeast(1),
        height = (h * scale).toInt().coerceAtLeast(1),
    ).asImageBitmap()
}.getOrNull()

/**
 * Widget picker with search, grouping and real previews.
 *
 * Previously this was a flat, unsorted list showing only the owning app's icon and
 * the widget's label - so widgets from the same app were scattered, there was no way
 * to search, and no way to tell what a widget would actually look like before adding
 * it. Now the list is sorted by app then widget, headed by app name, filterable, and
 * each row shows the provider's own preview image plus its declared grid size.
 */
@Composable
fun WidgetPickerScreen(
    palette: ColorPalette,
    onBack: () -> Unit,
    onProviderSelected: (AppWidgetProviderInfo) -> Unit,
) {
    val context = LocalContext.current
    var choices by remember { mutableStateOf<List<WidgetChoice>?>(null) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        choices = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            WidgetHostProvider.manager(context).installedProviders.mapNotNull { info ->
                runCatching {
                    val label = info.loadLabel(pm)?.trim().orEmpty()
                        .ifEmpty { info.provider.className.substringAfterLast('.') }
                    val appLabel = runCatching {
                        pm.getApplicationLabel(
                            pm.getApplicationInfo(info.provider.packageName, 0),
                        ).toString()
                    }.getOrNull() ?: info.provider.packageName
                    WidgetChoice(
                        info = info,
                        key = info.provider.flattenToString() + "#" + info.label,
                        label = label,
                        appLabel = appLabel,
                        preview = runCatching { info.loadPreviewImage(context, 0) }
                            .getOrNull()?.toImageBitmapOrNull(360),
                        icon = runCatching { info.loadIcon(context, 0) }.getOrNull()
                            ?.toImageBitmapOrNull(120)
                            ?: runCatching { pm.getApplicationIcon(info.provider.packageName) }
                                .getOrNull()?.toImageBitmapOrNull(120),
                        sizeLabel = "${cellsFor(info.minWidth)} × ${cellsFor(info.minHeight)}",
                    )
                }.getOrNull()
            }.sortedWith(compareBy({ it.appLabel.lowercase() }, { it.label.lowercase() }))
        }
    }

    val all = choices
    val filtered = remember(all, query) {
        val list = all ?: emptyList()
        if (query.isBlank()) list else list.filter {
            it.label.contains(query, ignoreCase = true) ||
                it.appLabel.contains(query, ignoreCase = true)
        }
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
            Text(
                text = "Añadir widget",
                color = palette.textPrimary,
                style = MaterialTheme.typography.titleLarge,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(palette.textPrimary.copy(alpha = 0.06f))
                .padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = palette.textSecondary,
                modifier = Modifier.size(20.dp),
            )
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.textPrimary),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(palette.accent),
                modifier = Modifier.weight(1f).padding(start = 12.dp, top = 14.dp, bottom = 14.dp),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(
                            text = "Buscar widget o app…",
                            color = palette.textSecondary,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    inner()
                },
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = { query = "" }) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Borrar búsqueda",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        when {
            all == null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 48.dp),
                ) {
                    CircularProgressIndicator(color = palette.accent)
                    Text(
                        text = "Buscando widgets disponibles…",
                        color = palette.textSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }

            filtered.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Text(
                    text = if (query.isBlank()) {
                        "No hay widgets disponibles en este dispositivo"
                    } else {
                        "Ningún widget coincide con “$query”"
                    },
                    color = palette.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 48.dp),
                )
            }

            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(filtered, key = { _, it -> it.key }) { index, choice ->
                    // App name headers group a company's widgets together instead of
                    // scattering them through the list, but only where they earn the
                    // space - a header repeated for every single row is just noise.
                    val isFirstOfApp = index == 0 || filtered[index - 1].appLabel != choice.appLabel
                    if (isFirstOfApp) {
                        Text(
                            text = choice.appLabel,
                            color = palette.accent,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 8.dp),
                        )
                    }
                    WidgetChoiceRow(
                        choice = choice,
                        palette = palette,
                        onClick = { onProviderSelected(choice.info) },
                    )
                }
                item { Box(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun WidgetChoiceRow(
    choice: WidgetChoice,
    palette: ColorPalette,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(palette.textPrimary.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .heightIn(min = 72.dp)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The preview is what actually tells you what you're adding; the app icon is
        // only a fallback for providers that don't ship one.
        Box(
            modifier = Modifier
                .widthIn(min = 64.dp, max = 96.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(palette.textPrimary.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center,
        ) {
            val bitmap = choice.preview ?: choice.icon
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    modifier = if (choice.preview != null) {
                        Modifier.fillMaxSize().padding(4.dp)
                    } else {
                        Modifier.size(32.dp)
                    },
                )
            }
        }

        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
                text = choice.label,
                color = palette.textPrimary,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = choice.sizeLabel,
                color = palette.textSecondary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, palette.textSecondary.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}
