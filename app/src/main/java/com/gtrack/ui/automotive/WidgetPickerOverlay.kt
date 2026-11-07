package com.gtrack.ui.automotive

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gtrack.R
import com.gtrack.telemetry.TelemetryData
import com.gtrack.widgets.WidgetType
import com.gtrack.widgets.formatValue

@Composable
fun WidgetPickerOverlay(
    slotIndex: Int,
    telemetry: TelemetryData,
    currentType: WidgetType?,
    onSelect: (WidgetType) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onDismiss)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    painter = painterResource(R.drawable.ic_car_prev),
                    contentDescription = stringResource(R.string.back),
                    tint = Color.White
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.widget_picker_title, slotIndex + 1),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                currentType?.let { type ->
                    Text(
                        text = stringResource(
                            R.string.widget_picker_current,
                            type.displayTitle()
                        ),
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            WidgetType.groupedPickerChoices().forEach { (category, types) ->
                item(key = "cat_${category.name}") {
                    Text(
                        text = stringResource(category.titleRes),
                        color = Color(0xFF80CBC4),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
                items(types, key = { it.id }) { type ->
                    WidgetPickerRow(
                        type = type,
                        telemetry = telemetry,
                        isCurrent = type == currentType,
                        onClick = { onSelect(type) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetPickerRow(
    type: WidgetType,
    telemetry: TelemetryData,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isCurrent) Color.White.copy(alpha = 0.08f) else Color.Transparent
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = type.displayTitle(),
            color = if (isCurrent) Color(0xFFFFEB3B) else Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = type.formatValue(telemetry),
            color = type.valueColor(telemetry),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
