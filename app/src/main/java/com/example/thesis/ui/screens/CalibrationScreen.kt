package com.example.thesis.ui.screens

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thesis.ui.components.ThemeToggle
import kotlinx.coroutines.delay

@Composable
fun CalibrationScreen(
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    onNavigateHome: () -> Unit,
    currentBpm: Double,
    heartRateSampleCounter: Int,
    isConnected: Boolean,
    onCalibrationComplete: (Double, List<Int>) -> Unit,
    onCancel: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var timeLeft by remember { mutableIntStateOf(30) }
    var isRunning by remember { mutableStateOf(false) }
    var hasStartedTiming by remember { mutableStateOf(false) }
    val collectedBpms = remember { mutableStateListOf<Double>() }
    
    val progress by animateFloatAsState(
        targetValue = if (hasStartedTiming) (30 - timeLeft) / 30f else 0f,
        label = "calibrationProgress"
    )

    // Handle Timer and BPM Collection
    LaunchedEffect(isRunning, isConnected) {
        if (isRunning && isConnected) {
            hasStartedTiming = true
            
            // Countdown 30 seconds
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
            }
            
            // Once timeLeft is 0, finish immediately
            val average = if (collectedBpms.isNotEmpty()) collectedBpms.average() else currentBpm
            Log.d("CalibrationLog", "30s complete. Average: $average. Samples: ${collectedBpms.size}")
            onCalibrationComplete(average, collectedBpms.map { it.toInt() })
        }
    }

    // BPM sampling logic - triggered by heartRateSampleCounter
    LaunchedEffect(heartRateSampleCounter) {
        if (isRunning && isConnected && hasStartedTiming && currentBpm > 0 && timeLeft > 0) {
            if (collectedBpms.size < 30) {
                collectedBpms.add(currentBpm)
                Log.d("CalibrationLog", "Stored ($timeLeft): $currentBpm")
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Row: Back button, Home button, and Theme Toggle on the same line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Cancel",
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

            Spacer(Modifier.height(32.dp))

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Physiological Calibration", 
                    fontSize = 22.sp, 
                    fontWeight = FontWeight.Light,
                    color = colorScheme.onBackground
                )
                
                Text(
                    text = if (isRunning && !hasStartedTiming) "WAITING FOR HEART RATE..." else "STABILIZING BASELINE",
                    fontSize = 11.sp, 
                    color = colorScheme.secondary, 
                    letterSpacing = 2.sp, 
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(48.dp))

                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(200.dp),
                        color = colorScheme.primary,
                        strokeWidth = 8.dp,
                        trackColor = colorScheme.surface
                    )
                    Text(
                        text = "$timeLeft", 
                        fontSize = 48.sp, 
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onBackground
                    )
                }

                Spacer(Modifier.height(48.dp))

                val statusText = when {
                    !isConnected -> "Please connect your watch."
                    !isRunning -> "Stay still to measure your baseline."
                    !hasStartedTiming -> "Waiting for heart rate signal to start timer..."
                    else -> "Measuring: ${currentBpm.toInt()} BPM"
                }
                
                Text(
                    text = statusText, 
                    textAlign = TextAlign.Center, 
                    color = colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(32.dp))

                if (!isRunning) {
                    Button(
                        onClick = { isRunning = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = isConnected
                    ) {
                        Text("Start Calibration", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    OutlinedButton(
                        onClick = onCancel, 
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Cancel", color = colorScheme.onSurface)
                    }
                }
            }
        }
    }
}
