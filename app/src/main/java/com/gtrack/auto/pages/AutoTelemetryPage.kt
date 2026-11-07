package com.gtrack.auto.pages

import androidx.annotation.StringRes
import com.gtrack.R

/**
 * Pages Android Auto fixes (cahier des charges G-Track Telemetry).
 */
enum class AutoTelemetryPage(@StringRes val titleResId: Int) {
    DATA_1(R.string.page_data_1),
    DATA_2(R.string.page_data_2),
    LIVE_MAP(R.string.page_live_map);

    companion object {
        val PAGES = entries.toList()

        fun fromIndex(index: Int): AutoTelemetryPage =
            PAGES.getOrElse(index.coerceIn(0, PAGES.lastIndex)) { DATA_1 }
    }
}
