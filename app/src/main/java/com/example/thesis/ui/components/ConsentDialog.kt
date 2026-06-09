package com.example.thesis.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ConsentDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Dialog(
        onDismissRequest = { /* Cannot dismiss without action */ },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text(
                    text = "INFORMED CONSENT FORM",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    SectionText("Background", "This project is part of a Master's dissertation in Multimedia. The study aims to analyze the relationship between music recommendation and the user's physiological reactions.")

                    Spacer(Modifier.height(12.dp))

                    SectionTitle("1. Procedure (What you will do)")
                    BodyText("By agreeing to participate, you will complete the following steps in the application:")
                    BulletItem("Select 4 seed tracks (2 Calm and 2 Energetic).")
                    BulletItem("Allow an initial 30-second heart rate measurement.")
                    BulletItem("Allow a 10-second measurement before each recommendation.")
                    BulletItem("Listen to 30-second recommended music snippets.")
                    BulletItem("Evaluate each song while your heart rate is recorded.")

                    Spacer(Modifier.height(12.dp))

                    SectionTitle("2. Data Collected")
                    BodyText("The application will collect:")
                    BulletItem("Demographic data: Gender and age.")
                    BulletItem("Usage data: Selected songs and subjective ratings.")
                    BulletItem("Physiological data: Real-time heart rate (BPM).")
                    Spacer(Modifier.height(4.dp))
                    BodyText("Note: No personally identifiable information (name, email, location) is collected.", fontWeight = FontWeight.Bold)

                    Spacer(Modifier.height(12.dp))

                    SectionText("3. Confidentiality", "All data is anonymous and intended exclusively for scientific research within the master's thesis context.")

                    Spacer(Modifier.height(12.dp))

                    SectionText("6. Voluntary Participation", "Participation is entirely voluntary. You can withdraw at any time without penalty.")

                    Spacer(Modifier.height(12.dp))

                    SectionTitle("User Consent")
                    BodyText("By clicking \"Accept and Participate\", you confirm you have read this info, clarified doubts, and authorize anonymous data collection for academic purposes.")
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDecline,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Decline", color = colorScheme.error)
                    }

                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Accept", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun SectionText(title: String, body: String) {
    SectionTitle(title)
    BodyText(body)
}

@Composable
private fun BodyText(text: String, fontWeight: FontWeight = FontWeight.Normal) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = fontWeight,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 18.sp
    )
}

@Composable
private fun BulletItem(text: String) {
    Row(modifier = Modifier.padding(start = 4.dp, top = 2.dp)) {
        Text("• ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
    }
}
