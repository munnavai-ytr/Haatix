package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.HaatixSettings
import com.example.data.PerformanceMode
import com.example.data.SettingsManager
import com.example.overlay.CursorMode
import com.example.service.AirTouchAccessibilityService
import com.example.service.GestureService
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.DarkNavyCard
import com.example.ui.theme.DarkNavyCardBorder
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonTeal
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPractice: () -> Unit,
    onNavigateToGuide: () -> Unit,
    onNavigateToPrivacy: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val settings by settingsManager.settings.collectAsState()

    val isServiceRunning by GestureService.isServiceRunning.collectAsState()
    val uiState by GestureService.uiStateFlow.collectAsState()
    val isAccessibilityConnected by AirTouchAccessibilityService.isConnected.collectAsState()

    var hasCameraPermission by remember { mutableStateOf(false) }
    var hasOverlayPermission by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember { mutableStateOf(false) }

    fun refreshPermissions() {
        hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        hasOverlayPermission = Settings.canDrawOverlays(context)

        hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        refreshPermissions()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    val allPermissionsReady = hasCameraPermission && hasOverlayPermission && isAccessibilityConnected

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isServiceRunning) NeonCyan else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Haatix V2",
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            letterSpacing = 1.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToPractice,
                        modifier = Modifier.testTag("practice_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "Practice Mode",
                            tint = NeonTeal
                        )
                    }
                    IconButton(
                        onClick = onNavigateToGuide,
                        modifier = Modifier.testTag("guide_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = "User Guide",
                            tint = NeonCyan
                        )
                    }
                    IconButton(
                        onClick = onNavigateToPrivacy,
                        modifier = Modifier.testTag("privacy_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Privacy Policy",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkNavyBackground)
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
            // Live Status Radar Card
            item {
                Spacer(modifier = Modifier.height(4.dp))
                LiveStatusCard(
                    isRunning = isServiceRunning,
                    uiState = uiState
                )
            }

            // Big Start/Stop Button
            item {
                StartStopButton(
                    isRunning = isServiceRunning,
                    canStart = allPermissionsReady,
                    onToggle = {
                        if (isServiceRunning) {
                            GestureService.stop(context)
                        } else {
                            if (allPermissionsReady) {
                                GestureService.start(context)
                            }
                        }
                    }
                )
            }

            // Practice Mode Quick Action Banner
            item {
                PracticeActionCard(onNavigateToPractice = onNavigateToPractice)
            }

            // Setup Checklist
            item {
                SetupChecklistCard(
                    hasCamera = hasCameraPermission,
                    hasOverlay = hasOverlayPermission,
                    hasAccessibility = isAccessibilityConnected,
                    hasNotification = hasNotificationPermission,
                    onRequestCamera = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onRequestOverlay = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    },
                    onRequestAccessibility = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    },
                    onRequestNotification = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            }

            // Keep Screen ON Switch
            item {
                KeepScreenOnCard(
                    keepScreenOn = settings.keepScreenOn,
                    onToggle = { settingsManager.updateKeepScreenOn(it) }
                )
            }

            // Easy Settings & Calibration
            item {
                EasySettingsCard(
                    settings = settings,
                    settingsManager = settingsManager
                )
            }

            // Temperature / Efficiency Note
            item {
                SafetyNoteCard()
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun LiveStatusCard(
    isRunning: Boolean,
    uiState: com.example.service.HaatixUiState
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isRunning) NeonCyan.copy(alpha = 0.8f) else DarkNavyCardBorder,
        label = "border"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        if (isRunning) NeonCyan.copy(alpha = 0.15f) else Color(0x22546E7A)
                    )
                    .scale(if (isRunning) pulseScale else 1f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.PlayArrow else Icons.Default.Stop,
                    contentDescription = null,
                    tint = if (isRunning) NeonCyan else TextMuted,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isRunning) "HAATIX ACTIVE" else "HAATIX STOPPED",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = if (isRunning) NeonCyan else TextSecondary,
                        letterSpacing = 0.8.sp
                    )
                    if (isRunning && uiState.hasHand) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${uiState.fps} FPS · ${uiState.latencyMs}ms",
                            fontSize = 11.sp,
                            color = NeonTeal,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                val subText = when {
                    !isRunning -> "Service idle. Complete checklist below and press Start."
                    !uiState.hasHand -> "Scanning front camera... (Put hand in frame)"
                    uiState.isPaused -> "Paused (Hold fist 1.5s to resume)"
                    uiState.mode == CursorMode.PINCH_READY -> "Click Ready (Release to tap)"
                    uiState.mode == CursorMode.DRAGGING -> "Dragging / Touching screen"
                    uiState.mode == CursorMode.SCROLL -> "Two-Finger Continuous Scroll active"
                    else -> "Hand tracking active & responsive"
                }
                Text(
                    text = subText,
                    fontSize = 12.5.sp,
                    color = if (isRunning && uiState.hasHand) NeonTeal else TextSecondary,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun StartStopButton(
    isRunning: Boolean,
    canStart: Boolean,
    onToggle: () -> Unit
) {
    Button(
        onClick = onToggle,
        enabled = isRunning || canStart,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .testTag("start_stop_button"),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isRunning) NeonRed else NeonCyan,
            disabledContainerColor = Color(0xFF1E2D3D)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (isRunning) Color.White else Color(0xFF00363D),
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (isRunning) "STOP HAATIX SERVICE" else if (canStart) "START HAATIX" else "PERMISSIONS REQUIRED FIRST",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (isRunning) Color.White else if (canStart) Color(0xFF00363D) else TextMuted,
                letterSpacing = 0.6.sp
            )
        }
    }
}

