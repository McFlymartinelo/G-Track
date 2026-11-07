package com.gtrack.auto

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.OnClickListener
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.gtrack.R
import com.gtrack.widgets.WidgetManager
import com.gtrack.widgets.WidgetType
import com.gtrack.widgets.formatValue
import com.gtrack.telemetry.TelemetryService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Choix de la zone (1–6) avant le menu de type de widget.
 */
class WidgetSlotSelectScreen(
    carContext: CarContext,
    private val pageIndex: Int
) : Screen(carContext) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val widgetManager = WidgetManager(carContext)
    private val sampleData = TelemetryService.createDemoSample()
    private var zoneWidgets: List<WidgetType> = CarWidgetDefaults.data1()

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                scope.launch {
                    widgetManager.pagesFlow.collectLatest { pages ->
                        zoneWidgets = pages.getOrElse(pageIndex) {
                            CarWidgetDefaults.layoutForPage(
                                com.gtrack.auto.pages.AutoTelemetryPage.fromIndex(pageIndex)
                            )
                        }
                        invalidate()
                    }
                }
            }

            override fun onDestroy(owner: LifecycleOwner) {
                scope.cancel()
            }
        })
    }

    override fun onGetTemplate(): Template {
        val list = ItemList.Builder()
        list.addItem(
            Row.Builder()
                .setTitle(carContext.getString(R.string.done))
                .setOnClickListener(OnClickListener { finishConfiguration() })
                .build()
        )
        zoneWidgets.take(WidgetType.WIDGETS_PER_PAGE).forEachIndexed { index, type ->
            list.addItem(
                Row.Builder()
                    .setTitle(carContext.getString(R.string.widget_zone_label, index + 1))
                    .addText(zoneLabel(type))
                    .setOnClickListener(
                        OnClickListener {
                            screenManager.push(
                                WidgetPickerScreen(carContext, pageIndex, index)
                            )
                        }
                    )
                    .build()
            )
        }
        return ListTemplate.Builder()
            .setTitle(carContext.getString(R.string.widget_slot_select_title))
            .setSingleList(list.build())
            .setActionStrip(
                CarScreenActions.backStrip(
                    carContext,
                    OnClickListener { finishConfiguration() }
                )
            )
            .build()
    }

    private fun zoneLabel(type: WidgetType): String =
        carContext.getString(type.titleRes) + " — " + type.formatValue(sampleData)
}
