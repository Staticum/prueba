package com.staticum.niagaralauncher.widget

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.widget.FrameLayout
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/** Wraps a real [AppWidgetHostView] so it can be embedded inside Compose. */
@Composable
fun ComposeAppWidgetHost(
    appWidgetId: Int,
    providerInfo: AppWidgetProviderInfo,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier.height(providerInfo.minHeight.dp.coerceAtLeastDp(120.dp)),
        factory = { context ->
            val host = WidgetHostProvider.get(context)
            host.createView(context, appWidgetId, providerInfo).apply {
                setAppWidget(appWidgetId, providerInfo)
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                )
            }
        },
        update = { view: AppWidgetHostView ->
            view.setAppWidget(appWidgetId, providerInfo)
        },
    )
}

private fun androidx.compose.ui.unit.Dp.coerceAtLeastDp(min: androidx.compose.ui.unit.Dp) =
    if (this < min) min else this
