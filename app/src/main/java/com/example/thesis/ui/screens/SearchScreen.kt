package com.example.thesis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thesis.data.model.TrackResult
import com.example.thesis.data.remote.DeezerService
import com.example.thesis.ui.components.ThemeToggle
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    onNavigateHome: () -> Unit,
    categoryLabel: String,
    selectedSeeds: MutableList<TrackResult>,
    maxSeeds: Int,
    onNavigateBack: () -> Unit,
    onNavigateToSeeds: () -> Unit
) {
    var artistQuery by remember { mutableStateOf("") }
    var trackQuery  by remember { mutableStateOf("") }
    var results     by remember { mutableStateOf<List<TrackResult>>(emptyList()) }
    var isLoading   by remember { mutableStateOf(false) }
    var errorMsg    by remember { mutableStateOf("") }
    var trackWithNoPreview by remember { mutableStateOf<TrackResult?>(null) }

    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    Box(modifier = Modifier.fillMaxSize().background(colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colorScheme.onBackground
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

            Text("Search $categoryLabel", fontSize = 22.sp, color = colorScheme.onBackground, fontWeight = FontWeight.Light)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("MUSIC SEEDS", fontSize = 13.sp, color = colorScheme.secondary, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                
                // New stylized counter/button to view seeds
                Surface(
                    onClick = onNavigateToSeeds,
                    shape = RoundedCornerShape(8.dp),
                    color = colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$categoryLabel: ${selectedSeeds.size}/$maxSeeds",
                            color = colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value         = artistQuery,
                onValueChange = { artistQuery = it },
                label = { Text("Artist", color = colorScheme.onSurfaceVariant) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorScheme.primary,
                    unfocusedBorderColor = colorScheme.outline,
                    focusedTextColor = colorScheme.onBackground,
                    unfocusedTextColor = colorScheme.onBackground,
                    cursorColor = colorScheme.primary
                )
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = trackQuery,
                onValueChange = { trackQuery = it },
                label = { Text("Song title", color = colorScheme.onSurfaceVariant) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorScheme.primary,
                    unfocusedBorderColor = colorScheme.outline,
                    focusedTextColor = colorScheme.onBackground,
                    unfocusedTextColor = colorScheme.onBackground,
                    cursorColor = colorScheme.primary
                )
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (trackQuery.isBlank() && artistQuery.isBlank()) {
                        errorMsg = "Please enter a song or artist to search."
                        return@Button
                    }
                    errorMsg  = ""
                    isLoading = true
                    results   = emptyList()

                    scope.launch {
                        val rawResults = DeezerService.searchTracks(artistQuery, trackQuery)
                        results = rawResults.distinctBy { 
                            "${it.name?.lowercase()?.trim()}|${it.artist?.lowercase()?.trim()}" 
                        }
                        isLoading = false
                        if (results.isEmpty()) errorMsg = "No results found."
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary, contentColor = colorScheme.onPrimary)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Search", fontWeight = FontWeight.SemiBold)
            }

            if (errorMsg.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(errorMsg, color = Color(0xFFEF4444), fontSize = 12.sp)
            }

            Spacer(Modifier.height(16.dp))

            if (isLoading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colorScheme.secondary, modifier = Modifier.size(32.dp))
                }
            }

            if (trackWithNoPreview != null) {
                AlertDialog(
                    onDismissRequest = { trackWithNoPreview = null },
                    title = { Text("No Preview Available") },
                    text = { Text("The song '${trackWithNoPreview?.name}' does not have a preview available on Deezer. Please select another song to use as a seed.") },
                    confirmButton = {
                        Button(onClick = { trackWithNoPreview = null }) {
                            Text("OK")
                        }
                    }
                )
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(results) { track ->
                    val alreadyAdded = selectedSeeds.any {
                        it.name == track.name && it.artist == track.artist
                    }
                    val canAdd = selectedSeeds.size < maxSeeds && !alreadyAdded

                    var isAdding by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colorScheme.surface)
                            .clickable(enabled = canAdd && !isAdding) {
                                isAdding = true
                                scope.launch {
                                    try {
                                        val (preview, bpm) =
                                            DeezerService.getTrackDetails(track.artist ?: "", track.name ?: "", track.deezerId)
                                        if (preview.isBlank()) {
                                            trackWithNoPreview = track
                                        } else {
                                            if (selectedSeeds.size < maxSeeds) {
                                                selectedSeeds.add(
                                                    track.copy(
                                                        category = categoryLabel,
                                                        previewUrl = preview,
                                                        bpm = bpm
                                                    )
                                                )
                                                if (selectedSeeds.size == maxSeeds) onNavigateToSeeds()
                                            }
                                        }
                                    } finally {
                                        isAdding = false
                                    }
                                }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(track.name ?: "", color = colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(track.artist ?: "", color = colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                        when {
                            alreadyAdded -> Text("✓ Added", color = colorScheme.tertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            isAdding -> CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                            canAdd -> Text("+ Add", color = colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            else -> Text("Full", color = colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
