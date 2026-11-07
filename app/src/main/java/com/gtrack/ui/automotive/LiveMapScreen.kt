package com.gtrack.ui.automotive

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gtrack.R
import com.gtrack.auto.render.SkeuomorphicGaugeRenderer
import com.gtrack.auto.theme.CarVisualTheme
import com.gtrack.telemetry.TelemetryData
import com.gtrack.track.TrackGeometry
import com.gtrack.track.TrackState

@Composable
fun LiveMapScreen(
    telemetry: TelemetryData,
    track: TrackState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val geometry = remember { TrackGeometry.redBullRing() }
    val mapBitmap = remember(telemetry, track) {
        SkeuomorphicGaugeRenderer(context).renderTrackMap(
            telemetry,
            track,
            geometry.circuitOutline
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF121212))
    ) {
        Image(
            bitmap = mapBitmap.asImageBitmap(),
            contentDescription = stringResource(R.string.track_red_bull_ring),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        Text(
            text = "${"%.0f".format(telemetry.speedKmh)} km/h · ${CarVisualTheme.formatLapTime(telemetry.lapTimeMs)}",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        )
    }
}
