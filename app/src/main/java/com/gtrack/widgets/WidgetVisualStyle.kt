package com.gtrack.widgets

import androidx.annotation.StringRes
import com.gtrack.R

/** Style d’affichage des widgets de la grille télémétrie. */
enum class WidgetVisualStyle(
    val storageKey: String,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int
) {
    GTI("gti", R.string.widget_style_gti_title, R.string.widget_style_gti_subtitle),
    EPURE("epure", R.string.widget_style_epure_title, R.string.widget_style_epure_subtitle),
    PREMIUM("premium", R.string.widget_style_premium_title, R.string.widget_style_premium_subtitle),
    NEON("neon", R.string.widget_style_neon_title, R.string.widget_style_neon_subtitle),
    RACING_HUD("racing_hud", R.string.widget_style_racing_hud_title, R.string.widget_style_racing_hud_subtitle),
    STRUCTURED("structured", R.string.widget_style_structured_title, R.string.widget_style_structured_subtitle),
    VINTAGE("vintage", R.string.widget_style_vintage_title, R.string.widget_style_vintage_subtitle),
    NIGHT("night", R.string.widget_style_night_title, R.string.widget_style_night_subtitle);

    /** Styles basés sur barres / chiffres (concepts maquettes). */
    fun usesConceptLayout(): Boolean = when (this) {
        GTI, EPURE, RACING_HUD, STRUCTURED -> true
        else -> false
    }

    companion object {
        private val removedStorageKeys = setOf("lcd", "hud", "minimal")

        fun fromStorageKey(key: String?): WidgetVisualStyle {
            if (key in removedStorageKeys) return GTI
            return entries.find { it.storageKey == key } ?: GTI
        }

        val pickerOrder: List<WidgetVisualStyle> = listOf(
            GTI,
            EPURE,
            PREMIUM,
            NEON,
            RACING_HUD,
            STRUCTURED,
            VINTAGE,
            NIGHT
        )
    }
}
