package com.staticum.niagaralauncher.widget

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/** Wraps a real [AppWidgetHostView] so it can be embedded inside Compose. The caller
 * is responsible for sizing it (e.g. via `Modifier.height(...)`) — [WidgetArea] in
 * `ui/home/HomeScreen.kt` drives this from a user-adjustable stored height. */
@Composable
fun ComposeAppWidgetHost(
    appWidgetId: Int,
    providerInfo: AppWidgetProviderInfo,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val host = WidgetHostProvider.get(context)
            host.createView(context, appWidgetId, providerInfo).apply {
                setAppWidget(appWidgetId, providerInfo)
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                )
            }
        },
        update = { view: AppWidgetHostView ->
            // Re-binding an already-bound widget on every unrelated recomposition
            // (e.g. adding/resizing a different widget nearby) has been seen to upset
            // some third-party widgets (media-session widgets in particular) badly
            // enough to crash the whole host app - only rebind when it actually changed.
            if (view.appWidgetId != appWidgetId) {
                view.setAppWidget(appWidgetId, providerInfo)
            }
        },
    )
}
