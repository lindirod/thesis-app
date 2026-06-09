package com.example.thesis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thesis.ui.components.ThemeToggle

@Composable
fun SeedSelectionScreen(
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    onNavigateHome: () -> Unit,
    onSelectCategory: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
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

            Spacer(Modifier.height(32.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Text(
                text = "Select your seeds",
                fontSize = 28.sp,
                color = colorScheme.onBackground,
                fontWeight = FontWeight.Light
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "You will be asked to select both calm and energetic songs. While would you like to start with?",
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(48.dp))

            CategorySelectionCard(
                title = "Energetic",
                icon = Icons.Default.ElectricBolt,
                onClick = { onSelectCategory("Energetic") }
            )

            Spacer(Modifier.height(16.dp))

            CategorySelectionCard(
                title = "Calm",
                icon = Icons.Default.SelfImprovement,
                onClick = { onSelectCategory("Calm") }
            )
        }
    }
}
}

@Composable
fun CategorySelectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(Modifier.width(20.dp))
            
            Column {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            }
        }
    }
}
