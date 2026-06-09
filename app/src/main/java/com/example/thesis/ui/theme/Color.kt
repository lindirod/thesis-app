package com.example.thesis.ui.theme

import androidx.compose.ui.graphics.Color

// Palette 1 (Light / Natural Sage)
val SageBg         = Color(0xFFEDE7DA)
val SageCard       = Color(0xFFC8D9D3)
val SageInteractive = Color(0xFFA8C8BE)
val SageButton     = Color(0xFF4E8C7E)
val SageBorder     = Color(0xFFB8A898)
val SageTextSec    = Color(0xFF8C6E5A)
val SageTextPri    = Color(0xFF1E3530)

// Palette 2 (Dark / Deep Forest)
val DeepBg         = Color(0xFF1C2130)
val DeepCard       = Color(0xFF2C3446)
val DeepInteractive = Color(0xFF4A7C6F)
val DeepButton     = Color(0xFF6B9E92)
val DeepBorder     = Color(0xFF8C7260)
val DeepTextSec    = Color(0xFFD4C8B8)
val DeepTextPri    = Color(0xFFF0EBE3)

// Legacy / Utility Colors (mapping to themes will be handled in Theme.kt)
// We keep these names to avoid breaking existing UI code, but we'll make them dynamic
val AccentGreen    = Color(0xFF22C55E) // Keeping this as it's used for success states
