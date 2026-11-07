package com.gtrack.ui.automotive

import androidx.compose.ui.graphics.Color

/** Seuils et couleurs de la jauge pneu — modifier ici pour retuner l'affichage. */
object TireGaugeConfig {
    /** Échelle affichée sur l'arc (bar). */
    const val SCALE_MIN_BAR = 1.2f
    const val SCALE_MAX_BAR = 3.0f

    /** Zone cible (OK). */
    const val TARGET_MIN_BAR = 1.9f
    const val TARGET_MAX_BAR = 2.3f

    /** Marge « attention » autour de la zone cible (bar). */
    const val ATTENTION_MARGIN_BAR = 0.15f

    /** Épaisseur de l'arc (px logiques Compose ≈ dp). */
    const val ARC_STROKE_DP = 12f

    /** Opacité des zones filigrane sous le remplissage. */
    const val ZONE_FILIGREE_ALPHA = 0.24f

    val attentionMinBar: Float get() = TARGET_MIN_BAR - ATTENTION_MARGIN_BAR
    val attentionMaxBar: Float get() = TARGET_MAX_BAR + ATTENTION_MARGIN_BAR

    fun barToFraction(bar: Float): Float {
        val range = SCALE_MAX_BAR - SCALE_MIN_BAR
        if (range <= 0f) return 0f
        return ((bar - SCALE_MIN_BAR) / range).coerceIn(0f, 1f)
    }

    /**
     * Segment d'arc sur le demi-cercle supérieur (balayage horaire +180°).
     * fraction 0 = extrémité gauche, 1 = extrémité droite.
     */
    fun arcSegment(minBar: Float, maxBar: Float): Pair<Float, Float> {
        val f1 = barToFraction(minBar)
        val f2 = barToFraction(maxBar.coerceAtLeast(minBar))
        val startAngle = 180f + 180f * f1
        val sweepAngle = 180f * (f2 - f1)
        return startAngle to sweepAngle
    }

    /** Balayage du remplissage depuis la gauche (0 = vide, 180 = plein). */
    fun fillSweep(pressureBar: Float): Float {
        val fraction = barToFraction(pressureBar)
        return if (fraction <= 0.001f) 0f else 180f * fraction
    }

    fun status(pressureBar: Float, tpmsLow: Boolean): TirePressureStatus {
        if (tpmsLow || pressureBar < attentionMinBar || pressureBar > attentionMaxBar) {
            return TirePressureStatus.CRITIQUE
        }
        if (pressureBar in TARGET_MIN_BAR..TARGET_MAX_BAR) {
            return TirePressureStatus.OK
        }
        return TirePressureStatus.ATTENTION
    }
}

enum class TirePressureStatus(val label: String) {
    OK("OK"),
    ATTENTION("ATTENTION"),
    CRITIQUE("CRITIQUE");

    val color: Color
        get() = when (this) {
            OK -> TireGaugeColors.ok
            ATTENTION -> TireGaugeColors.attention
            CRITIQUE -> TireGaugeColors.critical
        }
}

object TireGaugeColors {
    val trackBg = Color(0xFF26282E)
    val labelSecondary = Color(0xFF8A8D94)
    val textPrimary = Color(0xFFF2F2F2)
    val selectorAccent = Color(0xFF5DCAEB)
    val selectorInactive = Color(0xFF4A4D54)
    val divider = Color(0xFF2A2C32)
    val ok = Color(0xFF3FAE5C)
    val attention = Color(0xFFE0A233)
    val critical = Color(0xFFD65C5C)

    fun zoneRed(alpha: Float = TireGaugeConfig.ZONE_FILIGREE_ALPHA) = critical.copy(alpha = alpha)
    fun zoneGreen(alpha: Float = TireGaugeConfig.ZONE_FILIGREE_ALPHA) = ok.copy(alpha = alpha)
}
