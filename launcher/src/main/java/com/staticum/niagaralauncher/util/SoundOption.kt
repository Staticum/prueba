package com.staticum.niagaralauncher.util

import com.staticum.niagaralauncher.R

/** A selectable scroll/index sound. All samples are short synthesized one-shots
 * (no looping) played once per index/letter change. */
enum class SoundOption(val id: String, val label: String, val resId: Int) {
    NONE("none", "Sin sonido", -1),
    TICK_SOFT("tick_soft", "Tick suave", R.raw.s_tick_soft),
    BELL("bell", "Campana", R.raw.s_bell),
    BOWL("bowl", "Cuenco tibetano", R.raw.s_bowl),
    CHIME("chime", "Campanita", R.raw.s_chime),
    WOOD("wood", "Bloque de madera", R.raw.s_wood),
    MARIMBA("marimba", "Marimba", R.raw.s_marimba),
    XYLOPHONE("xylophone", "Xilófono", R.raw.s_xylophone),
    DROP("drop", "Gota de agua", R.raw.s_drop),
    HARP("harp", "Arpa", R.raw.s_harp),
    SOFT_POP("soft_pop", "Pop suave", R.raw.s_soft_pop),
    ;

    companion object {
        val DEFAULT = TICK_SOFT

        fun fromId(id: String): SoundOption = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
