package com.staticum.niagaralauncher.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.staticum.niagaralauncher.ui.theme.ColorPalette
import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** RGB/HSV color picker - lets the user pick literally any color for a custom
 * accent, instead of choosing between a handful of fixed presets. A hue/saturation
 * wheel drives quick visual selection; a brightness slider and three RGB sliders
 * (with the exact 0-255 values shown) allow precise/exact colors too. */
@Composable
fun ColorPickerScreen(
    palette: ColorPalette,
    initialColor: Color,
    onColorChange: (Color) -> Unit,
    onBack: () -> Unit,
) {
    var color by remember { mutableStateOf(initialColor) }

    LaunchedEffect(color) {
        delay(150)
        onColorChange(color)
    }

    val r = (color.red * 255f).roundToInt().coerceIn(0, 255)
    val g = (color.green * 255f).roundToInt().coerceIn(0, 255)
    val b = (color.blue * 255f).roundToInt().coerceIn(0, 255)

    val hsv = remember(color) {
        val out = FloatArray(3)
        android.graphics.Color.RGBToHSV(r, g, b, out)
        out
    }
    var brightness by remember { mutableFloatStateOf(hsv[2]) }

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
            Text("Color personalizado", color = palette.textPrimary, style = MaterialTheme.typography.titleLarge)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, palette.textSecondary.copy(alpha = 0.3f), CircleShape),
            )
            Text(
                text = "R $r  G $g  B $b",
                color = palette.textPrimary,
                modifier = Modifier.padding(start = 12.dp),
            )
        }

        HueSaturationWheel(
            hue = hsv[0],
            saturation = hsv[1],
            brightness = brightness,
            onPick = { newHue, newSat ->
                color = Color(android.graphics.Color.HSVToColor(floatArrayOf(newHue, newSat, brightness)))
            },
        )

        Text(
            text = "Brillo",
            color = palette.textSecondary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 16.dp),
        )
        Slider(
            value = brightness,
            onValueChange = { newBrightness ->
                brightness = newBrightness
                color = Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv[0], hsv[1], newBrightness)))
            },
            valueRange = 0f..1f,
        )

        RgbSlider(label = "Rojo", value = r, textColor = palette.textPrimary, onValueChange = { newR ->
            color = Color(red = newR / 255f, green = g / 255f, blue = b / 255f)
        })
        RgbSlider(label = "Verde", value = g, textColor = palette.textPrimary, onValueChange = { newG ->
            color = Color(red = r / 255f, green = newG / 255f, blue = b / 255f)
        })
        RgbSlider(label = "Azul", value = b, textColor = palette.textPrimary, onValueChange = { newB ->
            color = Color(red = r / 255f, green = g / 255f, blue = newB / 255f)
        })
    }
}

@Composable
private fun RgbSlider(
    label: String,
    value: Int,
    textColor: Color,
    onValueChange: (Int) -> Unit,
) {
    Text(
        text = "$label  $value",
        color = textColor,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 12.dp),
    )
    Slider(
        value = value.toFloat(),
        onValueChange = { onValueChange(it.roundToInt().coerceIn(0, 255)) },
        valueRange = 0f..255f,
    )
}

@Composable
private fun HueSaturationWheel(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onPick: (hue: Float, saturation: Float) -> Unit,
) {
    val density = LocalDensity.current
    var sizePx by remember { mutableFloatStateOf(0f) }

    fun pickFromOffset(offset: Offset) {
        if (sizePx <= 0f) return
        val center = sizePx / 2f
        val dx = offset.x - center
        val dy = offset.y - center
        val distance = sqrt(dx * dx + dy * dy)
        val normalizedSat = (distance / center).coerceIn(0f, 1f)
        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        if (angle < 0f) angle += 360f
        onPick(angle, normalizedSat)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(vertical = 8.dp)
            .onGloballyPositioned { sizePx = min(it.size.width, it.size.height).toFloat() }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { pickFromOffset(it) })
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    pickFromOffset(change.position)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = min(size.width, size.height) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            // Approximate a smooth hue/saturation wheel: a sweep gradient for hue,
            // plus a radial fade to white/gray at the center for saturation.
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = (0..360 step 30).map { deg ->
                        Color(android.graphics.Color.HSVToColor(floatArrayOf(deg.toFloat(), 1f, brightness)))
                    },
                    center = center,
                ),
                radius = radius,
                center = center,
            )
            val core = Color(android.graphics.Color.HSVToColor(floatArrayOf(0f, 0f, brightness)))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(core, core.copy(alpha = 0f)),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        }

        if (sizePx > 0f) {
            val center = sizePx / 2f
            val radius = center * saturation
            val angleRad = Math.toRadians(hue.toDouble())
            val markerX = center + radius * cos(angleRad).toFloat()
            val markerY = center + radius * sin(angleRad).toFloat()
            val markerSizePx = with(density) { 18.dp.toPx() }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        androidx.compose.ui.unit.IntOffset(
                            (markerX - markerSizePx / 2f).roundToInt(),
                            (markerY - markerSizePx / 2f).roundToInt(),
                        )
                    }
                    .size(18.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.White, CircleShape),
            )
        }
    }
}
