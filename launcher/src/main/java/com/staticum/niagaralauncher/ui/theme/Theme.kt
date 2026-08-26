package com.staticum.niagaralauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * A deliberate type ramp instead of Material's stock sizes used ad hoc.
 *
 * The screens were mixing default `titleMedium`/`bodyLarge`/`labelSmall` with
 * per-call-site overrides, so the same conceptual level (a section label, a row
 * title) could render at different sizes and weights depending on which composable
 * drew it. Fixing the ramp once here means every screen inherits consistent
 * hierarchy: a small number of steps, each clearly distinct from its neighbours,
 * with tighter tracking on large text and looser on small - the usual convention
 * that makes text look typeset rather than defaulted.
 */
private val LauncherTypography = Typography().let { base ->
    base.copy(
        headlineSmall = base.headlineSmall.copy(
            fontSize = 24.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp,
        ),
        titleLarge = base.titleLarge.copy(
            fontSize = 21.sp,
            lineHeight = 27.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.1).sp,
        ),
        titleMedium = base.titleMedium.copy(
            fontSize = 17.sp,
            lineHeight = 23.sp,
            fontWeight = FontWeight.Medium,
        ),
        titleSmall = base.titleSmall.copy(
            fontSize = 15.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.Medium,
        ),
        bodyLarge = base.bodyLarge.copy(
            fontSize = 16.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.1.sp,
        ),
        bodyMedium = base.bodyMedium.copy(
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),
        bodySmall = base.bodySmall.copy(
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.15.sp,
        ),
        labelLarge = base.labelLarge.copy(
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.2.sp,
        ),
        labelMedium = base.labelMedium.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.4.sp,
        ),
        labelSmall = base.labelSmall.copy(
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.4.sp,
        ),
        displayLarge = TextStyle(
            fontSize = 76.sp,
            lineHeight = 80.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-2).sp,
        ),
    )
}

@Composable
fun LauncherTheme(palette: ColorPalette, content: @Composable () -> Unit) {
    val isLight = palette.id == ColorPalette.LIGHT.id
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
    MaterialTheme(colorScheme = scheme, typography = LauncherTypography, content = content)
}
