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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.staticum.niagaralauncher.data.AppInfo
import com.staticum.niagaralauncher.data.SwipeDirection
import com.staticum.niagaralauncher.data.WidgetBackground
import com.staticum.niagaralauncher.ui.theme.ColorPalette
import com.staticum.niagaralauncher.util.TickPlayer
import com.staticum.niagaralauncher.widget.ComposeAppWidgetHost
import com.staticum.niagaralauncher.widget.WidgetEntry
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlin.math.abs

/**
 * One spacing scale for the whole home screen.
 *
 * Every block used to carry its own ad-hoc padding (4/8/12/14/20dp picked per
 * component), so the vertical rhythm between the quote, the widgets, the dock, the
 * search field and the list was arbitrary - the single most reliable tell that a
 * layout was assembled rather than designed.
 */
private val SpaceXs = 4.dp
private val SpaceSm = 8.dp
private val SpaceMd = 16.dp
private val SpaceLg = 20.dp
private val SpaceXl = 28.dp

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
    onMoveWidget: (Int, Int) -> Unit,
    onRemoveWidget: (Int) -> Unit,
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

    // Highlights the corresponding letter on the index bar as the app list is
    // scrolled normally (not just while dragging on the bar itself), so the bar
    // always shows roughly where in the alphabet the visible apps currently are.
    var scrollHighlightLetter by remember { mutableStateOf<Char?>(null) }
    LaunchedEffect(listState, displayedApps) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                scrollHighlightLetter = displayedApps.getOrNull(index)?.label?.firstOrNull()?.uppercaseChar()
            }
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
        Row(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                // Without this the last app row and the tail of the A-Z rail render
                // underneath the system navigation bar and get clipped.
                .navigationBarsPadding(),
        ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = SpaceLg, end = SpaceSm),
        ) {
            // A 40dp bar rather than a 48dp IconButton inside a full-width Box: the
            // old version reserved a whole header's worth of height to hold one gear.
            Box(
                modifier = Modifier.fillMaxWidth().height(40.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .clickable { selectedWidgetId = null; onOpenSettings() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Ajustes",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            ZenQuoteBanner(palette = palette)

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
                    widgetBackground = state.prefs.widgetBackground,
                    palette = palette,
                    onSelectWidget = { selectedWidgetId = it },
                    onResizeWidget = onResizeWidget,
                    onResizeWidgetWidth = onResizeWidgetWidth,
                    onMoveWidget = onMoveWidget,
                    onRemoveWidget = { id ->
                        selectedWidgetId = null
                        onRemoveWidget(id)
                    },
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
                    palette = palette,
                    onLaunchApp = onLaunchApp,
                )
            }

            SearchField(
                query = state.query,
                onQueryChange = { selectedWidgetId = null; onQueryChange(it) },
                palette = palette,
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
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

                // Previously a search with no matches just left a blank void with no
                // explanation - the user couldn't tell the difference between "nothing
                // matches" and "the app broke".
                if (displayedApps.isEmpty()) {
                    EmptyAppList(
                        query = state.query,
                        activeLetter = activeIndexLetter,
                        palette = palette,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp),
                    )
                }
            }
        }

        // Spans the entire right edge of the screen, top to bottom, regardless of
        // whether widgets/favorites push the app list down - so all 26 letters are
        // always available and never get squeezed out by other content above.
        AlphabetIndexBar(
            availableLetters = availableLetters,
            // Two distinct states, deliberately not merged: the letter under your
            // finger gets the full magnify-and-slide treatment, while the letter the
            // list happens to be scrolled to is only highlighted in place. Merging
            // them made a passively-highlighted letter jump to 2x and slide 24dp out
            // of the rail, which read as a rendering glitch floating over the list.
            touchedLetter = activeIndexLetter,
            scrollLetter = scrollHighlightLetter,
            onLetterActive = { letter ->
                activeIndexLetter = letter?.let { nearestAvailableLetter(it, availableLetters) }
            },
            waveOffsetDp = state.prefs.indexWaveOffsetDp,
            palette = palette,
            modifier = Modifier
                .fillMaxHeight()
                .width(36.dp)
                .padding(end = SpaceSm, top = SpaceSm, bottom = SpaceSm),
        )
        }
    }
}
private const val MIN_WIDGET_HEIGHT_DP = 60
private const val MAX_WIDGET_HEIGHT_DP = 520
private const val MIN_WIDGET_WIDTH_PERCENT = 25
private const val MAX_WIDGET_WIDTH_PERCENT = 100

