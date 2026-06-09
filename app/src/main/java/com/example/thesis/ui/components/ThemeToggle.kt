package com.example.thesis.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun ThemeToggle(
    isDarkMode: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    IconButton(
        onClick = onToggle,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = colorScheme.surface,
            contentColor = colorScheme.primary
        ),
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        Icon(
            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
            contentDescription = "Toggle Theme",
            modifier = Modifier.size(20.dp)
        )
    }
}
