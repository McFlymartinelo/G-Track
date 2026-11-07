package com.gtrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gtrack.R
import com.gtrack.obd.ObdConnectionState

@Composable
fun ObdStatusIndicator(
    connectionState: ObdConnectionState?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = indicatorColor(connectionState)
    val description = stringResource(R.string.obd_status_indicator_cd)
    Box(
        modifier = modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.5.dp, Color.White.copy(alpha = 0.45f), CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
    )
}

private fun indicatorColor(state: ObdConnectionState?): Color = when (state) {
    ObdConnectionState.CONNECTED -> Color(0xFF43A047)
    ObdConnectionState.CONNECTING, ObdConnectionState.SCANNING -> Color(0xFFFFB300)
    ObdConnectionState.ERROR, ObdConnectionState.DISCONNECTED -> Color(0xFFE53935)
    null -> Color(0xFFE53935)
}