@Composable
private fun PracticeActionCard(onNavigateToPractice: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToPractice() }
            .border(1.dp, NeonTeal.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkNavySurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(NeonTeal.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = NeonTeal,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Practice Mode (হাত দিয়ে প্র্যাকটিস করুন)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = NeonTeal
                )
                Text(
                    text = "Try tapping targets, scrolling lists & swiping pages in 2 minutes.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Text("Try →", fontWeight = FontWeight.Bold, color = NeonTeal, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SetupChecklistCard(
    hasCamera: Boolean,
    hasOverlay: Boolean,
    hasAccessibility: Boolean,
    hasNotification: Boolean,
    onRequestCamera: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onRequestNotification: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkNavyCardBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Setup Checklist (অনুমতি সেটআপ)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Haatix requires the following permissions to track hands and dispatch touch gestures.",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Camera
            ChecklistItem(
                title = "Front Camera Access",
                description = "Used strictly on-device to track hand landmarks",
                isGranted = hasCamera,
                icon = Icons.Default.CameraAlt,
                buttonLabel = "Grant",
                testTag = "camera_perm_button",
                onClick = onRequestCamera
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Overlay
            ChecklistItem(
                title = "Draw Over Other Apps",
                description = "Displays the holographic cyan ring cursor on top",
                isGranted = hasOverlay,
                icon = Icons.Default.Layers,
                buttonLabel = "Enable",
                testTag = "overlay_perm_button",
                onClick = onRequestOverlay
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Accessibility
            ChecklistItem(
                title = "Haatix Accessibility Service",
                description = "Dispatches taps, drags, scrolls & back (no screen reading)",
                isGranted = hasAccessibility,
                icon = Icons.Default.AccessibilityNew,
                buttonLabel = "Turn On",
                testTag = "accessibility_perm_button",
                onClick = onRequestAccessibility
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Spacer(modifier = Modifier.height(10.dp))

                // Notification
                ChecklistItem(
                    title = "Foreground Notification",
                    description = "Required to keep the camera service active & stop easily",
                    isGranted = hasNotification,
                    icon = Icons.Default.Notifications,
                    buttonLabel = "Allow",
                    testTag = "notification_perm_button",
                    onClick = onRequestNotification
                )
            }
        }
    }
}

@Composable
private fun ChecklistItem(
    title: String,
    description: String,
    isGranted: Boolean,
    icon: ImageVector,
    buttonLabel: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkNavySurface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isGranted) NeonTeal else NeonAmber,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
                color = TextPrimary
            )
            Text(
                text = description,
                fontSize = 11.5.sp,
                color = TextSecondary,
                lineHeight = 15.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        if (isGranted) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Granted",
                tint = NeonTeal,
                modifier = Modifier.size(24.dp)
            )
        } else {
            OutlinedButton(
                onClick = onClick,
                modifier = Modifier.testTag(testTag),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
            ) {
                Text(text = buttonLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun KeepScreenOnCard(
    keepScreenOn: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkNavyCardBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Keep screen ON while running",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Maintains display awake via an invisible overlay even outside the app.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Switch(
                checked = keepScreenOn,
                onCheckedChange = onToggle,
                modifier = Modifier.testTag("keep_screen_on_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NeonCyan,
                    checkedTrackColor = NeonCyan.copy(alpha = 0.35f)
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EasySettingsCard(
    settings: HaatixSettings,
    settingsManager: SettingsManager
) {
    var isAdvancedExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkNavyCardBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Easy Settings (সহজ সেটিংস)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cursor Speed
            Text(
                text = "Cursor Speed: ${String.format("%.2fx", settings.cursorSpeed)}",
                fontSize = 13.sp,
                color = TextSecondary
            )
            Slider(
                value = settings.cursorSpeed,
                onValueChange = { settingsManager.updateCursorSpeed(it) },
                valueRange = 0.8f..2.0f,
                steps = 11,
                colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Smoothness (One Euro Filter)
            Text(
                text = "Smoothness: ${if (settings.smoothness > 0.65f) "Ultra Smooth" else if (settings.smoothness < 0.35f) "Instant Response" else "Balanced (Recommended)"}",
                fontSize = 13.sp,
                color = TextSecondary
            )
            Slider(
                value = settings.smoothness,
                onValueChange = { settingsManager.updateSmoothness(it) },
                valueRange = 0.0f..1.0f,
                steps = 9,
                colors = SliderDefaults.colors(thumbColor = NeonTeal, activeTrackColor = NeonTeal)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Scroll Speed
            Text(
                text = "Scroll Speed: ${String.format("%.1fx", settings.scrollSpeed)}",
                fontSize = 13.sp,
                color = TextSecondary
            )
            Slider(
                value = settings.scrollSpeed,
                onValueChange = { settingsManager.updateScrollSpeed(it) },
                valueRange = 0.5f..2.0f,
                steps = 6,
                colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Performance Mode Chips
            Text(
                text = "Performance Mode (কর্মক্ষমতা)",
                fontSize = 13.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    PerformanceMode.AUTO to "Auto",
                    PerformanceMode.HIGH to "High (30 FPS)",
                    PerformanceMode.POWER_SAVER to "Power Saver (15 FPS)"
                ).forEach { (mode, label) ->
                    FilterChip(
                        selected = settings.performanceMode == mode,
                        onClick = { settingsManager.updatePerformanceMode(mode) },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan.copy(alpha = 0.25f),
                            selectedLabelColor = NeonCyan
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Behavior Toggles
            SettingToggleRow("Reverse scroll direction", settings.reverseScroll) {
                settingsManager.updateReverseScroll(it)
            }
            SettingToggleRow("Dwell click (hold still 1.0s to tap)", settings.dwellClick) {
                settingsManager.updateDwellClick(it)
            }
            SettingToggleRow("Haptic vibrations", settings.haptics) {
                settingsManager.updateHaptics(it)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Individual Gesture switches
            Text(
                text = "Gesture Toggles",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))

            SettingToggleRow("Pinch Click & Drag", settings.pinchClickEnabled) {
                settingsManager.updatePinchClickEnabled(it)
            }
            SettingToggleRow("Wave Flick Scroll (Open Palm)", settings.flickScrollEnabled) {
                settingsManager.updateFlickScrollEnabled(it)
            }
            SettingToggleRow("Two-Finger Continuous Scroll (V-Sign)", settings.vScrollEnabled) {
                settingsManager.updateVScrollEnabled(it)
            }
            SettingToggleRow("Fist (0.5s) to Back", settings.fistBackEnabled) {
                settingsManager.updateFistBackEnabled(it)
            }
            SettingToggleRow("Fist (1.5s) to Pause/Resume", settings.pauseEnabled) {
                settingsManager.updatePauseEnabled(it)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Collapsed Advanced Calibration Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isAdvancedExpanded = !isAdvancedExpanded }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Advanced / Calibration (ক্যালিব্রেশন)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = NeonTeal,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isAdvancedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = NeonTeal
                )
            }

            AnimatedVisibility(visible = isAdvancedExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkNavySurface)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Offset X (Horizontal calibration): ${settings.offsetX} px",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Slider(
                        value = settings.offsetX.toFloat(),
                        onValueChange = { settingsManager.updateOffsetX(it.toInt()) },
                        valueRange = -80f..80f,
                        steps = 15,
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Offset Y (Vertical calibration): ${settings.offsetY} px",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Slider(
                        value = settings.offsetY.toFloat(),
                        onValueChange = { settingsManager.updateOffsetY(it.toInt()) },
                        valueRange = -80f..80f,
                        steps = 15,
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NeonCyan,
                checkedTrackColor = NeonCyan.copy(alpha = 0.35f)
            )
        )
    }
}

@Composable
private fun SafetyNoteCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NeonAmber.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Thermostat,
                contentDescription = null,
                tint = NeonAmber,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Battery & Thermal Efficiency",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = NeonAmber
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Haatix V2 uses GPU acceleration and drops to 8 FPS during idle periods when no hand is in view to save power and prevent device warming.",
                    fontSize = 11.5.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
