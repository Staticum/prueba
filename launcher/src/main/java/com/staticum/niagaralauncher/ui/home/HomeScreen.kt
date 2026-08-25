package com.staticum.niagaralauncher.ui.home

import android.appwidget.AppWidgetManager
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.staticum.niagaralauncher.data.AppInfo
import com.staticum.niagaralauncher.data.SwipeDirection
import com.staticum.niagaralauncher.util.TickPlayer
import com.staticum.niagaralauncher.widget.ComposeAppWidgetHost
import com.staticum.niagaralauncher.widget.WidgetEntry
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlin.math.abs

@Composable
fun HomeScreen(
    state: HomeUiState,
    isDefaultLauncher: Boolean,
    widgets: List<WidgetEntry>,
    widgetsSuppressed: Boolean,
    onQueryChange: (String) -> Unit,
    onLaunchApp: (AppInfo) -> Unit,
    onLongPressApp: (AppInfo) -> Unit,
    onOpenSettings: () -> Unit,
    onSwipe: (SwipeDirection) -> Unit,
    onSetAsDefaultLauncher: () -> Unit,
    onResizeWidget: (Int, Int) -> Unit,
    onResizeWidgetWidth: (Int, Int) -> Unit,
    onRemoveInvalidWidget: (Int) -> Unit,
) {
    val palette = state.prefs.palette
    val backgroundColor = if (state.prefs.useWallpaper) {
        androidx.compose.ui.graphics.Color.Transparent
    } else {
        palette.background
    }
    var dragX = 0f
    var dragY = 0f

    val listState = rememberLazyListState()
    val context = LocalContext.current

    val tickPlayer = remember { TickPlayer(context) }
    DisposableEffect(Unit) { onDispose { tickPlayer.release() } }
    LaunchedEffect(state.prefs.soundId) { tickPlayer.setSound(com.staticum.niagaralauncher.util.SoundOption.fromId(state.prefs.soundId)) }
    LaunchedEffect(state.prefs.soundVolume) { tickPlayer.setVolume(state.prefs.soundVolume) }

    var selectedWidgetId by remember { mutableStateOf<Int?>(null) }

    var activeIndexLetter by remember { mutableStateOf<Char?>(null) }
    LaunchedEffect(activeIndexLetter) {
        if (activeIndexLetter != null) {
            tickPlayer.play()
            kotlinx.coroutines.delay(500)
            activeIndexLetter = null
        }
    }

    // A short one-shot per index change while scrolling the app list.
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .drop(1)
            .collect { tickPlayer.play() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { selectedWidgetId = null })
            }
            .pointerInput(state.prefs.gestureFavorites) {
                detectDragGestures(
                    onDragStart = { dragX = 0f; dragY = 0f },
                    onDrag = { change, offset ->
                        change.consume()
                        dragX += offset.x
                        dragY += offset.y
                    },
                    onDragEnd = {
                        val direction = when {
                            abs(dragY) > abs(dragX) && dragY < -120 -> SwipeDirection.UP
                            abs(dragY) > abs(dragX) && dragY > 120 -> SwipeDirection.DOWN
                            abs(dragX) > abs(dragY) && dragX < -120 -> SwipeDirection.LEFT
                            abs(dragX) > abs(dragY) && dragX > 120 -> SwipeDirection.RIGHT
                            else -> null
                        }
                        direction?.let(onSwipe)
                    },
                )
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                IconButton(onClick = { selectedWidgetId = null; onOpenSettings() }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Ajustes", tint = palette.textSecondary)
                }
            }

            ZenQuoteBanner(textColor = palette.textSecondary)

            if (!isDefaultLauncher) {
                Text(
                    text = "No eres el launcher predeterminado · Configurar",
                    color = palette.accent,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSetAsDefaultLauncher)
                        .padding(vertical = 4.dp),
                )
            }

            if (widgetsSuppressed) {
                Text(
                    text = "Widgets desactivados temporalmente (se detectó un cierre inesperado) · Ajustes",
                    color = palette.accent,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedWidgetId = null; onOpenSettings() }
                        .padding(vertical = 4.dp),
                )
            }

            if (widgets.isNotEmpty()) {
                WidgetArea(
                    widgets = widgets,
                    selectedWidgetId = selectedWidgetId,
                    onSelectWidget = { selectedWidgetId = it },
                    onResizeWidget = onResizeWidget,
                    onResizeWidgetWidth = onResizeWidgetWidth,
                    onRemoveInvalidWidget = onRemoveInvalidWidget,
                )
            }

            val favoriteApps = remember(state.allApps, state.prefs.favoriteAppKeys) {
                state.prefs.favoriteAppKeys.mapNotNull { key -> state.allApps.firstOrNull { it.key == key } }
            }
            if (favoriteApps.isNotEmpty()) {
                FavoritesRow(
                    apps = favoriteApps,
                    iconSizeFactor = state.prefs.iconSizeFactor,
                    monochrome = state.prefs.monochromeIcons,
                    accentColor = palette.accent,
                    onLaunchApp = onLaunchApp,
                )
            }

            SearchField(
                query = state.query,
                onQueryChange = { selectedWidgetId = null; onQueryChange(it) },
                textColor = palette.textPrimary,
                hintColor = palette.textSecondary,
            )

            val availableLetters = remember(state.visibleApps) {
                state.visibleApps.mapNotNullTo(sortedSetOf()) { it.label.firstOrNull()?.uppercaseChar() }
            }
            // While a letter is actively touched on the index bar, narrow the list down
            // to just that letter's apps (Niagara-style), instead of merely scrolling to it.
            val displayedApps = remember(state.visibleApps, activeIndexLetter) {
                val letter = activeIndexLetter
                if (letter == null) state.visibleApps else state.visibleApps.filter {
                    it.label.firstOrNull()?.uppercaseChar() == letter
                }
            }

            Row(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(displayedApps, key = { it.key }) { app ->
                        AppRow(
                            app = app,
                            iconSizeFactor = state.prefs.iconSizeFactor,
                            monochrome = state.prefs.monochromeIcons,
                            accentColor = palette.accent,
                            textColor = palette.textPrimary,
                            onClick = { selectedWidgetId = null; onLaunchApp(app) },
                            onLongClick = { selectedWidgetId = null; onLongPressApp(app) },
                            modifier = Modifier.animateItem(placementSpec = tween(220)),
                        )
                    }
                }

                AlphabetIndexBar(
                    availableLetters = availableLetters,
                    activeLetter = activeIndexLetter,
                    onLetterActive = { letter ->
                        activeIndexLetter = letter?.let { nearestAvailableLetter(it, availableLetters) }
                    },
                    waveOffsetDp = state.prefs.indexWaveOffsetDp,
                    accentColor = palette.accent,
                    textColor = palette.textSecondary,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(28.dp),
                )
            }
        }
    }
}

