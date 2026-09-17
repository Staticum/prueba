package com.staticum.diariocalorico.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.staticum.diariocalorico.data.MealType

@Composable
fun MealTypeBreakdownChart(breakdown: List<Pair<MealType, Int>>, modifier: Modifier = Modifier) {
    val total = breakdown.sumOf { it.second }.coerceAtLeast(1)
    Column(modifier = modifier) {
        breakdown.forEach { (type, calories) ->
            val percent = (calories * 100 / total)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text(type.label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(72.dp))
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .weight(percent.coerceAtLeast(1).toFloat())
                        .height(16.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                )
                if (percent < 100) {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight((100 - percent).toFloat()))
                }
                Text(" $percent% · $calories kcal", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
