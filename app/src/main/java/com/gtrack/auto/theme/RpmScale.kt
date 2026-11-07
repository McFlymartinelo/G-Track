package com.gtrack.auto.theme

/** Type de carburant retenu pour adapter l'échelle de régime. */
enum class FuelType(val redline: Int) {
    GASOLINE(7000),
    DIESEL(6000);

    companion object {
        /**
         * Mappe la valeur du PID standard 0151 (Fuel Type) vers un [FuelType].
         * 1 = essence, 4 = diesel (autres valeurs essence/hybride essence par défaut).
         */
        fun fromObdFuelTypeCode(code: Int): FuelType = when (code) {
            4, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 -> DIESEL
            else -> GASOLINE
        }
    }
}

/**
 * Source unique de vérité pour la zone rouge (redline) du compte-tours.
 * Mise à jour soit par détection OBD (PID 0151), soit par réglage manuel.
 */
object RpmScale {
    @Volatile
    var fuelType: FuelType = FuelType.GASOLINE
        private set

    /** Zone rouge courante en tr/min, dérivée du carburant. */
    val redline: Int get() = fuelType.redline

    fun setFuelType(type: FuelType) {
        fuelType = type
    }

    fun setFromObdFuelCode(code: Int) {
        setFuelType(FuelType.fromObdFuelTypeCode(code))
    }
}
