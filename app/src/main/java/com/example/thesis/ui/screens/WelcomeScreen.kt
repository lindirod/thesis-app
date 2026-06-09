package com.example.thesis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thesis.ui.components.ConsentDialog
import com.example.thesis.ui.components.ThemeToggle

@Composable
fun WelcomeScreen(
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    hasSeeds: Boolean,
    hasPlaylists: Boolean,
    onStartFlow: () -> Unit,
    onViewSeeds: () -> Unit,
    onViewPlaylists: () -> Unit,
    onResetSeeds: () -> Unit,
    onViewData: () -> Unit,
    onResumeSession: () -> Unit,
    onExitApp: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var showConsent by remember { mutableStateOf(!hasSeeds && !hasPlaylists) }

    if (showConsent) {
        ConsentDialog(
            onAccept = { showConsent = false },
            onDecline = onExitApp
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(24.dp)
    ) {
        // Theme Toggle at the top right
        ThemeToggle(
            isDarkMode = isDarkMode,
            onToggle = onToggleTheme,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 24.dp)
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Thesis Project",
                fontSize = 13.sp,
                color = colorScheme.secondary,
                letterSpacing = 4.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = "Music Recommendation",
                fontSize = 32.sp,
                color = colorScheme.onBackground,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
                lineHeight = 38.sp
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Explore personalized music recommendations and discover how different songs may influence your heart rate and emotional state",
                fontSize = 15.sp,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(48.dp))

            Button(
                onClick = if (hasSeeds || hasPlaylists) onResumeSession else onStartFlow,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary
                )
            ) {
                Text(
                    text = if (hasSeeds || hasPlaylists) "Resume Experiment" else "Get Started",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (hasSeeds) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onViewSeeds,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("View My Seeds", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (hasPlaylists) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onViewPlaylists,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Saved Playlists", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (hasSeeds) {
                Spacer(Modifier.height(24.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = onResetSeeds) {
                        Text(
                            "Start Over",
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text("•", color = colorScheme.onSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 8.dp))
                    TextButton(onClick = onViewData) {
                        Text(
                            "View Data",
                            color = colorScheme.secondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                // If no seeds, still show "View Data" at the bottom if someone wants to check it
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = onViewData) {
                    Text(
                        "View Data",
                        color = colorScheme.secondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
