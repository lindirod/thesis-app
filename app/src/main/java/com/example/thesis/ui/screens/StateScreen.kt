package com.example.thesis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StateScreen(
    currentBpm: Double,
    baselineBpm: Double,
    onContinue: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    // Determine state
    val diff = currentBpm - baselineBpm
    val (stateTitle, description, icon, color) = when {
        diff > 15 -> Quad(
            "Highly Elevated", 
            "We detected you're feeling Anxious or Stressed. Your heart rate is significantly above your baseline.",
            Icons.Default.Warning,
            MaterialTheme.colorScheme.error
        )
        diff > 5 -> Quad(
            "Slightly Active", 
            "We detected you're feeling slightly Energetic or Alert. You're a bit above your resting baseline.",
            Icons.Default.Info,
            MaterialTheme.colorScheme.primary
        )
        diff < -5 -> Quad(
            "Resting / Relaxed", 
            "We detected you're feeling very Calm. Your heart rate is below your recorded baseline.",
            Icons.Default.CheckCircle,
            MaterialTheme.colorScheme.secondary
        )
        else -> Quad(
            "Baseline Stable", 
            "We detected you're in a Neutral state. Your heart rate is stable at your baseline.",
            Icons.Default.Info,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Box(
        modifier = Modifier.fillMaxSize().background(colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Physiological State",
                fontSize = 13.sp,
                color = colorScheme.secondary,
                letterSpacing = 4.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(48.dp))

            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = color
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = stateTitle.uppercase(),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = description,
                fontSize = 16.sp,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(Modifier.height(32.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("BASELINE", fontSize = 10.sp, color = colorScheme.onSurfaceVariant)
                        Text("${baselineBpm.toInt()}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(32.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CURRENT", fontSize = 10.sp, color = colorScheme.onSurfaceVariant)
                        Text("${currentBpm.toInt()}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
                    }
                }
            }

            Spacer(Modifier.height(64.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
            ) {
                Text("See Recommendations", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
