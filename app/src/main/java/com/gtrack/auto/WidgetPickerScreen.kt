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
import kotlinx.coroutines.launch

/**
 * Menu de choix du type de widget pour une case (zone) de la grille 3×2.
 */
class WidgetPickerScreen(
    carContext: CarContext,
    private val pageIndex: Int,
    private val slotIndex: Int
) : Screen(carContext) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val widgetManager = WidgetManager(carContext)
    private val sampleData = TelemetryService.createDemoSample()

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
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
        list.addItem(
            Row.Builder()
                .setTitle(carContext.getString(R.string.back))
                .setOnClickListener(OnClickListener { popOneScreen() })
                .build()
        )
        WidgetType.pickerChoices().forEach { type ->
            list.addItem(
                Row.Builder()
                    .setTitle(widgetLabel(type))
                    .addText(type.formatValue(sampleData))
                    .setOnClickListener(
                        OnClickListener {
                            scope.launch {
                                widgetManager.replaceWidget(pageIndex, slotIndex, type)
                                finishConfiguration()
                            }
                        }
                    )
                    .build()
            )
        }
        return ListTemplate.Builder()
            .setTitle(
                carContext.getString(R.string.widget_picker_title, slotIndex + 1)
            )
            .setSingleList(list.build())
            .setActionStrip(
                CarScreenActions.backStrip(
                    carContext,
                    OnClickListener { popOneScreen() }
                )
            )
            .build()
    }

    private fun widgetLabel(type: WidgetType): String =
        carContext.getString(type.titleRes)
}
