package com.gtrack.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gtrack.R
import com.gtrack.telemetry.TelemetryService
import com.gtrack.ui.automotive.ConceptWidgetCell
import com.gtrack.ui.automotive.WidgetGaugeCanvas
import com.gtrack.widgets.WidgetManager
import com.gtrack.widgets.WidgetType
import com.gtrack.widgets.WidgetVisualStyle
import kotlinx.coroutines.launch

@Composable
fun WidgetThemeOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onDismiss)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val widgetManager = remember { WidgetManager(context) }
    var selected by remember { mutableStateOf(WidgetVisualStyle.GTI) }
    val previewTelemetry = remember {
        TelemetryService.createDemoSample()
    }

    LaunchedEffect(Unit) {
        selected = widgetManager.getVisualStyle()
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
                text = stringResource(R.string.widget_style_title),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = stringResource(R.string.widget_style_subtitle),
            color = Color.White.copy(alpha = 0.65f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(WidgetVisualStyle.pickerOrder) { style ->
                val isSelected = style == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selected = style
                            scope.launch { widgetManager.setVisualStyle(style) }
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .height(72.dp)
                            .weight(0.35f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1A1A1A))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFF1565C0) else Color(0xFF333333),
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        if (style.usesConceptLayout()) {
                            ConceptWidgetCell(
                                widgetType = WidgetType.SPEED,
                                telemetry = previewTelemetry,
                                visualStyle = style,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            WidgetGaugeCanvas(
                                widgetType = WidgetType.SPEED,
                                telemetry = previewTelemetry,
                                visualStyle = style,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(0.65f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(style.titleRes),
                                color = if (isSelected) Color(0xFFFFEB3B) else Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (isSelected) {
                                Text(
                                    text = " ✓",
                                    color = Color(0xFFFFEB3B),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                        Text(
                            text = stringResource(style.subtitleRes),
                            color = Color.White.copy(alpha = 0.55f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                HorizontalDivider(color = Color(0xFF2A2A2A))
            }
        }
    }
}