/** Snap points for widget width, so widgets combine cleanly into rows of 2, 3 or 4
 * (25/33/50/66/75/100) instead of any arbitrary drag position. */
private val WIDTH_STEPS = listOf(25, 33, 50, 66, 75, 100)

/** A row accepts more widgets while their combined width stays within this budget
 * (some slack over 100 absorbs rounding, e.g. three 33% widgets = 99). */
private const val ROW_WIDTH_BUDGET_PERCENT = 101
private const val MAX_WIDGETS_PER_ROW = 4

private fun snapWidthPercent(raw: Int): Int =
    WIDTH_STEPS.minByOrNull { kotlin.math.abs(it - raw) } ?: MAX_WIDGET_WIDTH_PERCENT

/**
 * The size limits a given provider actually declares, as opposed to the app-wide
 * defaults.
 *
 * These were being ignored entirely: every widget got both resize handles and a flat
 * 60dp floor, including widgets that declare `RESIZE_NONE` (they can't be resized at
 * all) or a legitimately larger minimum. Dragging those below what they support is
 * exactly what produces stretched or clipped renders.
 *
 * `allowsForce` exists because the declared minimum is sometimes plainly wrong - the
 * system digital clock declares a minHeight far larger than what it needs - so the
 * user can deliberately override it from the edit toolbar.
 */
private data class WidgetSizeLimits(
    val canResizeWidth: Boolean,
    val canResizeHeight: Boolean,
    val minHeightDp: Int,
    val maxHeightDp: Int,
    val minWidthPercent: Int,
)

private fun sizeLimitsFor(
    providerInfo: android.appwidget.AppWidgetProviderInfo,
    fullWidthDp: Int,
    forced: Boolean,
): WidgetSizeLimits {
    if (forced) {
        return WidgetSizeLimits(
            canResizeWidth = true,
            canResizeHeight = true,
            minHeightDp = MIN_WIDGET_HEIGHT_DP,
            maxHeightDp = MAX_WIDGET_HEIGHT_DP,
            minWidthPercent = MIN_WIDGET_WIDTH_PERCENT,
        )
    }
    val mode = providerInfo.resizeMode
    val horizontal = mode and android.appwidget.AppWidgetProviderInfo.RESIZE_HORIZONTAL != 0
    val vertical = mode and android.appwidget.AppWidgetProviderInfo.RESIZE_VERTICAL != 0

    // minResizeHeight is what the widget says it can shrink to; minHeight is its
    // preferred size. Prefer the former and fall back to the latter.
    val declaredMinHeight = providerInfo.minResizeHeight.takeIf { it > 0 }
        ?: providerInfo.minHeight
    val declaredMaxHeight = if (android.os.Build.VERSION.SDK_INT >= 31) {
        providerInfo.maxResizeHeight.takeIf { it > 0 }
    } else {
        null
    }
    val declaredMinWidth = providerInfo.minResizeWidth.takeIf { it > 0 }
        ?: providerInfo.minWidth

    val minH = declaredMinHeight.coerceIn(MIN_WIDGET_HEIGHT_DP, MAX_WIDGET_HEIGHT_DP)
    val maxH = (declaredMaxHeight ?: MAX_WIDGET_HEIGHT_DP)
        .coerceIn(minH, MAX_WIDGET_HEIGHT_DP)
    val minPercent = if (fullWidthDp > 0) {
        snapWidthPercent((declaredMinWidth * 100 / fullWidthDp))
            .coerceIn(MIN_WIDGET_WIDTH_PERCENT, MAX_WIDGET_WIDTH_PERCENT)
    } else {
        MIN_WIDGET_WIDTH_PERCENT
    }

    return WidgetSizeLimits(
        canResizeWidth = horizontal,
        canResizeHeight = vertical,
        minHeightDp = minH,
        maxHeightDp = maxH,
        minWidthPercent = minPercent,
    )
}

