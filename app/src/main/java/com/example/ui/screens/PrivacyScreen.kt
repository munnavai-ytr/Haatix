package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material.icons.filled.PublicOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.DarkNavyCard
import com.example.ui.theme.DarkNavyCardBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonTeal
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Haatix Privacy Policy",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NeonCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkNavyBackground
                )
            )
        },
        containerColor = DarkNavyBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                PrivacyHighlightBanner()
            }

            item {
                PrivacyItemCard(
                    icon = Icons.Default.PublicOff,
                    title = "100% Offline — No Internet Permission",
                    desc = "Haatix declares NO internet permission in its AndroidManifest. It is technically impossible for any video frame, gesture data, or telemetry to be transmitted from this device to any cloud or remote server."
                )
            }

            item {
                PrivacyItemCard(
                    icon = Icons.Default.NoPhotography,
                    title = "Volatile Frame Processing",
                    desc = "Camera frames captured by your front camera are passed directly in memory to MediaPipe Tasks Vision on-device machine learning model. Frames are analyzed for hand landmarks in RAM and discarded immediately. No photos, videos, or raw biometric data are ever written to disk or storage."
                )
            }

            item {
                PrivacyItemCard(
                    icon = Icons.Default.TouchApp,
                    title = "Accessibility Dispatch Only",
                    desc = "The Accessibility Service is strictly configured with canRetrieveWindowContent=\"false\". Haatix CANNOT and DOES NOT inspect, read, or harvest screen contents, text, passwords, or personal messages. It only uses dispatchGesture() to simulate touch taps, scrolls, and the system Back action on your behalf."
                )
            }

            item {
                PrivacyItemCard(
                    icon = Icons.Default.Lock,
                    title = "User Sovereignty & Instant Stop",
                    desc = "You maintain full control. You can stop the foreground service at any time with a single tap from the persistent notification or the app's Start/Stop button. Stopping the service instantly releases the camera and closes all overlay views."
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PrivacyHighlightBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NeonTeal.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = NeonTeal,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Zero-Cloud Privacy Guarantee",
                    color = NeonTeal,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Everything in Haatix happens entirely inside your device's memory. Your privacy and security are mathematically protected by zero-network capability.",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun PrivacyItemCard(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkNavyCardBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 14.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    color = TextSecondary,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
