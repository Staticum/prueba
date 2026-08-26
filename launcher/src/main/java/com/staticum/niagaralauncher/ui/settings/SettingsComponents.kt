package com.staticum.niagaralauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.staticum.niagaralauncher.ui.theme.ColorPalette

/**
 * Shared building blocks for the Settings screen.
 *
 * The design rules these encode, applied consistently everywhere:
 *  - Every interactive row is at least [MIN_TOUCH_TARGET] tall, so nothing is a
 *    bare piece of colored text that happens to be tappable.
 *  - Related settings live inside one rounded "card" surface, and cards are what
 *    separate groups - not hairline dividers between every single item.
 *  - A section header labels a *group of cards*, so scanning the screen reads as
 *    a few named areas instead of a dozen equally-weighted rows.
 */
private val MIN_TOUCH_TARGET = 56.dp
private val CARD_SHAPE = RoundedCornerShape(16.dp)

/** Slightly lifted surface for cards - derived from the palette so it works on
 * every theme, including a light one, without needing per-palette tuning. */
@Composable
fun cardSurface(palette: ColorPalette): Color =
    palette.textPrimary.copy(alpha = 0.06f)

@Composable
fun SettingsSectionHeader(text: String, palette: ColorPalette, first: Boolean = false) {
    Text(
        text = text.uppercase(),
        color = palette.accent,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 4.dp, top = if (first) 8.dp else 28.dp, bottom = 10.dp),
    )
}

/** Groups related rows onto one rounded surface. */
@Composable
fun SettingsCard(palette: ColorPalette, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CARD_SHAPE)
            .background(cardSurface(palette)),
    ) {
        content()
    }
}

/** Hairline separator *inside* a card, inset so it doesn't touch the card edges. */
@Composable
fun SettingsCardDivider(palette: ColorPalette) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp)
            .heightIn(min = 1.dp)
            .background(palette.textPrimary.copy(alpha = 0.08f)),
    )
}

/** Title + optional subtitle + optional trailing slot, on a full-height touch target. */
@Composable
fun SettingsRow(
    title: String,
    palette: ColorPalette,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = MIN_TOUCH_TARGET)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, color = palette.textPrimary, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = palette.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    palette: ColorPalette,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
) {
    SettingsRow(
        title = title,
        palette = palette,
        subtitle = subtitle,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = palette.background,
                    checkedTrackColor = palette.accent,
                ),
            )
        },
    )
}

/** A row that opens another screen: shows the current value plus a chevron, so it's
 * obvious it navigates rather than toggles. */
@Composable
fun SettingsNavRow(
    title: String,
    value: String,
    palette: ColorPalette,
    onClick: () -> Unit,
    subtitle: String? = null,
) {
    SettingsRow(
        title = title,
        palette = palette,
        subtitle = subtitle,
        onClick = onClick,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value, color = palette.accent, style = MaterialTheme.typography.bodyMedium)
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = palette.textSecondary,
                    modifier = Modifier.padding(start = 2.dp).size(20.dp),
                )
            }
        },
    )
}

/** Slider with its label and a live numeric readout - previously sliders had no
 * label and no value at all, so there was no way to tell what you had set. */
@Composable
fun SettingsSliderRow(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    palette: ColorPalette,
    onValueChange: (Float) -> Unit,
    valueLabel: (Float) -> String,
    subtitle: String? = null,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, color = palette.textPrimary, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = valueLabel(value),
                color = palette.accent,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
            )
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = palette.textSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
        )
    }
}

/** A real, tappable button surface - replaces the bare colored `Text` that used to
 * act as buttons all over Settings (no touch target, no affordance, no feedback). */
@Composable
fun SettingsButton(
    text: String,
    palette: ColorPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    enabled: Boolean = true,
) {
    val contentColor = when {
        !enabled -> palette.textSecondary
        filled -> palette.background
        else -> palette.accent
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (filled) {
                    Modifier.background(if (enabled) palette.accent else palette.textSecondary.copy(alpha = 0.3f))
                } else {
                    Modifier.border(1.dp, palette.accent.copy(alpha = if (enabled) 0.5f else 0.2f), RoundedCornerShape(12.dp))
                },
            )
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 44.dp)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * A palette swatch that actually shows what the palette looks like.
 *
 * The old version drew a single circle filled with `palette.background` - and since
 * four of the five presets are near-black, they all rendered as visually identical
 * dark circles with no way to tell them apart. This shows the background as the
 * body with the accent as a ring plus an inner dot, which is what actually differs
 * between them, and labels each one by name.
 */
@Composable
fun PaletteSwatch(
    palette: ColorPalette,
    selected: Boolean,
    themePalette: ColorPalette,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(end = 14.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(palette.background)
                .border(
                    width = if (selected) 3.dp else 2.dp,
                    color = if (selected) palette.accent else palette.accent.copy(alpha = 0.45f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(24.dp),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(palette.accent),
                )
            }
        }
        Text(
            text = palette.label,
            color = if (selected) themePalette.accent else themePalette.textSecondary,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            modifier = Modifier.padding(top = 7.dp),
        )
    }
}
