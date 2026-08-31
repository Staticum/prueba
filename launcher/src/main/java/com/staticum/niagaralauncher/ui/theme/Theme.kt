package com.staticum.niagaralauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** The fonts a user can pick in Settings, restricted to the generic families Android
 * itself guarantees on every device (System.SANS_SERIF/SERIF/MONOSPACE, plus the
 * cursive/casual typeface most manufacturers ship) - no font files to bundle, no APK
 * size cost, and no risk of a family silently falling back on some device. */
object AppFonts {
    const val DEFAULT_ID = "default"
    const val SERIF_ID = "serif"
    const val SANS_ID = "sans"
    const val MONOSPACE_ID = "monospace"
    const val CURSIVE_ID = "cursive"

    /** (id, display label) in the order shown in Settings. */
    val OPTIONS = listOf(
        DEFAULT_ID to "Predeterminada",
        SANS_ID to "Sans serif",
        SERIF_ID to "Serif",
        MONOSPACE_ID to "Monoespaciada",
        CURSIVE_ID to "Cursiva",
    )

    fun familyFor(id: String): FontFamily = when (id) {
        SERIF_ID -> FontFamily.Serif
        SANS_ID -> FontFamily.SansSerif
        MONOSPACE_ID -> FontFamily.Monospace
        CURSIVE_ID -> FontFamily.Cursive
        else -> FontFamily.Default
    }

    fun labelFor(id: String): String = OPTIONS.firstOrNull { it.first == id }?.second ?: OPTIONS[0].second
}

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
 *
 * Takes the font family as a parameter, rather than being a fixed value, so the
 * whole ramp can be rebuilt with a different typeface without touching every size
 * and weight decision above.
 */
private fun launcherTypography(family: FontFamily): Typography = Typography().let { base ->
    base.copy(
        headlineSmall = base.headlineSmall.copy(
            fontFamily = family,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp,
        ),
        titleLarge = base.titleLarge.copy(
            fontFamily = family,
            fontSize = 21.sp,
            lineHeight = 27.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.1).sp,
        ),
        titleMedium = base.titleMedium.copy(
            fontFamily = family,
            fontSize = 17.sp,
            lineHeight = 23.sp,
            fontWeight = FontWeight.Medium,
        ),
        titleSmall = base.titleSmall.copy(
            fontFamily = family,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.Medium,
        ),
        bodyLarge = base.bodyLarge.copy(
            fontFamily = family,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.1.sp,
        ),
        bodyMedium = base.bodyMedium.copy(
            fontFamily = family,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),
        bodySmall = base.bodySmall.copy(
            fontFamily = family,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.15.sp,
        ),
        labelLarge = base.labelLarge.copy(
            fontFamily = family,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.2.sp,
        ),
        labelMedium = base.labelMedium.copy(
            fontFamily = family,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.4.sp,
        ),
        labelSmall = base.labelSmall.copy(
            fontFamily = family,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.4.sp,
        ),
        displayLarge = TextStyle(
            fontFamily = family,
            fontSize = 76.sp,
            lineHeight = 80.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-2).sp,
        ),
    )
}

@Composable
fun LauncherTheme(palette: ColorPalette, fontFamilyId: String = AppFonts.DEFAULT_ID, content: @Composable () -> Unit) {
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
    val typography = launcherTypography(AppFonts.familyFor(fontFamilyId))
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
