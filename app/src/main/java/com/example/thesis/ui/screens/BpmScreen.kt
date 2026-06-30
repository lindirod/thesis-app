package com.example.thesis.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thesis.ui.components.PulseDot

@Composable
fun BpmScreen(
    bpm: Double,
    connected: Boolean,
    seedCount: Int,
    maxSeeds: Int,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToSeeds: () -> Unit,
    onGeneratePlaylist: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    val bpmColor by animateColorAsState(
        targetValue = if (connected) colorScheme.primary else colorScheme.onSurfaceVariant,
        animationSpec = tween(600), label = "bpmColor"
    )

    val allSeedsSelected = seedCount >= maxSeeds

    Box(
        modifier = Modifier.fillMaxSize().background(colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Status and Theme Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Connection Status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PulseDot(color = if (connected) colorScheme.primary else colorScheme.onSurfaceVariant, active = connected)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text          = if (connected) "CONNECTED" else "WAITING FOR WATCH",
                        fontSize      = 11.sp,
                        color         = if (connected) colorScheme.primary else colorScheme.onSurfaceVariant,
                        letterSpacing = 2.sp,
                        fontWeight    = FontWeight.Bold
                    )
                }

                // Theme Toggle Button
                IconButton(
                    onClick = onToggleTheme,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = colorScheme.surface,
                        contentColor = colorScheme.primary
                    ),
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Theme",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
            Text("Physiological Playlist", fontSize = 22.sp, color = colorScheme.onBackground, fontWeight = FontWeight.Light)
            Text(if (allSeedsSelected) "READY TO MATCH" else "PREPARING VIBES", 
                fontSize = 13.sp, color = colorScheme.secondary, letterSpacing = 4.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(40.dp))

            // Card BPM
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(colorScheme.surface)
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LIVE HEART RATE", fontSize = 11.sp, color = colorScheme.onSurfaceVariant, letterSpacing = 2.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text       = if (connected) bpm.toInt().toString() else "–",
                            fontSize   = 80.sp,
                            color      = bpmColor,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 80.sp
                        )
                        if (connected) {
                            Spacer(Modifier.width(6.dp))
                            Text("BPM", fontSize = 18.sp, color = colorScheme.onSurfaceVariant, fontWeight = FontWeight.Light,
                                modifier = Modifier.padding(bottom = 12.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            if (!allSeedsSelected) {
                Text(
                    "Connect your watch to start, then select your music seeds to build your profile.",
                    fontSize = 14.sp, color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onNavigateToSearch,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = connected, // Mandatory connection to proceed
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Set Vibe Seeds (${seedCount}/$maxSeeds)", fontWeight = FontWeight.SemiBold)
                }
            } else {
                Text(
                    "Seeds ready. Ensure your heart rate is active to generate your physiological playlist.",
                    fontSize = 14.sp, color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onGeneratePlaylist,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = connected, // Enforce active physiological data
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.secondary)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Generate Physiological Playlist", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onNavigateToSeeds,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.onSurfaceVariant),
                    border = BorderStroke(1.dp, colorScheme.outline)
                ) {
                    Text("Edit My Seeds", fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.weight(1f))
            Text("Physiological data is required for matching.", fontSize = 11.sp, color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}
