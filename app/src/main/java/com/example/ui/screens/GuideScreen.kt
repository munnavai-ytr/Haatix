package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FistBackIllustration
import com.example.ui.components.MoveIllustration
import com.example.ui.components.PauseResumeIllustration
import com.example.ui.components.PinchTapIllustration
import com.example.ui.components.ScrollIllustration
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.DarkNavyCard
import com.example.ui.theme.DarkNavyCardBorder
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonTeal
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "User Guide (ব্যবহার নির্দেশিকা)",
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
                SectionHeader(
                    title = "How to Use (কিভাবে ব্যবহার করবেন)",
                    subtitle = "সহজ ৩টি ধাপে এয়ারটাচ চালু করুন"
                )
            }

            // Step 1: Setup & Restricted Settings
            item {
                SetupStepCard(
                    stepNumber = "১",
                    title = "Setup Permissions (অনুমতি সেটআপ)",
                    bengaliDesc = "হোমপেজের চেকলিস্ট থেকে Camera, Overlay এবং Accessibility অন করুন। Android 13 বা তার নতুন ফোনে Accessibility অপশনটি ধূসর (Greyed out) থাকলে: Settings > Apps > AirTouch > উপরের ৩টি ডটে ট্যাপ করে 'Allow restricted settings' সিলেক্ট করুন।"
                )
            }

            // Step 2: Press Start
            item {
                SetupStepCard(
                    stepNumber = "২",
                    title = "Press Start (সার্ভিস চালু করুন)",
                    bengaliDesc = "হোম স্ক্রিনের বড় 'START AIRTOUCH' বাটনে ট্যাপ করুন। স্ক্রিনের উপরে একটি সাইয়ান (Cyan) রঙের হোলোগ্রাম কার্সর দেখা যাবে।"
                )
            }

            // Step 3: Positioning
            item {
                SetupStepCard(
                    stepNumber = "৩",
                    title = "Hand Position (হাত রাখার সঠিক নিয়ম)",
                    bengaliDesc = "ফোনটিকে সামনের ক্যামেরার দিকে ৩০-৪০ সেন্টিমিটার (30-40 cm) দূরত্বে রাখুন। রুমের আলো পর্যাপ্ত রাখুন যাতে পুরো হাতটি ক্যামেরার ফ্রেমে পরিষ্কার দেখা যায়।"
                )
            }

            // Gesture Controls Section
            item {
                Spacer(modifier = Modifier.height(12.dp))
                SectionHeader(
                    title = "How to Control in Your Hand (হাত দিয়ে নিয়ন্ত্রণ)",
                    subtitle = "প্রতিটি অঙ্গভঙ্গি (Gesture) এর নিয়ম ও কাজ"
                )
            }

            // Gesture 1: Move Cursor
            item {
                GestureCard(
                    title = "Cursor Movement (কার্সর নাড়ানো)",
                    englishLabel = "Move Open Palm",
                    bengaliDesc = "আপনার হাতের তালু (Open Palm) সামনের ক্যামেরার সামনে ডানে, বায়ে, উপরে বা নিচে নাড়ালে কার্সরটি স্ক্রিনের সেই স্থানে চলে যাবে। কার্সর হাতের তালুর কেন্দ্র অনুসারে সরে, তাই আঙ্গুল বাঁকা করলেও কার্সর লাফাবে না।",
                    illustration = { MoveIllustration(size = 90.dp) }
                )
            }

            // Gesture 2: Pinch = Tap
            item {
                GestureCard(
                    title = "Touch / Click (ট্যাপ বা ক্লিক)",
                    englishLabel = "Pinch (Thumb + Index)",
                    bengaliDesc = "কার্সরটি যে বাটনে নিতে চান সেখানে নিয়ে বৃদ্ধাঙ্গুল ও তর্জনী একসাথে জোড়া দিয়ে পিঞ্চ (Pinch) করুন। সাথে সাথে স্ক্রিনে ক্লিক বা ট্যাপ হয়ে যাবে।",
                    illustration = { PinchTapIllustration(size = 90.dp) }
                )
            }

            // Gesture 3: Scroll
            item {
                GestureCard(
                    title = "Page Scroll (পৃষ্ঠা স্ক্রোল)",
                    englishLabel = "Palm Swipe Up / Down",
                    bengaliDesc = "সোজা খোলা তালু দ্রুত উপরের দিকে তুললে নিচে স্ক্রোল হবে (Swipe Up), এবং দ্রুত নিচের দিকে নামালে উপরে স্ক্রোল হবে (Swipe Down)। ফেসবুক বা ব্রাউজিংয়ে এটি অত্যন্ত কার্যকর।",
                    illustration = { ScrollIllustration(size = 90.dp) }
                )
            }

            // Gesture 4: Fist = Back
            item {
                GestureCard(
                    title = "Back Button (পিছনে যাওয়া)",
                    englishLabel = "Hold Closed Fist for 0.7s",
                    bengaliDesc = "হাত দিয়ে একটি শক্ত মুঠি (Fist) করে ০.৭ সেকেন্ড ধরে রাখুন। সাথে সাথে ব্যাক বাটন (Android Back Action) কাজ করবে।",
                    illustration = { FistBackIllustration(size = 90.dp) }
                )
            }

            // Gesture 5: Hold Palm 2s = Pause / Resume
            item {
                GestureCard(
                    title = "Safety Pause (সাময়িক বিরতি)",
                    englishLabel = "Hold Open Palm Still for 2s",
                    bengaliDesc = "ভিডিও দেখার সময় বা ভুল টাচ এড়াতে খোলা হাত ২ সেকেন্ড স্থির ধরে রাখুন। কার্সর অ্যাম্বার (হলুদ) রঙে পজ (Paused) হয়ে যাবে। পুনরায় চালু করতে আবার হাত ২ সেকেন্ড স্থির রাখুন।",
                    illustration = { PauseResumeIllustration(size = 90.dp) }
                )
            }

            // Troubleshooting Section
            item {
                Spacer(modifier = Modifier.height(12.dp))
                SectionHeader(
                    title = "Troubleshooting (সমস্যা ও সমাধান)",
                    subtitle = "কোনো অসুবিধা হলে নিচের সমাধানগুলো দেখুন"
                )
            }

            item {
                TroubleshootCard(
                    title = "Cursor is Shaky (কার্সর বেশি কাঁপলে)",
                    desc = "হোম স্ক্রিনের Settings-এ গিয়ে 'Cursor Smoothing' কিছুটা বাড়িয়ে নিন। এছাড়াও রুমের আলো উজ্জ্বল রাখুন এবং হাত বেশি দ্রুত নাড়াবেন না।"
                )
            }

            item {
                TroubleshootCard(
                    title = "Phone Gets Warm (ফোন গরম হলে)",
                    desc = "AI ভিশন মডেল ক্রমাগত ক্যামেরার ফ্রেম বিশ্লেষণ করে বলে কিছুটা গরম হওয়া স্বাভাবিক। কম তাপমাত্রার জন্য Settings থেকে ক্যামেরা FPS '15 FPS' বা '20 FPS' নির্বাচন করুন।"
                )
            }

            item {
                TroubleshootCard(
                    title = "Service Stops Unexpectedly (সার্ভিস বন্ধ হয়ে গেলে)",
                    desc = "ফোনের ব্যাটারি অপটিমাইজেশন সেটিংস থেকে AirTouch অ্যাপের ব্যাটারি 'Unrestricted' বা 'No restrictions' করুন, যাতে ব্যাকগ্রাউন্ডে অ্যান্ড্রয়েড সিস্টেম সার্ভিস বন্ধ না করে।"
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            )
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextSecondary
            )
        )
    }
}

@Composable
private fun SetupStepCard(
    stepNumber: String,
    title: String,
    bengaliDesc: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkNavyCardBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(NeonCyan.copy(alpha = 0.2f))
                    .border(1.dp, NeonCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepNumber,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    fontSize = 15.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = bengaliDesc,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

@Composable
private fun GestureCard(
    title: String,
    englishLabel: String,
    bengaliDesc: String,
    illustration: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkNavyCardBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkNavyBackground)
                    .border(1.dp, DarkNavyCardBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                illustration()
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    fontSize = 15.sp
                )
                Text(
                    text = englishLabel,
                    fontWeight = FontWeight.Medium,
                    color = NeonTeal,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = bengaliDesc,
                    color = TextSecondary,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun TroubleshootCard(
    title: String,
    desc: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkNavyCardBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = NeonAmber,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    fontSize = 13.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}
