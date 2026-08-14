package com.staticum.mientreno.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Orange40,
    secondary = Slate40,
    tertiary = Teal40,
    error = ErrorRed,
    background = SurfaceLight,
    surface = SurfaceLight
)

private val DarkColors = darkColorScheme(
    primary = Orange80,
    secondary = Slate80,
    tertiary = Teal80,
    error = ErrorRed,
    background = SurfaceDark,
    surface = SurfaceDark
)

private val DeportivoColors = darkColorScheme(
    primary = DeportivoPrimary,
    secondary = DeportivoSecondary,
    tertiary = DeportivoTertiary,
    error = ErrorRed,
    background = SurfaceDeportivo,
    surface = SurfaceDeportivo
)

private val NocturnoColors = darkColorScheme(
    primary = NocturnoPrimary,
    secondary = NocturnoSecondary,
    tertiary = NocturnoTertiary,
    error = ErrorRed,
    background = SurfaceNocturno,
    surface = SurfaceNocturno
)

private val CalmaColors = lightColorScheme(
    primary = CalmaPrimary,
    secondary = CalmaSecondary,
    tertiary = CalmaTertiary,
    error = ErrorRed,
    background = SurfaceCalma,
    surface = SurfaceCalma
)

enum class AppTheme(
    val displayName: String,
    val previewPrimary: Color,
    val previewSecondary: Color,
    val previewTertiary: Color
) {
    OSCURO("Oscuro", Orange80, Slate80, Teal80),
    CLARO("Claro", Orange40, Slate40, Teal40),
    DEPORTIVO("Deportivo", DeportivoPrimary, DeportivoSecondary, DeportivoTertiary),
    NOCTURNO("Nocturno", NocturnoPrimary, NocturnoSecondary, NocturnoTertiary),
    CALMA("Calma", CalmaPrimary, CalmaSecondary, CalmaTertiary)
}

private fun AppTheme.toColorScheme(): ColorScheme = when (this) {
    AppTheme.OSCURO -> DarkColors
    AppTheme.CLARO -> LightColors
    AppTheme.DEPORTIVO -> DeportivoColors
    AppTheme.NOCTURNO -> NocturnoColors
    AppTheme.CALMA -> CalmaColors
}

@Composable
fun MiEntrenoTheme(theme: AppTheme, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = theme.toColorScheme(),
        typography = AppTypography,
        content = content
    )
}
