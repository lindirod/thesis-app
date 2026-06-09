package com.example.thesis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thesis.ui.components.ThemeToggle
import com.example.thesis.data.model.TrackResult

@Composable
fun SeedsScreen(
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    onNavigateHome: () -> Unit,
    energeticSeeds: List<TrackResult>,
    calmSeeds: List<TrackResult>,
    maxSeedsPerType: Int,
    onRemoveEnergetic: (TrackResult) -> Unit,
    onRemoveCalm: (TrackResult) -> Unit,
    onAddMore: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToRecommendations: () -> Unit
) {
    val totalSeeds = energeticSeeds.size + calmSeeds.size
    val totalMax = maxSeedsPerType * 2
    val colorScheme = MaterialTheme.colorScheme

    Box(modifier = Modifier.fillMaxSize().background(colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            // Header Row: Back button, Home button, and Theme Toggle on the same line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colorScheme.onSurfaceVariant
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

            Text("My Seeds", fontSize = 22.sp, color = colorScheme.onBackground, fontWeight = FontWeight.Light)
            Text("$totalSeeds/$totalMax SELECTED", fontSize = 13.sp, color = colorScheme.secondary,
                letterSpacing = 4.sp, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(24.dp))

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Energetic Section
                item {
                    SeedCategoryHeader("Energetic", energeticSeeds.size, maxSeedsPerType) {
                        onAddMore("Energetic")
                    }
                }
                items(energeticSeeds) { track ->
                    SeedItem(track, colorScheme.primary) { onRemoveEnergetic(track) }
                }

                item { Spacer(Modifier.height(8.dp)) }

                // Calm Section
                item {
                    SeedCategoryHeader("Calm", calmSeeds.size, maxSeedsPerType) {
                        onAddMore("Calm")
                    }
                }
                items(calmSeeds) { track ->
                    SeedItem(track, colorScheme.secondary) { onRemoveCalm(track) }
                }
            }

            Spacer(Modifier.height(16.dp))
            
            // Get Recommendations Button
            Button(
                onClick = onNavigateToRecommendations,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary, contentColor = colorScheme.onPrimary),
                enabled = totalSeeds > 0
            ) {
                Text("Get Recommendations", fontWeight = FontWeight.SemiBold)
            }

            if (totalSeeds == totalMax) {
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorScheme.tertiary.copy(alpha = 0.1f))
                        .padding(14.dp)
                ) {
                    Text(
                        "All seeds selected! Ready for recommendations.",
                        color = colorScheme.tertiary, fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun SeedCategoryHeader(label: String, count: Int, max: Int, onAdd: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label.uppercase(), color = colorScheme.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text("$count/$max Selected", color = colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        if (count < max) {
            IconButton(
                onClick = onAdd,
                modifier = Modifier.size(32.dp).clip(CircleShape).background(colorScheme.surface)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = colorScheme.secondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun SeedItem(track: TrackResult, color: Color, onRemove: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(track.name ?: "", color = colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(track.artist ?: "", color = colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }

        IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Remove", tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
    }
}
