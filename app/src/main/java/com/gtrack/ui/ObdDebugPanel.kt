package com.gtrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gtrack.R
import com.gtrack.obd.ObdDebugLog
import com.gtrack.telemetry.TelemetryService

@Composable
fun ObdDebugPanel(
    modifier: Modifier = Modifier
) {
    val lines by ObdDebugLog.lines.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.obd_debug_title),
                color = Color.White,
                style = MaterialTheme.typography.titleSmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { TelemetryService.getInstance()?.startPidScan() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) {
                    Text(stringResource(R.string.obd_scan_pids), fontSize = 12.sp)
                }
                Button(
                    onClick = { TelemetryService.getInstance()?.startMode22Scan() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A))
                ) {
                    Text(stringResource(R.string.obd_scan_mode22), fontSize = 12.sp)
                }
                Button(
                    onClick = { ObdDebugLog.clear() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F))
                ) {
                    Text(stringResource(R.string.obd_debug_clear), fontSize = 12.sp)
                }
            }
        }
        Text(
            text = stringResource(R.string.obd_debug_hint),
            color = Color(0xFFFFCC80),
            style = MaterialTheme.typography.bodySmall
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 220.dp)
                .background(Color(0xFF111111))
                .padding(8.dp),
            reverseLayout = true
        ) {
            if (lines.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.obd_debug_empty),
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                items(lines) { line ->
                    Text(
                        text = line,
                        color = when {
                            line.contains(" TX ") -> Color(0xFF81D4FA)
                            line.contains(" RX ") -> Color(0xFFA5D6A7)
                            else -> Color(0xFFB0BEC5)
                        },
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }
    }
}
