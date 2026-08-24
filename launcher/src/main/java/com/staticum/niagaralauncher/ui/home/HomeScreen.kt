package com.staticum.niagaralauncher.ui.home

import android.view.SoundEffectConstants
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.staticum.niagaralauncher.data.AppInfo
import com.staticum.niagaralauncher.data.SwipeDirection
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun HomeScreen(
    state: HomeUiState,
    isDefaultLauncher: Boolean,
    onQueryChange: (String) -> Unit,
    onLaunchApp: (AppInfo) -> Unit,
    onLongPressApp: (AppInfo) -> Unit,
    onOpenSettings: () -> Unit,
    onSwipe: (SwipeDirection) -> Unit,
    onSetAsDefaultLauncher: () -> Unit,
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
    val view = LocalView.current

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .drop(1)
            .collect { view.playSoundEffect(SoundEffectConstants.CLICK) }
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
                    onLetterSelected = { letter ->
                        val target = nearestAvailableLetter(letter, availableLetters) ?: return@AlphabetIndexBar
                        val index = state.visibleApps.indexOfFirst {
                            it.label.firstOrNull()?.uppercaseChar() == target
                        }
                        if (index >= 0) {
                            coroutineScope.launch { listState.scrollToItem(index) }
                        }
                    },
                    textColor = palette.textSecondary,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(24.dp),
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

@Composable
private fun AlphabetIndexBar(
    availableLetters: Set<Char>,
    onLetterSelected: (Char) -> Unit,
    textColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    var heightPx by remember { mutableFloatStateOf(0f) }

    fun selectLetterAt(y: Float) {
        if (heightPx <= 0f) return
        val index = (y / heightPx * ALPHABET.size).toInt().coerceIn(0, ALPHABET.size - 1)
        onLetterSelected(ALPHABET[index])
    }

    Column(
        modifier = modifier
            .onGloballyPositioned { heightPx = it.size.height.toFloat() }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { offset -> selectLetterAt(offset.y) })
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> selectLetterAt(offset.y) },
                    onDrag = { change, _ -> change.consume(); selectLetterAt(change.position.y) },
                )
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ALPHABET.forEach { letter ->
            Text(
                text = letter.toString(),
                color = if (letter in availableLetters) textColor else textColor.copy(alpha = 0.25f),
                style = MaterialTheme.typography.labelSmall,
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