private const val MIN_WIDGET_HEIGHT_DP = 60
private const val MAX_WIDGET_HEIGHT_DP = 400
private const val MIN_WIDGET_WIDTH_PERCENT = 50
private const val MAX_WIDGET_WIDTH_PERCENT = 100
/** A widget at or below this width is considered "half", and eligible to share a
 * row with the next half-width widget instead of always taking the full row. */
private const val PAIRABLE_WIDTH_PERCENT = 50

@Composable
private fun WidgetArea(
    widgets: List<WidgetEntry>,
    selectedWidgetId: Int?,
    onSelectWidget: (Int?) -> Unit,
    onResizeWidget: (Int, Int) -> Unit,
    onResizeWidgetWidth: (Int, Int) -> Unit,
    onRemoveInvalidWidget: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val manager = remember(context) { AppWidgetManager.getInstance(context) }

    // Pack widgets into rows: two consecutive "half" widgets (<=50%) share a row,
    // everything else gets its own full-width row - a simple, order-preserving grid
    // that the user configures just by dragging a selected widget's frame handles.
    val rows = remember(widgets) {
        val result = mutableListOf<List<WidgetEntry>>()
        var i = 0
        while (i < widgets.size) {
            val entry = widgets[i]
            val isHalf = (entry.widthPercent ?: MAX_WIDGET_WIDTH_PERCENT) <= PAIRABLE_WIDTH_PERCENT
            val next = widgets.getOrNull(i + 1)
            val nextIsHalf = next != null && (next.widthPercent ?: MAX_WIDGET_WIDTH_PERCENT) <= PAIRABLE_WIDTH_PERCENT
            if (isHalf && nextIsHalf) {
                result += listOf(entry, next!!)
                i += 2
            } else {
                result += listOf(entry)
                i += 1
            }
        }
        result
    }

    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val fullWidthPx = with(density) { maxWidth.toPx() }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            rows.forEach { row ->
                if (row.size == 1) {
                    val entry = row[0]
                    val providerInfo = remember(entry.id) { manager.getAppWidgetInfo(entry.id) }
                    if (providerInfo == null) {
                        OrphanedWidgetRow(entry, onRemoveInvalidWidget)
                    } else {
                        WidgetCell(
                            entry = entry,
                            providerInfo = providerInfo,
                            fullWidthPx = fullWidthPx,
                            isSelected = entry.id == selectedWidgetId,
                            onSelect = { onSelectWidget(entry.id) },
                            onResizeWidget = onResizeWidget,
                            onResizeWidgetWidth = onResizeWidgetWidth,
                            standalone = true,
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { entry ->
                            val providerInfo = remember(entry.id) { manager.getAppWidgetInfo(entry.id) }
                            if (providerInfo == null) {
                                Box(modifier = Modifier.weight(1f)) { OrphanedWidgetRow(entry, onRemoveInvalidWidget) }
                            } else {
                                WidgetCell(
                                    entry = entry,
                                    providerInfo = providerInfo,
                                    fullWidthPx = fullWidthPx,
                                    isSelected = entry.id == selectedWidgetId,
                                    onSelect = { onSelectWidget(entry.id) },
                                    onResizeWidget = onResizeWidget,
                                    onResizeWidgetWidth = onResizeWidgetWidth,
                                    standalone = false,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrphanedWidgetRow(entry: WidgetEntry, onRemoveInvalidWidget: (Int) -> Unit) {
    Text(
        text = "Widget no disponible · Toca para quitar",
        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRemoveInvalidWidget(entry.id) }
            .padding(vertical = 10.dp),
    )
}

/** One widget host. Normally borderless; long-pressing it shows a frame with two
 * small drag knobs (right edge = width, bottom edge = height) so the size can be
 * set in place, then disappears again on deselect - no permanent bars around widgets.
 * The width knob always measures against the full screen width ([fullWidthPx]),
 * not the widget's own (possibly halved) slot, so dragging a paired widget past 50%
 * correctly un-pairs it back to its own row on the next recomposition. */
@Composable
private fun WidgetCell(
    entry: WidgetEntry,
    providerInfo: android.appwidget.AppWidgetProviderInfo,
    fullWidthPx: Float,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onResizeWidget: (Int, Int) -> Unit,
    onResizeWidgetWidth: (Int, Int) -> Unit,
    standalone: Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val minHeightDp = providerInfo.minHeight.coerceAtLeast(MIN_WIDGET_HEIGHT_DP)
    val heightDp = (entry.heightDp ?: minHeightDp).coerceIn(minHeightDp, MAX_WIDGET_HEIGHT_DP)
    val widthPercent = (entry.widthPercent ?: MAX_WIDGET_WIDTH_PERCENT)
        .coerceIn(MIN_WIDGET_WIDTH_PERCENT, MAX_WIDGET_WIDTH_PERCENT)

    var dragWidthPx by remember(entry.id) { mutableFloatStateOf(fullWidthPx * widthPercent / 100f) }
    var dragHeightPx by remember(entry.id) { mutableFloatStateOf(with(density) { heightDp.dp.toPx() }) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (standalone) Arrangement.Center else Arrangement.Start,
    ) {
        Box(
            modifier = (if (standalone) Modifier.fillMaxWidth(widthPercent / 100f) else Modifier.fillMaxWidth())
                .height(heightDp.dp)
                .then(
                    if (isSelected) {
                        Modifier.border(
                            width = 2.dp,
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        )
                    } else {
                        Modifier
                    },
                )
                .pointerInput(entry.id) {
                    detectTapGestures(onLongPress = { onSelect() })
                },
        ) {
            ComposeAppWidgetHost(
                appWidgetId = entry.id,
                providerInfo = providerInfo,
                modifier = Modifier.fillMaxSize(),
            )

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(28.dp)
                        .pointerInput(entry.id, fullWidthPx) {
                            detectDragGestures(
                                onDragStart = { dragWidthPx = fullWidthPx * widthPercent / 100f },
                                onDrag = { change, offset ->
                                    change.consume()
                                    dragWidthPx += offset.x * 2
                                    val newWidthPercent = (dragWidthPx / fullWidthPx * 100f).toInt()
                                        .coerceIn(MIN_WIDGET_WIDTH_PERCENT, MAX_WIDGET_WIDTH_PERCENT)
                                    onResizeWidgetWidth(entry.id, newWidthPercent)
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    ResizeKnob()
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(28.dp)
                        .pointerInput(entry.id) {
                            detectDragGestures(
                                onDragStart = { dragHeightPx = with(density) { heightDp.dp.toPx() } },
                                onDrag = { change, offset ->
                                    change.consume()
                                    dragHeightPx += offset.y
                                    val newHeightDp = with(density) { dragHeightPx.toDp().value.toInt() }
                                        .coerceIn(minHeightDp, MAX_WIDGET_HEIGHT_DP)
                                    onResizeWidget(entry.id, newHeightDp)
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    ResizeKnob()
                }
            }
        }
    }
}

@Composable
private fun ResizeKnob() {
    Box(
        modifier = Modifier
            .size(14.dp)
            .background(
                androidx.compose.ui.graphics.Color.White,
                shape = androidx.compose.foundation.shape.CircleShape,
            ),
    )
}

@Composable
private fun ZenQuoteBanner(textColor: androidx.compose.ui.graphics.Color) {
    val quote = remember { ZenQuotes.random() }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(600)) +
            androidx.compose.animation.slideInVertically(
                animationSpec = tween(600),
                initialOffsetY = { -it / 3 },
            ),
    ) {
        Text(
            text = quote,
            color = textColor,
            style = MaterialTheme.typography.titleMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 14.dp),
        )
    }
}

@Composable
private fun FavoritesRow(
    apps: List<AppInfo>,
    iconSizeFactor: Float,
    monochrome: Boolean,
    accentColor: androidx.compose.ui.graphics.Color,
    onLaunchApp: (AppInfo) -> Unit,
) {
    val density = LocalDensity.current
    val baseSizeDp = 40.dp
    val iconSize = baseSizeDp * iconSizeFactor

    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items(apps, key = { "fav_${it.key}" }) { app ->
            Image(
                bitmap = app.icon.toBitmap(
                    width = with(density) { iconSize.toPx() }.toInt().coerceAtLeast(1),
                    height = with(density) { iconSize.toPx() }.toInt().coerceAtLeast(1),
                ).asImageBitmap(),
                contentDescription = app.label,
                colorFilter = if (monochrome) ColorFilter.tint(accentColor) else null,
                modifier = Modifier
                    .size(iconSize)
                    .clickable { onLaunchApp(app) },
            )
        }
    }
}

private val ALPHABET = ('A'..'Z').toList()

private fun nearestAvailableLetter(letter: Char, available: Set<Char>): Char? {
    if (available.isEmpty()) return null
    if (letter in available) return letter
    return available.minByOrNull { abs(it.code - letter.code) }
}

private const val MAGNIFY_BUMP = 2.6f
private const val MAGNIFY_SIGMA = 1.7f
private const val ACTIVE_LETTER_SCALE = 2.0f

@Composable
private fun AlphabetIndexBar(
    availableLetters: Set<Char>,
    activeLetter: Char?,
    onLetterActive: (Char?) -> Unit,
    waveOffsetDp: Float,
    accentColor: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    var heightPx by remember { mutableFloatStateOf(0f) }
    // Continuous fractional index under the finger, independent of activeLetter (which
    // snaps to the nearest available letter) - this is what drives the dock-style
    // magnification wave, so it moves smoothly even while activeLetter is unchanged.
    var touchIndex by remember { mutableStateOf<Float?>(null) }

    fun indexAt(y: Float): Float? {
        if (heightPx <= 0f) return null
        return (y / heightPx * ALPHABET.size).coerceIn(0f, ALPHABET.size - 1f)
    }

    fun letterAt(y: Float): Char? = indexAt(y)?.let { ALPHABET[it.toInt()] }

    Column(
        modifier = modifier
            .onGloballyPositioned { heightPx = it.size.height.toFloat() }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { offset -> onLetterActive(letterAt(offset.y)) })
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        touchIndex = indexAt(offset.y)
                        onLetterActive(letterAt(offset.y))
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        touchIndex = indexAt(change.position.y)
                        onLetterActive(letterAt(change.position.y))
                    },
                    onDragEnd = { touchIndex = null; onLetterActive(null) },
                    onDragCancel = { touchIndex = null; onLetterActive(null) },
                )
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        // Right-aligned: as a letter's font size grows with the wave, it expands
        // toward the left (away from the screen edge) instead of around a center,
        // which is what makes the wave actually read as reaching left.
        horizontalAlignment = Alignment.End,
    ) {
        ALPHABET.forEachIndexed { index, letter ->
            val isActive = letter == activeLetter
            val waveScale = touchIndex?.let { t ->
                val distance = abs(t - index)
                1f + MAGNIFY_BUMP * kotlin.math.exp(-(distance * distance) / (2 * MAGNIFY_SIGMA * MAGNIFY_SIGMA))
            } ?: 1f
            // The active (snapped-to) letter always stands out clearly on its own,
            // on top of whatever the wave already gives it - this replaces the old
            // centered circle overlay as the "which letter am I on" indicator.
            val targetScale = if (isActive) maxOf(waveScale, ACTIVE_LETTER_SCALE) else waveScale
            val scale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = targetScale,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
                ),
                label = "letterMagnify",
            )
            Text(
                text = letter.toString(),
                color = when {
                    isActive -> accentColor
                    letter in availableLetters -> textColor
                    else -> textColor.copy(alpha = 0.25f)
                },
                fontSize = MaterialTheme.typography.labelSmall.fontSize * scale,
                fontWeight = if (isActive) androidx.compose.ui.text.font.FontWeight.Bold else null,
                modifier = Modifier
                    .padding(end = if (isActive) 4.dp else 0.dp)
                    .offset(x = waveOffsetDp.dp * -(scale - 1f)),
            )
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    textColor: androidx.compose.ui.graphics.Color,
    hintColor: androidx.compose.ui.graphics.Color,
) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = textColor),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(textColor),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = "Buscar app…",
                        color = hintColor,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                inner()
            },
        )
    }
}

@Composable
private fun AppRow(
    app: AppInfo,
    iconSizeFactor: Float,
    monochrome: Boolean,
    accentColor: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val baseSizeDp = 28.dp
    val iconSize = baseSizeDp * iconSizeFactor

    androidx.compose.foundation.layout.Row(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(app.key) {
                detectTapAndLongPress(onTap = onClick, onLongPress = onLongClick)
            }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            bitmap = app.icon.toBitmap(
                width = with(density) { iconSize.toPx() }.toInt().coerceAtLeast(1),
                height = with(density) { iconSize.toPx() }.toInt().coerceAtLeast(1),
            ).asImageBitmap(),
            contentDescription = null,
            colorFilter = if (monochrome) ColorFilter.tint(accentColor) else null,
            modifier = Modifier.size(iconSize),
        )
        Text(
            text = app.label,
            color = textColor,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectTapAndLongPress(
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    detectTapGestures(
        onTap = { onTap() },
        onLongPress = { onLongPress() },
    )
}
