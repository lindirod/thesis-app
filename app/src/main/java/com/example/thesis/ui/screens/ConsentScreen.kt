package com.example.thesis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConsentScreen(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "INFORMED CONSENT FORM",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            SectionTitle("Background")
            BodyText("This project is part of a Master's dissertation in Multimedia. The study aims to analyze the relationship between music recommendation and the user's physiological reactions.")

            Spacer(Modifier.height(16.dp))

            SectionTitle("1. Procedure (What you will do)")
            BodyText("By agreeing to participate, you will complete the following steps in the application:")
            BulletPoint("Select 4 seed tracks (2 you consider Calm and 2 Energetic).")
            BulletPoint("Allow an initial measurement of your heart rate (BPM) for 30 seconds.")
            BulletPoint("Allow a 10-second stabilization measurement before each recommended track.")
            BulletPoint("Listen to 30-second recommended music snippets.")
            BulletPoint("Briefly evaluate each song (perceived energy level and familiarity) while the application records your heart rate.")

            Spacer(Modifier.height(16.dp))

            SectionTitle("2. Data Collected")
            BodyText("The application will exclusively collect and associate the following data:")
            BulletPoint("Demographic data: Gender and age (provided by you at the beginning).")
            BulletPoint("Usage data: Selected songs and your subjective ratings.")
            BulletPoint("Physiological data: Your heart rate (BPM) recorded in real-time while listening to the songs.")
            Spacer(Modifier.height(8.dp))
            BodyText("Important note: No direct personally identifiable information (such as your name, email, contacts, or location) is collected.", fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(16.dp))

            SectionTitle("3. Confidentiality and Data Protection")
            BodyText("All collected data is completely anonymous. The obtained data is intended exclusively for scientific research purposes, being analyzed and reported strictly within the academic context of the master's thesis.")

            Spacer(Modifier.height(16.dp))

            SectionTitle("6. Voluntary Participation")
            BodyText("Your participation is entirely voluntary. You can stop using the application and withdraw from the study at any time, without providing a reason and without any form of penalty.")

            Spacer(Modifier.height(24.dp))

            SectionTitle("User Consent")
            BodyText("By clicking the \"Accept and Participate\" button, you confirm that:")
            BulletPoint("You have read and understood the information described above.")
            BulletPoint("You had the opportunity to clarify any doubts.")
            BulletPoint("You voluntarily agree to participate in the study and authorize the anonymous collection of your demographic and physiological data for the indicated academic purposes.")

            Spacer(Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.error)
                ) {
                    Text("Decline and Exit", textAlign = TextAlign.Center)
                }
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                ) {
                    Text("Accept and Participate", textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun BodyText(text: String, fontWeight: FontWeight = FontWeight.Normal) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = fontWeight,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 20.sp,
        textAlign = TextAlign.Justify,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun BulletPoint(text: String) {
    Row(modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
        Text(
            text = "•",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )
    }
}
