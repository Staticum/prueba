package com.staticum.niagaralauncher.ui.home

import android.appwidget.AppWidgetManager
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.staticum.niagaralauncher.data.AppInfo
import com.staticum.niagaralauncher.data.SwipeDirection
import com.staticum.niagaralauncher.util.TickPlayer
import com.staticum.niagaralauncher.widget.ComposeAppWidgetHost
import com.staticum.niagaralauncher.widget.WidgetEntry
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun HomeScreen(
    state: HomeUiState,
    isDefaultLauncher: Boolean,
    widgets: List<WidgetEntry>,
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
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val tickPlayer = remember { TickPlayer(context) }
    DisposableEffect(Unit) { onDispose { tickPlayer.release() } }

    var activeIndexLetter by remember { mutableStateOf<Char?>(null) }
    LaunchedEffect(activeIndexLetter) {
        if (activeIndexLetter != null) {
            kotlinx.coroutines.delay(500)
            activeIndexLetter = null
        }
    }

    // A single continuous wind loop for both plain list scrolling and dragging the
    // A-Z index — rather than restarting the sample from zero on every letter/index
    // change, which read as a stutter instead of one continuous sound.
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress to activeIndexLetter }
            .distinctUntilChanged()
            .collect { (scrolling, letter) ->
                if (scrolling || letter != null) tickPlayer.startLoop() else tickPlayer.stopLoop()
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
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
                IconButton(onClick = onOpenSettings) {
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

            if (widgets.isNotEmpty()) {
                WidgetArea(
                    widgets = widgets,
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
                onQueryChange = onQueryChange,
                textColor = palette.textPrimary,
                hintColor = palette.textSecondary,
            )

            Row(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(state.visibleApps, key = { it.key }) { app ->
                        AppRow(
                            app = app,
                            iconSizeFactor = state.prefs.iconSizeFactor,
                            monochrome = state.prefs.monochromeIcons,
                            accentColor = palette.accent,
                            textColor = palette.textPrimary,
                            onClick = { onLaunchApp(app) },
                            onLongClick = { onLongPressApp(app) },
                            modifier = Modifier.animateItem(placementSpec = tween(220)),
                        )
                    }
                }

                val availableLetters = remember(state.visibleApps) {
                    state.visibleApps.mapNotNullTo(sortedSetOf()) { it.label.firstOrNull()?.uppercaseChar() }
                }
                AlphabetIndexBar(
                    availableLetters = availableLetters,
                    activeLetter = activeIndexLetter,
                    onLetterActive = { letter ->
                        activeIndexLetter = letter
                        if (letter == null) return@AlphabetIndexBar
                        val target = nearestAvailableLetter(letter, availableLetters) ?: return@AlphabetIndexBar
                        val index = state.visibleApps.indexOfFirst {
                            it.label.firstOrNull()?.uppercaseChar() == target
                        }
                        if (index >= 0) {
                            coroutineScope.launch { listState.scrollToItem(index) }
                        }
                    },
                    accentColor = palette.accent,
                    textColor = palette.textSecondary,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(24.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = activeIndexLetter != null,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(palette.surface, shape = androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = activeIndexLetter?.toString().orEmpty(),
                    color = palette.accent,
                    fontSize = 40.sp,
                    style = MaterialTheme.typography.headlineLarge,
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
    onResizeWidget: (Int, Int) -> Unit,
    onResizeWidgetWidth: (Int, Int) -> Unit,
    onRemoveInvalidWidget: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val manager = remember(context) { AppWidgetManager.getInstance(context) }

    // Pack widgets into rows: two consecutive "half" widgets (<=50%) share a row,
    // everything else gets its own full-width row - a simple, order-preserving grid
    // that the user configures just by dragging each widget's width handle.
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

/** One widget host plus its resize handles. The width handle is always shown - even
 * when the widget is currently paired side-by-side with another - and always measures
 * against the full screen width ([fullWidthPx]), not the widget's own (possibly halved)
 * slot. That's what lets dragging it back out past 50% "un-pair" the widget: on the
 * next recomposition it no longer qualifies as "half" and reclaims its own full row. */
@Composable
private fun WidgetCell(
    entry: WidgetEntry,
    providerInfo: android.appwidget.AppWidgetProviderInfo,
    fullWidthPx: Float,
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

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            var dragWidthPx by remember(entry.id) { mutableFloatStateOf(fullWidthPx * widthPercent / 100f) }

            if (standalone) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    ComposeAppWidgetHost(
                        appWidgetId = entry.id,
                        providerInfo = providerInfo,
                        modifier = Modifier.fillMaxWidth(widthPercent / 100f).height(heightDp.dp),
                    )
                }
            } else {
                // Sharing a row: the row's weight() sets the actual rendered width,
                // this widget fills its whole slot until dragged wide enough to un-pair.
                ComposeAppWidgetHost(
                    appWidgetId = entry.id,
                    providerInfo = providerInfo,
                    modifier = Modifier.fillMaxWidth().height(heightDp.dp),
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .height(heightDp.dp)
                    .width(24.dp)
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
                ResizeGrip(vertical = true)
            }
        }

        var dragHeightPx by remember(entry.id) { mutableFloatStateOf(with(density) { heightDp.dp.toPx() }) }
        Box(
            modifier = Modifier
                .fillMaxWidth(if (standalone) widthPercent / 100f else 1f)
                .height(14.dp)
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
            ResizeGrip(vertical = false)
        }
    }
}

@Composable
private fun ResizeGrip(vertical: Boolean) {
    Box(
        modifier = if (vertical) {
            Modifier.width(3.dp).height(32.dp)
        } else {
            Modifier.width(32.dp).height(3.dp)
        }.background(
            androidx.compose.ui.graphics.Color.White.copy(alpha = 0.25f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp),
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
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 10.dp),
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

@Composable
private fun AlphabetIndexBar(
    availableLetters: Set<Char>,
    activeLetter: Char?,
    onLetterActive: (Char?) -> Unit,
    accentColor: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    var heightPx by remember { mutableFloatStateOf(0f) }

    fun letterAt(y: Float): Char? {
        if (heightPx <= 0f) return null
        val index = (y / heightPx * ALPHABET.size).toInt().coerceIn(0, ALPHABET.size - 1)
        return ALPHABET[index]
    }

    Column(
        modifier = modifier
            .onGloballyPositioned { heightPx = it.size.height.toFloat() }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { offset -> onLetterActive(letterAt(offset.y)) })
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> onLetterActive(letterAt(offset.y)) },
                    onDrag = { change, _ -> change.consume(); onLetterActive(letterAt(change.position.y)) },
                    onDragEnd = { onLetterActive(null) },
                    onDragCancel = { onLetterActive(null) },
                )
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ALPHABET.forEach { letter ->
            val isActive = letter == activeLetter
            Text(
                text = letter.toString(),
                color = when {
                    isActive -> accentColor
                    letter in availableLetters -> textColor
                    else -> textColor.copy(alpha = 0.25f)
                },
                style = if (isActive) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall,
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
