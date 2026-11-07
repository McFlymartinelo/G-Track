package com.gtrack.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gtrack.R
import com.gtrack.obd.ObdPresetCatalog
import com.gtrack.obd.ObdSettingsStore
import com.gtrack.obd.ObdVehiclePreset
import com.gtrack.telemetry.TelemetryService
import kotlinx.coroutines.launch

@Composable
fun ObdPresetOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onDismiss)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { ObdSettingsStore(context) }
    var selectedId by remember { mutableStateOf(ObdPresetCatalog.DEFAULT_PRESET_ID) }

    LaunchedEffect(Unit) {
        selectedId = settings.getPresetId()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    painter = painterResource(R.drawable.ic_car_prev),
                    contentDescription = stringResource(R.string.back),
                    tint = Color.White
                )
            }
            Text(
                text = stringResource(R.string.obd_preset_title),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = stringResource(R.string.obd_preset_subtitle),
            color = Color.White.copy(alpha = 0.65f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.15f),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(ObdPresetCatalog.all, key = { it.id }) { preset ->
                PresetRow(
                    preset = preset,
                    isSelected = preset.id == selectedId,
                    onClick = {
                        selectedId = preset.id
                        scope.launch {
                            settings.setPresetId(preset.id)
                            TelemetryService.getInstance()?.applyObdPreset(preset.id)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PresetRow(
    preset: ObdVehiclePreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isSelected) Color.White.copy(alpha = 0.08f) else Color.Transparent
            )
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = preset.displayName,
                color = if (isSelected) Color(0xFFFFEB3B) else Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${preset.queries.size} PID",
                color = Color.White.copy(alpha = 0.45f),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            text = preset.summary,
            color = Color.White.copy(alpha = 0.55f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
