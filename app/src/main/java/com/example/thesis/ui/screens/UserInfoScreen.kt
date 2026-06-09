package com.example.thesis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thesis.ui.components.ThemeToggle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInfoScreen(
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    onCreateSession: (String, String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var age by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val genders = listOf("Male", "Female")

    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(24.dp)
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
                        tint = colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Home button removed from here as per instructions:
                    // only allow going to front page AFTER putting info.
                    // This screen IS the gatekeeper.

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
                    text = "User Profile",
                    fontSize = 32.sp,
                    color = colorScheme.onBackground,
                    fontWeight = FontWeight.Light
                )

                Spacer(Modifier.height(8.dp))
                
                Text(
                    text = "Please provide your details to continue.",
                    fontSize = 14.sp,
                    color = colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(32.dp))

                OutlinedTextField(
                    value = age,
                    onValueChange = { if (it.all { char -> char.isDigit() }) age = it },
                    label = { Text("Age") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedGender,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Gender") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        genders.forEach { gender ->
                            DropdownMenuItem(
                                text = { Text(gender) },
                                onClick = {
                                    selectedGender = gender
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(48.dp))

                Button(
                    onClick = {
                        if (age.isNotBlank() && selectedGender.isNotBlank()) {
                            onCreateSession(age, selectedGender)
                        }
                    },
                    enabled = age.isNotBlank() && selectedGender.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary
                    )
                ) {
                    Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
