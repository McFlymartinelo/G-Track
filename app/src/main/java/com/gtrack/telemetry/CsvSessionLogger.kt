package com.gtrack.telemetry

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Enregistre chaque session dans un CSV daté visible dans Documents/G-Track
 * (via MediaStore, compatibilité DashWare). Accessible par explorateur de fichiers / USB.
 */
class CsvSessionLogger(private val context: Context) {

    private var writer: BufferedWriter? = null
    private var sessionUri: Uri? = null

    suspend fun startSession(): Uri? = withContext(Dispatchers.IO) {
        endSessionInternal()
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "gtrack_$stamp.csv")
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "${Environment.DIRECTORY_DOCUMENTS}/$SUBFOLDER"
            )
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, values) ?: return@withContext null
        sessionUri = uri
        val out = resolver.openOutputStream(uri, "w")
        writer = out?.let { BufferedWriter(OutputStreamWriter(it, Charsets.UTF_8)) }
        writer?.apply {
            write(TelemetryData.CSV_HEADER)
            newLine()
            flush()
        }
        uri
    }

    suspend fun append(sample: TelemetryData) = withContext(Dispatchers.IO) {
        try {
            writer?.apply {
                write(sample.toCsvRow())
                newLine()
                flush()
            }
        } catch (_: Exception) {
        }
        Unit
    }

    suspend fun endSession() = withContext(Dispatchers.IO) {
        endSessionInternal()
    }

    private fun endSessionInternal() {
        try {
            writer?.flush()
            writer?.close()
        } catch (_: Exception) {
        }
        writer = null
        sessionUri?.let { uri ->
            try {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                context.contentResolver.update(uri, values, null, null)
            } catch (_: Exception) {
            }
        }
        sessionUri = null
    }

    fun currentSessionUri(): Uri? = sessionUri

    private companion object {
        const val SUBFOLDER = "G-Track"
    }
}
