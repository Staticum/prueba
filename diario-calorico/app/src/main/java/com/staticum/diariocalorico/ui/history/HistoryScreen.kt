package com.staticum.diariocalorico.ui.history

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.staticum.diariocalorico.data.MealWithPhotos
import com.staticum.diariocalorico.util.DateTimeFormatters
import java.io.File

@Composable
fun HistoryScreen(viewModel: HistoryViewModel, onBack: () -> Unit, onEditMeal: (Long) -> Unit) {
    val meals by viewModel.meals.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } }
            )
        }
    ) { padding ->
        if (meals.isEmpty()) {
            Column(
                modifier = Modifier.padding(padding).fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Todavía no registras comidas", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(meals) { item ->
                    HistoryRow(
                        item,
                        onDelete = { viewModel.deleteMeal(item.meal) },
                        onClick = { onEditMeal(item.meal.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(item: MealWithPhotos, onDelete: () -> Unit, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val photoPath = item.allFoodPhotoPaths.firstOrNull()
            if (photoPath != null) {
                Image(
                    painter = rememberAsyncImagePainter(File(photoPath)),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Filled.RestaurantMenu, contentDescription = null, modifier = Modifier.size(56.dp))
            }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text("${item.meal.mealType.label} · ${DateTimeFormatters.formatDateTime(item.meal.consumedAt)}", style = MaterialTheme.typography.bodyMedium)
                Text("${item.meal.calories} kcal · P:${item.meal.proteinGrams}g C:${item.meal.carbsGrams}g G:${item.meal.fatGrams}g", style = MaterialTheme.typography.bodySmall)
                Text(item.meal.detectedFoods, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar") }
        }
    }
}
