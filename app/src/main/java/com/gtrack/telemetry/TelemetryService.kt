package com.gtrack.telemetry

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.gtrack.R
import com.gtrack.obd.ObdConnectionState
import com.gtrack.obd.ObdConnector
import com.gtrack.obd.ObdDebugLog
import com.gtrack.obd.ObdConnectorFactory
import com.gtrack.obd.ObdLinkType
import com.gtrack.obd.ObdPidReader
import com.gtrack.obd.ObdPresetCatalog
import com.gtrack.obd.ObdSettingsStore
import com.gtrack.obd.ObdSnapshot
import com.gtrack.track.TrackManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.random.Random

/**
 * Service de premier plan : acquisition télémétrique, GPS, chronométrage et enregistrement CSV.
 */
class TelemetryService : Service() {

    companion object {
        private const val TAG = "TelemetryService"

        const val ACTION_START = "com.gtrack.telemetry.START"
        const val ACTION_DEMO = "com.gtrack.telemetry.DEMO"
        const val ACTION_STOP = "com.gtrack.telemetry.STOP"
        const val CHANNEL_ID = "gtrack_telemetry"
        const val NOTIFICATION_ID = 1001

        private var instance: TelemetryService? = null

        /** Intervalle min entre deux tentatives de reconnexion ELM327. */
        private const val OBD_RECONNECT_MIN_MS = 12_000L
        private const val OBD_RECONNECT_MAX_MS = 60_000L

        /** Évite d’activer/désactiver le GPS en boucle (icône qui clignote). */
        private const val GPS_POLL_INTERVAL_MS = 1_500L

        fun getInstance(): TelemetryService? = instance

        /** Valeurs au repos quand aucune session n'est active. */
        fun createIdleSample(): TelemetryData = TelemetryData(
            speedKmh = 0f,
            rpm = 0,
            waterTempC = 0f,
            oilTempC = 0f,
            turboPressureBar = 0f,
            lateralGForce = 0f,
            latitude = 0.0,
            longitude = 0.0,
            lapTimeMs = 0L,
            sectorTimeMs = 0L,
            deltaChronoMs = 0L,
            afr = 0f,
            ignitionAdvanceBtdc = 0f,
            engineLoadPercent = 0f,
            throttlePercent = 0f,
            o2VoltageV = 0f
        )

        fun createDemoSample(): TelemetryData = simulatedSample().copy(
            speedKmh = 142f,
            rpm = 6500,
            waterTempC = 92f,
            oilTempC = 108f,
            turboPressureBar = 1.2f,
            lateralGForce = 1.1f,
            lapTimeMs = 105_320L,
            deltaChronoMs = -1_100L,
            afr = 14.7f,
            ignitionAdvanceBtdc = 24f,
            engineLoadPercent = 85f,
            throttlePercent = 85f,
            o2VoltageV = 0.78f
        )
    }

    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(serviceJob + Dispatchers.Default)

    private val _telemetryFlow = MutableStateFlow(simulatedSample())
    val telemetryFlow: StateFlow<TelemetryData> = _telemetryFlow.asStateFlow()

    private val _recordingFlow = MutableStateFlow(false)
    val recordingFlow: StateFlow<Boolean> = _recordingFlow.asStateFlow()

    private lateinit var csvLogger: CsvSessionLogger
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var obdSettings: ObdSettingsStore
    private lateinit var obdConnector: ObdConnector
    private lateinit var obdPidReader: ObdPidReader
    private var activeLinkType: ObdLinkType = ObdLinkType.BLE
    private val trackManager = TrackManager()

    val trackState = trackManager.state

    private var acquisitionJob: Job? = null
    private var gpsPollJob: Job? = null
    private var useSimulation = true
    private var obdConfigured = false
    @Volatile private var scanPaused = false
    private var lastLatitude = 0.0
    private var lastLongitude = 0.0
    private var lastReconnectAttemptMs = 0L
    private var obdReconnectBackoffMs = OBD_RECONNECT_MIN_MS

