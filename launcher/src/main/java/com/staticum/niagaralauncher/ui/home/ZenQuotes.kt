package com.staticum.niagaralauncher.ui.home

/** Short zen / minimalist phrases shown once each time Home enters composition. */
object ZenQuotes {
    private val phrases = listOf(
        "Menos, pero mejor.",
        "La quietud también es movimiento.",
        "Simplifica: lo esencial ya está ahí.",
        "Un solo respiro basta para empezar de nuevo.",
        "El espacio vacío también tiene forma.",
        "No hagas más. Haz lo que importa.",
        "La calma no se busca, se hace espacio para ella.",
        "Menos ruido, más presencia.",
        "Lo simple no es pobre, es claro.",
        "Cada cosa en su lugar, y nada de más.",
        "El silencio también responde.",
        "Ordena el espacio y la mente sigue.",
        "Vive con lo que eliges, no con lo que sobra.",
        "La atención es el lujo más simple.",
        "Deja ir lo que no necesitas hoy.",
        "Un momento a la vez es suficiente.",
        "La belleza está en lo que decides no tener.",
        "Respira antes de decidir.",
        "Lo esencial rara vez grita.",
        "El vacío no es ausencia, es posibilidad.",
        "Hoy, elige menos.",
        "La mente clara empieza en un espacio claro.",
        "No necesitas todo. Necesitas lo justo.",
    )

    fun random(): String = phrases.random()
}
