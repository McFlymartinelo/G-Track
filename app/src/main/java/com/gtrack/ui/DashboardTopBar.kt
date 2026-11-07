package com.gtrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gtrack.R
import com.gtrack.obd.ObdConnectionState
import com.gtrack.ui.automotive.EpureColors
import com.gtrack.widgets.WidgetVisualStyle
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Barre de tâches unifiée (identique sur tous les thèmes), divisée en 5 zones :
 * nom du thème · numéro de page · bouton REC · bouton OBD · heure.
 */
@Composable
fun DashboardTopBar(
    visualStyle: WidgetVisualStyle,
    pageNumber: Int,
    recording: Boolean,
    sessionActive: Boolean,
    demoActive: Boolean,
    liveObdActive: Boolean,
    obdConnectionState: ObdConnectionState?,
    onRecClick: () -> Unit,
    onObdClick: () -> Unit,
    onThemeClick: () -> Unit,
    onPresetClick: () -> Unit,
    onDemoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var clock by remember { mutableStateOf(formatClock()) }
    LaunchedEffect(Unit) {
        while (true) {
            clock = formatClock()
            delay(1000)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(EpureColors.BgHeader)
            .statusBarsPadding()
            .height(46.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Gauche — bouton réglages (thème / préréglage / démo) puis nom du thème
        BarCell(alignment = Alignment.CenterStart) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingsMenu(
                    demoActive = demoActive,
                    liveObdActive = liveObdActive,
                    onThemeClick = onThemeClick,
                    onPresetClick = onPresetClick,
                    onDemoClick = onDemoClick
                )
                Text(
                    text = stringResource(visualStyle.titleRes),
                    color = EpureColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }

        // Centre — numéro de page (centré à l'écran)
        BarCell(alignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.dashboard_page_title, pageNumber),
                color = EpureColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Droite — REC · OBD · heure
        BarCell(alignment = Alignment.CenterEnd) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                RecButton(
                    recording = recording,
                    enabled = sessionActive,
                    onClick = onRecClick
                )
                ObdStatusIndicator(
                    connectionState = obdConnectionState,
                    onClick = onObdClick
                )
                Text(
                    text = clock,
                    color = EpureColors.Grey,
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        fontFeatureSettings = "tnum"
                    ),
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun RowScope.BarCell(
    alignment: Alignment,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 2.dp),
        contentAlignment = alignment
    ) {
        content()
    }
}

@Composable
private fun SettingsMenu(
    demoActive: Boolean,
    liveObdActive: Boolean,
    onThemeClick: () -> Unit,
    onPresetClick: () -> Unit,
    onDemoClick: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { menuOpen = true }
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_menu_settings),
                contentDescription = stringResource(R.string.widget_style_button_cd),
                tint = Color(0xFF80CBC4),
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.widget_style_button_cd)) },
                onClick = {
                    menuOpen = false
                    onThemeClick()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.obd_preset_button_cd)) },
                onClick = {
                    menuOpen = false
                    onPresetClick()
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        if (demoActive) stringResource(R.string.demo_stop_cd)
                        else stringResource(R.string.demo_start_cd)
                    )
                },
                enabled = !liveObdActive,
                onClick = {
                    menuOpen = false
                    if (!liveObdActive) onDemoClick()
                }
            )
        }
    }
}

@Composable
private fun RecButton(
    recording: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val activeColor = EpureColors.Red
    val idleColor = Color(0xFF6A6A6A)
    val tint = when {
        !enabled -> Color(0xFF3A3A3A)
        recording -> activeColor
        else -> idleColor
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(tint)
        )
        Text(
            text = "REC",
            color = tint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
    }
}

private fun formatClock(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
