package com.gtrack.obd

/** Formules Alpine (liste constructeur mode 22 + quelques PID standard mode 01). */
object ObdAlpineDecoders {

    private fun a(bytes: IntArray): Float? = ObdExtendedPid.byteA(bytes)?.toFloat()
    private fun ab(bytes: IntArray): Float? = ObdExtendedPid.combineAb(bytes)?.toFloat()

    // Capteurs principaux (déjà câblés aux champs existants)
    fun speedKmh(bytes: IntArray): Float? = a(bytes)?.let { it * 2f }
    fun rpm(bytes: IntArray): Float? = ab(bytes)?.let { 12.5f * it }
    fun throttlePercent(bytes: IntArray): Float? = a(bytes)?.let { 100f * it / 255f }
    fun engineLoadPercent(bytes: IntArray): Float? = a(bytes)?.let { it / 2.55f }
    fun coolantC(bytes: IntArray): Float? = a(bytes)?.let { it - 50f }
    fun oilTempC(bytes: IntArray): Float? = a(bytes)?.let { it - 50f }
    fun baroMbar(bytes: IntArray): Float? = ab(bytes)?.let { it / 51.2f }
    fun baroKpa(bytes: IntArray): Float? = ab(bytes)?.let { it / 512f }
    fun turboBar(bytes: IntArray): Float? = a(bytes)?.let { it / 1000f }

    // Métriques étendues (écrites via writer dans ObdSnapshot)
    fun pedalPercent(bytes: IntArray): Float? = a(bytes)?.let { 100f * it / 255f }
    fun steeringDeg(bytes: IntArray): Float? = ab(bytes)?.let { 0.1f * it - 3276.7f }
    fun battCurrentA(bytes: IntArray): Float? = ab(bytes)
    fun battSocPct(bytes: IntArray): Float? = a(bytes)?.let { 0.5f * it + 69f }
    fun battTempC(bytes: IntArray): Float? = a(bytes)?.let { it - 40f }
    fun battVoltV(bytes: IntArray): Float? = a(bytes)?.let { 0.08f * it }
    fun fuelLiters(bytes: IntArray): Float? = a(bytes)?.let { 63f - 0.52f * it }
    fun torqueNm(bytes: IntArray): Float? = ab(bytes)?.let { 0.5f * it - 400f }
    fun tirePressureBar(bytes: IntArray): Float? = ab(bytes)?.let { 0.0075f * it }
    fun tireTempC(bytes: IntArray): Float? = a(bytes)?.let { it - 30f }
    fun tpmsLow(bytes: IntArray): Float? = a(bytes)
    fun intakePressureMbar(bytes: IntArray): Float? = ObdPid.mapKpa(bytes)?.let { it * 10f }
    fun atmoMbar(bytes: IntArray): Float? = ab(bytes)?.let { it / 51.2f }
    fun brakeBar(bytes: IntArray): Float? = a(bytes)
    fun oilPressureBar(bytes: IntArray): Float? = ab(bytes)?.let { 0.005f * it }
    fun gear(bytes: IntArray): Float? = a(bytes)
    fun intakeTempC(bytes: IntArray): Float? = a(bytes)?.let { it - 50f }
    fun catTempC(bytes: IntArray): Float? = ab(bytes)?.let { it / 10f - 40f }
    fun extTempC(bytes: IntArray): Float? = ab(bytes)?.let { 0.1f * it - 400f }
    fun egtC(bytes: IntArray): Float? = ab(bytes)?.let { 0.02f * it }
    fun gearboxOilTempC(bytes: IntArray): Float? = ab(bytes)?.let { it / 4f }
}
