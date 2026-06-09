package com.example.thesis.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PulseDot(color: Color, active: Boolean) {
    val infinite = rememberInfiniteTransition(label = "ring")
    val ringAlpha by infinite.animateFloat(
        initialValue = 0.6f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1200)), label = "ringAlpha"
    )
    val ringScale by infinite.animateFloat(
        initialValue = 1f, targetValue = 2.2f,
        animationSpec = infiniteRepeatable(tween(1200)), label = "ringScale"
    )
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(20.dp)) {
        if (active) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .scale(ringScale)
                    .clip(CircleShape)
                    .background(color.copy(alpha = ringAlpha))
            )
        }
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color))
    }
}
