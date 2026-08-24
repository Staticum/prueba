package com.staticum.niagaralauncher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun LauncherTheme(palette: ColorPalette, content: @Composable () -> Unit) {
    val isLight = palette == ColorPalette.LIGHT
    val scheme = if (isLight) {
        lightColorScheme(
            primary = palette.accent,
            background = palette.background,
            surface = palette.surface,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary,
        )
    } else {
        darkColorScheme(
            primary = palette.accent,
            background = palette.background,
            surface = palette.surface,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary,
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
