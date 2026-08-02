package com.staticum.urodiario.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.staticum.urodiario.data.UrineColor

@Composable
fun ColorSwatch(color: UrineColor, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Box(
        modifier = modifier
            .size(size)
            .background(Color(color.colorHex), CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
    )
}

@Composable
fun ScaleSelector(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text("$label: $value", style = MaterialTheme.typography.bodyLarge)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            items(range.toList()) { option ->
                FilterChip(
                    selected = value == option,
                    onClick = { onValueChange(option) },
                    label = { Text(option.toString()) }
                )
            }
        }
    }
}
