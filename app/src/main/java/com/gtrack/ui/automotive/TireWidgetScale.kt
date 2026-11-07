package com.gtrack.ui.automotive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

/** Métriques adaptatives selon la taille réelle de la cellule (téléphone / tablette / Auto). */
data class TireWidgetScale(
    val cellPaddingH: Dp,
    val cellPaddingV: Dp,
    val headerFont: TextUnit,
    val selectorSize: Dp,
    val selectorFont: TextUnit,
    val selectorGap: Dp,
    val arcStrokeDp: Float,
    val pressureFont: TextUnit,
    val pressureUnitFont: TextUnit,
    val tempFont: TextUnit,
    val tempUnitFont: TextUnit,
    val tpmsLabelFont: TextUnit,
    val tpmsStatusFont: TextUnit,
    val tpmsDotSize: Dp,
    val footerPaddingTop: Dp,
    val compact: Boolean
)

@Composable
fun rememberTireWidgetScale(maxWidth: Dp, maxHeight: Dp): TireWidgetScale {
    return remember(maxWidth, maxHeight) {
        val w = maxWidth.value
        val h = maxHeight.value
        val ref = min(w, h * 1.1f)
        val compact = h < 130f || w < 150f
        val medium = h < 170f || w < 200f

        TireWidgetScale(
            cellPaddingH = if (compact) 6.dp else if (medium) 8.dp else 12.dp,
            cellPaddingV = if (compact) 5.dp else if (medium) 7.dp else 10.dp,
            headerFont = scaledSp(ref, compact, 7.5f, 8.5f, 10f),
            selectorSize = if (compact) 18.dp else if (medium) 22.dp else 26.dp,
            selectorFont = scaledSp(ref, compact, 6.5f, 7.5f, 8f),
            selectorGap = if (compact) 2.dp else 4.dp,
            arcStrokeDp = when {
                compact -> 7f
                medium -> 9f
                else -> 12f
            },
            pressureFont = scaledSp(ref, compact, 16f, 22f, 28f),
            pressureUnitFont = scaledSp(ref, compact, 9f, 11f, 13f),
            tempFont = scaledSp(ref, compact, 14f, 20f, 26f),
            tempUnitFont = scaledSp(ref, compact, 9f, 12f, 16f),
            tpmsLabelFont = scaledSp(ref, compact, 7f, 8f, 9f),
            tpmsStatusFont = scaledSp(ref, compact, 8f, 9f, 11f),
            tpmsDotSize = if (compact) 6.dp else 8.dp,
            footerPaddingTop = if (compact) 4.dp else 8.dp,
            compact = compact
        )
    }
}

private fun scaledSp(ref: Float, compact: Boolean, small: Float, medium: Float, large: Float): TextUnit {
    val base = when {
        compact -> small
        ref < 200f -> medium
        else -> large
    }
    return maxOf(base, 6f).sp
}
