package com.example.thesis.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thesis.data.model.TrackResult
import com.example.thesis.ui.components.ThemeToggle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    track: TrackResult,
    isDarkMode: Boolean,
    isSeed: Boolean = false,
    onToggleTheme: () -> Unit,
    onSubmit: (rating: Int, arousal: Int, familiarity: String, clipEval: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) }
    val totalSteps = if (isSeed) 3 else 4
    val colorScheme = MaterialTheme.colorScheme

    // State for answers
    // Using null for unselected states to enforce requirement
    var energyRating by remember { mutableStateOf<Int?>(null) }
    var moodCaptureRating by remember { mutableStateOf(if (isSeed) 0 else null) }
    var selectedFamiliarity by remember { mutableStateOf<String?>(null) }
    var clipEvaluation by remember { mutableStateOf<String?>(null) }

    val familiarityOptions = listOf(
        "I know it very well",
        "I've heard it before",
        "It's completely new to me"
    )
    
    val clipOptions = listOf("Yes", "No")

    // Handle system back button to go between steps
    BackHandler(enabled = currentStep > 1) {
        currentStep--
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isSeed) "Seed Feedback" else "Track Feedback", fontWeight = FontWeight.Light) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 1) {
                            currentStep--
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    ThemeToggle(isDarkMode = isDarkMode, onToggle = onToggleTheme)
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress Bar
            LinearProgressIndicator(
                progress = { currentStep.toFloat() / totalSteps },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = colorScheme.primary,
                trackColor = colorScheme.surfaceVariant,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                text = "Step $currentStep of $totalSteps",
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(32.dp))

            Text(
                text = "Feedback for \"${track.name}\"",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "by ${track.artist}",
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            // Logic to determine which question to show based on step and isSeed
            val isStepAnswered = when {
                currentStep == 1 -> energyRating != null
                !isSeed && currentStep == 2 -> moodCaptureRating != null
                (isSeed && currentStep == 2) || (!isSeed && currentStep == 3) -> selectedFamiliarity != null
                (isSeed && currentStep == 3) || (!isSeed && currentStep == 4) -> clipEvaluation != null
                else -> false
            }

            when {
                currentStep == 1 -> {
                    SliderQuestion(
                        title = "How would you describe the energy of this song?",
                        selectedValue = energyRating ?: 3, // default visually to middle
                        onValueChange = { energyRating = it },
                        labelLow = "Very calm",
                        labelHigh = "Very Energetic",
                        color = colorScheme.primary,
                        isAnswered = energyRating != null
                    )
                }
                !isSeed && currentStep == 2 -> {
                    SliderQuestion(
                        title = "Did the recommended song capture the vibe or mood of the song you originally chose?",
                        selectedValue = moodCaptureRating ?: 3,
                        onValueChange = { moodCaptureRating = it },
                        labelLow = "Not at all",
                        labelHigh = "Perfectly",
                        color = colorScheme.secondary,
                        isAnswered = moodCaptureRating != null
                    )
                }
                (isSeed && currentStep == 2) || (!isSeed && currentStep == 3) -> {
                    FamiliarityQuestion(
                        title = "How familiar are you with this song?",
                        options = familiarityOptions,
                        selectedOption = selectedFamiliarity ?: "",
                        onOptionSelected = { selectedFamiliarity = it }
                    )
                }
                (isSeed && currentStep == 3) || (!isSeed && currentStep == 4) -> {
                    FamiliarityQuestion(
                        title = "Was the 30-second clip enough for you to evaluate the song?",
                        options = clipOptions,
                        selectedOption = clipEvaluation ?: "",
                        onOptionSelected = { clipEvaluation = it }
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentStep > 1) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Previous", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Button(
                    onClick = {
                        if (currentStep < totalSteps) {
                            currentStep++
                        } else {
                            onSubmit(
                                moodCaptureRating ?: 0,
                                energyRating ?: 3,
                                selectedFamiliarity ?: "",
                                clipEvaluation ?: ""
                            )
                        }
                    },
                    enabled = isStepAnswered,
                    modifier = Modifier.weight(if (currentStep > 1) 1.5f else 1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (currentStep < totalSteps) "Next" else "Submit Feedback",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun SliderQuestion(
    title: String,
    selectedValue: Int,
    onValueChange: (Int) -> Unit,
    labelLow: String,
    labelHigh: String,
    color: androidx.compose.ui.graphics.Color,
    isAnswered: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(48.dp))
        
        Slider(
            value = selectedValue.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 1f..5f,
            steps = 3,
            colors = SliderDefaults.colors(
                thumbColor = if (isAnswered) color else colorScheme.outline,
                activeTrackColor = if (isAnswered) color else colorScheme.outlineVariant,
                inactiveTrackColor = colorScheme.outlineVariant.copy(alpha = 0.24f)
            )
        )
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            (1..5).forEach { i ->
                Text(
                    text = i.toString(),
                    fontSize = 14.sp,
                    fontWeight = if (selectedValue == i && isAnswered) FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedValue == i && isAnswered) color else colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(labelLow, fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
            Text(labelHigh, fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
        }
        
        if (!isAnswered) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Please move the slider to answer",
                fontSize = 12.sp,
                color = colorScheme.primary.copy(alpha = 0.7f),
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}

@Composable
fun FeedbackQuestion4(
    title: String,
    labelLow: String,
    labelHigh: String,
    selectedValue: Int,
    onValueChange: (Int) -> Unit,
    color: androidx.compose.ui.graphics.Color
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            (1..4).forEach { i ->
                val isSelected = selectedValue == i
                Surface(
                    onClick = { onValueChange(i) },
                    modifier = Modifier.size(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) color else colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = i.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) {
                                if (color == colorScheme.primary) colorScheme.onPrimary else colorScheme.onSecondary
                            } else colorScheme.onSurface
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(labelLow, fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
            Text(labelHigh, fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun FamiliarityQuestion(
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(32.dp))
        Column(Modifier.selectableGroup()) {
            options.forEach { text ->
                val isSelected = selectedOption == text
                Surface(
                    onClick = { onOptionSelected(text) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, colorScheme.primary) else null
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = colorScheme.primary)
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = text,
                            fontSize = 16.sp,
                            color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
