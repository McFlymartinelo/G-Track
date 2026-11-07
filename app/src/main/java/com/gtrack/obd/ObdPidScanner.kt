package com.gtrack.obd

/**
 * Scanner OBD-II : détecte les PID mode 01 réellement supportés par le calculateur
 * (via les bitmaps 0100/0120/0140/0160/0180/01A0) puis lit la valeur brute de chacun.
 * Résultats journalisés dans [ObdDebugLog].
 */
object ObdPidScanner {

    /** Bitmaps « PIDs supportés » par plage de 0x20. */
    private val SUPPORT_RANGES = listOf("00", "20", "40", "60", "80", "A0")

    /** Noms des PID mode 01 standards SAE (les plus courants). */
    private val STANDARD_NAMES: Map<String, String> = mapOf(
        "0103" to "Statut système carburant",
        "0104" to "Charge moteur calculée",
        "0105" to "Temp. liquide refroid.",
        "0106" to "Trim carburant CT B1",
        "0107" to "Trim carburant LT B1",
        "010A" to "Pression carburant",
        "010B" to "Pression admission (MAP)",
        "010C" to "Régime moteur",
        "010D" to "Vitesse véhicule",
        "010E" to "Avance allumage",
        "010F" to "Temp. air admission",
        "0110" to "Débit d'air (MAF)",
        "0111" to "Position papillon",
        "0114" to "Sonde O2 B1S1",
        "011C" to "Standard OBD",
        "011F" to "Durée depuis démarrage",
        "0121" to "Distance avec MIL",
        "0123" to "Pression rampe carburant",
        "012F" to "Niveau carburant",
        "0133" to "Pression barométrique",
        "013C" to "Temp. catalyseur B1S1",
        "0142" to "Tension module contrôle",
        "0143" to "Charge absolue",
        "0144" to "Ratio équivalence cmd",
        "0145" to "Position papillon relative",
        "0146" to "Temp. air ambiant",
        "0149" to "Position pédale accél. D",
        "014A" to "Position pédale accél. E",
        "014C" to "Papillon commandé",
        "015C" to "Temp. huile moteur",
        "015E" to "Débit carburant moteur",
        "0163" to "Couple moteur réel",
        "0178" to "Temp. gaz échappement B1"
    )

    /** Noms des PID constructeur (mode 22) du catalogue Alpine. */
    private val ALPINE_NAMES: Map<String, String> by lazy {
        ObdAlpinePidCatalog.allDefinitions.associate { it.modeAndPid.uppercase() to it.name }
    }

    /** Nom connu d'une commande («010C» ou «221201»), sinon null. */
    fun knownName(command: String): String? {
        val key = command.uppercase()
        return STANDARD_NAMES[key] ?: ALPINE_NAMES[key]
    }

    private fun nameSuffix(command: String): String =
        knownName(command)?.let { "  · $it" } ?: ""

    data class ScannedPid(val pid: Int, val bytes: IntArray) {
        val hex: String get() = "%02X".format(pid)
        val rawHex: String get() = bytes.joinToString(" ") { "%02X".format(it) }
    }

    suspend fun scan(connector: ObdConnector, readValues: Boolean = true): List<ScannedPid> {
        if (connector.connectionState.value != ObdConnectionState.CONNECTED) {
            ObdDebugLog.logInfo("Scan PID annulé — adaptateur non connecté")
            return emptyList()
        }
        ObdDebugLog.logInfo("════ Scan PID (mode 01) démarré ════")
        val supported = detectSupportedPids(connector)
        ObdDebugLog.logInfo("PIDs supportés détectés : ${supported.size}")

        val results = ArrayList<ScannedPid>(supported.size)
        if (readValues) {
            for (pid in supported) {
                val hex = "%02X".format(pid)
                val response = connector.sendCommand("01$hex")
                val bytes = response?.let { ObdPid.parseMode01Bytes(it, hex) }
                if (bytes != null) {
                    val scanned = ScannedPid(pid, bytes)
                    results.add(scanned)
                    ObdDebugLog.logInfo("PID 01$hex = ${scanned.rawHex}${nameSuffix("01$hex")}")
                } else {
                    ObdDebugLog.logInfo("PID 01$hex = (pas de donnée)${nameSuffix("01$hex")}")
                }
            }
        } else {
            ObdDebugLog.logInfo(
                "Supportés : " + supported.joinToString(" ") { "01%02X".format(it) }
            )
        }
        ObdDebugLog.logInfo("════ Scan PID terminé (${results.size} valeurs) ════")
        return results
    }

