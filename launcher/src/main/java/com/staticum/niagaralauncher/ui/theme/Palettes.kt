package com.staticum.niagaralauncher.ui.theme

import androidx.compose.ui.graphics.Color

/** A data class (not an enum) so a "Personalizado" entry can carry a live
 * user-chosen [accent] color picked from an RGB/HSV wheel, in addition to the
 * fixed preset palettes below. */
data class ColorPalette(
    val id: String,
    val label: String,
    val background: Color,
    val surface: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
) {
    companion object {
        val MATTE_BLACK = ColorPalette(
            id = "matte_black",
            label = "Negro mate",
            background = Color(0xFF000000),
            surface = Color(0xFF121212),
            accent = Color(0xFFE0E0E0),
            textPrimary = Color(0xFFF5F5F5),
            textSecondary = Color(0xFF9E9E9E),
        )
        val SLATE = ColorPalette(
            id = "slate",
            label = "Pizarra",
            background = Color(0xFF1B1F23),
            surface = Color(0xFF23272B),
            accent = Color(0xFF7FB3FF),
            textPrimary = Color(0xFFECEFF1),
            textSecondary = Color(0xFF90A4AE),
        )
        val FOREST = ColorPalette(
            id = "forest",
            label = "Bosque",
            background = Color(0xFF0D1F17),
            surface = Color(0xFF15291F),
            accent = Color(0xFF7CE0A0),
            textPrimary = Color(0xFFE8F5E9),
            textSecondary = Color(0xFF9CCC9F),
        )
        val SUNSET = ColorPalette(
            id = "sunset",
            label = "Atardecer",
            background = Color(0xFF241014),
            surface = Color(0xFF33161B),
            accent = Color(0xFFFF8A65),
            textPrimary = Color(0xFFFFF1EE),
            textSecondary = Color(0xFFD7A79A),
        )
        val LIGHT = ColorPalette(
            id = "light",
            label = "Claro",
            background = Color(0xFFFAFAFA),
            surface = Color(0xFFFFFFFF),
            accent = Color(0xFF1A73E8),
            textPrimary = Color(0xFF1A1A1A),
            textSecondary = Color(0xFF5F6368),
        )

        /** Id used for the user's own custom accent color (chosen via the RGB/HSV
         * picker) - keeps the matte-black background/surface/text, only the accent
         * is replaced with whatever [Color] the user picked. */
        const val CUSTOM_ID = "custom"
        val CUSTOM_BASE = ColorPalette(
            id = CUSTOM_ID,
            label = "Personalizado",
            background = MATTE_BLACK.background,
            surface = MATTE_BLACK.surface,
            accent = MATTE_BLACK.accent,
            textPrimary = MATTE_BLACK.textPrimary,
            textSecondary = MATTE_BLACK.textSecondary,
        )

        val PRESETS = listOf(MATTE_BLACK, SLATE, FOREST, SUNSET, LIGHT)

        fun fromId(id: String?): ColorPalette =
            PRESETS.firstOrNull { it.id == id } ?: if (id == CUSTOM_ID) CUSTOM_BASE else MATTE_BLACK
    }
}
