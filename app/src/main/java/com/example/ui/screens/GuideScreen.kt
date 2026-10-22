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
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
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
                        text = "Haatix Guide (ব্যবহার নির্দেশিকা)",
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
                    title = "Best Position (হাত রাখার সঠিক নিয়ম)",
                    subtitle = "সহজে ও নিখুঁতভাবে নিয়ন্ত্রণের কৌশল"
                )
            }

            item {
                SetupStepCard(
                    stepNumber = "১",
                    title = "Distance & Light (দূরত্ব ও আলো)",
                    bengaliDesc = "ফোনটি মুখ ও হাত থেকে ৩০-৪০ সেমি (30-40 cm) দূরে রাখুন। পর্যাপ্ত আলোতে ব্যবহার করুন যাতে সামনের ক্যামেরা পুরো হাতটি স্পষ্ট দেখতে পারে।"
                )
            }

            item {
                SetupStepCard(
                    stepNumber = "২",
                    title = "Hand in Frame (হাত ক্যামেরার মাঝে রাখুন)",
                    bengaliDesc = "হাতের কবজি ও সব আঙ্গুল যেন ক্যামেরার ফ্রেমে থাকে। স্ক্রিনের রিং-এর নিচে কোনো সতর্কবার্তা (যেমন: 'Bring hand closer' বা 'Keep full hand in view') এলে সে অনুযায়ী হাত ঠিক করুন।"
                )
            }

            item {
                SetupStepCard(
                    stepNumber = "৩",
                    title = "Android 13+ Restricted Settings",
                    bengaliDesc = "যদি Accessibility সার্ভিস ধূসর বা অফ থাকে: Settings > Apps > Haatix > উপরের ৩টি ডটে ট্যাপ করে 'Allow restricted settings' সিলেক্ট করে পাসওয়ার্ড বা ফিঙ্গারপ্রিন্ট দিয়ে আনলক করুন।"
                )
            }

            // Gesture Cards
            item {
                Spacer(modifier = Modifier.height(12.dp))
                SectionHeader(
                    title = "How to Control in Your Hand (হাত দিয়ে নিয়ন্ত্রণ)",
                    subtitle = "Haatix V2 অঙ্গভঙ্গি (Gestures)"
                )
            }

            // Move
            item {
                GestureCard(
                    title = "Cursor Movement (কার্সর নাড়ানো)",
                    englishLabel = "Move Palm Center",
                    bengaliDesc = "হাতের তালু ডানে, বাঁয়ে, উপরে বা নিচে সরান। কার্সর তালুর কেন্দ্র অনুসরণ করে, তাই ক্লিক বা পিঞ্চ করার সময় কার্সর কাঁপবে না বা লক্ষ্য থেকে সরবে না।",
                    illustration = { MoveIllustration(size = 90.dp) }
                )
            }

            // Click
            item {
                GestureCard(
                    title = "Click / Tap (ক্লিক বা ট্যাপ)",
                    englishLabel = "Quick Pinch (Thumb + Index) & Release",
                    bengaliDesc = "কার্সরটি কাঙ্ক্ষিত বাটনে নিয়ে বৃদ্ধাঙ্গুল ও তর্জনী একসাথে লাগিয়ে সাথে সাথে ছেড়ে দিন (Release)। কার্সরটি ঠিক স্থানে লক হয়ে মাউসের মতো নিখুঁত ক্লিক করবে।",
                    illustration = { PinchTapIllustration(size = 90.dp) }
                )
            }

            // Drag
            item {
                GestureCard(
                    title = "Finger Drag (টেনে সরানো বা ড্র্যাগ)",
                    englishLabel = "Pinch & Hold for 350ms, then Move",
                    bengaliDesc = "বৃদ্ধাঙ্গুল ও তর্জনী জোড়া লাগিয়ে ০.৩৫ সেকেন্ড ধরে রাখুন (হালকা ভাইব্রেশন হবে)। এরপর হাত সরিয়ে হোম পেজ সোয়াইপ, অ্যাপ ড্রয়ার বা স্লাইডার টেনে সরান। আঙ্গুল খুললে ড্রপ হবে।",
                    illustration = { PinchTapIllustration(size = 90.dp) }
                )
            }

            // Wave Flick
            item {
                GestureCard(
                    title = "Wave Scroll / Flick (ঝটিকা সোয়াইপ)",
                    englishLabel = "Open Palm Quick Wave (Up/Down/Left/Right)",
                    bengaliDesc = "খোলা হাত দ্রুত উপরে/নিচে নাড়ালে পৃষ্ঠা স্ক্রোল হবে, এবং ডানে/বাঁয়ে নাড়ালে হোম স্ক্রিনের পৃষ্ঠা বদলাবে। ফিরতি হাত নামানোর সময় ভুল স্ক্রোল প্রতিরোধ ব্যবস্থা রয়েছে।",
                    illustration = { ScrollIllustration(size = 90.dp) }
                )
            }

            // Two Finger Scroll
            item {
                GestureCard(
                    title = "Continuous Scroll (অনবরত স্ক্রোল)",
                    englishLabel = "Two Fingers (V-Sign) Up / Down",
                    bengaliDesc = "ল্যাপটপের টাচপ্যাডের মতো তর্জনী ও মধ্যমা আঙ্গুল (V সাইন) তুলে হাত উপরে বা নিচে সরান। হাত যত দূরে সরাবেন তত দ্রুত স্ক্রোল হবে। হাত মাঝে রাখলে স্ক্রোল থেমে যাবে।",
                    illustration = { ScrollIllustration(size = 90.dp) }
                )
            }

            // Fist Back
            item {
                GestureCard(
                    title = "Back Action (পিছনে যাওয়া)",
                    englishLabel = "Hold Fist for 0.5s & Open",
                    bengaliDesc = "হাত শক্ত মুঠি (Fist) করে ০.৫ সেকেন্ড ধরে রাখুন (সবুজ বৃত্ত দেখা যাবে), তারপর হাত খুলুন। তাৎক্ষণিকভাবে ব্যাক বাটন (Back Action) কাজ করবে।",
                    illustration = { FistBackIllustration(size = 90.dp) }
                )
            }

            // Fist Pause
            item {
                GestureCard(
                    title = "Pause / Resume (সাময়িক বিরতি)",
                    englishLabel = "Hold Fist for 1.5s",
                    bengaliDesc = "ভিডিও দেখার সময় বা হাত বিশ্রাম দিতে মুঠি করে ১.৫ সেকেন্ড ধরে রাখুন (হলুদ বৃত্ত পূর্ণ হবে ও ভাইব্রেশন হবে)। আবার চালু করতে একই নিয়মে মুঠি ১.৫ সেকেন্ড ধরে রাখুন।",
                    illustration = { PauseResumeIllustration(size = 90.dp) }
                )
            }

            // Troubleshooting
            item {
                Spacer(modifier = Modifier.height(12.dp))
                SectionHeader(
                    title = "Troubleshooting (সমস্যা ও সমাধান)",
                    subtitle = "সহজে সমাধান করুন"
                )
            }

            item {
                TroubleshootCard(
                    title = "Cursor is Shaky (কার্সর কাঁপলে)",
                    desc = "হোম পেজের Settings-এ 'Cursor Smoothness' বাড়ান। হাত ক্যামেরার বেশি কাছে রাখবেন না এবং ভালো আলো ব্যবহার করুন।"
                )
            }

            item {
                TroubleshootCard(
                    title = "Click Missing Target (ক্লিক লক্ষ্য মিস করলে)",
                    desc = "Settings-এর Advanced Calibration অপশন থেকে Offset X এবং Offset Y সামঞ্জস্য করে আপনার হাত ও দৃষ্টির সাথে রিং-এর স্থান ক্যালিব্রেট করুন।"
                )
            }

            item {
                TroubleshootCard(
                    title = "Phone Gets Warm (ফোন গরম হলে)",
                    desc = "কম্পিউটার ভিশন প্রসেসিং এর কারণে ফোন কিছুটা উষ্ণ হওয়া স্বাভাবিক। ব্যাটারি সাশ্রয় করতে Settings থেকে Performance Mode 'Power Saver' বা 'Auto' নির্বাচন করুন।"
                )
            }

            item {
                TroubleshootCard(
                    title = "Service Stops Unexpectedly (সার্ভিস বন্ধ হলে)",
                    desc = "ফোনের ব্যাটারি অপটিমাইজেশন সেটিংসে গিয়ে Haatix অ্যাপের ব্যাটারি 'Unrestricted' বা 'No restrictions' করুন।"
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
