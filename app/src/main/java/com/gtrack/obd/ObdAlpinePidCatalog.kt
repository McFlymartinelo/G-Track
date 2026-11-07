package com.gtrack.obd

/**
 * Catalogue PID Alpine (mode 22, format Torque / FT86) issu de Configuration_PIDs_Telemetrie.
 * [telemetryQueries] alimente la télémétrie G-Track (avec headers CAN et writers de snapshot).
 */
object ObdAlpinePidCatalog {

    /** Header CAN par défaut (ECU moteur) pour les PID marqués « ALL » et les PID mode 01. */
    const val DEFAULT_HEADER = "7E0"

    data class Definition(
        val name: String,
        val shortName: String,
        val modeAndPid: String,
        val equation: String,
        val units: String,
        val header: String = "ALL"
    )

    /** Liste documentaire complète (tableau constructeur). */
    val allDefinitions: List<Definition> = listOf(
        Definition("Angle Papillon", "A Pap", "221117", "100 * A / 255", "%"),
        Definition("Angle Pédale Accel", "A Ped Accel", "0149", "100 * A / 255", "%"),
        Definition("Angle Volant", "A Vol", "220100", "0.1 * combine(A,B) - 3276.7", "°", "740"),
        Definition("Batterie Courant", "Batt I", "226082", "combine(A,B)", "A", "745"),
        Definition("Batterie SOC", "Batt SOC", "222958", "0.5 * A + 69", "%"),
        Definition("Batterie Tempé", "Batt T", "224039", "A - 40", "°C", "745"),
        Definition("Batterie Tension", "Batt U", "221103", "0.08 * A", "V"),
        Definition("Carburant restant", "Carb rest", "221114", "63 - 0.52 * A", "L"),
        Definition("Charge Moteur", "Ch Mot", "221117", "A / 2.55", "%"),
        Definition("Couple Moteur", "C Mot", "222004", "0.5 * combine(A,B) - 400", "Nm"),
        Definition("Pneu ArD TPMS Low", "P ArD TPMS L", "228030", "A", "-", "745"),
        Definition("Pneu ArD Press", "P ArD P", "228031", "0.0075 * combine(A,B)", "bar", "745"),
        Definition("Pneu ArD Tempé", "P ArD T", "228032", "A - 30", "°C", "745"),
        Definition("Pneu ArG TPMS Low", "P ArG TPMS L", "228023", "A", "-", "745"),
        Definition("Pneu ArG Press", "P ArG P", "228024", "0.0075 * combine(A,B)", "bar", "745"),
        Definition("Pneu ArG Tempé", "P ArG T", "228025", "A - 30", "°C", "745"),
        Definition("Pneu AvD TPMS Low", "P AvD TPMS L", "228016", "A", "-", "745"),
        Definition("Pneu AvD Press", "P AvD P", "228017", "0.0075 * combine(A,B)", "bar", "745"),
        Definition("Pneu AvD Tempé", "P AvD T", "228018", "A - 30", "°C", "745"),
        Definition("Pneu AvG TPMS Low", "P AvG TPMS L", "228009", "A", "-", "745"),
        Definition("Pneu AvG Press", "P AvG P", "228010", "0.0075 * combine(A,B)", "bar", "745"),
        Definition("Pneu AvG Tempé", "P AvG T", "228011", "A - 30", "°C", "745"),
        Definition("Pression Admission", "P Adm", "010B", "10 * MAP", "mbar"),
        Definition("Pression Atmo", "P Atmo", "222009", "combine(A,B) / 51.2", "mbar"),
        Definition("Pression Freins", "P Freins", "22223E", "A", "bar"),
        Definition("Pression Huile Moteur", "P Huil Mot", "221278", "0.005 * combine(A,B)", "bar"),
        Definition("Régime Moteur", "RPM Mot", "221201", "12.5 * combine(A,B)", "RPM"),
        Definition("Rapport Engagé", "Rap BV", "222C04", "A", "-"),
        Definition("Rapport Pré-Engagé", "Rap BV Pré", "222C03", "A", "-"),
        Definition("Tempé Admission", "T Adm", "221106", "A - 50", "°C"),
        Definition("Tempé Cata", "T Cata", "013C", "(combine(A,B))/10 - 40", "°C"),
        Definition("Tempé Exter", "T Ext", "2220DE", "0.1 * combine(A,B) - 400", "°C"),
        Definition("Tempé Gaz Échappement", "T Gaz Ech", "2230B2", "0.02 * combine(A,B)", "°C"),
        Definition("Tempé Huile Carter BV", "T Huile BV", "2220A1", "combine(A,B) / 4", "°C", "7E1"),
        Definition("Tempé Huile Moteur", "T Huile Mot", "22111F", "A - 50", "°C"),
        Definition("Tempé LdR", "T LdR", "221101", "A - 50", "°C"),
        Definition("Vitesse Vh", "Vit Vh", "221102", "2 * A", "km/h")
    )

    private fun ext(
        pid: String,
        modeAndPid: String?,
        header: String?,
        decoder: ObdPidDecoder,
        writer: ObdSnapshotWriter
    ) = ObdPidQuery(
        pidHex = pid,
        field = ObdTelemetryField.EXTENDED,
        modeAndPid = modeAndPid,
        decoder = decoder,
        header = header,
        writer = writer
    )

