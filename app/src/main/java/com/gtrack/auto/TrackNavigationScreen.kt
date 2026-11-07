package com.gtrack.auto

import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.OnClickListener
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.gtrack.auto.pages.AutoTelemetryPage
import com.gtrack.telemetry.TelemetryData
import com.gtrack.telemetry.TelemetryService
import com.gtrack.track.TrackState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Navigation Data 1 / Data 2 / Live Map — phase 1 : grille 3×2 numérotée.
 */
class TrackNavigationScreen(carContext: CarContext) : Screen(carContext) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val templateFactory = SkeuomorphicTemplateFactory(carContext)

    private var telemetry: TelemetryData = TelemetryService.createDemoSample()
    private var trackState: TrackState = TrackState()
    private var currentPageIndex = 0
    /** Case affichée après appui (1–6) ; null = grille normale. */
    private var activeCellNumber: Int? = null
    /** Ligne choisie (0 = cases 1–3, 1 = cases 4–6) avant sélection du numéro. */
    private var pickerRowIndex: Int? = null

    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            when {
                activeCellNumber != null -> dismissCell()
                pickerRowIndex != null -> closeRowPicker()
            }
            syncBackCallback()
        }
    }

    init {
        carContext.onBackPressedDispatcher.addCallback(this, backCallback)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                scope.launch {
                    val service = TelemetryService.getInstance()
                    if (service != null) {
                        combine(service.telemetryFlow, service.trackState) { sample, track ->
                            sample to track
                        }.collectLatest { (sample, track) ->
                            telemetry = sample
                            trackState = track
                            invalidate()
                        }
                    }
                }
            }

            override fun onDestroy(owner: LifecycleOwner) {
                scope.cancel()
            }
        })
    }

    override fun onGetTemplate(): Template = try {
        when {
            activeCellNumber != null -> templateFactory.buildCellFocus(
                activeCellNumber!!,
                OnClickListener { dismissCell() }
            )
            pickerRowIndex != null -> templateFactory.buildCellPickerList(
                cellNumbers = cellsForRow(pickerRowIndex!!),
                onCellPress = { cellNumber ->
                    closeRowPicker()
                    onCellPressed(cellNumber)
                },
                onBack = OnClickListener { closeRowPicker() }
            )
            else -> {
                val page = AutoTelemetryPage.fromIndex(currentPageIndex)
                templateFactory.buildPage(
                    page = page,
                    data = telemetry,
                    track = trackState,
                    pageIndex = currentPageIndex,
                    activeCellNumber = null,
                    onPrev = OnClickListener { navigate(-1) },
                    onNext = OnClickListener { navigate(+1) },
                    onDismissCell = OnClickListener { dismissCell() },
                    onRowPress = { rowIndex -> openRowPicker(rowIndex) }
                )
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "onGetTemplate failed", e)
        val pane = Pane.Builder()
            .addRow(
                Row.Builder()
                    .setTitle("G-Track Telemetry")
                    .addText(e.message ?: "Erreur inconnue")
                    .build()
            )
            .build()
        PaneTemplate.Builder(pane).setTitle("Erreur").build()
    }

    /**
     * Appui sur une case (l'appui long n'existe pas dans la Car App Library — un appui déclenche le numéro).
     */
    private fun onCellPressed(cellNumber: Int) {
        activeCellNumber = cellNumber
        syncBackCallback()
        invalidate()
    }

    private fun dismissCell() {
        activeCellNumber = null
        syncBackCallback()
        invalidate()
    }

    private fun openRowPicker(rowIndex: Int) {
        pickerRowIndex = rowIndex
        syncBackCallback()
        invalidate()
    }

    private fun closeRowPicker() {
        pickerRowIndex = null
        syncBackCallback()
        invalidate()
    }

    private fun cellsForRow(rowIndex: Int): List<Int> {
        val first = rowIndex * 3 + 1
        return listOf(first, first + 1, first + 2)
    }

    private fun syncBackCallback() {
        backCallback.isEnabled = activeCellNumber != null || pickerRowIndex != null
    }

    private fun navigate(delta: Int) {
        activeCellNumber = null
        pickerRowIndex = null
        syncBackCallback()
        val total = AutoTelemetryPage.PAGES.size
        currentPageIndex = (currentPageIndex + delta + total) % total
        invalidate()
    }

    companion object {
        private const val TAG = "GTrackCar"
    }
}
