package com.staticum.diariocalorico.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta oscura, pensada como experiencia principal (no un tema claro con colores invertidos):
// fondo carbón cálido, verde vibrante como acento de salud/nutrición, superficies con jerarquía
// propia en vez de dejar que Material3 rellene con su lila genérico por defecto.

val BackgroundDark = Color(0xFF10120F)
val SurfaceDark = Color(0xFF1A1D18)
val SurfaceVariantDark = Color(0xFF262A22)
val SurfaceContainerDark = Color(0xFF20241D)

val OnBackgroundDark = Color(0xFFEDEFE9)
val OnSurfaceDark = Color(0xFFEDEFE9)
val OnSurfaceVariantDark = Color(0xFFB7BDAF)
val OutlineDark = Color(0xFF3E453A)
val OutlineVariantDark = Color(0xFF2C3227)

val GreenPrimary = Color(0xFF4ADE80)
val OnGreenPrimary = Color(0xFF06301A)
val GreenPrimaryContainer = Color(0xFF1F4D30)
val OnGreenPrimaryContainer = Color(0xFFBFF3D2)

val TealSecondary = Color(0xFF7DD3C0)
val OnTealSecondary = Color(0xFF07332B)
val TealSecondaryContainer = Color(0xFF1D4A41)
val OnTealSecondaryContainer = Color(0xFFCFF5EB)

val AmberTertiary = Color(0xFFFFC168)
val OnAmberTertiary = Color(0xFF452B00)
val AmberTertiaryContainer = Color(0xFF603F00)
val OnAmberTertiaryContainer = Color(0xFFFFDFB0)

val ErrorDark = Color(0xFFFF8A80)
val OnErrorDark = Color(0xFF4E0002)
val ErrorContainerDark = Color(0xFF6B0F0C)
val OnErrorContainerDark = Color(0xFFFFDAD5)

// Colores fijos para gráficos (no dependen del theme, pero calibrados para verse bien sobre fondo oscuro).
val ChartProtein = Color(0xFF4ADE80)
val ChartCarbs = Color(0xFFFFC168)
val ChartFat = Color(0xFFFF8A80)
val ChartExpenditure = Color(0xFFFFC168)
val ChartWeight = Color(0xFF7EC8F2)
val ChartGridLabel = Color(0xFFB7BDAF)

// Tema claro, por si en el futuro se agrega un selector; hoy la app abre siempre en oscuro.
val BackgroundLight = Color(0xFFF7FAF5)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFE3E9DD)
val OnBackgroundLight = Color(0xFF1A1C18)
val GreenPrimaryLight = Color(0xFF2E7D32)
val TealSecondaryLight = Color(0xFF2F7D6E)
val AmberTertiaryLight = Color(0xFF8A5700)

/** Para usar un Color de Compose en APIs de android.graphics (ej. Paint de un Canvas nativo). */
fun Color.toAndroidArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()
)
