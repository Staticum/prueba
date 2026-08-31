package com.staticum.niagaralauncher.ui.home

import android.appwidget.AppWidgetManager
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.toArgb
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
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.material3.AlertDialog
import com.staticum.niagaralauncher.ui.settings.SettingsButton
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
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.staticum.niagaralauncher.data.AppInfo
import com.staticum.niagaralauncher.data.SwipeDirection
import com.staticum.niagaralauncher.data.WidgetBackground
import com.staticum.niagaralauncher.ui.theme.ColorPalette
import com.staticum.niagaralauncher.util.TickPlayer
import com.staticum.niagaralauncher.widget.ComposeAppWidgetHost
import com.staticum.niagaralauncher.widget.WidgetEntry
import com.staticum.niagaralauncher.widget.WidgetGrid
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlin.math.abs
import kotlin.math.roundToInt

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
    onOpenAppInfo: (AppInfo) -> Unit,
    nextAlarmMillis: Long?,
    onOpenSettings: () -> Unit,
    onSwipe: (SwipeDirection) -> Unit,
    onSetAsDefaultLauncher: () -> Unit,
    onPlaceWidget: (Int, Int, Int, Int, Int) -> Unit,
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
    // A long press used to hide the app immediately, no confirmation - a single
    // accidental gesture (a slightly slow tap, a finger lingering while thinking)
    // silently removed it from the list with no way to tell what happened. Now it
    // opens a small menu instead, and hiding requires a second, deliberate tap.
    var longPressedApp by remember { mutableStateOf<AppInfo?>(null) }

    // Two separate states on purpose. Before, one value did both jobs, which forced a
    // choice between "the selection dies with the finger" and "the letter stays
    // magnified and slid out of the rail for as long as the filter lasts".
    //  - touchedLetter: only while a finger is on the rail. Drives the wave.
    //  - filterLetter: the selection that outlives the gesture and filters the list.
    var touchedLetter by remember { mutableStateOf<Char?>(null) }
    var filterLetter by remember { mutableStateOf<Char?>(null) }

    // Picking a letter puts you at the top of that letter's block. Since the block is
    // ordered by usage, the first row is the app you use most with that letter, which
    // is the whole point of picking it.
    LaunchedEffect(filterLetter) {
        if (filterLetter != null) {
            tickPlayer.play()
            listState.scrollToItem(0)
        }
    }

    // None of this is a mode you have to dismiss: a letter filter, an active search,
    // or simply having scrolled away from the top are all states that let go on
    // their own after a spell of doing nothing, and Frecuentes comes back. Tracking
    // "last activity" as a timestamp, rather than one delay-then-reset per state,
    // covers plain scrolling too - not just the letter filter - and restarts cleanly
    // no matter which of the three combination of things changed.
    var lastActivityAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(filterLetter, touchedLetter, state.query, listState.isScrollInProgress) {
        lastActivityAt = System.currentTimeMillis()
    }
    LaunchedEffect(state.prefs.homeResetSeconds) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            val idleMs = System.currentTimeMillis() - lastActivityAt
            val awayFromHome = filterLetter != null ||
                state.query.isNotBlank() ||
                listState.firstVisibleItemIndex > 0
            if (touchedLetter == null &&
                !listState.isScrollInProgress &&
                awayFromHome &&
                idleMs >= state.prefs.homeResetSeconds * 1000L
            ) {
                filterLetter = null
                if (state.query.isNotBlank()) onQueryChange("")
                listState.scrollToItem(0)
                lastActivityAt = System.currentTimeMillis()
            }
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
    // Picking a letter narrows the list to that letter's apps (Niagara-style) rather
    // than merely scrolling to it, and holds until the inactivity timeout above.
    val displayedApps = remember(state.visibleApps, filterLetter, state.localScores, state.systemScores) {
        val letter = filterLetter
        if (letter == null) {
            state.visibleApps
        } else {
            // Inside a single letter, alphabetical order is arbitrary to the user -
            // what they want is the app of that letter they actually use. Ties (score
            // 0, i.e. never used) keep the alphabetical order they already had, since
            // sortedByDescending is stable.
            state.visibleApps
                .filter { it.label.firstOrNull()?.uppercaseChar() == letter }
                .sortedByDescending { state.usageScoreFor(it) }
        }
    }

    // Suggestions must not get in the way of an explicit intent: while searching or
    // while a letter is being filtered, the user already knows what they are after.
    val frequentApps = if (state.query.isBlank() && filterLetter == null) {
        state.frequentApps
    } else {
        emptyList()
    }

    // Highlights the corresponding letter on the index bar as the app list is
    // scrolled normally (not just while dragging on the bar itself), so the bar
    // always shows roughly where in the alphabet the visible apps currently are.
    var scrollHighlightLetter by remember { mutableStateOf<Char?>(null) }
    // Derived from the first visible item's *key*, not its position: the list now
    // starts with the Frecuentes items, so an index into the LazyColumn is no longer
    // an index into displayedApps and highlighted a letter further down the alphabet.
    // Frecuentes keys are prefixed, so they match no app and simply highlight nothing.
    LaunchedEffect(listState, displayedApps) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.firstOrNull()?.key }
            .collect { key ->
                scrollHighlightLetter = displayedApps
                    .firstOrNull { it.key == key }
                    ?.label?.firstOrNull()?.uppercaseChar()
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { selectedWidgetId = null; filterLetter = null })
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

            NextAlarmBanner(millis = nextAlarmMillis, palette = palette)

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
                    onPlaceWidget = onPlaceWidget,
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
                    silhouette = state.prefs.iconSilhouette,
                    palette = palette,
                    onLaunchApp = onLaunchApp,
                )
            }

            SearchField(
                query = state.query,
                onQueryChange = { selectedWidgetId = null; filterLetter = null; onQueryChange(it) },
                palette = palette,
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (frequentApps.isNotEmpty()) {
                        // Deliberately items *of* the list rather than a fixed block
                        // above it: this way the section scrolls away instead of
                        // permanently eating vertical space.
                        item(key = "frequents_header") {
                            Text(
                                text = "Frecuentes",
                                color = palette.textSecondary,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(
                                    start = SpaceSm,
                                    top = SpaceXs,
                                    bottom = SpaceXs,
                                ),
                            )
                        }
                        items(frequentApps, key = { "freq_${it.key}" }) { app ->
                            AppRow(
                                app = app,
                                iconSizeFactor = state.prefs.iconSizeFactor,
                                monochrome = state.prefs.monochromeIcons,
                                silhouette = state.prefs.iconSilhouette,
                                accentColor = palette.accent,
                                textColor = palette.textPrimary,
                                onClick = { selectedWidgetId = null; onLaunchApp(app) },
                                onLongClick = { selectedWidgetId = null; longPressedApp = app },
                                modifier = Modifier.animateItem(placementSpec = tween(220)),
                            )
                        }
                        item(key = "frequents_divider") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = SpaceSm, vertical = SpaceSm)
                                    .height(1.dp)
                                    .background(palette.textSecondary.copy(alpha = 0.15f)),
                            )
                        }
                    }

                    items(displayedApps, key = { it.key }) { app ->
                        AppRow(
                            app = app,
                            iconSizeFactor = state.prefs.iconSizeFactor,
                            monochrome = state.prefs.monochromeIcons,
                            silhouette = state.prefs.iconSilhouette,
                            accentColor = palette.accent,
                            textColor = palette.textPrimary,
                            onClick = { selectedWidgetId = null; onLaunchApp(app) },
                            onLongClick = { selectedWidgetId = null; longPressedApp = app },
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
                        activeLetter = filterLetter,
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
            touchedLetter = touchedLetter,
            scrollLetter = scrollHighlightLetter,
            onLetterActive = { letter ->
                val snapped = letter?.let { nearestAvailableLetter(it, availableLetters) }
                touchedLetter = snapped
                // Only a real letter updates the filter; releasing the rail reports
                // null and must leave the selection standing - the timeout owns it.
                if (snapped != null) filterLetter = snapped
            },
            waveOffsetDp = state.prefs.indexWaveOffsetDp,
            palette = palette,
            modifier = Modifier
                .fillMaxHeight()
                .width(36.dp)
                .padding(end = SpaceSm, top = SpaceSm, bottom = SpaceSm),
        )
        }

        longPressedApp?.let { app ->
            AlertDialog(
                onDismissRequest = { longPressedApp = null },
                title = { Text(app.label) },
                text = { Text("¿Qué quieres hacer con esta app?") },
                confirmButton = {
                    SettingsButton(
                        text = "Ocultar",
                        palette = palette,
                        onClick = {
                            onLongPressApp(app)
                            longPressedApp = null
                        },
                    )
                },
                dismissButton = {
                    androidx.compose.foundation.layout.Row(
                        horizontalArrangement = Arrangement.spacedBy(SpaceSm),
                    ) {
                        SettingsButton(
                            text = "Info de la app",
                            palette = palette,
                            onClick = {
                                onOpenAppInfo(app)
                                longPressedApp = null
                            },
                        )
                        SettingsButton(
                            text = "Cancelar",
                            palette = palette,
                            onClick = { longPressedApp = null },
                        )
                    }
                },
                containerColor = palette.surface,
                titleContentColor = palette.textPrimary,
                textContentColor = palette.textSecondary,
            )
        }
    }
}
/**
 * The size limits a given provider actually declares, expressed in grid cells
 * rather than dp/percent - so a widget's own minimum lines up with the same units
 * the user drags and resizes in.
 *
 * These used to be ignored entirely: every widget got resize handles regardless of
 * declaring RESIZE_NONE, and a flat dp floor instead of its own minimum. Dragging
 * those below what they support is exactly what produces stretched or clipped
 * renders. `allowsForce` exists because the declared minimum is sometimes plainly
 * wrong - the system digital clock declares a minHeight far larger than what it
 * needs - so the user can deliberately override it from the edit toolbar.
 */