    override fun onCreate() {
        super.onCreate()
        instance = this
        val appContext = applicationContext
        obdSettings = ObdSettingsStore(appContext)
        obdConnector = ObdConnectorFactory.create(appContext, scope, ObdLinkType.BLE)
        obdPidReader = ObdPidReader(obdConnector)
        scope.launch {
            val linkType = obdSettings.getLinkType()
            if (linkType != ObdLinkType.BLE) rebuildObdConnector(linkType)
            obdPidReader.setPreset(ObdPresetCatalog.byId(obdSettings.getPresetId()))
        }
        csvLogger = CsvSessionLogger(appContext)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext)
        createNotificationChannel()
    }

    private fun rebuildObdConnector(linkType: ObdLinkType) {
        if (::obdConnector.isInitialized) {
            obdConnector.disconnect()
        }
        activeLinkType = linkType
        obdConnector = ObdConnectorFactory.create(applicationContext, scope, linkType)
        if (::obdPidReader.isInitialized) {
            obdPidReader = ObdPidReader(obdConnector)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopAcquisition()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_DEMO -> startDemoSession()
            ACTION_START -> startObdSession()
            else -> Unit
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopAcquisition()
        if (::obdConnector.isInitialized) obdConnector.disconnect()
        scope.cancel()
        instance = null
        super.onDestroy()
    }

    fun setSimulationEnabled(enabled: Boolean) {
        useSimulation = enabled
    }

    /** `false` quand une session OBD réelle est active (adaptateur configuré). */
    fun isSimulationActive(): Boolean = useSimulation

    fun isObdConfigured(): Boolean = obdConfigured

    /** Session télémétrie réelle via ELM327 (pas la démo). */
    fun isLiveObdSession(): Boolean = obdConfigured && !useSimulation

    fun getObdPollStats(): Pair<Int, Long> =
        obdPidReader.lastPollMatchedCount to obdPidReader.lastPollAtMs

    fun getActivePresetId(): String = obdPidReader.getActivePreset().id

    fun getObdConnector(): ObdConnector = obdConnector

    fun getObdPidReader(): ObdPidReader = obdPidReader

    fun applyObdPreset(presetId: String) {
        obdPidReader.setPreset(ObdPresetCatalog.byId(presetId))
    }

    fun getTrackManager(): TrackManager = trackManager

    fun isRecording(): Boolean = _recordingFlow.value

    /** Démarre l'enregistrement CSV de la session courante (bouton REC). */
    fun startRecording() {
        if (_recordingFlow.value) return
        _recordingFlow.value = true
        scope.launch { csvLogger.startSession() }
    }

    /** Stoppe et clôture le fichier CSV en cours. */
    fun stopRecording() {
        if (!_recordingFlow.value) return
        _recordingFlow.value = false
        scope.launch { csvLogger.endSession() }
    }

    fun toggleRecording() {
        if (_recordingFlow.value) stopRecording() else startRecording()
    }

    /** Scanne les PID mode 01 supportés par le véhicule (pause le polling le temps du scan). */
    fun startPidScan() {
        if (!::obdConnector.isInitialized) {
            com.gtrack.obd.ObdDebugLog.logInfo("Scan PID indisponible — connecteur non initialisé")
            return
        }
        scope.launch {
            scanPaused = true
            try {
                com.gtrack.obd.ObdPidScanner.scan(obdConnector, readValues = true)
            } finally {
                scanPaused = false
            }
        }
    }

    /** Teste les PID constructeur (mode 22) du profil actif, header par header. */
    fun startMode22Scan() {
        if (!::obdConnector.isInitialized || !::obdPidReader.isInitialized) {
            com.gtrack.obd.ObdDebugLog.logInfo("Scan mode 22 indisponible — connecteur non initialisé")
            return
        }
        val queries = obdPidReader.getActivePreset().queries
        scope.launch {
            scanPaused = true
            try {
                com.gtrack.obd.ObdPidScanner.scanMode22(obdConnector, queries)
            } finally {
                scanPaused = false
            }
        }
    }

    private fun startDemoSession() {
        stopAcquisition()
        if (::obdConnector.isInitialized) {
            obdConnector.disconnect()
        }
        useSimulation = true
        obdConfigured = false
        obdReconnectBackoffMs = OBD_RECONNECT_MIN_MS
        if (!startTelemetryForeground(demoMode = true)) {
            stopSelf()
            return
        }
        startAcquisition()
    }

    private fun startObdSession() {
        useSimulation = false
        if (!startTelemetryForeground(demoMode = false)) {
            stopSelf()
            return
        }
        scope.launch {
            stopAcquisition()
            _telemetryFlow.value = createIdleSample()
            obdPidReader.reset()
            ObdDebugLog.clear()
            ObdDebugLog.logInfo("Démarrage session OBD")
            prepareObdSession()
            startAcquisition()
        }
    }

    /**
     * Mode démo : FGS « location » uniquement.
     * Session OBD : + connectedDevice si permission Bluetooth accordée.
     */
    private fun startTelemetryForeground(demoMode: Boolean): Boolean {
        val notification = buildNotification()
        val types = foregroundServiceTypes(demoMode)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, types)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed (types=$types)", e)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                true
            } catch (e2: Exception) {
                Log.e(TAG, "startForeground fallback failed", e2)
                false
            }
        }
    }

    private fun foregroundServiceTypes(demoMode: Boolean): Int {
        if (demoMode) return ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        val hasBt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return if (hasBt) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        } else {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        }
    }

    private fun startAcquisition() {
        acquisitionJob?.cancel()
        gpsPollJob?.cancel()
        trackManager.resetSession()
        acquisitionJob = scope.launch {
            while (isActive) {
                if (scanPaused) {
                    delay(80)
                    continue
                }
                if (!useSimulation) {
                    ensureObdConnected()
                }
                val sample = if (useSimulation) {
                    buildSimulatedSample()
                } else {
                    readFromObdAndGps()
                }
                _telemetryFlow.value = sample
                if (_recordingFlow.value) {
                    csvLogger.append(sample)
                }
                delay(if (useSimulation) 100L else 30L)
            }
        }
        if (!useSimulation && hasLocationPermission()) {
            gpsPollJob = scope.launch { pollGpsForTrackManager() }
        }
    }

    private fun stopAcquisition() {
        acquisitionJob?.cancel()
        acquisitionJob = null
        gpsPollJob?.cancel()
        gpsPollJob = null
        stopRecording()
    }

    private fun hasLocationPermission(): Boolean =
        checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Une requête GPS toutes les 1,5 s (pas toutes les 200 ms) pour éviter que la puce
     * s’allume/s’éteigne en permanence. En mode démo, le GPS réel n’est pas utilisé.
     */
    private suspend fun pollGpsForTrackManager() {
        while (scope.coroutineContext.isActive && !useSimulation) {
            if (!hasLocationPermission()) {
                delay(GPS_POLL_INTERVAL_MS)
                continue
            }
            try {
                val token = CancellationTokenSource()
                val location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    token.token
                ).awaitOrNull()
                location?.let {
                    lastLatitude = it.latitude
                    lastLongitude = it.longitude
                    trackManager.onGpsUpdate(it.latitude, it.longitude)
                }
            } catch (_: SecurityException) {
                break
            }
            delay(GPS_POLL_INTERVAL_MS)
        }
    }

    private suspend fun prepareObdSession() {
        val address = obdSettings.getDeviceAddress()
        val linkType = obdSettings.getLinkType()
        if (linkType != activeLinkType) {
            rebuildObdConnector(linkType)
        }
        obdConfigured = !address.isNullOrBlank()
        useSimulation = !obdConfigured
        if (obdConfigured) {
            val preset = ObdPresetCatalog.byId(obdSettings.getPresetId())
            obdPidReader.setPreset(preset)
            obdConnector.disconnect()
            delay(400)
            obdConnector.bindDeviceAddress(address!!)
            obdConnector.connect()
        }
    }

    private fun ensureObdConnected() {
        if (!obdConfigured || useSimulation) return
        val state = obdConnector.connectionState.value
        if (state == ObdConnectionState.CONNECTED) {
            obdReconnectBackoffMs = OBD_RECONNECT_MIN_MS
            return
        }
        if (state == ObdConnectionState.CONNECTING) return
        val now = System.currentTimeMillis()
        if (now - lastReconnectAttemptMs < obdReconnectBackoffMs) return
        lastReconnectAttemptMs = now
        Log.d(TAG, "OBD reconnect attempt (backoff=${obdReconnectBackoffMs}ms)")
        obdConnector.connect()
        obdReconnectBackoffMs =
            (obdReconnectBackoffMs * 1.5).toLong().coerceAtMost(OBD_RECONNECT_MAX_MS)
    }

    private suspend fun readFromObdAndGps(): TelemetryData {
        val track = trackManager.state.value
        val prev = _telemetryFlow.value
        val obd: ObdSnapshot = obdPidReader.poll()
        return prev.copy(
            speedKmh = obd.speedKmh ?: prev.speedKmh,
            rpm = obd.rpm ?: prev.rpm,
            waterTempC = obd.waterTempC ?: prev.waterTempC,
            oilTempC = obd.oilTempC ?: prev.oilTempC,
            turboPressureBar = obd.turboPressureBar ?: prev.turboPressureBar,
            afr = obd.afr ?: prev.afr,
            ignitionAdvanceBtdc = obd.ignitionAdvanceBtdc ?: prev.ignitionAdvanceBtdc,
            engineLoadPercent = obd.engineLoadPercent ?: prev.engineLoadPercent,
            throttlePercent = obd.throttlePercent ?: prev.throttlePercent,
            o2VoltageV = obd.o2VoltageV ?: prev.o2VoltageV,
            pedalPercent = obd.pedalPercent ?: prev.pedalPercent,
            steeringAngleDeg = obd.steeringAngleDeg ?: prev.steeringAngleDeg,
            battCurrentA = obd.battCurrentA ?: prev.battCurrentA,
            battSocPct = obd.battSocPct ?: prev.battSocPct,
            battTempC = obd.battTempC ?: prev.battTempC,
            battVoltV = obd.battVoltV ?: prev.battVoltV,
            fuelLiters = obd.fuelLiters ?: prev.fuelLiters,
            engineTorqueNm = obd.engineTorqueNm ?: prev.engineTorqueNm,
            intakePressureMbar = obd.intakePressureMbar ?: prev.intakePressureMbar,
            atmoPressureMbar = obd.atmoPressureMbar ?: prev.atmoPressureMbar,
            brakePressureBar = obd.brakePressureBar ?: prev.brakePressureBar,
            oilPressureBar = obd.oilPressureBar ?: prev.oilPressureBar,
            gear = obd.gear ?: prev.gear,
            gearPre = obd.gearPre ?: prev.gearPre,
            intakeTempC = obd.intakeTempC ?: prev.intakeTempC,
            catTempC = obd.catTempC ?: prev.catTempC,
            extTempC = obd.extTempC ?: prev.extTempC,
            egtC = obd.egtC ?: prev.egtC,
            gearboxOilTempC = obd.gearboxOilTempC ?: prev.gearboxOilTempC,
            tirePressureFLBar = obd.tirePressureFLBar ?: prev.tirePressureFLBar,
            tirePressureFRBar = obd.tirePressureFRBar ?: prev.tirePressureFRBar,
            tirePressureRLBar = obd.tirePressureRLBar ?: prev.tirePressureRLBar,
            tirePressureRRBar = obd.tirePressureRRBar ?: prev.tirePressureRRBar,
            tireTempFLC = obd.tireTempFLC ?: prev.tireTempFLC,
            tireTempFRC = obd.tireTempFRC ?: prev.tireTempFRC,
            tireTempRLC = obd.tireTempRLC ?: prev.tireTempRLC,
            tireTempRRC = obd.tireTempRRC ?: prev.tireTempRRC,
            tpmsFL = obd.tpmsFL ?: prev.tpmsFL,
            tpmsFR = obd.tpmsFR ?: prev.tpmsFR,
            tpmsRL = obd.tpmsRL ?: prev.tpmsRL,
            tpmsRR = obd.tpmsRR ?: prev.tpmsRR,
            latitude = lastLatitude,
            longitude = lastLongitude,
            lapTimeMs = track.currentLapMs,
            sectorTimeMs = track.currentSectorMs,
            deltaChronoMs = track.deltaChronoMs,
            sectorIndex = track.sectorIndex,
            sectorDeltaPositive = track.sectorFasterThanBest,
            timestampEpochMs = System.currentTimeMillis()
        )
    }

    private fun buildSimulatedSample(): TelemetryData {
        val track = trackManager.state.value
        val base = simulatedSample()
        trackManager.onGpsUpdate(
            base.latitude + Random.nextDouble(-0.00005, 0.00005),
            base.longitude + Random.nextDouble(-0.00005, 0.00005)
        )
        val updatedTrack = trackManager.state.value
        val throttle = 40f + Random.nextFloat() * 60f
        val speed = 80f + Random.nextFloat() * 120f
        val turbo = 0.5f + Random.nextFloat() * 1.5f
        val atmo = 1008f + Random.nextFloat() * 12f
        val gear = when {
            speed < 30f -> 1
            speed < 60f -> 2
            speed < 95f -> 3
            speed < 130f -> 4
            speed < 170f -> 5
            else -> 6
        }
        return base.copy(
            lapTimeMs = updatedTrack.currentLapMs,
            sectorTimeMs = updatedTrack.currentSectorMs,
            deltaChronoMs = updatedTrack.deltaChronoMs,
            sectorIndex = updatedTrack.sectorIndex,
            sectorDeltaPositive = updatedTrack.sectorFasterThanBest,
            speedKmh = speed,
            rpm = 3000 + Random.nextInt(4000),
            lateralGForce = Random.nextFloat() * 2.5f - 1.25f,
            waterTempC = 88f + Random.nextFloat() * 20f,
            oilTempC = 95f + Random.nextFloat() * 28f,
            turboPressureBar = turbo,
            afr = 12.5f + Random.nextFloat() * 3f,
            ignitionAdvanceBtdc = 18f + Random.nextFloat() * 12f,
            engineLoadPercent = throttle,
            throttlePercent = throttle,
            o2VoltageV = 0.6f + Random.nextFloat() * 0.35f,
            pedalPercent = throttle,
            steeringAngleDeg = Random.nextFloat() * 360f - 180f,
            battCurrentA = 20f + Random.nextFloat() * 100f,
            battSocPct = 65f + Random.nextFloat() * 25f,
            battTempC = 28f + Random.nextFloat() * 12f,
            battVoltV = 13.6f + Random.nextFloat() * 0.8f,
            fuelLiters = 18f + Random.nextFloat() * 22f,
            engineTorqueNm = 80f + Random.nextFloat() * 240f,
            intakePressureMbar = atmo + turbo * 1000f,
            atmoPressureMbar = atmo,
            brakePressureBar = Random.nextFloat() * 30f,
            oilPressureBar = 2.5f + Random.nextFloat() * 2.5f,
            gear = gear,
            gearPre = gear,
            intakeTempC = 28f + Random.nextFloat() * 18f,
            catTempC = 350f + Random.nextFloat() * 350f,
            extTempC = 12f + Random.nextFloat() * 12f,
            egtC = 400f + Random.nextFloat() * 400f,
            gearboxOilTempC = 60f + Random.nextFloat() * 35f,
            tirePressureFLBar = 2.2f + Random.nextFloat() * 0.35f,
            tirePressureFRBar = 2.2f + Random.nextFloat() * 0.35f,
            tirePressureRLBar = 2.1f + Random.nextFloat() * 0.35f,
            tirePressureRRBar = 2.1f + Random.nextFloat() * 0.35f,
            tireTempFLC = 45f + Random.nextFloat() * 30f,
            tireTempFRC = 45f + Random.nextFloat() * 30f,
            tireTempRLC = 42f + Random.nextFloat() * 30f,
            tireTempRRC = 42f + Random.nextFloat() * 30f,
            tpmsFL = 0f,
            tpmsFR = 0f,
            tpmsRL = 0f,
            tpmsRR = 0f,
            latitude = base.latitude + Random.nextDouble(-0.00008, 0.00008),
            longitude = base.longitude + Random.nextDouble(-0.00008, 0.00008)
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_telemetry),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_telemetry_running))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitOrNull(): T? =
        suspendCancellableCoroutine { cont ->
            addOnSuccessListener { cont.resume(it) }
            addOnFailureListener { cont.resume(null) }
        }
}

