package com.example.thesis.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.thesis.data.ResearchDataManager
import com.example.thesis.data.UserSession
import com.example.thesis.data.model.SavedPlaylist
import com.example.thesis.data.model.TrackResult
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import com.google.gson.GsonBuilder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataExportScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    val dataManager = remember { ResearchDataManager(context) }
    var researchData by remember { mutableStateOf(dataManager.loadData()) }
    var sessionToDelete by remember { mutableStateOf<UserSession?>(null) }
    val file = File(context.filesDir, "research_data.json")

    if (sessionToDelete != null) {
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Delete Session?") },
            text = { Text("Are you sure you want to delete this session data? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    dataManager.deleteSession(sessionToDelete!!.sessionId)
                    researchData = dataManager.loadData()
                    sessionToDelete = null
                }) {
                    Text("Delete", color = colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Research Data History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (file.exists()) {
                        IconButton(onClick = { shareFile(context, file) }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(colorScheme.background)
        ) {
            if (researchData.sessions.orEmpty().isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No data collected yet.", color = colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(researchData.sessions.orEmpty().reversed()) { session ->
                        SessionDataCard(
                            session = session,
                            onDelete = { sessionToDelete = session },
                            onShare = { shareIndividualSession(context, session) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SessionDataCard(
    session: UserSession,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dateStr = sdf.format(Date(session.sessionId))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Session: $dateStr",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                    Text(
                        text = "${session.userProfile?.age ?: "?"}y, ${session.userProfile?.gender ?: "Unknown"}",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                    session.playlists.orEmpty().firstOrNull()?.let {
                        Text(
                            text = "Reference Heart Rate: ${it.averageBpm} BPM",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.secondary
                        )
                    }
                }
                
                Row {
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, contentDescription = "Share Session", tint = colorScheme.secondary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Session", tint = colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
            }

            if (session.initialCalibrationReadings.orEmpty().isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Initial Calibration - 30s Readings",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                CalibrationReadingsRow(session.initialCalibrationReadings.orEmpty())
            }

            if (session.playlists.orEmpty().isEmpty()) {
                Text(
                    "No playlists saved in this session.",
                    fontSize = 12.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                session.playlists.orEmpty().forEach { playlist ->
                    PlaylistDataSection(playlist)
                }
            }
        }
    }
}

@Composable
fun PlaylistDataSection(playlist: SavedPlaylist) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.padding(top = 12.dp)) {
        HorizontalDivider(thickness = 0.5.dp, color = colorScheme.outlineVariant)
        Spacer(Modifier.height(8.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                playlist.category?.uppercase() ?: "UNKNOWN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = colorScheme.secondary,
                letterSpacing = 1.sp
            )
        }

        playlist.tracks.orEmpty().forEach { track ->
            TrackDataRow(track)
        }
    }
}

@Composable
fun TrackDataRow(track: TrackResult) {
    val colorScheme = MaterialTheme.colorScheme
    val isSeed = track.fromSeed.isNullOrEmpty()
    
    Column(modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)) {
        // Main Row: Title and Metadata
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Indicator for Seed vs Recommendation
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        if (isSeed) colorScheme.primary else colorScheme.secondary,
                        shape = RoundedCornerShape(2.dp)
                    )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "${track.name} - ${track.artist}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }

        // Type Label (Seed vs Rec)
        val typeLabel = if (isSeed) "[SEED]" else "[REC from ${track.fromSeed}]"
        Text(
            typeLabel,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSeed) colorScheme.primary.copy(alpha = 0.7f) else colorScheme.secondary.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 16.dp, top = 2.dp)
        )

        // Tempo Row
        if ((track.bpm ?: 0) > 0) {
            Text(
                "Tempo: ${track.bpm} BPM",
                fontSize = 11.sp,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 2.dp)
            )
        }

        // Heart Rate Row (Base & Measured)
        if (track.measuredBpm != null && track.measuredBpm > 0) {
            Row(modifier = Modifier.padding(start = 16.dp, top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                if (track.preTrackAvgBpm != null) {
                    Text(
                        "Base: ${track.preTrackAvgBpm} | ",
                        fontSize = 11.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "Measured: ${track.measuredBpm} BPM",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary
                )
            }
        }

        // Pre-track Readings (10s)
        if (track.preTrackReadings.orEmpty().isNotEmpty()) {
            Text(
                "Pre-track Calibration (10s)",
                fontSize = 9.sp,
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
            Box(modifier = Modifier.padding(start = 16.dp)) {
                CalibrationReadingsRow(track.preTrackReadings.orEmpty())
            }
        }

        // During-track Readings Chart
        if (track.duringTrackReadings.orEmpty().isNotEmpty()) {
            Text(
                "During Track Readings",
                fontSize = 9.sp,
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
            Box(modifier = Modifier.padding(start = 16.dp)) {
                CalibrationReadingsRow(track.duringTrackReadings.orEmpty())
            }
        }
        
        // Metadata Column (Questionnaire Data)
        if ((track.rating ?: 0) > 0) {
            Column(modifier = Modifier.padding(start = 16.dp, top = 2.dp)) {
                val evalText = if (track.clipEvaluation != null) " | Clip Eval: ${track.clipEvaluation}" else ""
                val ratingText = "Vibe: ${track.rating}/5 | Arousal: ${track.arousalScale}/5 | ${track.familiarity}$evalText"

                Text(
                    text = ratingText,
                    fontSize = 10.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }

        val infoToShow = if (!track.recommendationMethod.isNullOrEmpty()) {
            "Method: ${track.recommendationMethod}"
        } else if (track.tags.orEmpty().isNotEmpty()) {
            "Tags: ${track.tags.orEmpty().take(5).joinToString(", ")}"
        } else null

        if (infoToShow != null) {
            Text(
                text = infoToShow,
                fontSize = 9.sp,
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 16.dp, top = 2.dp)
            )
        }
    }
}

private fun shareIndividualSession(context: Context, session: UserSession) {
    val gson = GsonBuilder().setPrettyPrinting().create()
    val sessionJson = gson.toJson(session)
    
    val sdf = SimpleDateFormat("dd_MM_yyyy_HHmm", Locale.getDefault())
    val dateStr = sdf.format(Date(session.sessionId))

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_TEXT, sessionJson)
        putExtra(Intent.EXTRA_SUBJECT, "Thesis Session Data ($dateStr)")
    }
    context.startActivity(Intent.createChooser(intent, "Export Session"))
}

private fun shareFile(context: Context, file: File) {
    if (!file.exists()) return
    
    // Using a simpler sharing method that doesn't strictly require FileProvider for small text files
    // though FileProvider is better for larger apps, for a thesis prototype this is often sufficient
    // and easier to implement quickly.
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_TEXT, file.readText())
        putExtra(Intent.EXTRA_SUBJECT, "Thesis Research Data")
    }
    context.startActivity(Intent.createChooser(intent, "Export Data"))
}

@Composable
fun CalibrationReadingsRow(readings: List<Int>) {
    val colorScheme = MaterialTheme.colorScheme
    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(readings.size) { index ->
            val hr = readings[index]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text("${index + 1}s", fontSize = 7.sp, color = colorScheme.onSurfaceVariant)
                Text("$hr", fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
