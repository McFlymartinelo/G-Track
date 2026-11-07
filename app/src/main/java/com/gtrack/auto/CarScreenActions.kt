package com.gtrack.auto

import android.content.Context
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.OnClickListener
import androidx.core.graphics.drawable.IconCompat
import com.gtrack.R

object CarScreenActions {

    fun backStrip(context: Context, onBack: OnClickListener): ActionStrip {
        val backIcon = CarIcon.Builder(
            IconCompat.createWithResource(context, R.drawable.ic_car_prev)
        ).build()
        return ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setIcon(backIcon)
                    .setOnClickListener(onBack)
                    .build()
            )
            .build()
    }
}

/** Ferme tous les écrans de configuration et revient au tableau de bord. */
fun Screen.finishConfiguration() {
    screenManager.popToRoot()
}

fun Screen.popOneScreen() {
    screenManager.pop()
}
