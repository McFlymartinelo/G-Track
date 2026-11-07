package com.gtrack.ui

import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** Masque barre de statut et navigation (plein écran immersif). */
fun Activity.enablePhoneFullscreen() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    hideSystemBars()
}

fun Activity.hideSystemBars() {
    val controller = WindowInsetsControllerCompat(window, window.decorView)
    controller.hide(WindowInsetsCompat.Type.systemBars())
    controller.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
}
