package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.MainActivity
import com.example.R
import com.example.data.HaatixSettings
import com.example.data.PerformanceMode
import com.example.data.SettingsManager
import com.example.engine.ActionSink
import com.example.engine.GestureEngine
import com.example.overlay.CursorMode
import com.example.overlay.CursorOverlay
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import kotlin.math.max
import kotlin.math.min

data class HaatixUiState(
    val isRunning: Boolean = false,
    val hasHand: Boolean = false,
    val isPaused: Boolean = false,
    val mode: CursorMode = CursorMode.NO_HAND,
    val statusText: String = "No Hand",
    val fps: Int = 20,
    val latencyMs: Int = 0
)

class GestureService : LifecycleService(), ActionSink {

    private lateinit var settingsManager: SettingsManager
    private lateinit var cursorOverlay: CursorOverlay
    private lateinit var gestureEngine: GestureEngine

    private var handLandmarker: HandLandmarker? = null
    private var cameraExecutor: ExecutorService? = null
    private var cameraProvider: ProcessCameraProvider? = null

    // Bitmap Pool (3 reusable bitmaps to prevent per-frame allocations)
    private val bitmapPoolSize = 3
    private val bitmapPool = arrayOfNulls<Bitmap>(bitmapPoolSize)
    private var poolIdx = 0

    // Reusable FloatArray(63) for landmarks
    private val reusableLandmarks = FloatArray(63)

    // Timestamp tracking for strictly monotonic detectAsync calls
    private var lastDetectTimestampMs: Long = 0L
    private var lastAnalyzedFrameTimeMs: Long = 0L

    // Adaptive FPS and Latency measurement
    private var currentTargetFps = 20
    private var latencyEma = 30f
    private var highLatencyStartTimeMs = 0L
    private var lowLatencyStartTimeMs = 0L
    private var sendTimestampMs = 0L

    // Vibrator for haptics
    private var vibrator: Vibrator? = null

    // UI State update throttling (max 5 updates/sec = 200ms)
    private var lastUiStateUpdateTimeMs = 0L

    override fun onCreate() {
        super.onCreate()
        settingsManager = SettingsManager.getInstance(this)
        cursorOverlay = CursorOverlay(this)
        gestureEngine = GestureEngine(this)

        initVibrator()

        // Create elevated priority executor for camera analysis
        val threadFactory = ThreadFactory { r ->
            Thread {
                Process.setThreadPriority(Process.THREAD_PRIORITY_MORE_FAVORABLE)
                r.run()
            }.apply { name = "HaatixVisionThread" }
        }
        cameraExecutor = Executors.newSingleThreadExecutor(threadFactory)

        createNotificationChannel()
        startForegroundServiceNotification()

        cursorOverlay.show()
        cursorOverlay.setKeepScreenOn(settingsManager.settings.value.keepScreenOn)

        val realSize = cursorOverlay.realSize
        gestureEngine.screenWidth = realSize.width
        gestureEngine.screenHeight = realSize.height

        lifecycleScope.launch {
            settingsManager.settings.collect { settings ->
                cursorOverlay.setKeepScreenOn(settings.keepScreenOn)
            }
        }

        initializeMediaPipe()
        startCamera()

        _isServiceRunning.value = true
        Log.i(TAG, "GestureService initialized and running")
    }

