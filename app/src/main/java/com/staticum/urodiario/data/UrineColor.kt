package com.staticum.urodiario.data

enum class UrineColor(val label: String, val colorHex: Long) {
    TRANSPARENTE("Transparente", 0xFFF5F9FF),
    AMARILLO_PALIDO("Amarillo pálido", 0xFFFAF3C8),
    AMARILLO("Amarillo", 0xFFF4E04D),
    AMARILLO_OSCURO("Amarillo oscuro", 0xFFD9B310),
    AMBAR("Ámbar / miel", 0xFFC98A1E),
    MARRON("Marrón", 0xFF6B4423),
    ROSADO_ROJIZO("Rosado / rojizo (sangre)", 0xFFE0566E),
    NARANJA("Naranja", 0xFFE0792A),
    VERDOSO("Verdoso / azulado", 0xFF6FA98B),
    TURBIA("Turbia / con sedimento", 0xFFB9AE8D),
    OTRO("Otro", 0xFF9E9E9E)
}
