package com.staticum.niagaralauncher.widget

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
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
    val density = LocalDensity.current.density
    var sizePx by remember { mutableStateOf(IntSize.Zero) }

    AndroidView(
        modifier = modifier.onSizeChanged { sizePx = it },
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

            // Several widgets (the system digital clock among them) pick which of
            // their responsive layouts to render based on the size Android reports
            // to them via updateAppWidgetSize/updateAppWidgetOptions - without ever
            // calling it, they can be left with no valid size bucket and render
            // nothing at all. Push our actual on-screen size (in dp) whenever it's
            // known and changes, same as a real launcher's widget host would.
            if (sizePx.width > 0 && sizePx.height > 0) {
                val widthDp = (sizePx.width / density).toInt()
                val heightDp = (sizePx.height / density).toInt()
                view.updateAppWidgetSize(null, widthDp, heightDp, widthDp, heightDp)
            }
        },
    )
}