    private fun initVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            Log.i(TAG, "Stop action received in onStartCommand")
            stopSelf()
            return START_NOT_STICKY
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceNotification() {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, GestureService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.action_stop),
                stopPendingIntent
            )
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createLandmarker(useGpu: Boolean): HandLandmarker {
        val base = BaseOptions.builder()
            .setModelAssetPath("hand_landmarker.task")
            .setDelegate(if (useGpu) Delegate.GPU else Delegate.CPU)
            .build()

        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(base)
            .setMinHandDetectionConfidence(0.5f)
            .setMinHandPresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setNumHands(1)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener { result: HandLandmarkerResult, _: MPImage ->
                handleLandmarkResult(result)
            }
            .setErrorListener { error: RuntimeException ->
                Log.e(TAG, "MediaPipe error", error)
                if (useGpu) {
                    try {
                        handLandmarker?.close()
                        handLandmarker = createLandmarker(false)
                        Log.i(TAG, "Fell back to CPU delegate after GPU error")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to fallback to CPU", e)
                    }
                }
            }
            .build()

        return HandLandmarker.createFromOptions(this, options)
    }

    private fun initializeMediaPipe() {
        handLandmarker = try {
            createLandmarker(true).also { Log.i(TAG, "MediaPipe initialized with GPU delegate") }
        } catch (e: Throwable) {
            Log.w(TAG, "GPU delegate failed on init, trying CPU", e)
            try {
                createLandmarker(false).also { Log.i(TAG, "MediaPipe initialized with CPU delegate") }
            } catch (e2: Throwable) {
                Log.e(TAG, "Failed to initialize MediaPipe", e2)
                null
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                val settings = settingsManager.settings.value
                val targetSize = if (settings.performanceMode == PerformanceMode.POWER_SAVER) {
                    Size(320, 240)
                } else {
                    Size(480, 360)
                }

                val resolutionSelector = ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            targetSize,
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .build()

                val imageAnalysis = ImageAnalysis.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor ?: ContextCompat.getMainExecutor(this)) { imageProxy ->
                    processImageProxy(imageProxy)
                }

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(this, cameraSelector, imageAnalysis)
                Log.i(TAG, "CameraX front camera bound with ResolutionSelector: $targetSize")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start front camera", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        val now = SystemClock.uptimeMillis()
        val settings = settingsManager.settings.value

        // Adaptive FPS calculation
        val maxFps = when (settings.performanceMode) {
            PerformanceMode.HIGH -> 30
            PerformanceMode.POWER_SAVER -> 15
            PerformanceMode.AUTO -> currentTargetFps
        }

        // Idle check: if no hand detected, drop to 8 FPS to save battery
        val targetFps = if (!gestureEngine.hasHand) {
            8
        } else {
            maxFps
        }

        val minIntervalMs = 1000L / targetFps.coerceIn(8, 30)
        if (now - lastAnalyzedFrameTimeMs < minIntervalMs) {
            imageProxy.close()
            return
        }
        lastAnalyzedFrameTimeMs = now

        val landmarker = handLandmarker
        if (landmarker == null) {
            imageProxy.close()
            return
        }

        try {
            val width = imageProxy.width
            val height = imageProxy.height

            // Use round-robin reusable bitmap from pool
            var pooledBmp = bitmapPool[poolIdx]
            if (pooledBmp == null || pooledBmp.width != width || pooledBmp.height != height) {
                pooledBmp?.recycle()
                pooledBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmapPool[poolIdx] = pooledBmp
            }
            poolIdx = (poolIdx + 1) % bitmapPoolSize

            // Fast pixel buffer copy if rowStride matches, otherwise toBitmap() fallback
            val plane = imageProxy.planes[0]
            val buffer = plane.buffer
            if (plane.rowStride == width * 4) {
                buffer.rewind()
                pooledBmp.copyPixelsFromBuffer(buffer)
            } else {
                val tempBmp = imageProxy.toBitmap()
                val canvas = android.graphics.Canvas(pooledBmp)
                canvas.drawBitmap(tempBmp, 0f, 0f, null)
                tempBmp.recycle()
            }

            // ImageProcessingOptions handles rotation without creating rotated bitmap copies
            val options = ImageProcessingOptions.builder()
                .setRotationDegrees(imageProxy.imageInfo.rotationDegrees)
                .build()

            val mpImage = BitmapImageBuilder(pooledBmp).build()
            val detectTimestamp = max(now, lastDetectTimestampMs + 1)
            lastDetectTimestampMs = detectTimestamp
            sendTimestampMs = now

            landmarker.detectAsync(mpImage, options, detectTimestamp)
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing image frame", e)
        } finally {
            imageProxy.close()
        }
    }

    private fun handleLandmarkResult(result: HandLandmarkerResult) {
        val nowMs = SystemClock.uptimeMillis()

        // Measure roundtrip latency
        if (sendTimestampMs > 0L) {
            val lat = (nowMs - sendTimestampMs).toFloat()
            latencyEma = 0.85f * latencyEma + 0.15f * lat

            // Auto adaptive FPS adjustment
            val settings = settingsManager.settings.value
            if (settings.performanceMode == PerformanceMode.AUTO) {
                if (latencyEma > 55f) {
                    if (highLatencyStartTimeMs == 0L) highLatencyStartTimeMs = nowMs
                    if (nowMs - highLatencyStartTimeMs > 3000L) {
                        if (currentTargetFps > 15) {
                            currentTargetFps = if (currentTargetFps > 20) 20 else 15
                            highLatencyStartTimeMs = 0L
                        }
                    }
                } else {
                    highLatencyStartTimeMs = 0L
                }

                if (latencyEma < 25f && currentTargetFps < 30) {
                    if (lowLatencyStartTimeMs == 0L) lowLatencyStartTimeMs = nowMs
                    if (nowMs - lowLatencyStartTimeMs > 10000L) {
                        currentTargetFps = if (currentTargetFps < 20) 20 else 30
                        lowLatencyStartTimeMs = 0L
                    }
                } else {
                    lowLatencyStartTimeMs = 0L
                }
            }
        }

        val allLandmarks = result.landmarks()
        val hasHand = allLandmarks.isNotEmpty() && allLandmarks[0].isNotEmpty()

        if (hasHand) {
            val list = allLandmarks[0]
            val count = min(21, list.size)
            for (i in 0 until count) {
                val lm = list[i]
                reusableLandmarks[i * 3] = lm.x()
                reusableLandmarks[i * 3 + 1] = lm.y()
                reusableLandmarks[i * 3 + 2] = lm.z()
            }
        }

        val settings = settingsManager.settings.value
        gestureEngine.processFrame(
            landmarks = if (hasHand) reusableLandmarks else null,
            hasLandmarks = hasHand,
            settings = settings,
            nowMs = nowMs
        )

        // Update overlay view directly (dirty-rect invalidation handles performance)
        cursorOverlay.update(
            cursorX = gestureEngine.cursorX,
            cursorY = gestureEngine.cursorY,
            mode = gestureEngine.mode,
            fistProgress = gestureEngine.fistProgress,
            dwellProgress = gestureEngine.dwellProgress,
            scrollVelocityNorm = gestureEngine.scrollVelocityNorm,
            scrollIsHorizontal = gestureEngine.scrollIsHorizontal,
            hasHand = gestureEngine.hasHand,
            statusLabel = gestureEngine.statusLabel,
            hintLabel = gestureEngine.hintLabel,
            settings = settings,
            nowMs = nowMs
        )

        // Throttle UI State updates to max 5 Hz (200ms) for Compose HomeScreen
        if (nowMs - lastUiStateUpdateTimeMs >= 200L) {
            lastUiStateUpdateTimeMs = nowMs
            _uiStateFlow.value = HaatixUiState(
                isRunning = true,
                hasHand = gestureEngine.hasHand,
                isPaused = gestureEngine.isPaused,
                mode = gestureEngine.mode,
                statusText = gestureEngine.statusLabel,
                fps = currentTargetFps,
                latencyMs = latencyEma.toInt()
            )
        }
    }

    // ActionSink implementation: forwards to AirTouchAccessibilityService
    override fun tap(x: Float, y: Float) {
        cursorOverlay.triggerClickRipple(SystemClock.uptimeMillis())
        AirTouchAccessibilityService.instance?.tap(x, y)
    }

    override fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, ms: Long) {
        AirTouchAccessibilityService.instance?.swipe(x1, y1, x2, y2, ms)
    }

    override fun dragStart(x: Float, y: Float) {
        AirTouchAccessibilityService.instance?.dragStart(x, y)
    }

    override fun dragMove(x: Float, y: Float) {
        AirTouchAccessibilityService.instance?.dragMove(x, y)
    }

    override fun dragEnd() {
        AirTouchAccessibilityService.instance?.dragEnd()
    }

    override fun back() {
        AirTouchAccessibilityService.instance?.performBack()
    }

    override fun pauseChanged(paused: Boolean) {
        // Overlay and engine update automatically
    }

    override fun haptic(ms: Int) {
        if (!settingsManager.settings.value.haptics) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(
                        ms.toLong(),
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(ms.toLong())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration failed", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceRunning.value = false

        try {
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            Log.e(TAG, "Error unbinding camera", e)
        }

        try {
            handLandmarker?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing handLandmarker", e)
        }

        try {
            cameraExecutor?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down executor", e)
        }

        cursorOverlay.hide()
        gestureEngine.reset()

        // Recycle bitmaps
        for (i in 0 until bitmapPoolSize) {
            bitmapPool[i]?.recycle()
            bitmapPool[i] = null
        }

        _uiStateFlow.value = HaatixUiState(isRunning = false)
        Log.i(TAG, "GestureService stopped and clean up completed")
    }

    companion object {
        private const val TAG = "GestureService"
        private const val CHANNEL_ID = "haatix_foreground_channel"
        private const val NOTIFICATION_ID = 2001
        const val ACTION_STOP = "com.example.haatix.ACTION_STOP"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _uiStateFlow = MutableStateFlow(HaatixUiState())
        val uiStateFlow: StateFlow<HaatixUiState> = _uiStateFlow.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, GestureService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, GestureService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
