package com.gtrack.ui.automotive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gtrack.telemetry.OBDData
import com.gtrack.telemetry.TelemetryData
import com.gtrack.telemetry.toOBDData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.round
import kotlin.random.Random

class EpureDashboardViewModel : ViewModel() {

    private val _obdData = MutableStateFlow(OBDData())
    val obdData: StateFlow<OBDData> = _obdData.asStateFlow()

    private val _simulationActive = MutableStateFlow(false)
    val simulationActive: StateFlow<Boolean> = _simulationActive.asStateFlow()

    private var tickerJob: Job? = null

    fun startSimulation() {
        if (tickerJob?.isActive == true) return
        _simulationActive.value = true
        tickerJob = viewModelScope.launch {
            while (isActive) {
                _obdData.value = randomObdSample()
                delay(500)
            }
        }
    }

    fun stopSimulation() {
        tickerJob?.cancel()
        tickerJob = null
        _simulationActive.value = false
    }

    fun syncFromTelemetry(telemetry: TelemetryData) {
        _obdData.value = telemetry.toOBDData()
    }

    override fun onCleared() {
        stopSimulation()
        super.onCleared()
    }

    private fun randomObdSample(): OBDData = OBDData(
        speed = Random.nextInt(60, 220),
        rpm = Random.nextInt(1200, 6800),
        turbo = roundOneDecimal(0.3f + Random.nextFloat() * 1.5f),
        waterTemp = Random.nextInt(78, 112),
        oilTemp = Random.nextInt(88, 125),
        gForce = roundOneDecimal(Random.nextFloat() * 1.4f)
    )

    private fun roundOneDecimal(value: Float): Float =
        (round(value * 10f) / 10f)
}
