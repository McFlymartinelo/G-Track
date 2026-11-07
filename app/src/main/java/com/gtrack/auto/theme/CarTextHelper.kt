package com.gtrack.auto.theme

import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarText
import androidx.car.app.model.ForegroundCarColorSpan

/**
 * Création de [CarText] coloré pour la Car App Library (spans obligatoires).
 */
object CarTextHelper {

    fun create(text: String, color: CarColor = CarColor.DEFAULT): CarText {
        val span = SpannableString(text)
        span.setSpan(
            ForegroundCarColorSpan.create(color),
            0,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return CarText.create(span)
    }

    fun create(vararg parts: Pair<String, CarColor>): CarText {
        val builder = SpannableStringBuilder()
        parts.forEach { (text, color) ->
            val start = builder.length
            builder.append(text)
            builder.setSpan(
                ForegroundCarColorSpan.create(color),
                start,
                builder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return CarText.create(builder)
    }
}
