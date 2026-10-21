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
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
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
import com.example.data.AirTouchSettings
import com.example.data.SettingsManager
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
    onNavigateToGuide: () -> Unit,
    onNavigateToPrivacy: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val settings by settingsManager.settings.collectAsState()

    val isServiceRunning by GestureService.isServiceRunning.collectAsState()
    val engineState by GestureService.engineStateFlow.collectAsState()
    val isAccessibilityConnected by AirTouchAccessibilityService.isConnected.collectAsState()

    // Permission states checked reactively on resume
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
                            text = "AirTouch V1",
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            letterSpacing = 1.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToGuide,
                        modifier = Modifier.testTag("guide_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
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
                            tint = NeonTeal
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
                    engineState = engineState
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

            // Gestures & Tuning Settings
            item {
                SettingsCard(
                    settings = settings,
                    settingsManager = settingsManager
                )
            }

            // Warmth Warning / Battery note
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
    engineState: com.example.engine.EngineState
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
                Text(
                    text = if (isRunning) "AIRTOUCH ACTIVE" else "AIRTOUCH STOPPED",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = if (isRunning) NeonCyan else TextSecondary,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                val subText = when {
                    !isRunning -> "Service idle. Complete checklist below and press Start."
                    !engineState.isHandDetected -> "Scanning front camera... (Put hand in frame)"
                    engineState.isPaused -> "Paused (Hold open palm still 2s to resume)"
                    engineState.isPinching -> "Pinch Detected (Click triggered)"
                    engineState.isFist -> "Fist Detected (Holding for Back...)"
                    else -> "Hand tracking active & smooth"
                }
                Text(
                    text = subText,
                    fontSize = 12.5.sp,
                    color = if (isRunning && engineState.isHandDetected) NeonTeal else TextSecondary,
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
                text = if (isRunning) "STOP AIRTOUCH SERVICE" else if (canStart) "START AIRTOUCH" else "PERMISSIONS REQUIRED FIRST",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (isRunning) Color.White else if (canStart) Color(0xFF00363D) else TextMuted,
                letterSpacing = 0.6.sp
            )
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
                text = "Setup Checklist (প্রয়োজনীয় অনুমতি)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "AirTouch requires the following system permissions to work completely on-device.",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Camera
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

            // 2. Overlay
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

            // 3. Accessibility
            ChecklistItem(
                title = "AirTouch Accessibility Service",
                description = "Dispatches taps, scrolls & back actions (no screen reading)",
                isGranted = hasAccessibility,
                icon = Icons.Default.AccessibilityNew,
                buttonLabel = "Turn On",
                testTag = "accessibility_perm_button",
                onClick = onRequestAccessibility
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Spacer(modifier = Modifier.height(10.dp))

                // 4. Notifications
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
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = NeonCyan
                )
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
                    text = "Prevents display timeout even outside the app via an invisible overlay.",
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
private fun SettingsCard(
    settings: AirTouchSettings,
    settingsManager: SettingsManager
) {
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
                    text = "Gestures & Sensitivity Settings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sensitivity Slider
            Text(
                text = "Cursor Sensitivity: ${String.format("%.1fx", settings.sensitivity)}",
                fontSize = 13.sp,
                color = TextSecondary
            )
            Slider(
                value = settings.sensitivity,
                onValueChange = { settingsManager.updateSensitivity(it) },
                valueRange = 0.6f..2.4f,
                steps = 8,
                colors = SliderDefaults.colors(
                    thumbColor = NeonCyan,
                    activeTrackColor = NeonCyan
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Smoothing Slider
            Text(
                text = "Cursor Smoothing: ${if (settings.smoothing > 0.5f) "Fast / Responsive" else "Smooth / Stable"}",
                fontSize = 13.sp,
                color = TextSecondary
            )
            Slider(
                value = settings.smoothing,
                onValueChange = { settingsManager.updateSmoothing(it) },
                valueRange = 0.15f..0.75f,
                steps = 5,
                colors = SliderDefaults.colors(
                    thumbColor = NeonTeal,
                    activeTrackColor = NeonTeal
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Target FPS Chips
            Text(
                text = "Camera Frame Rate (Target FPS)",
                fontSize = 13.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 20, 30).forEach { fps ->
                    FilterChip(
                        selected = settings.targetFps == fps,
                        onClick = { settingsManager.updateTargetFps(fps) },
                        label = { Text("$fps FPS", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan.copy(alpha = 0.25f),
                            selectedLabelColor = NeonCyan
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Active Gestures Toggle",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            GestureToggleRow("Pinch to Tap", settings.pinchTapEnabled) {
                settingsManager.updatePinchTapEnabled(it)
            }
            GestureToggleRow("Palm Swipe to Scroll", settings.swipeScrollEnabled) {
                settingsManager.updateSwipeScrollEnabled(it)
            }
            GestureToggleRow("Fist (0.7s) to Back", settings.fistBackEnabled) {
                settingsManager.updateFistBackEnabled(it)
            }
            GestureToggleRow("Hold Still (2s) to Pause", settings.pauseResumeEnabled) {
                settingsManager.updatePauseResumeEnabled(it)
            }
        }
    }
}

@Composable
private fun GestureToggleRow(
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
                    text = "Notice: Device May Get Warm",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = NeonAmber
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Continuous neural net hand tracking utilizes GPU/NPU on-device. Choosing 15 or 20 FPS saves battery and keeps temperatures cooler.",
                    fontSize = 11.5.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