@Composable
private fun WidgetArea(
    widgets: List<WidgetEntry>,
    selectedWidgetId: Int?,
    widgetBackground: WidgetBackground,
    palette: ColorPalette,
    onSelectWidget: (Int?) -> Unit,
    onResizeWidget: (Int, Int) -> Unit,
    onResizeWidgetWidth: (Int, Int) -> Unit,
    onMoveWidget: (Int, Int) -> Unit,
    onRemoveWidget: (Int) -> Unit,
    onRemoveInvalidWidget: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val manager = remember(context) { AppWidgetManager.getInstance(context) }

    // Pack widgets into rows greedily: keep adding the next widget to the current
    // row while it still fits under the width budget and the row isn't full yet -
    // this lets 2, 3 or 4 narrower widgets share a row in any combination. The user
    // controls the packing indirectly, by setting each widget's width and by dragging
    // it left/right/up/down with the move handle, which reorders the flat list that
    // this derives from.
    val rows = remember(widgets) {
        val result = mutableListOf<List<WidgetEntry>>()
        var row = mutableListOf<WidgetEntry>()
        var rowWidth = 0
        for (entry in widgets) {
            val width = (entry.widthPercent ?: MAX_WIDGET_WIDTH_PERCENT)
                .coerceIn(MIN_WIDGET_WIDTH_PERCENT, MAX_WIDGET_WIDTH_PERCENT)
            val fitsRow = row.isNotEmpty() &&
                row.size < MAX_WIDGETS_PER_ROW &&
                rowWidth + width <= ROW_WIDTH_BUDGET_PERCENT
            if (fitsRow) {
                row.add(entry)
                rowWidth += width
            } else {
                if (row.isNotEmpty()) result += row
                row = mutableListOf(entry)
                rowWidth = width
            }
        }
        if (row.isNotEmpty()) result += row
        result
    }

    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val fullWidthPx = with(density) { maxWidth.toPx() }
        val fullWidthDp = maxWidth.value.toInt()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (row.size == 1) Arrangement.Center else Arrangement.spacedBy(8.dp),
                    // Heights stay independent per widget, so a row is as tall as its
                    // tallest member and shorter widgets align to the top rather than
                    // stretching - the ragged bottom edge then reads as intentional
                    // spacing instead of a layout bug.
                    verticalAlignment = Alignment.Top,
                ) {
                    row.forEach { entry ->
                        val providerInfo = remember(entry.id) { manager.getAppWidgetInfo(entry.id) }
                        // Keyed by entry.id (not list position) so a widget keeps its
                        // Compose identity - and any in-progress drag gesture - as it
                        // moves between rows during reordering.
                        androidx.compose.runtime.key(entry.id) {
                            val cellModifier = if (row.size == 1) {
                                Modifier.fillMaxWidth(
                                    (entry.widthPercent ?: MAX_WIDGET_WIDTH_PERCENT)
                                        .coerceIn(MIN_WIDGET_WIDTH_PERCENT, MAX_WIDGET_WIDTH_PERCENT) / 100f,
                                )
                            } else {
                                Modifier.weight(1f)
                            }
                            if (providerInfo == null) {
                                OrphanedWidgetCard(
                                    entry = entry,
                                    palette = palette,
                                    onRemove = onRemoveInvalidWidget,
                                    modifier = cellModifier,
                                )
                            } else {
                                WidgetCell(
                                    entry = entry,
                                    providerInfo = providerInfo,
                                    fullWidthPx = fullWidthPx,
                                    fullWidthDp = fullWidthDp,
                                    isSelected = entry.id == selectedWidgetId,
                                    widgetBackground = widgetBackground,
                                    palette = palette,
                                    onSelect = { onSelectWidget(entry.id) },
                                    onDeselect = { onSelectWidget(null) },
                                    onResizeWidget = onResizeWidget,
                                    onResizeWidgetWidth = onResizeWidgetWidth,
                                    onMoveWidget = onMoveWidget,
                                    onRemoveWidget = onRemoveWidget,
                                    modifier = cellModifier,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A widget whose provider no longer resolves - the owning app was uninstalled, or
 * reinstalled with a new id.
 *
 * This used to be a bare line of text hardcoded to `Color.White.copy(alpha = 0.5f)`,
 * which is invisible on the light palette, and gave no hint that the whole line was
 * the tap target.
 */
@Composable
private fun OrphanedWidgetCard(
    entry: WidgetEntry,
    palette: ColorPalette,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .background(palette.textPrimary.copy(alpha = 0.06f))
            .border(
                1.dp,
                palette.textSecondary.copy(alpha = 0.25f),
                androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            )
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Widget no disponible",
                color = palette.textPrimary,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "La app que lo proveía ya no está instalada",
                color = palette.textSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        WidgetToolbarAction(
            icon = Icons.Filled.Delete,
            label = "Quitar",
            palette = palette,
            onClick = { onRemove(entry.id) },
        )
    }
}

/**
 * One widget host.
 *
 * Normally borderless. Long-pressing selects it, which shows a frame plus small
 * corner knobs and a floating toolbar *below* the widget.
 *
 * The controls deliberately do not sit on top of the widget any more: the previous
 * design stacked three 36dp strips over the content (top/right/bottom), and since the
 * minimum widget height is 60dp, the top and bottom strips alone covered more than
 * the whole widget - you were sizing something you couldn't see. The move handle is
 * still a separate touch target rather than a gesture on the widget body, because
 * interactive widgets (a digital clock among them) consume touches before Compose's
 * gesture detection sees them.
 */
@Composable
private fun WidgetCell(
    entry: WidgetEntry,
    providerInfo: android.appwidget.AppWidgetProviderInfo,
    fullWidthPx: Float,
    fullWidthDp: Int,
    isSelected: Boolean,
    widgetBackground: WidgetBackground,
    palette: ColorPalette,
    onSelect: () -> Unit,
    onDeselect: () -> Unit,
    onResizeWidget: (Int, Int) -> Unit,
    onResizeWidgetWidth: (Int, Int) -> Unit,
    onMoveWidget: (Int, Int) -> Unit,
    onRemoveWidget: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current

    // Overriding the provider's declared minimum is opt-in, per widget, and resets
    // when the widget is deselected - it's an escape hatch, not a mode.
    var forceSize by remember(entry.id) { mutableStateOf(false) }
    val limits = remember(providerInfo, fullWidthDp, forceSize) {
        sizeLimitsFor(providerInfo, fullWidthDp, forceSize)
    }

    val defaultHeightDp = providerInfo.minHeight.coerceIn(limits.minHeightDp, limits.maxHeightDp)
    val heightDp = (entry.heightDp ?: defaultHeightDp).coerceIn(limits.minHeightDp, limits.maxHeightDp)
    val widthPercent = (entry.widthPercent ?: MAX_WIDGET_WIDTH_PERCENT)
        .coerceIn(limits.minWidthPercent, MAX_WIDGET_WIDTH_PERCENT)

    var dragWidthPx by remember(entry.id) { mutableFloatStateOf(fullWidthPx * widthPercent / 100f) }
    var dragHeightPx by remember(entry.id) { mutableFloatStateOf(with(density) { heightDp.dp.toPx() }) }
    var reorderDragX by remember(entry.id) { mutableFloatStateOf(0f) }
    var reorderDragY by remember(entry.id) { mutableFloatStateOf(0f) }
    val reorderStepPx = with(density) { 40.dp.toPx() }
    var lastSnappedWidth by remember(entry.id) { mutableIntStateOf(widthPercent) }
    // Shown only while a resize drag is in flight, so you get a number instead of
    // guessing from the outline.
    var sizeReadout by remember(entry.id) { mutableStateOf<String?>(null) }

    val containerShape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    val containerColor = when (widgetBackground) {
        WidgetBackground.NONE -> androidx.compose.ui.graphics.Color.Transparent
        WidgetBackground.SUBTLE -> palette.textPrimary.copy(alpha = 0.07f)
        WidgetBackground.SOLID -> palette.surface
    }
    val containerPadding = if (widgetBackground == WidgetBackground.NONE) 0.dp else 8.dp

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp.dp)
                .clip(containerShape)
                .background(containerColor)
                .then(
                    if (isSelected) {
                        Modifier.border(2.dp, palette.accent, containerShape)
                    } else {
                        Modifier
                    },
                )
                .padding(containerPadding)
                .pointerInput(entry.id) {
                    detectTapGestures(onLongPress = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelect()
                    })
                },
        ) {
            ComposeAppWidgetHost(
                appWidgetId = entry.id,
                providerInfo = providerInfo,
                modifier = Modifier.fillMaxSize(),
            )

            if (isSelected) {
                // Small knobs on the edges rather than full-width/height strips, so
                // the widget stays visible while you size it.
                if (limits.canResizeWidth) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(28.dp)
                            .pointerInput(entry.id, fullWidthPx, limits) {
                                detectDragGestures(
                                    onDragStart = {
                                        dragWidthPx = fullWidthPx * widthPercent / 100f
                                    },
                                    onDragEnd = { sizeReadout = null },
                                    onDragCancel = { sizeReadout = null },
                                    onDrag = { change, offset ->
                                        change.consume()
                                        dragWidthPx += offset.x * 2
                                        val rawPercent = (dragWidthPx / fullWidthPx * 100f).toInt()
                                            .coerceIn(limits.minWidthPercent, MAX_WIDGET_WIDTH_PERCENT)
                                        val snapped = snapWidthPercent(rawPercent)
                                            .coerceIn(limits.minWidthPercent, MAX_WIDGET_WIDTH_PERCENT)
                                        if (snapped != lastSnappedWidth) {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            lastSnappedWidth = snapped
                                        }
                                        sizeReadout = "$snapped %"
                                        onResizeWidgetWidth(entry.id, snapped)
                                    },
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        ResizeKnob(palette)
                    }
                }

                if (limits.canResizeHeight) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .size(28.dp)
                            .pointerInput(entry.id, limits) {
                                detectDragGestures(
                                    onDragStart = {
                                        dragHeightPx = with(density) { heightDp.dp.toPx() }
                                    },
                                    onDragEnd = { sizeReadout = null },
                                    onDragCancel = { sizeReadout = null },
                                    onDrag = { change, offset ->
                                        change.consume()
                                        dragHeightPx += offset.y
                                        val newHeightDp = with(density) { dragHeightPx.toDp().value.toInt() }
                                            .coerceIn(limits.minHeightDp, limits.maxHeightDp)
                                        sizeReadout = "$newHeightDp dp"
                                        onResizeWidget(entry.id, newHeightDp)
                                    },
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        ResizeKnob(palette)
                    }
                }

                val readout = sizeReadout
                if (readout != null) {
                    Text(
                        text = readout,
                        color = palette.background,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            .background(palette.accent)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }

        if (isSelected) {
            WidgetEditToolbar(
                palette = palette,
                canForce = !limits.canResizeWidth || !limits.canResizeHeight || forceSize,
                forced = forceSize,
                onToggleForce = { forceSize = !forceSize },
                onRemove = { onRemoveWidget(entry.id) },
                onDone = { onDeselect() },
                onMoveDrag = { dx, dy ->
                    // Vertical moves the widget between rows; horizontal repositions
                    // it within its row. Both map onto the same ordered list, so
                    // placement stays persistable without a free-floating canvas.
                    reorderDragX += dx
                    reorderDragY += dy
                    while (reorderDragY > reorderStepPx) {
                        onMoveWidget(entry.id, 1)
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        reorderDragY -= reorderStepPx
                    }
                    while (reorderDragY < -reorderStepPx) {
                        onMoveWidget(entry.id, -1)
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        reorderDragY += reorderStepPx
                    }
                    while (reorderDragX > reorderStepPx) {
                        onMoveWidget(entry.id, 1)
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        reorderDragX -= reorderStepPx
                    }
                    while (reorderDragX < -reorderStepPx) {
                        onMoveWidget(entry.id, -1)
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        reorderDragX += reorderStepPx
                    }
                },
                onMoveDragStart = { reorderDragX = 0f; reorderDragY = 0f },
            )
        }
    }
}

/** The floating action bar shown under a selected widget. */
@Composable
private fun WidgetEditToolbar(
    palette: ColorPalette,
    canForce: Boolean,
    forced: Boolean,
    onToggleForce: () -> Unit,
    onRemove: () -> Unit,
    onDone: () -> Unit,
    onMoveDrag: (Float, Float) -> Unit,
    onMoveDragStart: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .background(palette.surface)
            .border(
                1.dp,
                palette.accent.copy(alpha = 0.4f),
                androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Row(
            modifier = Modifier
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { onMoveDragStart() },
                        onDrag = { change, offset ->
                            change.consume()
                            onMoveDrag(offset.x, offset.y)
                        },
                    )
                }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.OpenWith,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "Mover",
                color = palette.accent,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 6.dp),
            )
        }

        if (canForce) {
            WidgetToolbarAction(
                icon = if (forced) Icons.Filled.LockOpen else Icons.Filled.Lock,
                label = if (forced) "Libre" else "Forzar",
                palette = palette,
                onClick = onToggleForce,
                highlighted = forced,
            )
        }

        // Destructive and primary actions must not look identical to each other or
        // to "Mover": remove reads in an error tone, done reads as the filled
        // primary. Previously all three were the same neutral label.
        WidgetToolbarAction(
            icon = Icons.Filled.Delete,
            label = "Quitar",
            palette = palette,
            onClick = onRemove,
            destructive = true,
        )

        WidgetToolbarAction(
            icon = Icons.Filled.Check,
            label = "Listo",
            palette = palette,
            onClick = onDone,
            filled = true,
        )
    }
}

