package com.gtrack.auto

import android.content.Intent
import androidx.car.app.Session
class TrackCarSession : Session() {

    override fun onCreateScreen(intent: Intent): TrackNavigationScreen =
        TrackNavigationScreen(carContext)
}
