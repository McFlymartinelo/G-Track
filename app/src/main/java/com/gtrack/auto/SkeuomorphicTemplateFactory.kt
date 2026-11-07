package com.gtrack.auto

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.OnClickListener
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import com.gtrack.R
import com.gtrack.auto.pages.AutoTelemetryPage
import com.gtrack.auto.render.SkeuomorphicGaugeRenderer
import com.gtrack.auto.theme.CarVisualTheme
import com.gtrack.auto.theme.RpmShiftLightBuilder
import com.gtrack.telemetry.TelemetryData
import com.gtrack.track.TrackGeometry
import com.gtrack.track.TrackState

/**
 * Grille 3×2 (6 cases numérotées 1→6, gauche→droite, haut→bas).
 */
class SkeuomorphicTemplateFactory(private val context: Context) {

    private val gaugeRenderer = SkeuomorphicGaugeRenderer(context)
    private val trackGeometry = TrackGeometry.redBullRing()

    fun buildPage(
        page: AutoTelemetryPage,
        data: TelemetryData,
        track: TrackState,
        pageIndex: Int,
        activeCellNumber: Int?,
        onPrev: OnClickListener,
        onNext: OnClickListener,
        onDismissCell: OnClickListener,
        onRowPress: (Int) -> Unit
    ): Template = try {
        if (activeCellNumber != null) {
            buildCellFocus(activeCellNumber, onDismissCell)
        } else {
            val strip = buildBottomPagination(onPrev, onNext)
            when (page) {
                AutoTelemetryPage.DATA_1, AutoTelemetryPage.DATA_2 -> buildNumberedGrid3x2(
                    pageTitle = context.getString(page.titleResId),
                    pageIndex = pageIndex,
                    actionStrip = strip,
                    onRowPress = onRowPress
                )
                AutoTelemetryPage.LIVE_MAP -> buildLiveMapPage(data, track, pageIndex, strip)
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "buildPage failed", e)
        buildErrorPane(e.message ?: "Erreur template")
    }

    /** Plein écran : numéro de la case (réaction à l'appui sur la grille). */
    fun buildCellFocus(cellNumber: Int, onDismiss: OnClickListener): Template {
        val pane = Pane.Builder()
            .addRow(
                Row.Builder()
                    .setTitle(context.getString(R.string.cell_number_label, cellNumber))
                    .addText(context.getString(R.string.cell_number_hint))
                    .build()
            )
            .build()
        return PaneTemplate.Builder(pane)
            .setTitle(context.getString(R.string.cell_focus_title))
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(
                        Action.Builder()
                            .setTitle(context.getString(R.string.done))
                            .setOnClickListener(onDismiss)
                            .build()
                    )
                    .build()
            )
            .build()
    }

    /**
     * Grille 3×2 dessinée en bitmap, affichée en 2 [Row] dans un [PaneTemplate].
     *
     * Il n’existe pas de Row/Column façon Compose dans la Car App Library : [GridTemplate]
     * affiche des tuiles minuscules (layout imposé par l’hôte). Ici chaque ligne = une image
     * 3 cases en pleine largeur, empilée verticalement.
     */
    private fun buildNumberedGrid3x2(
        pageTitle: String,
        pageIndex: Int,
        actionStrip: ActionStrip,
        onRowPress: (Int) -> Unit
    ): Template {
        val paneBuilder = Pane.Builder()
        for (rowIndex in 0..1) {
            val bitmap = gaugeRenderer.renderNumberedGridRow(
                rowIndex = rowIndex,
                cellSize = GRID_ROW_CELL_PX
            )
            val icon = toCarIcon(bitmap, GRID_ROW_ICON_MAX_PX)
            val rowTitle = if (rowIndex == 0) {
                context.getString(R.string.grid_row_top)
            } else {
                context.getString(R.string.grid_row_bottom)
            }
            paneBuilder.addRow(
                Row.Builder()
                    .setTitle(rowTitle)
                    .setImage(icon)
                    .setOnClickListener(OnClickListener { onRowPress(rowIndex) })
                    .build()
            )
        }
        val pane = paneBuilder.build()
        val title =
            "${context.getString(R.string.app_name)} · $pageTitle ${pageIndex + 1}/${AutoTelemetryPage.PAGES.size}"
        val header = Header.Builder()
            .setTitle(
                buildString {
                    append(title)
                    append("\n")
                    append(context.getString(R.string.grid_tap_hint))
                }
            )
            .build()
        return PaneTemplate.Builder(pane)
            .setHeader(header)
            .setActionStrip(actionStrip)
            .build()
    }

