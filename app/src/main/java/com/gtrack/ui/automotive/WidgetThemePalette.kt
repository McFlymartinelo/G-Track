package com.gtrack.ui.automotive

import androidx.compose.ui.graphics.Color
import com.gtrack.widgets.WidgetVisualStyle

data class WidgetThemePalette(
    val track: Color,
    val accent: Color,
    val needle: Color,
    val segmentGreen: Color,
    val segmentYellow: Color,
    val segmentRed: Color,
    val valueUnit: Color,
    val label: Color,
    val cellGradientTop: Color,
    val cellGradientBottom: Color,
    val borderWidthDp: Float,
    val useValueBorder: Boolean,
    val arcStrokeFactor: Float,
    val thinStrokeFactor: Float,
    /** Valeur affichée au centre du cadran (pas de ligne en bas de la case). */
    val embedValueInGauge: Boolean = false,
    /** Icône + unité au centre du cadran (pas de ligne en bas de la case). */
    val embedIconInGauge: Boolean = false
)

fun WidgetVisualStyle.palette(): WidgetThemePalette = when (this) {
    WidgetVisualStyle.NEON -> WidgetThemePalette(
        track = Color(0xFF1A2540),
        accent = Color(0xFF00E5FF),
        needle = Color.White,
        segmentGreen = Color(0xFF00E5FF),
        segmentYellow = Color(0xFFFFD54F),
        segmentRed = Color(0xFFFF5252),
        valueUnit = Color(0xFF64B5F6),
        label = Color(0xFF607D8B),
        cellGradientTop = Color(0xFF0C1220),
        cellGradientBottom = Color(0xFF060A14),
        borderWidthDp = 0f,
        useValueBorder = false,
        arcStrokeFactor = 0.09f,
        thinStrokeFactor = 0.03f,
        embedValueInGauge = true
    )
    WidgetVisualStyle.EPURE -> WidgetThemePalette(
        track = Color(0xFF1E1E28),
        accent = Color(0xFFC0392B),
        needle = Color.White,
        segmentGreen = Color(0xFF27AE60),
        segmentYellow = Color(0xFFE67E22),
        segmentRed = Color(0xFFC0392B),
        valueUnit = Color(0xFF4A4A5E),
        label = Color(0xFF3A3A50),
        cellGradientTop = Color(0xFF13131A),
        cellGradientBottom = Color(0xFF13131A),
        borderWidthDp = 0f,
        useValueBorder = false,
        arcStrokeFactor = 0.04f,
        thinStrokeFactor = 0.02f
    )
    WidgetVisualStyle.GTI -> WidgetThemePalette(
        track = Color(0xFF2C2C30),
        accent = Color(0xFFE53935),
        needle = Color.White,
        segmentGreen = Color(0xFF43A047),
        segmentYellow = Color(0xFFFF8C00),
        segmentRed = Color(0xFFE53935),
        valueUnit = Color(0xFF8A8A8A),
        label = Color(0xFF707070),
        cellGradientTop = Color(0xFF1A1A1C),
        cellGradientBottom = Color(0xFF141416),
        borderWidthDp = 1f,
        useValueBorder = false,
        arcStrokeFactor = 0.14f,
        thinStrokeFactor = 0.06f
    )
    WidgetVisualStyle.PREMIUM -> WidgetThemePalette(
        track = Color(0xFF252525),
        accent = Color(0xFFFFD600),
        needle = Color.White,
        segmentGreen = Color(0xFFFFD600),
        segmentYellow = Color(0xFFFFB300),
        segmentRed = Color(0xFFFF5252),
        valueUnit = Color(0xFFB0BEC5),
        label = Color(0xFF90A4AE),
        cellGradientTop = Color(0xFF121212),
        cellGradientBottom = Color(0xFF080808),
        borderWidthDp = 0f,
        useValueBorder = false,
        arcStrokeFactor = 0.12f,
        thinStrokeFactor = 0.04f,
        embedValueInGauge = true
    )
    WidgetVisualStyle.VINTAGE -> WidgetThemePalette(
        track = Color(0xFF6D4C41),
        accent = Color(0xFFB71C1C),
        needle = Color(0xFF3E2723),
        segmentGreen = Color(0xFF558B2F),
        segmentYellow = Color(0xFFF9A825),
        segmentRed = Color(0xFFC62828),
        valueUnit = Color(0xFF5D4037),
        label = Color(0xFF8D6E63),
        cellGradientTop = Color(0xFF2C2419),
        cellGradientBottom = Color(0xFF1A1510),
        borderWidthDp = 1.5f,
        useValueBorder = true,
        arcStrokeFactor = 0.08f,
        thinStrokeFactor = 0.05f
    )
    WidgetVisualStyle.NIGHT -> WidgetThemePalette(
        track = Color(0xFF0D1B2A),
        accent = Color(0xFF00E5FF),
        needle = Color(0xFF80DEEA),
        segmentGreen = Color(0xFF26C6DA),
        segmentYellow = Color(0xFFFFD54F),
        segmentRed = Color(0xFFFF5252),
        valueUnit = Color(0xFF4DD0E1),
        label = Color(0xFF546E7A),
        cellGradientTop = Color(0xFF050810),
        cellGradientBottom = Color(0xFF000000),
        borderWidthDp = 1f,
        useValueBorder = false,
        arcStrokeFactor = 0.07f,
        thinStrokeFactor = 0.04f
    )
    WidgetVisualStyle.RACING_HUD -> WidgetThemePalette(
        track = Color(0xFF2A2A2C),
        accent = Color(0xFFE53935),
        needle = Color.White,
        segmentGreen = Color(0xFF43A047),
        segmentYellow = Color(0xFFFF8C00),
        segmentRed = Color(0xFFE53935),
        valueUnit = Color(0xFF8A8A8A),
        label = Color(0xFF707070),
        cellGradientTop = Color(0xFF1E1E20),
        cellGradientBottom = Color(0xFF18181A),
        borderWidthDp = 1f,
        useValueBorder = false,
        arcStrokeFactor = 0.04f,
        thinStrokeFactor = 0.02f
    )
    WidgetVisualStyle.STRUCTURED -> WidgetThemePalette(
        track = Color(0xFF3A3A3A),
        accent = Color(0xFF42A5F5),
        needle = Color.White,
        segmentGreen = Color(0xFF43A047),
        segmentYellow = Color(0xFFFF8C00),
        segmentRed = Color(0xFFE53935),
        valueUnit = Color(0xFF9E9E9E),
        label = Color(0xFF9E9E9E),
        cellGradientTop = Color(0xFF1E1E1E),
        cellGradientBottom = Color(0xFF161616),
        borderWidthDp = 0f,
        useValueBorder = false,
        arcStrokeFactor = 0.04f,
        thinStrokeFactor = 0.02f
    )
}