@Composable
private fun WidgetToolbarAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    palette: ColorPalette,
    onClick: () -> Unit,
    highlighted: Boolean = false,
    destructive: Boolean = false,
    filled: Boolean = false,
) {
    val danger = androidx.compose.ui.graphics.Color(0xFFE5484D)
    val tint = when {
        filled -> palette.background
        destructive -> danger
        highlighted -> palette.accent
        else -> palette.textSecondary
    }
    Row(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            .then(if (filled) Modifier.background(palette.accent) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
        Text(
            text = label,
            color = tint,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun ResizeKnob(palette: ColorPalette) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .background(palette.accent, shape = androidx.compose.foundation.shape.CircleShape)
            .border(
                2.dp,
                palette.background,
                androidx.compose.foundation.shape.CircleShape,
            ),
    )
}

@Composable
private fun ZenQuoteBanner(palette: ColorPalette) {
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
        // Lighter weight and a touch of letter spacing so the quote reads as a quiet
        // epigraph rather than competing with the widgets below it for attention.
        Text(
            text = quote,
            color = palette.textSecondary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
            letterSpacing = 0.3.sp,
            lineHeight = 22.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = SpaceMd, end = SpaceMd, top = SpaceXs, bottom = SpaceXl),
        )
    }
}

/**
 * A uniform frame for an app icon.
 *
 * Launcher icons arrive in wildly different shapes - some are full-bleed rounded
 * squares with their own background, some are transparent glyphs, some are circles.
 * Rendered raw, a list of them has no shared silhouette and reads as a pile of
 * assets. Giving every icon the same footprint (and insetting the artwork slightly)
 * makes the column line up without clipping anyone's artwork.
 */