private fun simulatedSample(): TelemetryData = TelemetryData(
    speedKmh = 0f,
    rpm = 800,
    waterTempC = 85f,
    oilTempC = 95f,
    turboPressureBar = 0.8f,
    lateralGForce = 0f,
    latitude = 47.2235,
    longitude = 14.7600,
    lapTimeMs = 0L,
    sectorTimeMs = 0L,
    deltaChronoMs = 0L,
    afr = 14.7f,
    ignitionAdvanceBtdc = 18f,
    engineLoadPercent = 0f,
    throttlePercent = 0f,
    o2VoltageV = 0.45f,
    pedalPercent = 0f,
    steeringAngleDeg = 0f,
    battCurrentA = 35f,
    battSocPct = 78f,
    battTempC = 30f,
    battVoltV = 14.1f,
    fuelLiters = 32f,
    engineTorqueNm = 0f,
    intakePressureMbar = 1013f,
    atmoPressureMbar = 1013f,
    brakePressureBar = 0f,
    oilPressureBar = 3.2f,
    gear = 0,
    gearPre = 0,
    intakeTempC = 32f,
    catTempC = 420f,
    extTempC = 18f,
    egtC = 450f,
    gearboxOilTempC = 72f,
    tirePressureFLBar = 2.4f,
    tirePressureFRBar = 2.4f,
    tirePressureRLBar = 2.3f,
    tirePressureRRBar = 2.3f,
    tireTempFLC = 55f,
    tireTempFRC = 55f,
    tireTempRLC = 52f,
    tireTempRRC = 52f,
    tpmsFL = 0f,
    tpmsFR = 0f,
    tpmsRL = 0f,
    tpmsRR = 0f
)
