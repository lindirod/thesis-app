package com.example.thesis.ui.screens

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thesis.ui.components.ThemeToggle
import com.example.thesis.ui.components.CalibrationOverlay
import com.example.thesis.ui.components.RecommendationItem
import com.example.thesis.data.model.SavedPlaylist
import com.example.thesis.data.model.TrackResult
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PlaylistsScreen(
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    onNavigateHome: () -> Unit,
    playlists: List<SavedPlaylist>,
    liveHeartRate: Double,
    heartRateSampleCounter: Int,
    isConnected: Boolean,
    isInternetConnected: Boolean,
    onNavigateBack: () -> Unit,
    onTrackFinished: (TrackResult, Double, Int, Int, String, String, List<Int>, List<Int>) -> Unit,
    onNavigateToFeedback: (TrackResult, Double, List<Int>, List<Int>) -> Unit
) {
    var selectedPlaylistId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selectedPlaylist = playlists.find { it.id == selectedPlaylistId }
    val colorScheme = MaterialTheme.colorScheme
    val sessionTotalFinished = remember(playlists) {
        playlists.sumOf { p -> 
            p.tracks.orEmpty().count { (it.measuredBpm ?: 0) > 0 } 
        }
    }

    val playbackSamples = remember { mutableStateListOf<Double>() }
    var currentTrack by remember { mutableStateOf<TrackResult?>(null) }
    var isPaused by remember { mutableStateOf(false) }
    var isCalibrating by remember { mutableStateOf(false) }
    var calibrationProgress by remember { mutableFloatStateOf(0f) }
    val calibrationSamples = remember { mutableStateListOf<Double>() }
    var calibrationTrack by remember { mutableStateOf<TrackResult?>(null) }
    var playingUrl by remember { mutableStateOf<String?>(null) }
    val mediaPlayer = remember { 
        MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
        }
    }

    var playbackError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(mediaPlayer) {
        mediaPlayer.setOnErrorListener { _, what, extra ->
            android.util.Log.e("RecsLog", "MediaPlayer Error: what=$what, extra=$extra")
            playbackError = "Preview unavailable for this song. Please try another one."
            playingUrl = null
            isPaused = false
            isCalibrating = false
            true
        }
    }

    LaunchedEffect(heartRateSampleCounter, isPaused, isCalibrating) {
        if (isConnected && liveHeartRate > 0) {
            if (isCalibrating) {
                if (calibrationSamples.size < 10) {
                    calibrationSamples.add(liveHeartRate)
                }
            } else if (playingUrl != null && !isPaused) {
                if (playbackSamples.size < 30) {
                    playbackSamples.add(liveHeartRate)
                    android.util.Log.d("RecsLog", "Monitoring [${currentTrack?.name}]: Current BPM $liveHeartRate")
                }
            }
        }
    }

    // Calibration Timer (10s)
    LaunchedEffect(isCalibrating) {
        if (isCalibrating) {
            calibrationProgress = 0f
            calibrationSamples.clear()
            val duration = 10000L
            val steps = 100
            for (i in 1..steps) {
                kotlinx.coroutines.delay(duration / steps)
                calibrationProgress = i.toFloat() / steps
            }
            val samplesSnapshot = calibrationSamples.toList()
            val avg = if (samplesSnapshot.isNotEmpty()) samplesSnapshot.average() else liveHeartRate
            val track = calibrationTrack

            if (track != null) {
                try {
                    mediaPlayer.reset()
                    mediaPlayer.setDataSource(track.previewUrl)
                    mediaPlayer.prepareAsync()
                    mediaPlayer.setOnPreparedListener { it.start() }
                    playingUrl = track.previewUrl
                    currentTrack = track.copy(
                        preTrackAvgBpm = avg.toInt(),
                        preTrackReadings = samplesSnapshot.map { it.toInt() }
                    )
                    isPaused = false
                    playbackSamples.clear()
                } catch (_: Exception) {
                    playingUrl = null
                }
            }
            isCalibrating = false
            calibrationTrack = null
        }
    }

    DisposableEffect(Unit) {
        mediaPlayer.setOnCompletionListener {
            val average = if (playbackSamples.isNotEmpty()) playbackSamples.average() else 0.0
            val samplesSnapshot = playbackSamples.toList().map { it.toInt() }

            android.util.Log.d("RecsLog", "Playback Finished Naturally: ${currentTrack?.name} | Average HR: ${average.toInt()} BPM")

            if (currentTrack?.fromSeed != null) {
                onNavigateToFeedback(currentTrack!!, average, currentTrack!!.preTrackReadings.orEmpty(), samplesSnapshot)
            } else {
                currentTrack?.let { track ->
                    onTrackFinished(track, average, 0, 0, "Seed", "Seed", track.preTrackReadings.orEmpty(), samplesSnapshot)
                }
            }
            playingUrl = null
            isPaused = false
            playbackSamples.clear()
        }
        onDispose { mediaPlayer.release() }
    }

    Box(modifier = Modifier.fillMaxSize().background(colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 24.dp)) {
            // Header Row: Back button and Theme Toggle on the same line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (selectedPlaylistId != null) {
                        selectedPlaylistId = null
                        mediaPlayer.reset()
                        playingUrl = null
                    } else {
                        onNavigateBack()
                    }
                }) {
                    val arrowColor = when (selectedPlaylist?.category) {
                        "Energetic" -> colorScheme.primary
                        "Calm" -> colorScheme.secondary
                        else -> colorScheme.onBackground // Main list
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, 
                        contentDescription = "Back",
                        tint = arrowColor
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateHome,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = colorScheme.onBackground
                        )
                    ) {
                        Icon(Icons.Default.Home, contentDescription = "Home")
                    }

                    ThemeToggle(
                        isDarkMode = isDarkMode,
                        onToggle = onToggleTheme
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (selectedPlaylistId == null) "My Playlists" else "Playlist Details",
                fontSize = 22.sp,
                fontWeight = FontWeight.Light,
                color = colorScheme.onBackground
            )
            Spacer(Modifier.height(24.dp))

            if (!isInternetConnected) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "⚠️ No internet connection. Music playback is unavailable.",
                        modifier = Modifier.padding(12.dp),
                        color = Color(0xFF92400E),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!isConnected) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "⚠️ Smartwatch disconnected. Heart rate monitoring paused.",
                        modifier = Modifier.padding(12.dp),
                        color = Color(0xFF991B1B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (selectedPlaylistId == null) {
                if (playlists.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No playlists saved yet.", color = colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(playlists.reversed()) { playlist ->
                            PlaylistCard(playlist) { selectedPlaylistId = playlist.id }
                        }
                    }
                }
            } else {
                selectedPlaylist?.let { playlist ->
                    if (playbackError != null) {
                        Text(
                            text = playbackError!!,
                            color = colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    if (isCalibrating) {
                        CalibrationOverlay(progress = calibrationProgress, track = calibrationTrack)
                        Spacer(Modifier.height(16.dp))
                    }
                    PlaylistDetailView(
                        playlist = playlist,
                        playingUrl = playingUrl,
                        isPaused = isPaused,
                        isCalibrating = isCalibrating,
                        isInternetConnected = isInternetConnected,
                        onPlay = { track ->
                            if (!isInternetConnected) {
                                playbackError = "Internet connection required to play previews."
                            } else {
                                playbackError = null
                                val isDifferentTrack = playingUrl != track.previewUrl
                                val shouldCalibrate = isConnected && sessionTotalFinished > 0 && isDifferentTrack

                                if (shouldCalibrate) {
                                    isCalibrating = true
                                    calibrationTrack = track
                                    try { mediaPlayer.reset() } catch(_: Exception) {}
                                    playingUrl = null
                                    isPaused = false
                                } else if (playingUrl == null || isDifferentTrack) {
                                    try {
                                        mediaPlayer.reset()
                                        mediaPlayer.setDataSource(track.previewUrl)
                                        mediaPlayer.prepareAsync()
                                        mediaPlayer.setOnPreparedListener { it.start() }
                                        playingUrl = track.previewUrl
                                        
                                        currentTrack = if (sessionTotalFinished == 0) {
                                            track.copy(preTrackAvgBpm = playlist.averageBpm)
                                        } else {
                                            track
                                        }
                                        isPaused = false
                                        playbackSamples.clear()
                                    } catch (e: Exception) {
                                        android.util.Log.e("RecsLog", "Error playing preview", e)
                                        playbackError = "Failed to play preview."
                                    }
                                } else {
                                    if (isPaused) {
                                        mediaPlayer.start()
                                        isPaused = false
                                    } else {
                                        mediaPlayer.pause()
                                        isPaused = true
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistCard(playlist: SavedPlaylist, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val date = remember(playlist.timestamp) {
        SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(playlist.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
                colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    "${playlist.category ?: "Unknown"} Playlist",
                    fontSize = 16.sp, 
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Text(
                    "$date • ${playlist.tracks.orEmpty().size} tracks",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PlaylistDetailView(
    playlist: SavedPlaylist,
    playingUrl: String?,
    isPaused: Boolean,
    isCalibrating: Boolean,
    isInternetConnected: Boolean,
    onPlay: (TrackResult) -> Unit
) {
    val seeds = playlist.tracks.orEmpty().filter { it.fromSeed.isNullOrEmpty() }
    val recommendations = playlist.tracks.orEmpty().filter { !it.fromSeed.isNullOrEmpty() }
    // ... items updated to use isInternetConnected ...

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column {
                Text(
                    "${playlist.category?.uppercase() ?: "UNKNOWN"} PLAYLIST",
                    fontSize = 11.sp,
                    color = if (playlist.category == "Energetic") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    "Generated at ${playlist.averageBpm ?: 0} BPM reference",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (seeds.isNotEmpty()) {
            item {
                Text(
                    "YOUR SEEDS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (playlist.category == "Energetic") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(seeds) { track ->
                RecommendationItem(
                    track = track, 
                    isPlaying = playingUrl == track.previewUrl,
                    isPaused = isPaused,
                    isEnabled = !isCalibrating && isInternetConnected,
                    onPlay = { onPlay(track) }
                )
            }
        }

        if (recommendations.isNotEmpty()) {
            item {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Text(
                        "RECOMMENDATIONS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (playlist.category == "Energetic") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        letterSpacing = 1.sp
                    )
                }
            }
            items(recommendations) { track ->
                RecommendationItem(
                    track = track, 
                    isPlaying = playingUrl == track.previewUrl,
                    isPaused = isPaused,
                    isEnabled = !isCalibrating && isInternetConnected,
                    onPlay = { onPlay(track) }
                )
            }
        }
    }
}