@Composable
private fun AppIcon(
    app: AppInfo,
    sizeDp: androidx.compose.ui.unit.Dp,
    monochrome: Boolean,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val px = with(density) { sizeDp.toPx() }.toInt().coerceAtLeast(1)
    Box(
        modifier = modifier.size(sizeDp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            bitmap = app.icon.toBitmap(width = px, height = px).asImageBitmap(),
            contentDescription = null,
            colorFilter = if (monochrome) ColorFilter.tint(accentColor) else null,
            modifier = Modifier
                .fillMaxSize()
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(sizeDp * 0.24f)),
        )
    }
}

@Composable
private fun FavoritesRow(
    apps: List<AppInfo>,
    iconSizeFactor: Float,
    monochrome: Boolean,
    palette: ColorPalette,
    onLaunchApp: (AppInfo) -> Unit,
) {
    val iconSize = 44.dp * iconSizeFactor

    // Reads as a dock rather than two icons stranded against the left margin: the
    // row sits on its own surface, is centred, and each icon gets a real 48dp+ touch
    // target instead of only the bitmap's own bounds.
    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = SpaceXl)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
            .background(palette.textPrimary.copy(alpha = 0.05f))
            .padding(horizontal = SpaceSm, vertical = SpaceSm),
        horizontalArrangement = Arrangement.spacedBy(SpaceSm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(apps, key = { "fav_${it.key}" }) { app ->
            Box(
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                    .clickable { onLaunchApp(app) }
                    .padding(SpaceSm),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(
                    app = app,
                    sizeDp = iconSize,
                    monochrome = monochrome,
                    accentColor = palette.accent,
                )
            }
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
    touchedLetter: Char?,
    scrollLetter: Char?,
    onLetterActive: (Char?) -> Unit,
    waveOffsetDp: Float,
    palette: ColorPalette,
    modifier: Modifier = Modifier,
) {
    val accentColor = palette.accent
    val textColor = palette.textSecondary
    var heightPx by remember { mutableFloatStateOf(0f) }
    // Continuous fractional index under the finger, independent of activeLetter (which
    // snaps to the nearest available letter) - this is what drives the dock-style
    // magnification wave, so it moves smoothly even while activeLetter is unchanged.
    var touchIndex by remember { mutableStateOf<Float?>(null) }

    // One light tick each time the finger crosses into a new letter, so scrubbing the
    // index feels like a physical detent rather than a silent slide. Tracked
    // separately from activeLetter so it fires on every letter boundary crossed,
    // including letters with no apps behind them.
    val haptics = LocalHapticFeedback.current
    var lastHapticLetter by remember { mutableStateOf<Char?>(null) }

    val density = LocalDensity.current
    // Shrinks the base letter size to whatever the actually available height allows,
    // so all 26 letters always fit without clipping/overlap - independent of screen
    // size or how much vertical space other content above leaves for this bar - while
    // never growing past the normal design size on tall screens.
    // Auto-fit still caps the size so 26 letters never overlap, but a floor keeps
    // them legible: spread over a full phone height the previous formula left ~11sp
    // glyphs separated by ~24dp of nothing, which read as scattered rather than as
    // a deliberate rail.
    val designFontSize = MaterialTheme.typography.labelMedium.fontSize
    val baseFontSize = if (heightPx <= 0f) {
        designFontSize
    } else {
        val slotHeightPx = heightPx / ALPHABET.size
        val autoSizeValue = with(density) { (slotHeightPx * 0.62f).toSp().value }
        kotlin.math.min(designFontSize.value, autoSizeValue).coerceAtLeast(9f).sp
    }

    fun indexAt(y: Float): Float? {
        if (heightPx <= 0f) return null
        return (y / heightPx * ALPHABET.size).coerceIn(0f, ALPHABET.size - 1f)
    }

    fun letterAt(y: Float): Char? = indexAt(y)?.let { ALPHABET[it.toInt()] }

    fun tickFor(letter: Char?) {
        if (letter != null && letter != lastHapticLetter) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        lastHapticLetter = letter
    }

    // A faint track appears only while scrubbing, so at rest the rail stays as quiet
    // as it is now, but the moment you touch it the interactive area becomes visible
    // instead of leaving you guessing where the hit region is.
    val trackAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (touchIndex != null) 0.07f else 0f,
        animationSpec = tween(180),
        label = "indexTrack",
    )

    Column(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
            .background(palette.textPrimary.copy(alpha = trackAlpha))
            .onGloballyPositioned { heightPx = it.size.height.toFloat() }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { offset ->
                    val letter = letterAt(offset.y)
                    tickFor(letter)
                    onLetterActive(letter)
                })
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        touchIndex = indexAt(offset.y)
                        val letter = letterAt(offset.y)
                        tickFor(letter)
                        onLetterActive(letter)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        touchIndex = indexAt(change.position.y)
                        val letter = letterAt(change.position.y)
                        tickFor(letter)
                        onLetterActive(letter)
                    },
                    onDragEnd = { touchIndex = null; lastHapticLetter = null; onLetterActive(null) },
                    onDragCancel = { touchIndex = null; lastHapticLetter = null; onLetterActive(null) },
                )
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        // Right-aligned: as a letter's font size grows with the wave, it expands
        // toward the left (away from the screen edge) instead of around a center,
        // which is what makes the wave actually read as reaching left.
        horizontalAlignment = Alignment.End,
    ) {
        ALPHABET.forEachIndexed { index, letter ->
            // Being under the finger and merely being where the list is scrolled to
            // are different states and must look different. Only the touched letter
            // may scale and slide out of the rail; the scroll position is shown in
            // place with colour and weight alone. Conflating the two made a letter
            // nobody was touching jump to 2x and slide 24dp left, hanging over the
            // app list like a rendering artifact.
            val isTouched = touchedLetter != null && letter == touchedLetter
            val isScrolledTo = !isTouched && touchedLetter == null && letter == scrollLetter
            val waveScale = touchIndex?.let { t ->
                val distance = abs(t - index)
                1f + MAGNIFY_BUMP * kotlin.math.exp(-(distance * distance) / (2 * MAGNIFY_SIGMA * MAGNIFY_SIGMA))
            } ?: 1f
            val targetScale = if (isTouched) maxOf(waveScale, ACTIVE_LETTER_SCALE) else waveScale
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
                    isTouched -> accentColor
                    isScrolledTo -> accentColor
                    letter in availableLetters -> textColor
                    else -> textColor.copy(alpha = 0.22f)
                },
                fontSize = baseFontSize * scale,
                fontWeight = if (isTouched || isScrolledTo) {
                    androidx.compose.ui.text.font.FontWeight.Bold
                } else {
                    null
                },
                modifier = Modifier
                    .padding(end = if (isTouched) SpaceXs else 0.dp)
                    // Gated on there being a finger on the rail at all - not on this
                    // being the active letter - so the neighbours still slide left
                    // with the wave, while nothing shifts when nobody is scrubbing.
                    .offset(
                        x = if (touchIndex != null) waveOffsetDp.dp * -(scale - 1f) else 0.dp,
                    ),
            )
        }
    }
}