    /** PIDs interrogés pour le tableau de bord (capteurs principaux + métriques étendues). */
    fun telemetryQueries(): List<ObdPidQuery> = listOf(
        // Capteurs principaux (champs natifs)
        ObdPidQuery("1102", ObdTelemetryField.SPEED, "221102", ObdAlpineDecoders::speedKmh),
        ObdPidQuery("1201", ObdTelemetryField.RPM, "221201", ObdAlpineDecoders::rpm),
        ObdPidQuery("1117", ObdTelemetryField.THROTTLE, "221117", ObdAlpineDecoders::throttlePercent),
        ObdPidQuery("1117", ObdTelemetryField.ENGINE_LOAD, "221117", ObdAlpineDecoders::engineLoadPercent),
        ObdPidQuery("1101", ObdTelemetryField.COOLANT, "221101", ObdAlpineDecoders::coolantC),
        ObdPidQuery("111F", ObdTelemetryField.OIL_TEMP, "22111F", ObdAlpineDecoders::oilTempC),
        ObdPidQuery("2009", ObdTelemetryField.BAROMETRIC, "222009", ObdAlpineDecoders::baroKpa),
        ObdPidQuery(ObdPid.INTAKE_MAP, ObdTelemetryField.INTAKE_MAP),
        ObdPidQuery(ObdPid.O2_VOLTAGE_B1S1, ObdTelemetryField.O2_VOLTAGE),

        // Métriques étendues
        ext("49", null, null, ObdAlpineDecoders::pedalPercent) { s, v -> s.copy(pedalPercent = v) },
        ext("0100", "220100", "740", ObdAlpineDecoders::steeringDeg) { s, v -> s.copy(steeringAngleDeg = v) },
        ext("6082", "226082", "745", ObdAlpineDecoders::battCurrentA) { s, v -> s.copy(battCurrentA = v) },
        ext("2958", "222958", null, ObdAlpineDecoders::battSocPct) { s, v -> s.copy(battSocPct = v) },
        ext("4039", "224039", "745", ObdAlpineDecoders::battTempC) { s, v -> s.copy(battTempC = v) },
        ext("1103", "221103", null, ObdAlpineDecoders::battVoltV) { s, v -> s.copy(battVoltV = v) },
        ext("1114", "221114", null, ObdAlpineDecoders::fuelLiters) { s, v -> s.copy(fuelLiters = v) },
        ext("2004", "222004", null, ObdAlpineDecoders::torqueNm) { s, v -> s.copy(engineTorqueNm = v) },
        ext("2009p", "222009", null, ObdAlpineDecoders::atmoMbar) { s, v -> s.copy(atmoPressureMbar = v) },
        ext("223E", "22223E", null, ObdAlpineDecoders::brakeBar) { s, v -> s.copy(brakePressureBar = v) },
        ext("1278", "221278", null, ObdAlpineDecoders::oilPressureBar) { s, v -> s.copy(oilPressureBar = v) },
        ext("0B", null, null, ObdAlpineDecoders::intakePressureMbar) { s, v -> s.copy(intakePressureMbar = v) },
        ext("2C04", "222C04", null, ObdAlpineDecoders::gear) { s, v -> s.copy(gear = v.toInt()) },
        ext("2C03", "222C03", null, ObdAlpineDecoders::gear) { s, v -> s.copy(gearPre = v.toInt()) },
        ext("1106", "221106", null, ObdAlpineDecoders::intakeTempC) { s, v -> s.copy(intakeTempC = v) },
        ext("3C", null, null, ObdAlpineDecoders::catTempC) { s, v -> s.copy(catTempC = v) },
        ext("20DE", "2220DE", null, ObdAlpineDecoders::extTempC) { s, v -> s.copy(extTempC = v) },
        ext("30B2", "2230B2", null, ObdAlpineDecoders::egtC) { s, v -> s.copy(egtC = v) },
        ext("20A1", "2220A1", "7E1", ObdAlpineDecoders::gearboxOilTempC) { s, v -> s.copy(gearboxOilTempC = v) },

        // Pneus (header 745)
        ext("8009", "228009", "745", ObdAlpineDecoders::tpmsLow) { s, v -> s.copy(tpmsFL = v) },
        ext("8010", "228010", "745", ObdAlpineDecoders::tirePressureBar) { s, v -> s.copy(tirePressureFLBar = v) },
        ext("8011", "228011", "745", ObdAlpineDecoders::tireTempC) { s, v -> s.copy(tireTempFLC = v) },
        ext("8016", "228016", "745", ObdAlpineDecoders::tpmsLow) { s, v -> s.copy(tpmsFR = v) },
        ext("8017", "228017", "745", ObdAlpineDecoders::tirePressureBar) { s, v -> s.copy(tirePressureFRBar = v) },
        ext("8018", "228018", "745", ObdAlpineDecoders::tireTempC) { s, v -> s.copy(tireTempFRC = v) },
        ext("8023", "228023", "745", ObdAlpineDecoders::tpmsLow) { s, v -> s.copy(tpmsRL = v) },
        ext("8024", "228024", "745", ObdAlpineDecoders::tirePressureBar) { s, v -> s.copy(tirePressureRLBar = v) },
        ext("8025", "228025", "745", ObdAlpineDecoders::tireTempC) { s, v -> s.copy(tireTempRLC = v) },
        ext("8030", "228030", "745", ObdAlpineDecoders::tpmsLow) { s, v -> s.copy(tpmsRR = v) },
        ext("8031", "228031", "745", ObdAlpineDecoders::tirePressureBar) { s, v -> s.copy(tirePressureRRBar = v) },
        ext("8032", "228032", "745", ObdAlpineDecoders::tireTempC) { s, v -> s.copy(tireTempRRC = v) }
    )
}
