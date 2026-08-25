package com.staticum.niagaralauncher.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.staticum.niagaralauncher.ui.theme.ColorPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

/** An "ambient display" style landing screen shown before the actual home screen -
 * a big clock/date, tap or swipe up to dismiss. This is NOT a real lock (Android
 * doesn't allow a normal app to replace the system's PIN/pattern/biometric lock),
 * just an optional visual step matching the minimalist/zen concept, similar to what
 * some AOD/ambient displays show. */
@Composable
fun AmbientLockScreen(palette: ColorPalette, onUnlock: () -> Unit) {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(1000)
        }
    }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE d 'de' MMMM", Locale.getDefault()) }

    val infiniteTransition = rememberInfiniteTransition(label = "hintPulse")
    val hintAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "hintAlpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onUnlock() })
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(onDragEnd = {}) { change, dragAmount ->
                    change.consume()
                    if (dragAmount < -8f) onUnlock()
                }
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = timeFormat.format(now),
                color = palette.textPrimary,
                style = MaterialTheme.typography.displayLarge,
            )
            Text(
                text = dateFormat.format(now).replaceFirstChar { it.uppercase() },
                color = palette.textSecondary,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }

        Text(
            text = "Desliza hacia arriba o toca para abrir",
            color = palette.textSecondary.copy(alpha = hintAlpha),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
        )
    }
}