private data class WidgetCellLimits(
    val canResizeWidth: Boolean,
    val canResizeHeight: Boolean,
    val minColSpan: Int,
    val minRowSpan: Int,
    val maxRowSpan: Int,
)

private fun cellLimitsFor(
    providerInfo: android.appwidget.AppWidgetProviderInfo,
    cellWidthDp: Float,
    forced: Boolean,
): WidgetCellLimits {
    if (forced) {
        return WidgetCellLimits(
            canResizeWidth = true,
            canResizeHeight = true,
            minColSpan = WidgetGrid.MIN_COL_SPAN,
            minRowSpan = WidgetGrid.MIN_ROW_SPAN,
            maxRowSpan = WidgetGrid.MAX_ROW_SPAN,
        )
    }
    val mode = providerInfo.resizeMode
    val horizontal = mode and android.appwidget.AppWidgetProviderInfo.RESIZE_HORIZONTAL != 0
    val vertical = mode and android.appwidget.AppWidgetProviderInfo.RESIZE_VERTICAL != 0

    // minResizeWidth/Height is what the widget says it can shrink to; minWidth/Height
    // is its preferred size. Prefer the former and fall back to the latter.
    val declaredMinWidthDp = (providerInfo.minResizeWidth.takeIf { it > 0 } ?: providerInfo.minWidth).toFloat()
    val declaredMinHeightDp = (providerInfo.minResizeHeight.takeIf { it > 0 } ?: providerInfo.minHeight).toFloat()
    val declaredMaxHeightDp = if (android.os.Build.VERSION.SDK_INT >= 31) {
        providerInfo.maxResizeHeight.takeIf { it > 0 }?.toFloat()
    } else {
        null
    }

    val minColSpan = if (cellWidthDp > 0f) {
        kotlin.math.ceil(declaredMinWidthDp / cellWidthDp).toInt().coerceIn(WidgetGrid.MIN_COL_SPAN, WidgetGrid.COLUMNS)
    } else {
        WidgetGrid.MIN_COL_SPAN
    }
    val minRowSpan = kotlin.math.ceil(declaredMinHeightDp / WidgetGrid.CELL_HEIGHT_DP)
        .toInt()
        .coerceIn(WidgetGrid.MIN_ROW_SPAN, WidgetGrid.MAX_ROW_SPAN)
    val maxRowSpan = declaredMaxHeightDp
        ?.let { kotlin.math.floor(it / WidgetGrid.CELL_HEIGHT_DP).toInt() }
        ?.coerceIn(minRowSpan, WidgetGrid.MAX_ROW_SPAN)
        ?: WidgetGrid.MAX_ROW_SPAN

    return WidgetCellLimits(
        canResizeWidth = horizontal,
        canResizeHeight = vertical,
        minColSpan = minColSpan,
        minRowSpan = minRowSpan,
        maxRowSpan = maxRowSpan,
    )
}

