package com.gtrack.widgets

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import com.gtrack.auto.CarWidgetDefaults
import org.json.JSONArray

private val Context.widgetDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "gtrack_widgets"
)

/**
 * Gestion des pages de widgets (6 par page), réorganisation et persistance DataStore.
 */
class WidgetManager(private val context: Context) {

    companion object {
        private val KEY_PAGES_JSON = stringPreferencesKey("widget_pages_json")
        private val KEY_ACTIVE_PAGE = intPreferencesKey("active_page_index")
        private val KEY_VISUAL_STYLE = stringPreferencesKey("widget_visual_style")
    }

    val visualStyleFlow: Flow<WidgetVisualStyle> = context.widgetDataStore.data.map { prefs ->
        WidgetVisualStyle.fromStorageKey(prefs[KEY_VISUAL_STYLE])
    }

    suspend fun getVisualStyle(): WidgetVisualStyle = visualStyleFlow.first()

    suspend fun setVisualStyle(style: WidgetVisualStyle) {
        context.widgetDataStore.edit { prefs ->
            prefs[KEY_VISUAL_STYLE] = style.storageKey
        }
    }

    val pagesFlow: Flow<List<List<WidgetType>>> = context.widgetDataStore.data.map { prefs ->
        parsePages(prefs[KEY_PAGES_JSON] ?: defaultPagesJson())
    }

    val activePageFlow: Flow<Int> = context.widgetDataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_PAGE] ?: 0
    }

    suspend fun getPages(): List<List<WidgetType>> =
        pagesFlow.first()

    suspend fun getActivePageIndex(): Int =
        activePageFlow.first()

    suspend fun setActivePage(index: Int) {
        context.widgetDataStore.edit { prefs ->
            val pages = parsePages(prefs[KEY_PAGES_JSON] ?: defaultPagesJson())
            prefs[KEY_ACTIVE_PAGE] = index.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
        }
    }

    suspend fun setPages(pages: List<List<WidgetType>>) {
        require(pages.all { it.size == WidgetType.WIDGETS_PER_PAGE }) {
            "Chaque page doit contenir exactement ${WidgetType.WIDGETS_PER_PAGE} widgets"
        }
        context.widgetDataStore.edit { prefs ->
            prefs[KEY_PAGES_JSON] = pagesToJson(pages)
            val active = prefs[KEY_ACTIVE_PAGE] ?: 0
            prefs[KEY_ACTIVE_PAGE] = active.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
        }
    }

    suspend fun addPage(widgets: List<WidgetType> = WidgetType.defaultPage()) {
        val normalized = normalizeToSix(widgets)
        val current = getPages().toMutableList()
        current.add(normalized)
        setPages(current)
    }

    suspend fun removePage(pageIndex: Int) {
        val current = getPages().toMutableList()
        if (current.size <= 1) return
        if (pageIndex !in current.indices) return
        current.removeAt(pageIndex)
        setPages(current)
        val active = getActivePageIndex()
        if (active >= current.size) setActivePage(current.size - 1)
    }

    suspend fun reorderWidget(pageIndex: Int, fromIndex: Int, toIndex: Int) {
        val pages = getPages().toMutableList()
        if (pageIndex !in pages.indices) return
        val page = pages[pageIndex].toMutableList()
        if (fromIndex !in page.indices || toIndex !in page.indices) return
        val item = page.removeAt(fromIndex)
        page.add(toIndex, item)
        pages[pageIndex] = page
        setPages(pages)
    }

    suspend fun replaceWidget(pageIndex: Int, slotIndex: Int, newType: WidgetType) {
        val pages = getPages().toMutableList()
        if (pageIndex !in pages.indices || slotIndex !in 0 until WidgetType.WIDGETS_PER_PAGE) return
        val page = pages[pageIndex].toMutableList()
        page[slotIndex] = newType
        pages[pageIndex] = page
        setPages(pages)
    }

    suspend fun addWidgetToSlot(pageIndex: Int, slotIndex: Int, type: WidgetType) {
        replaceWidget(pageIndex, slotIndex, type)
    }

    /** Initialise les 2 pages de widgets (Data 1, Data 2) personnalisables. */
    suspend fun ensureCarTelemetryPages() {
        val pages = getPages()
        if (pages.size == 2) return
        setPages(CarWidgetDefaults.twoCarPages())
    }

    suspend fun removeWidgetFromSlot(pageIndex: Int, slotIndex: Int) {
        replaceWidget(pageIndex, slotIndex, WidgetType.SPEED)
    }

    private fun normalizeToSix(widgets: List<WidgetType>): List<WidgetType> {
        val result = widgets.take(WidgetType.WIDGETS_PER_PAGE).toMutableList()
        val pool = WidgetType.allAvailable().filter { it !in result }.toMutableList()
        while (result.size < WidgetType.WIDGETS_PER_PAGE && pool.isNotEmpty()) {
            result.add(pool.removeAt(0))
        }
        while (result.size < WidgetType.WIDGETS_PER_PAGE) {
            result.add(WidgetType.SPEED)
        }
        return result
    }

    private fun defaultPagesJson(): String =
        pagesToJson(listOf(WidgetType.defaultPage()))

    private fun pagesToJson(pages: List<List<WidgetType>>): String {
        val root = JSONArray()
        pages.forEach { page ->
            val arr = JSONArray()
            page.forEach { arr.put(it.id) }
            root.put(arr)
        }
        return root.toString()
    }

    private fun parsePages(json: String): List<List<WidgetType>> {
        return try {
            val root = JSONArray(json)
            buildList {
                for (i in 0 until root.length()) {
                    val arr = root.getJSONArray(i)
                    val page = buildList {
                        for (j in 0 until arr.length()) {
                            WidgetType.fromId(arr.getString(j))?.let { add(it) }
                        }
                    }
                    add(normalizeToSix(page))
                }
            }.ifEmpty { listOf(WidgetType.defaultPage()) }
        } catch (_: Exception) {
            listOf(WidgetType.defaultPage())
        }
    }
}
