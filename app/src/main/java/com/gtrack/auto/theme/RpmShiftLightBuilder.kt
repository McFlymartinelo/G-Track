package com.gtrack.auto.theme

import androidx.car.app.model.CarColor
import androidx.car.app.model.CarText

/**
 * Barre type shift-light (vert → jaune → rouge) pour l'en-tête des pages widgets.
 */
object RpmShiftLightBuilder {

    private const val SEGMENTS = 16

    fun build(rpm: Int, redline: Int = CarVisualTheme.RPM_REDLINE): CarText {
        val parts = rpmBarParts(rpm, redline)
        return CarTextHelper.create(*parts.toTypedArray())
    }

    /** Titre compatible [GridTemplate.setTitle] (CharSequence simple). */
    fun buildTitlePlain(appTitle: String, rpm: Int): String {
        val lit = if (CarVisualTheme.RPM_REDLINE <= 0) 0
        else (rpm.toFloat() / CarVisualTheme.RPM_REDLINE * SEGMENTS).toInt().coerceIn(0, SEGMENTS)
        val bar = buildString {
            repeat(SEGMENTS) { index ->
                append(if (index < lit) '#' else '.')
            }
        }
        return "$appTitle  $bar"
    }

    /** Titre court pour laisser la place à la grille (Automotive). */
    fun buildTitleShort(appTitle: String, rpm: Int): String {
        val lit = if (CarVisualTheme.RPM_REDLINE <= 0) 0
        else (rpm.toFloat() / CarVisualTheme.RPM_REDLINE * 8).toInt().coerceIn(0, 8)
        val bar = "#".repeat(lit.coerceAtLeast(0)) + ".".repeat((8 - lit).coerceAtLeast(0))
        return "$appTitle  $rpm RPM  $bar"
    }

    fun buildTitleWithRpm(appTitle: String, rpm: Int): CarText {
        val parts = mutableListOf<Pair<String, CarColor>>()
        parts.add("$appTitle  " to CarColor.DEFAULT)
        parts.addAll(rpmBarParts(rpm, CarVisualTheme.RPM_REDLINE))
        return CarTextHelper.create(*parts.toTypedArray())
    }

    private fun rpmBarParts(rpm: Int, redline: Int): List<Pair<String, CarColor>> {
        val lit = if (redline <= 0) 0
        else (rpm.toFloat() / redline * SEGMENTS).toInt().coerceIn(0, SEGMENTS)

        return buildList {
            repeat(SEGMENTS) { index ->
                val segmentColor = when {
                    index < SEGMENTS / 3 -> CarColor.GREEN
                    index < (SEGMENTS * 2) / 3 -> CarColor.YELLOW
                    else -> CarColor.RED
                }
                val glyph = if (index < lit) "▮" else "▯"
                val color = if (index < lit) segmentColor else CarColor.DEFAULT
                add(glyph to color)
            }
        }
    }
}
