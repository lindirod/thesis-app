package com.example.thesis.ui.screens

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thesis.data.model.TrackResult
import com.example.thesis.data.remote.DeezerService
import com.example.thesis.data.remote.LastFmService
import com.example.thesis.ui.components.ThemeToggle
import com.example.thesis.ui.components.CalibrationOverlay
import com.example.thesis.ui.components.RecommendationItem
import com.example.thesis.ui.components.PlaylistSelectionCard
import kotlinx.coroutines.delay

@Composable
fun RecommendationsScreen(
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    seeds: List<TrackResult>,
    savedPlaylists: List<com.example.thesis.data.model.SavedPlaylist> = emptyList(),
    userHeartRate: Double,
    liveHeartRate: Double,
    heartRateSampleCounter: Int,
    isConnected: Boolean,
    isInternetConnected: Boolean,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit,
    onSavePlaylist: (List<TrackResult>, List<TrackResult>, List<TrackResult>, List<TrackResult>) -> Unit,
    onNavigateToFeedback: (TrackResult, Double, List<Int>, List<Int>) -> Unit
) {
    // Use rememberSaveable to keep state across navigation/recomposition
    var recommendations by rememberSaveable { mutableStateOf<List<TrackResult>>(emptyList()) }
    
    // Track if ANY song has been evaluated in this session
    // (Used to decide if we need 10s calibration before playing)
    val sessionTotalFinished = remember(seeds, savedPlaylists) {
        val seedFinished = seeds.count { it.measuredBpm != null && (it.measuredBpm ?: 0) > 0 }
        val recsFinished = savedPlaylists.sumOf { p -> 
            p.tracks.orEmpty().count { it.fromSeed != null && it.measuredBpm != null && (it.measuredBpm ?: 0) > 0 } 
        }
        seedFinished + recsFinished
    }

    // Sync recommendations state with any external updates (like handleTrackFinished in MainActivity)
    LaunchedEffect(seeds, savedPlaylists) {
         if (recommendations.isNotEmpty()) {
             val allSessionTracks = savedPlaylists.flatMap { it.tracks.orEmpty() }
             recommendations = recommendations.map { rec ->
                 val updatedSeed = seeds.find { it.name == rec.name && it.artist == rec.artist }
                 if (updatedSeed != null && updatedSeed.measuredBpm != null) {
                     return@map rec.copy(
                         measuredBpm = updatedSeed.measuredBpm,
                         preTrackAvgBpm = updatedSeed.preTrackAvgBpm
                     )
                 }
                 val updatedRec = allSessionTracks.find { it.name == rec.name && it.artist == rec.artist }
                 if (updatedRec != null && updatedRec.measuredBpm != null) {
                     rec.copy(
                         measuredBpm = updatedRec.measuredBpm,
                         preTrackAvgBpm = updatedRec.preTrackAvgBpm
                     )
                 } else rec
             }
         }
    }

    var isLoading by remember { mutableStateOf(recommendations.isEmpty()) }
    var statusMessage by remember { mutableStateOf("Initializing...") }
    var errorMsg by remember { mutableStateOf("") }
    var retryCount by remember { mutableIntStateOf(0) }
    var isSaved by rememberSaveable { mutableStateOf(false) }

    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    
    // Hidden monitoring state
    val playbackSamples = remember { mutableStateListOf<Double>() }
    var currentTrack by remember { mutableStateOf<TrackResult?>(null) }
    var isPaused by remember { mutableStateOf(false) }
    var playingUrl by remember { mutableStateOf<String?>(null) }

    // Calibration State
    var isCalibrating by remember { mutableStateOf(false) }
    var calibrationProgress by remember { mutableFloatStateOf(0f) }
    val calibrationSamples = remember { mutableStateListOf<Double>() }
    var calibrationTrack by remember { mutableStateOf<TrackResult?>(null) }

    val colorScheme = MaterialTheme.colorScheme

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

    // Error handling for MediaPlayer
    var playbackError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(mediaPlayer) {
        mediaPlayer.setOnErrorListener { _, what, extra ->
            Log.e("RecsLog", "MediaPlayer Error: what=$what, extra=$extra")
            playbackError = "Preview unavailable for this song. Please try another one."
            playingUrl = null
            isPaused = false
            isCalibrating = false
            true // error handled
        }
    }

    // Monitor HR for calibration or playback - use heartRateSampleCounter to ensure every sample is caught
    LaunchedEffect(heartRateSampleCounter, isPaused, isCalibrating) {
        if (isConnected && liveHeartRate > 0) {
            if (isCalibrating) {
                if (calibrationSamples.size < 10) {
                    calibrationSamples.add(liveHeartRate)
                }
            } else if (playingUrl != null && !isPaused) {
                if (playbackSamples.size < 30) {
                    playbackSamples.add(liveHeartRate)
                    Log.d("RecsLog", "Monitoring [${currentTrack?.name}]: Current BPM $liveHeartRate")
                }
            }
        }
    }

    // Calibration Timer
    LaunchedEffect(isCalibrating) {
        if (isCalibrating) {
            calibrationProgress = 0f
            calibrationSamples.clear()
            val duration = 10000L
            val steps = 100
            for (i in 1..steps) {
                delay(duration / steps)
                calibrationProgress = i.toFloat() / steps
            }
            
            // Finish Calibration
            val samplesSnapshot = calibrationSamples.toList()
            val avg = if (samplesSnapshot.isNotEmpty()) samplesSnapshot.average() else liveHeartRate
            
            Log.d("RecsLog", "--- Baseline Calibration ---")
            Log.d("RecsLog", "Values: $samplesSnapshot")
            Log.d("RecsLog", "Average: ${samplesSnapshot.sum()} / ${samplesSnapshot.size} = $avg")
            
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
                    Log.d("RecsLog", "Calibration finished (${avg.toInt()} BPM). Starting playback for: ${track.name}")
                } catch (e: Exception) {
                    Log.e("RecsLog", "Error playing preview after calibration", e)
                }
            }
            isCalibrating = false
            calibrationTrack = null
        }
    }

    // Stop playback if disconnected
    LaunchedEffect(isConnected) {
        if (!isConnected && (playingUrl != null || isPaused || isCalibrating)) {
            Log.d("RecsLog", "Watch disconnected. Stopping.")
            try {
                mediaPlayer.reset()
            } catch (_: Exception) { }
            playingUrl = null
            currentTrack = null
            isPaused = false
            isCalibrating = false
            playbackSamples.clear()
            calibrationSamples.clear()
        }
    }

    DisposableEffect(Unit) {
        mediaPlayer.setOnCompletionListener {
            val average = if (playbackSamples.isNotEmpty()) playbackSamples.average() else 0.0
            val samplesSnapshot = playbackSamples.toList().map { it.toInt() }
            
            Log.d(
                "RecsLog",
                "Playback Finished Naturally: ${currentTrack?.name} | Samples: ${playbackSamples.size} | Average HR: ${average.toInt()} BPM"
            )

            currentTrack?.let { track ->
                Log.d("RecsLog", "Triggering feedback navigation for ${track.name}")
                onNavigateToFeedback(track, average, track.preTrackReadings.orEmpty(), samplesSnapshot)
            } ?: Log.w("RecsLog", "Playback finished but currentTrack was null!")

            playingUrl = null
            isPaused = false
            playbackSamples.clear()
        }
        onDispose { mediaPlayer.release() }
    }

    LaunchedEffect(seeds, retryCount) {
        if (recommendations.isNotEmpty() && errorMsg.isEmpty()) return@LaunchedEffect
        
        // Try to load existing recommendations from saved playlists if we're resuming
        val existingRecs = savedPlaylists.flatMap { it.tracks.orEmpty() }.filter { !it.fromSeed.isNullOrEmpty() }
        if (existingRecs.isNotEmpty() && retryCount == 0) {
            Log.d("RecsLog", "Resuming session: Loaded ${existingRecs.size} recommendations from storage.")
            recommendations = existingRecs.distinctBy { "${it.name?.lowercase()}|${it.artist?.lowercase()}" }
            isSaved = true
            isLoading = false
            return@LaunchedEffect
        }

        if (seeds.isEmpty()) {
            Log.d("RecsLog", "No seeds provided, skipping recommendation fetching.")
            isLoading = false
            return@LaunchedEffect
        }

        try {
            isLoading = true
            Log.d("RecsLog", "Starting recommendation fetching for ${seeds.size} seeds...")
            val seedKeys = seeds.map { "${it.artist?.lowercase()}|${it.name?.lowercase()}" }.toSet()
            val finalRecommendations = mutableListOf<TrackResult>()
            val updatedSeeds = mutableListOf<TrackResult>()
            val seenKeys = mutableSetOf<String>()

            seeds.forEach { seed ->
                Log.d("RecsLog", "Processing seed: ${seed.name} by ${seed.artist}")
                try {
                    val seedTags = try {
                        LastFmService.getTrackTags(seed.artist ?: "", seed.name ?: "").ifEmpty {
                            Log.d("RecsLog", "No track tags for ${seed.name}, trying artist tags...")
                            LastFmService.getArtistTags(seed.artist ?: "")
                        }
                    } catch (e: Exception) {
                        Log.e("RecsLog", "Error fetching tags for ${seed.name}: ${e.message}")
                        emptyList()
                    }

                    updatedSeeds.add(seed.copy(tags = seedTags))
                    Log.d("RecsLog", "Found ${seedTags.size} tags for ${seed.name}: $seedTags")

                    var currentMethod = ""
                    // Fetch slightly more to ensure we have 5 candidates after filtering the same artist
                    var candidateTracks = LastFmService.getSimilarTracks(seed.artist ?: "", seed.name ?: "", limit = 50)

                    if (candidateTracks.isEmpty()) {
                        Log.d("RecsLog", "No similar tracks found for ${seed.name}, falling back to tags.")
                        if (seedTags.isNotEmpty()) {
                            val topTags = seedTags.take(2)
                            currentMethod = "Tags: ${topTags.joinToString(", ")}"
                            val tracksFromTags = mutableListOf<TrackResult>()
                            topTags.forEach { tag ->
                                try {
                                    val tagTracks = LastFmService.getTagTopTracks(tag, limit = 10)
                                    Log.d("RecsLog", "Found ${tagTracks.size} tracks for tag: $tag")
                                    tracksFromTags.addAll(tagTracks)
                                } catch (_: Exception) { }
                            }
                            // Pool of top 10 from tags, from which we will pick 2
                            candidateTracks = tracksFromTags
                                .distinctBy { "${it.name?.lowercase()}|${it.artist?.lowercase()}" }
                                .take(10)
                                .shuffled()
                        }
                    } else {
                        Log.d("RecsLog", "Found ${candidateTracks.size} similar tracks for ${seed.name}.")
                        currentMethod = "Similar Tracks"
                        // Pool of top 5 (excluding same artist), from which we will pick 2
                        candidateTracks = candidateTracks
                            .filter { (it.artist?.lowercase() ?: "") != (seed.artist?.lowercase() ?: "") }
                            .take(5)
                            .shuffled()
                    }

                    Log.d("RecsLog", "Evaluating ${candidateTracks.size} candidates for ${seed.name}...")
                    var count = 0
                    for (candidate in candidateTracks) {
                        if (count >= 2) break
                        val key = "${candidate.artist?.lowercase()}|${candidate.name?.lowercase()}"
                        if (key in seedKeys || key in seenKeys) continue

                        Log.d("RecsLog", "Checking details for candidate: ${candidate.name}")
                        val (preview, bpm, duration) = DeezerService.getTrackDetails(candidate.artist ?: "", candidate.name ?: "", candidate.deezerId)
                        if (preview.isBlank()) {
                            Log.d("RecsLog", "No preview available for ${candidate.name}, skipping.")
                            continue
                        }

                        val candidateTags = try {
                            LastFmService.getTrackTags(candidate.artist ?: "", candidate.name ?: "")
                        } catch (_: Exception) {
                            emptyList()
                        }

                        Log.d("RecsLog", "Successfully added candidate: ${candidate.name} (BPM: $bpm)")
                        finalRecommendations.add(
                            candidate.copy(
                                previewUrl = preview,
                                bpm = bpm,
                                durationSeconds = duration,
                                category = seed.category,
                                fromSeed = seed.name,
                                tags = candidateTags,
                                recommendationMethod = currentMethod
                            )
                        )
                        seenKeys.add(key)
                        count++
                        delay(50)
                    }
                } catch (e: Exception) {
                    Log.e("RecsLog", "Unexpected error processing seed ${seed.name}: ${e.message}")
                }
            }
            recommendations = finalRecommendations.distinctBy { "${it.name?.lowercase()}|${it.artist?.lowercase()}" }
            Log.d("RecsLog", "Finished fetching. Total recommendations: ${recommendations.size}")

            // Log Summary of BPM data for research verification
            Log.d("RecsLog", "--- BPM DATA SUMMARY ---")
            seeds.forEach { Log.d("RecsLog", "SEED: ${it.name} - BPM: ${it.bpm}") }
            recommendations.forEach { Log.d("RecsLog", "REC: ${it.name} - BPM: ${it.bpm}") }
            Log.d("RecsLog", "------------------------")

            if (finalRecommendations.isNotEmpty() && !isSaved) {
                Log.d("RecsLog", "Auto-saving playlists...")
                val energeticRecs = finalRecommendations.filter { it.category == "Energetic" }
                val calmRecs = finalRecommendations.filter { it.category == "Calm" }
                val energeticSeeds = updatedSeeds.filter { it.category == "Energetic" }
                val calmSeeds = updatedSeeds.filter { it.category == "Calm" }

                onSavePlaylist(energeticRecs, calmRecs, energeticSeeds, calmSeeds)
                isSaved = true
            }
        } catch (e: Exception) {
            Log.e("RecsLog", "Failed to load recommendations: ${e.message}")
            errorMsg = "Failed to load recommendations."
        } finally {
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 24.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (selectedCategory != null) {
                        selectedCategory = null
                        mediaPlayer.reset()
                        playingUrl = null
                        isCalibrating = false
                    } else {
                        onNavigateBack()
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colorScheme.onBackground)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateHome) {
                        Icon(Icons.Default.Home, contentDescription = "Home", tint = colorScheme.onBackground)
                    }
                    ThemeToggle(isDarkMode = isDarkMode, onToggle = onToggleTheme)
                }
            }

            Spacer(Modifier.height(16.dp))

            if (isSaved) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    Text("Auto-Saved", color = colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))
            }

            Text(
                text = when (selectedCategory) {
                    "Energetic" -> "Energetic Recommendations"
                    "Calm" -> "Calm Recommendations"
                    else -> "Your Music Recommendations"
                },
                fontSize = 22.sp,
                fontWeight = FontWeight.Light,
                color = colorScheme.onBackground
            )
            Text(
                "${userHeartRate.toInt()} BPM Reference",
                fontSize = 11.sp,
                color = if (selectedCategory == "Energetic") colorScheme.primary else colorScheme.secondary,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            )

            if (errorMsg.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Connection Error",
                            color = colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "We couldn't fetch recommendations. Please check your internet connection and try again.",
                            color = colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { 
                                errorMsg = ""
                                retryCount++ 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error)
                        ) {
                            Text("Retry Connection")
                        }
                    }
                }
            }

            if (playbackError != null) {
                Spacer(Modifier.height(8.dp))
                Text(text = playbackError!!, color = colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(24.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = colorScheme.primary, strokeWidth = 2.dp)
                        Spacer(Modifier.height(16.dp))
                        Text(statusMessage, fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                    }
                }
            } else {
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
                            "⚠️ Smartwatch disconnected.",
                            modifier = Modifier.padding(12.dp),
                            color = Color(0xFF991B1B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (selectedCategory == null) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        PlaylistSelectionCard("Energetic Playlist") { 
                            if (isInternetConnected) selectedCategory = "Energetic"
                        }
                        PlaylistSelectionCard("Calm Playlist") { 
                            if (isInternetConnected) selectedCategory = "Calm"
                        }
                    }
                } else {
                    val onPlayTrack: (TrackResult) -> Unit = { track ->
                        if (!isInternetConnected) {
                            playbackError = "Internet connection required to play previews."
                        } else {
                            playbackError = null
                            val isDifferentTrack = playingUrl != track.previewUrl
                            
                            // The first song excludes the 10s calibration because it follows the 30s session calibration
                            val shouldCalibrate = isConnected && sessionTotalFinished > 0 && isDifferentTrack
                            
                            if (shouldCalibrate) {
                                isCalibrating = true
                                calibrationTrack = track
                                try { mediaPlayer.reset() } catch(_: Exception) {}
                                playingUrl = null
                                isPaused = false
                            } else if (playingUrl == null || isDifferentTrack) {
                                // Play directly (First song or already calibrated)
                                try {
                                    mediaPlayer.reset()
                                    mediaPlayer.setDataSource(track.previewUrl)
                                    mediaPlayer.prepareAsync()
                                    mediaPlayer.setOnPreparedListener { it.start() }
                                    playingUrl = track.previewUrl
                                    
                                    // For the first track, use the session baseline HR
                                    currentTrack = if (sessionTotalFinished == 0) {
                                        track.copy(preTrackAvgBpm = userHeartRate.toInt())
                                    } else {
                                        track
                                    }

                                    isPaused = false
                                    playbackSamples.clear()
                                } catch (e: Exception) {
                                    playbackError = "Failed to play preview."
                                }
                            } else {
                                // Toggle Pause/Resume for the same track
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

                    val filteredSeeds = seeds.filter { it.category == selectedCategory }
                    val filteredRecs = recommendations.filter { it.category == selectedCategory }
                    val isCategoryComplete = remember(filteredSeeds, filteredRecs) {
                        (filteredSeeds + filteredRecs).isNotEmpty() && 
                        (filteredSeeds + filteredRecs).all { it.measuredBpm != null && it.measuredBpm > 0 }
                    }

                    val allFinished = remember(recommendations, seeds) {
                        recommendations.isNotEmpty() && 
                        recommendations.all { it.measuredBpm != null && it.measuredBpm > 0 } &&
                        seeds.all { it.measuredBpm != null && it.measuredBpm > 0 }
                    }

                    if (isCategoryComplete) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = colorScheme.primaryContainer,
                                contentColor = colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (allFinished) "✨ Experiment Complete! ✨" else "✅ Playlist Complete!",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = if (allFinished) 
                                        "Thank you so much for participating! We hope you liked the music selection. Your heart rate data and feedback have been successfully recorded for our research."
                                        else "You've finished this playlist! Thank you for your feedback. Feel free to explore the other category if you haven't yet.",
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    } else {
                        val allSeedsFinished = filteredSeeds.all { it.measuredBpm != null && it.measuredBpm > 0 }
                        val hasStartedAny = currentTrack != null || sessionTotalFinished > 0

                        if (!allSeedsFinished && !hasStartedAny) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = colorScheme.secondaryContainer,
                                    contentColor = colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "💡 To ensure the accuracy of our research, please listen to and evaluate all tracks in this playlist, including your initial seeds, so that we can better understand how your heart rate responds to different types of music.",
                                    modifier = Modifier.padding(16.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    if (isCalibrating) {
                        CalibrationOverlay(progress = calibrationProgress, track = calibrationTrack)
                        Spacer(Modifier.height(16.dp))
                    }

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (filteredSeeds.isNotEmpty()) {
                            item {
                                Text(
                                    "YOUR SEEDS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedCategory == "Energetic") colorScheme.primary else colorScheme.secondary,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                                items(filteredSeeds) { track ->
                                RecommendationItem(
                                    track = track,
                                    isPlaying = playingUrl == track.previewUrl,
                                    isPaused = isPaused,
                                    isEnabled = !isCalibrating && isInternetConnected,
                                    onPlay = { onPlayTrack(track) }
                                )
                            }
                        }

                        if (filteredRecs.isNotEmpty()) {
                            item {
                                Column {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = colorScheme.outlineVariant)
                                    Text(
                                        "RECOMMENDATIONS",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedCategory == "Energetic") colorScheme.primary else colorScheme.secondary,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                            items(filteredRecs) { track ->
                                RecommendationItem(
                                    track = track,
                                    isPlaying = playingUrl == track.previewUrl,
                                    isPaused = isPaused,
                                    isEnabled = !isCalibrating && isInternetConnected,
                                    onPlay = { onPlayTrack(track) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

