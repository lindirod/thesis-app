package com.example.thesis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thesis.data.model.TrackResult

@Composable
fun CalibrationOverlay(progress: Float, track: TrackResult?) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.primaryContainer.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Calibrating baseline for \"${track?.name}\"...",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = colorScheme.primary,
                trackColor = colorScheme.primary.copy(alpha = 0.1f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Recording your resting state (10s)",
                fontSize = 11.sp,
                color = colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun PlaylistSelectionCard(title: String, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Save, contentDescription = null, tint = colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
        }
    }
}

@Composable
fun RecommendationItem(
    track: TrackResult,
    isPlaying: Boolean,
    isPaused: Boolean = false,
    isEnabled: Boolean = true,
    onPlay: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val itemColor = if (track.category == "Energetic") colorScheme.primary else colorScheme.secondary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colorScheme.surface.copy(alpha = if (isEnabled || (isPlaying && !isPaused)) 1f else 0.5f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!track.previewUrl.isNullOrBlank()) {
            IconButton(
                onClick = onPlay,
                enabled = isEnabled,
                modifier = Modifier.size(36.dp).clip(CircleShape).background(itemColor.copy(alpha = 0.1f))
            ) {
                Icon(
                    if (isPlaying && !isPaused) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isEnabled) itemColor else colorScheme.outline
                )
            }
            Spacer(Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(track.name ?: "Unknown", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colorScheme.onSurface)
            Text(track.artist ?: "Unknown", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
            if (!track.fromSeed.isNullOrEmpty()) {
                Text("From: ${track.fromSeed}", fontSize = 10.sp, color = itemColor.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
            }
        }

        if (track.measuredBpm != null && track.measuredBpm > 0) {
            Column(horizontalAlignment = Alignment.End) {
                if (track.preTrackAvgBpm != null) {
                    Text("Base: ${track.preTrackAvgBpm}", fontSize = 9.sp, color = colorScheme.onSurfaceVariant)
                }
                Text("${track.measuredBpm}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = itemColor)
                Text("BPM", fontSize = 9.sp, color = colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
        }
    }
}