    /** Choix d'une case dans la ligne tapée (3 numéros). */
    fun buildCellPickerList(
        cellNumbers: List<Int>,
        onCellPress: (Int) -> Unit,
        onBack: OnClickListener
    ): Template {
        val items = ItemList.Builder()
        for (cellNumber in cellNumbers) {
            items.addItem(
                Row.Builder()
                    .setTitle(context.getString(R.string.cell_number_label, cellNumber))
                    .setOnClickListener(OnClickListener { onCellPress(cellNumber) })
                    .build()
            )
        }
        val header = Header.Builder()
            .setTitle(context.getString(R.string.pick_cell_title))
            .setStartHeaderAction(
                Action.Builder(Action.BACK)
                    .setOnClickListener(onBack)
                    .build()
            )
            .build()
        return ListTemplate.Builder()
            .setHeader(header)
            .setSingleList(items.build())
            .build()
    }

    private fun buildLiveMapPage(
        data: TelemetryData,
        track: TrackState,
        pageIndex: Int,
        actionStrip: ActionStrip
    ): Template {
        val paneBuilder = Pane.Builder()
        try {
            val mapBitmap = gaugeRenderer.renderTrackMap(
                data,
                track,
                trackGeometry.circuitOutline
            )
            val mapIcon = toCarIcon(mapBitmap, MAP_ICON_MAX_PX)
            paneBuilder.addRow(
                Row.Builder()
                    .setTitle(context.getString(R.string.track_red_bull_ring))
                    .setImage(mapIcon)
                    .build()
            )
        } catch (e: Exception) {
            Log.w(TAG, "Track map bitmap failed", e)
            paneBuilder.addRow(
                Row.Builder()
                    .setTitle(context.getString(R.string.track_red_bull_ring))
                    .addText(
                        "${"%.0f".format(data.speedKmh)} km/h | ${CarVisualTheme.formatLapTime(data.lapTimeMs)}"
                    )
                    .build()
            )
        }
        paneBuilder
            .addRow(
                Row.Builder()
                    .setTitle(context.getString(R.string.section_live_data))
                    .addText(CarVisualTheme.formatLapTime(data.lapTimeMs))
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle(context.getString(R.string.section_sectors))
                    .addText("—")
                    .build()
            )

        return PaneTemplate.Builder(paneBuilder.build())
            .setTitle(
                "${context.getString(R.string.page_live_map)} ${pageIndex + 1}/${AutoTelemetryPage.PAGES.size}"
            )
            .setActionStrip(actionStrip)
            .build()
    }

    private fun buildErrorPane(message: String): Template {
        val pane = Pane.Builder()
            .addRow(
                Row.Builder()
                    .setTitle("G-Track Telemetry")
                    .addText(message)
                    .build()
            )
            .build()
        return PaneTemplate.Builder(pane)
            .setTitle("Erreur")
            .build()
    }

    fun buildBottomPagination(
        onPrev: OnClickListener,
        onNext: OnClickListener
    ): ActionStrip {
        val prevIcon = CarIcon.Builder(
            IconCompat.createWithResource(context, R.drawable.ic_car_prev)
        ).build()
        val nextIcon = CarIcon.Builder(
            IconCompat.createWithResource(context, R.drawable.ic_car_next)
        ).build()
        return ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setIcon(prevIcon)
                    .setOnClickListener(onPrev)
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setIcon(nextIcon)
                    .setOnClickListener(onNext)
                    .build()
            )
            .build()
    }

    private fun toCarIcon(source: Bitmap, maxPx: Int): CarIcon {
        val scale = minOf(1f, maxPx.toFloat() / maxOf(source.width, source.height))
        val w = (source.width * scale).toInt().coerceAtLeast(1)
        val h = (source.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(source, w, h, true)
        return CarIcon.Builder(IconCompat.createWithBitmap(scaled)).build()
    }

    companion object {
        private const val TAG = "GTrackCar"
        /** Pixels par case dans une bande (3 cases × cette largeur = largeur bitmap). */
        private const val GRID_ROW_CELL_PX = 640
        private const val GRID_ROW_ICON_MAX_PX = 1920
        private const val MAP_ICON_MAX_PX = 320
    }
}
