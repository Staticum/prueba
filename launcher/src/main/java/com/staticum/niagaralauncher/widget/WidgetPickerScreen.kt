package com.staticum.niagaralauncher.widget

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.staticum.niagaralauncher.ui.theme.ColorPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun WidgetPickerScreen(
    palette: ColorPalette,
    onBack: () -> Unit,
    onProviderSelected: (AppWidgetProviderInfo) -> Unit,
) {
    val context = LocalContext.current
    var providers by remember { mutableStateOf<List<AppWidgetProviderInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        providers = withContext(Dispatchers.IO) {
            WidgetHostProvider.manager(context).installedProviders
        }
    }

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
            Text("Elegir widget", color = palette.textPrimary, style = MaterialTheme.typography.titleLarge)
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(providers, key = { it.provider.flattenToString() }) { provider ->
                WidgetProviderRow(
                    provider = provider,
                    textColor = palette.textPrimary,
                    onClick = { onProviderSelected(provider) },
                )
            }
        }
    }
}

@Composable
private fun WidgetProviderRow(
    provider: AppWidgetProviderInfo,
    textColor: Color,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val packageManager = context.packageManager

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val icon = remember(provider) {
            runCatching { provider.loadIcon(context, 0) }.getOrNull()
                ?: runCatching { packageManager.getApplicationIcon(provider.provider.packageName) }.getOrNull()
        }
        if (icon != null) {
            Image(
                bitmap = icon.toBitmap(width = 96, height = 96).asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )
        }
        Text(
            text = runCatching { provider.loadLabel(packageManager) }.getOrNull() ?: provider.provider.className,
            color = textColor,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}