/**
 * The search field used to be a bare line of hint text with no icon, no container
 * and no way to clear it - it read as a label rather than an input. It's now a
 * proper field: a search icon anchors it, the container lifts on focus, and a
 * clear button appears as soon as there's something to clear.
 */
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    palette: ColorPalette,
) {
    var focused by remember { mutableStateOf(false) }
    val active = focused || query.isNotEmpty()
    val borderColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (active) palette.accent.copy(alpha = 0.55f) else palette.textPrimary.copy(alpha = 0.12f),
        animationSpec = tween(200),
        label = "searchBorder",
    )

    // Deliberately *not* a floating pill: users often keep a Google search widget
    // right above this, and two identical pills 8dp apart read as one control
    // duplicated. This sits flush as a list header - flat, full-bleed, separated by
    // a rule - so its role (filter the list below) is legible at a glance.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = SpaceSm)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .background(palette.textPrimary.copy(alpha = if (active) 0.07f else 0.0f))
            .border(1.dp, borderColor, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .padding(start = SpaceMd, end = SpaceXs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = if (active) palette.accent else palette.textSecondary,
            modifier = Modifier.size(20.dp),
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.textPrimary),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(palette.accent),
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, top = 14.dp, bottom = 14.dp)
                .onFocusChanged { focused = it.isFocused },
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
        AnimatedVisibility(visible = query.isNotEmpty(), enter = fadeIn(tween(150))) {
            IconButton(onClick = { onQueryChange("") }) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Borrar búsqueda",
                    tint = palette.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** Explains *why* the list is empty instead of showing a blank screen. */
@Composable
private fun EmptyAppList(
    query: String,
    activeLetter: Char?,
    palette: ColorPalette,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = palette.textSecondary.copy(alpha = 0.4f),
            modifier = Modifier.size(40.dp),
        )
        Text(
            text = when {
                query.isNotBlank() -> "Sin resultados para “$query”"
                activeLetter != null -> "Ninguna app empieza con $activeLetter"
                else -> "No hay apps para mostrar"
            },
            color = palette.textPrimary,
            style = MaterialTheme.typography.titleSmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = if (query.isNotBlank()) {
                "Revisa la ortografía, o comprueba si la app está oculta en Ajustes."
            } else {
                "Puedes volver a mostrar apps ocultas desde Ajustes."
            },
            color = palette.textSecondary,
            style = MaterialTheme.typography.bodySmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
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
    val iconSize = 32.dp * iconSizeFactor

    androidx.compose.foundation.layout.Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .pointerInput(app.key) {
                detectTapAndLongPress(onTap = onClick, onLongPress = onLongClick)
            }
            // 48dp minimum row height: the old 10dp vertical padding around a 28dp
            // icon left rows below the minimum comfortable touch target.
            .heightIn(min = 52.dp)
            .padding(horizontal = SpaceSm, vertical = SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(
            app = app,
            sizeDp = iconSize,
            monochrome = monochrome,
            accentColor = accentColor,
        )
        Text(
            text = app.label,
            color = textColor,
            style = MaterialTheme.typography.bodyLarge,
            letterSpacing = 0.1.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = SpaceMd),
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
