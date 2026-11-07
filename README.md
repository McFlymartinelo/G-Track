# G-Track Telemetry

Application Android native (Kotlin) pour circuit, compatible **Android Auto** (Car App Library), esthétique skeuomorphique inspirée **GTI Track Monitor**.

## Ce que le cahier des charges change

| Exigence | Implémentation |
|----------|----------------|
| 3 pages fixes | `Data 1`, `Data 2`, `Live Map` (`AutoTelemetryPage`) |
| Grille 2×3 + bordure rouge | `SkeuomorphicGaugeRenderer` (bitmap + contour rouge) |
| Fond carbone | `bg_carbon_fiber.xml` + `GridTemplate.setBackgroundImage` |
| Shift-light header | `RpmShiftLightBuilder` dans le titre de chaque page |
| Jauges analogiques / LED / AFR | Rendu Canvas par slot (`TelemetryWidgetSlot`) |
| Live Map + chrono | `MapWithContentTemplate` + schéma Red Bull Ring sectorisé |
| OBD ELM327 Bluetooth | `ObdBluetoothConnector`, `ObdPidReader`, `ObdSettingsStore` ; démo si aucun adaptateur |
| TrackManager 3 secteurs | `TrackManager`, `GeoFenceDetector` |
| CSV DashWare | `CsvSessionLogger` |

## Limitation Android Auto

La Car App Library **ne permet pas** les polices F1 personnalisées ni un layout HTML libre : le rendu skeuomorphique passe par des **bitmaps générées** affichées dans `GridTemplate`. Le host peut encore simplifier couleurs/tailles.

## Pages

1. **Data 1** — Speed (aiguille), RPM (LED), Turbo, Water, Oil (orange si chaud), G latéral (circulaire).
2. **Data 2** — AFR (barre Lean/STOICH/Rich), Ignition, Engine Load, Throttle, Delta (ROUGE/SLOW, VERT/FAST), O2.
3. **Live Map** — Carte GPS (host) + schéma circuit + LIVE DATA / SESSION / LAP HISTORY + état secteurs.

Navigation : **`<`** et **`>`** centrés en barre d’actions basse.

## Validation sur téléphone (prioritaire)

1. Appairer l’**ELM327** dans Paramètres Bluetooth du téléphone.
2. Installer l’APK, ouvrir **G-Track Telemetry** (grille 3×2).
3. Appuyer sur la **pastille OBD** (barre du haut) : vert = connecté, rouge = déconnecté/erreur, orange = connexion en cours.
4. Dans la popup : choisir l’adaptateur → **Permissions** → **Démarrer la session**.
5. Vérifier les jauges (RPM, vitesse, etc.) avec contact ou moteur allumé.

Android Auto / écran voiture : à brancher plus tard (`app-projected`).

PIDs lus en rotation : vitesse, RPM, papillon, charge, temp. eau, avance, MAP/turbo, O2/AFR, temp. huile (si supportée par l’ECU).

## Ouverture

Android Studio → Open `G_Track` → JDK 17 → Run + Desktop Head Unit pour Android Auto.