    data class Mode22Result(val header: String, val modeAndPid: String, val bytes: IntArray?) {
        val responded: Boolean get() = bytes != null
        val rawHex: String get() = bytes?.joinToString(" ") { "%02X".format(it) } ?: "—"
    }

    /**
     * Teste les PID constructeur (mode 22) déclarés dans [queries], header par header
     * (pose ATSH<header> puis envoie la commande Torque «22xxxx»). Journalise ceux qui répondent.
     */
    suspend fun scanMode22(connector: ObdConnector, queries: List<ObdPidQuery>): List<Mode22Result> {
        if (connector.connectionState.value != ObdConnectionState.CONNECTED) {
            ObdDebugLog.logInfo("Scan mode 22 annulé — adaptateur non connecté")
            return emptyList()
        }
        val mode22 = queries
            .filter { it.modeAndPid?.startsWith("22") == true }
            .distinctBy { (it.header ?: ObdAlpinePidCatalog.DEFAULT_HEADER) + it.modeAndPid }
        if (mode22.isEmpty()) {
            ObdDebugLog.logInfo("Aucun PID mode 22 dans le profil actif")
            return emptyList()
        }
        ObdDebugLog.logInfo("════ Scan mode 22 démarré (${mode22.size} PID) ════")

        val byHeader = mode22.groupBy { it.header ?: ObdAlpinePidCatalog.DEFAULT_HEADER }
        val results = ArrayList<Mode22Result>(mode22.size)
        var ok = 0
        for ((header, list) in byHeader) {
            connector.sendCommand("ATSH$header")
            ObdDebugLog.logInfo("── Header $header (${list.size} PID) ──")
            for (q in list) {
                val mp = q.modeAndPid!!.uppercase()
                val response = connector.sendCommand(mp)
                val bytes = ObdExtendedPid.parseBytes(response, mp)
                val result = Mode22Result(header, mp, bytes)
                results.add(result)
                if (bytes != null) {
                    ok++
                    ObdDebugLog.logInfo("$mp = ${result.rawHex} ✓${nameSuffix(mp)}")
                } else {
                    ObdDebugLog.logInfo("$mp = (pas de réponse)${nameSuffix(mp)}")
                }
            }
        }
        connector.sendCommand("ATSH${ObdAlpinePidCatalog.DEFAULT_HEADER}")
        ObdDebugLog.logInfo("════ Scan mode 22 terminé ($ok/${mode22.size} répondent) ════")
        return results
    }

    /** Interroge les 6 bitmaps et renvoie la liste triée des PID supportés (0x01..0xC0). */
    private suspend fun detectSupportedPids(connector: ObdConnector): List<Int> {
        val supported = sortedSetOf<Int>()
        for (range in SUPPORT_RANGES) {
            val response = connector.sendCommand("01$range") ?: continue
            val bytes = ObdPid.parseMode01Bytes(response, range) ?: continue
            if (bytes.size < 4) continue
            val base = range.toInt(16)
            var mask = 0L
            for (i in 0 until 4) mask = (mask shl 8) or bytes[i].toLong()
            for (i in 0 until 32) {
                val bit = 1L shl (31 - i)
                if (mask and bit != 0L) supported.add(base + i + 1)
            }
        }
        return supported.toList()
    }
}