/**
 * Free-form grid, same spirit as a standard Android home screen: a fixed number of
 * columns, each widget spanning whole cells, positioned anywhere rather than packed
 * into rows by the app. Superseded the previous row-packing layout (width percent +
 * height dp, auto-arranged by a greedy fit) because packing order decided placement
 * instead of the user - dragging a widget only ever reordered a list, it never put
 * it "here" in any spatial sense.
 */
@Composable
private fun WidgetArea(
    widgets: List<WidgetEntry>,
    selectedWidgetId: Int?,
    widgetBackground: WidgetBackground,
    palette: ColorPalette,
    onSelectWidget: (Int?) -> Unit,
    onPlaceWidget: (Int, Int, Int, Int, Int) -> Unit,
    onRemoveWidget: (Int) -> Unit,
    onRemoveInvalidWidget: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val manager = remember(context) { AppWidgetManager.getInstance(context) }
    val density = LocalDensity.current

    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val cellWidthPx = with(density) { maxWidth.toPx() } / WidgetGrid.COLUMNS
        val cellWidthDp = maxWidth.value / WidgetGrid.COLUMNS
        val cellHeightPx = with(density) { WidgetGrid.CELL_HEIGHT_DP.dp.toPx() }
        val maxRow = widgets.maxOfOrNull { it.row + it.rowSpan } ?: 0

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((maxRow * WidgetGrid.CELL_HEIGHT_DP).dp)
                .padding(bottom = 8.dp),
        ) {
            widgets.forEach { entry ->
                // Keyed by entry.id so a widget keeps its Compose identity - and any
                // in-progress drag or resize - independent of where it sits in the
                // list or how many other widgets exist.
                androidx.compose.runtime.key(entry.id) {
                    val providerInfo = remember(entry.id) { manager.getAppWidgetInfo(entry.id) }
                    val xDp = with(density) { (entry.col * cellWidthPx).toDp() }
                    val yDp = with(density) { (entry.row * cellHeightPx).toDp() }
                    val wDp = with(density) { (entry.colSpan * cellWidthPx).toDp() }
                    val hDp = (entry.rowSpan * WidgetGrid.CELL_HEIGHT_DP).dp

                    if (providerInfo == null) {
                        OrphanedWidgetCard(
                            entry = entry,
                            palette = palette,
                            onRemove = onRemoveInvalidWidget,
                            modifier = Modifier
                                .offset(x = xDp, y = yDp)
                                .size(wDp, hDp)
                                .padding(4.dp),
                        )
                    } else {
                        WidgetCell(
                            entry = entry,
                            providerInfo = providerInfo,
                            cellWidthPx = cellWidthPx,
                            cellWidthDp = cellWidthDp,
                            cellHeightPx = cellHeightPx,
                            xDp = xDp,
                            yDp = yDp,
                            widthDp = wDp,
                            heightDp = hDp,
                            isSelected = entry.id == selectedWidgetId,
                            widgetBackground = widgetBackground,
                            palette = palette,
                            onSelect = { onSelectWidget(entry.id) },
                            onDeselect = { onSelectWidget(null) },
                            onPlace = onPlaceWidget,
                            onRemoveWidget = onRemoveWidget,
                        )
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
 * One widget host, positioned at an explicit grid cell rect.
 *
 * Normally borderless. Long-pressing selects it, which shows a frame, a single
 * corner resize handle, and a floating toolbar below the widget with a "Mover"
 * handle that drags it - live, in pixels - to any free cell.
 *
 * Move and resize both work the same way: track a raw pixel offset for the duration
 * of the gesture (visual only, applied via `offset`/`size` so it snaps to whole
 * cells as you drag rather than only on release), and commit the final cell rect to
 * [onPlace] once the finger lifts. The move handle stays a separate touch target
 * rather than a gesture on the widget body, because interactive widgets (a digital
 * clock among them) consume touches before Compose's own gesture detection sees them.
 */
@Composable
private fun WidgetCell(
    entry: WidgetEntry,
    providerInfo: android.appwidget.AppWidgetProviderInfo,
    cellWidthPx: Float,
    cellWidthDp: Float,
    cellHeightPx: Float,
    xDp: androidx.compose.ui.unit.Dp,
    yDp: androidx.compose.ui.unit.Dp,
    widthDp: androidx.compose.ui.unit.Dp,
    heightDp: androidx.compose.ui.unit.Dp,
    isSelected: Boolean,
    widgetBackground: WidgetBackground,
    palette: ColorPalette,
    onSelect: () -> Unit,
    onDeselect: () -> Unit,
    onPlace: (Int, Int, Int, Int, Int) -> Unit,
    onRemoveWidget: (Int) -> Unit,
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current

    // Overriding the provider's declared minimum is opt-in, per widget, and resets
    // when the widget is deselected - it's an escape hatch, not a mode.
    var forceSize by remember(entry.id) { mutableStateOf(false) }
    val limits = remember(providerInfo, cellWidthDp, forceSize) {
        cellLimitsFor(providerInfo, cellWidthDp, forceSize)
    }

    // Live drag/resize state, in pixels - zeroed once the gesture commits via onPlace.
    var moveOffsetX by remember(entry.id) { mutableFloatStateOf(0f) }
    var moveOffsetY by remember(entry.id) { mutableFloatStateOf(0f) }
    var resizeWidthPx by remember(entry.id, cellWidthPx) { mutableFloatStateOf(entry.colSpan * cellWidthPx) }
    var resizeHeightPx by remember(entry.id) { mutableFloatStateOf(entry.rowSpan * cellHeightPx) }
    var liveColSpan by remember(entry.id) { mutableIntStateOf(entry.colSpan) }
    var liveRowSpan by remember(entry.id) { mutableIntStateOf(entry.rowSpan) }
    var sizeReadout by remember(entry.id) { mutableStateOf<String?>(null) }

    val containerShape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    val containerColor = when (widgetBackground) {
        WidgetBackground.NONE -> androidx.compose.ui.graphics.Color.Transparent
        WidgetBackground.SUBTLE -> palette.textPrimary.copy(alpha = 0.07f)
        WidgetBackground.SOLID -> palette.surface
    }
    val containerPadding = if (widgetBackground == WidgetBackground.NONE) 0.dp else 8.dp
    val canResize = limits.canResizeWidth || limits.canResizeHeight

    Box(
        modifier = Modifier
            .offset(
                x = xDp + with(density) { moveOffsetX.toDp() },
                y = yDp + with(density) { moveOffsetY.toDp() },
            )
            .size(
                width = if (limits.canResizeWidth) with(density) { resizeWidthPx.toDp() } else widthDp,
                height = if (limits.canResizeHeight) with(density) { resizeHeightPx.toDp() } else heightDp,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
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

            if (isSelected && canResize) {
                // A single corner handle rather than separate width/height edges -
                // clearer as "resize the box" on a grid, and each axis simply stays
                // fixed when the provider doesn't support resizing it.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp)
                        .pointerInput(entry.id, cellWidthPx, cellHeightPx, limits) {
                            var lastColSpan = liveColSpan
                            var lastRowSpan = liveRowSpan
                            detectDragGestures(
                                onDragStart = {
                                    resizeWidthPx = liveColSpan * cellWidthPx
                                    resizeHeightPx = liveRowSpan * cellHeightPx
                                    lastColSpan = liveColSpan
                                    lastRowSpan = liveRowSpan
                                },
                                onDragEnd = {
                                    onPlace(entry.id, entry.col, entry.row, liveColSpan, liveRowSpan)
                                    sizeReadout = null
                                },
                                onDragCancel = { sizeReadout = null },
                                onDrag = { change, offset ->
                                    change.consume()
                                    if (limits.canResizeWidth) {
                                        resizeWidthPx += offset.x
                                        val maxSpan = WidgetGrid.COLUMNS - entry.col
                                        liveColSpan = Math.round(resizeWidthPx / cellWidthPx)
                                            .coerceIn(limits.minColSpan, maxSpan)
                                        resizeWidthPx = liveColSpan * cellWidthPx
                                    }
                                    if (limits.canResizeHeight) {
                                        resizeHeightPx += offset.y
                                        liveRowSpan = Math.round(resizeHeightPx / cellHeightPx)
                                            .coerceIn(limits.minRowSpan, limits.maxRowSpan)
                                        resizeHeightPx = liveRowSpan * cellHeightPx
                                    }
                                    if (liveColSpan != lastColSpan || liveRowSpan != lastRowSpan) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        lastColSpan = liveColSpan
                                        lastRowSpan = liveRowSpan
                                    }
                                    sizeReadout = "$liveColSpan × $liveRowSpan"
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

        if (isSelected) {
            Box(modifier = Modifier.offset(y = heightDp)) {
                WidgetEditToolbar(
                    palette = palette,
                    canForce = !limits.canResizeWidth || !limits.canResizeHeight || forceSize,
                    forced = forceSize,
                    onToggleForce = { forceSize = !forceSize },
                    onRemove = { onRemoveWidget(entry.id) },
                    onDone = { onDeselect() },
                    onMoveDragStart = { moveOffsetX = 0f; moveOffsetY = 0f },
                    onMoveDrag = { dx, dy ->
                        moveOffsetX += dx
                        moveOffsetY += dy
                    },
                    onMoveDragEnd = {
                        val targetCol = Math.round((entry.col * cellWidthPx + moveOffsetX) / cellWidthPx)
                            .coerceIn(0, WidgetGrid.COLUMNS - entry.colSpan)
                        val targetRow = Math.round((entry.row * cellHeightPx + moveOffsetY) / cellHeightPx)
                            .coerceAtLeast(0)
                        onPlace(entry.id, targetCol, targetRow, entry.colSpan, entry.rowSpan)
                        moveOffsetX = 0f
                        moveOffsetY = 0f
                    },
                )
            }
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
    onMoveDragEnd: () -> Unit,
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
                        onDragEnd = { onMoveDragEnd() },
                        onDragCancel = { onMoveDragEnd() },
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

/**
 * The single next alarm, system-wide - not a list. Android has no public API for a
 * launcher to enumerate every alarm across every app (that's each app's own internal
 * state); AlarmManager.nextAlarmClock is the one value every app that sets a visible
 * alarm reports to the system, which is exactly what the lock screen's own alarm
 * indicator is built on. Renders nothing at all when there's no alarm set, rather
 * than an empty slot - a minimalist launcher shouldn't reserve space for absence.
 */
@Composable
private fun NextAlarmBanner(millis: Long?, palette: ColorPalette) {
    if (millis == null) return
    val context = LocalContext.current
    val label = remember(millis) {
        val timeText = android.text.format.DateFormat.getTimeFormat(context).format(java.util.Date(millis))
        val isToday = android.text.format.DateUtils.isToday(millis)
        if (isToday) timeText else "$timeText · mañana"
    }
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpaceXs),
        modifier = Modifier.padding(top = SpaceXs),
    ) {
        Text(
            text = "⏰",
            fontSize = 12.sp,
        )
        Text(
            text = label,
            color = palette.textSecondary,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun ZenQuoteBanner(palette: ColorPalette) {
    val quote = remember { ZenQuotes.random() }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(600)),
    ) {
        ZenQuoteMarquee(quote = quote, palette = palette)
    }
}

/**
 * Drifts the quote from fully off-screen right to fully off-screen left, once per
 * loop, at a constant reading speed - a fixed duration would make short quotes race
 * by and long ones crawl, so the duration is derived from how far the text actually
 * has to travel instead of picked per quote.
 *
 * Needs both the container's width and the text's own unconstrained width before it
 * can compute that travel distance, so nothing moves until both have reported in via
 * onGloballyPositioned - a plain `Modifier.offset` with an unmeasured width would
 * either not move at all or jump once the real width arrives.
 */
@Composable
private fun ZenQuoteMarquee(quote: String, palette: ColorPalette) {
    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    var textWidthPx by remember { mutableFloatStateOf(0f) }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(quote, containerWidthPx, textWidthPx) {
        if (containerWidthPx <= 0f || textWidthPx <= 0f) return@LaunchedEffect
        val travelPx = containerWidthPx + textWidthPx
        val durationMs = (travelPx / MARQUEE_PX_PER_SECOND * 1000)
            .toInt()
            .coerceAtLeast(MARQUEE_MIN_DURATION_MS)
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMs, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = SpaceXs, bottom = SpaceXl)
            .clipToBounds()
            .onGloballyPositioned { containerWidthPx = it.size.width.toFloat() },
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
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .wrapContentWidth(unbounded = true)
                .onGloballyPositioned { textWidthPx = it.size.width.toFloat() }
                .offset {
                    // +containerWidth (fully hidden past the right edge) to
                    // -textWidth (fully hidden past the left edge) - right to left.
                    val x = containerWidthPx - progress.value * (containerWidthPx + textWidthPx)
                    IntOffset(x.roundToInt(), 0)
                },
        )
    }
}

// Constant travel speed rather than a fixed duration, so quote length doesn't change
// how fast it reads.
private const val MARQUEE_PX_PER_SECOND = 70f
private const val MARQUEE_MIN_DURATION_MS = 3000

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
    silhouette: Boolean,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val px = with(density) { sizeDp.toPx() }.toInt().coerceAtLeast(1)
    val accentArgb = accentColor.toArgb()
    // Keyed on the icon's own identity, not the Drawable instance, so scrolling a
    // row out of the LazyColumn and back in doesn't reprocess it every time.
    val bitmap = remember(app.key, px, silhouette, accentArgb) {
        val raw = app.icon.toBitmap(width = px, height = px)
        if (silhouette) silhouetteOf(raw, accentArgb) else raw
    }.asImageBitmap()
    Box(
        modifier = modifier.size(sizeDp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            colorFilter = if (monochrome && !silhouette) ColorFilter.tint(accentColor) else null,
            modifier = Modifier
                .fillMaxSize()
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(sizeDp * 0.24f)),
        )
    }
}

/**
 * Reduces an app icon to its dominant shape in a single accent color, instead of
 * merely tinting it (that only reads as a silhouette when the icon is already a
 * transparent glyph - most launcher icons are full-bleed with an opaque background,
 * where a tint just paints a solid accent square).
 *
 * A fixed brightness cutoff would fail just as often (a light glyph on a dark
 * background inverts it). Instead this treats whichever color dominates the icon's
 * own border as "background" and keeps whatever differs enough from it as
 * "foreground" - works for dark-on-light, light-on-dark, and colored-on-white alike.
 *
 * The first version sampled a single corner pixel, which broke on real icons: a
 * corner can land on anti-aliasing, a rounded-icon mask edge, or a soft gradient that
 * isn't representative of the background at all, and then almost the whole icon gets
 * classified as "foreground" - not a subtle miss, a solid tinted blob covering the
 * icon's shape entirely. Sampling the full border and taking its most common color is
 * far harder to throw off with one bad pixel. And when a result still comes out
 * essentially blank or essentially solid, that isn't a usable silhouette either way -
 * falling back to the original icon beats shipping a broken one.
 */
private fun silhouetteOf(source: Bitmap, accentArgb: Int): Bitmap {
    val width = source.width
    val height = source.height
    val pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)

    val bg = dominantBorderColor(pixels, width, height)
    val accentR = AndroidColor.red(accentArgb)
    val accentG = AndroidColor.green(accentArgb)
    val accentB = AndroidColor.blue(accentArgb)

    if (bg == null) {
        // The border is entirely transparent: this is already a real transparent-
        // background glyph (like an app that ships a proper adaptive icon foreground
        // layer), so the alpha channel alone is the mask - every opaque pixel is
        // foreground, exactly like the tint path already handled correctly.
        for (i in pixels.indices) {
            val alpha = AndroidColor.alpha(pixels[i])
            pixels[i] = if (alpha == 0) 0 else AndroidColor.argb(alpha, accentR, accentG, accentB)
        }
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    val bgR = AndroidColor.red(bg)
    val bgG = AndroidColor.green(bg)
    val bgB = AndroidColor.blue(bg)

    var opaqueCount = 0
    var foregroundCount = 0
    for (i in pixels.indices) {
        val pixel = pixels[i]
        val alpha = AndroidColor.alpha(pixel)
        if (alpha == 0) continue
        opaqueCount++
        val dr = AndroidColor.red(pixel) - bgR
        val dg = AndroidColor.green(pixel) - bgG
        val db = AndroidColor.blue(pixel) - bgB
        val distance = dr * dr + dg * dg + db * db
        if (distance > SILHOUETTE_THRESHOLD) {
            foregroundCount++
            pixels[i] = AndroidColor.argb(alpha, accentR, accentG, accentB)
        } else {
            pixels[i] = 0
        }
    }

    // A real glyph is a minority of the icon's area, not nearly all of it or next to
    // none of it - either extreme means the background reference didn't hold for this
    // particular icon, so the honest move is to hand back the untouched icon rather
    // than a shape nobody would recognize.
    if (opaqueCount == 0) return source
    val foregroundRatio = foregroundCount.toFloat() / opaqueCount
    if (foregroundRatio < 0.03f || foregroundRatio > 0.85f) return source

    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    result.setPixels(pixels, 0, width, 0, 0, width, height)
    return result
}

/** Most common color among the icon's opaque border pixels, or null if the border is
 * entirely transparent (a real transparent-background glyph, which needs no
 * background reference at all - every opaque pixel already is the foreground). */
private fun dominantBorderColor(pixels: IntArray, width: Int, height: Int): Int? {
    val counts = HashMap<Int, Int>()
    fun tally(x: Int, y: Int) {
        val pixel = pixels[y * width + x]
        if (AndroidColor.alpha(pixel) == 0) return
        counts[pixel] = (counts[pixel] ?: 0) + 1
    }
    for (x in 0 until width) {
        tally(x, 0)
        tally(x, height - 1)
    }
    for (y in 0 until height) {
        tally(0, y)
        tally(width - 1, y)
    }
    return counts.maxByOrNull { it.value }?.key
}

// Squared Euclidean distance in 0-255 RGB space; empirically distinguishes a
// genuinely different foreground color from anti-aliasing noise around the
// background without a real reference set of icons to tune against.
private const val SILHOUETTE_THRESHOLD = 60 * 60

@Composable
private fun FavoritesRow(
    apps: List<AppInfo>,
    iconSizeFactor: Float,
    monochrome: Boolean,
    silhouette: Boolean,
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
                    silhouette = silhouette,
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

    Column(
        modifier = modifier
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
    silhouette: Boolean,
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
            silhouette = silhouette,
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
