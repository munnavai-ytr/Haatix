package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.MainActivity
import com.example.R
import com.example.data.AirTouchSettings
import com.example.data.SettingsManager
import com.example.engine.EngineState
import com.example.engine.GestureAction
import com.example.engine.GestureEngine
import com.example.engine.Point3D
import com.example.overlay.CursorOverlay
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class GestureService : LifecycleService() {

    private lateinit var settingsManager: SettingsManager
    private lateinit var cursorOverlay: CursorOverlay
    private val gestureEngine = GestureEngine()

    private var handLandmarker: HandLandmarker? = null
    private var cameraExecutor: ExecutorService? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private var lastAnalyzedFrameTimeMs: Long = 0L

    override fun onCreate() {
        super.onCreate()
        settingsManager = SettingsManager.getInstance(this)
        cursorOverlay = CursorOverlay(this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        createNotificationChannel()
        startForegroundServiceNotification()

        cursorOverlay.show()
        cursorOverlay.setKeepScreenOn(settingsManager.settings.value.keepScreenOn)

        // Observe settings changes
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

    private fun initializeMediaPipe() {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("hand_landmarker.task")
                .build()

            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
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
                }
                .build()

            handLandmarker = HandLandmarker.createFromOptions(this, options)
            Log.i(TAG, "MediaPipe HandLandmarker loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load MediaPipe HandLandmarker", e)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(640, 480))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor ?: ContextCompat.getMainExecutor(this)) { imageProxy ->
                    processImageProxy(imageProxy)
                }

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(this, cameraSelector, imageAnalysis)
                Log.i(TAG, "CameraX front camera bound to service lifecycle")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start front camera", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        val now = SystemClock.uptimeMillis()
        val currentSettings = settingsManager.settings.value
        val minIntervalMs = 1000L / currentSettings.targetFps.coerceIn(10, 30)

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
            val bitmap = imageProxy.toBitmap()
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees

            val processedBitmap = if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }

            val mpImage = BitmapImageBuilder(processedBitmap).build()
            landmarker.detectAsync(mpImage, now)
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing image frame", e)
        } finally {
            imageProxy.close()
        }
    }

    private fun handleLandmarkResult(result: HandLandmarkerResult) {
        val allLandmarks = result.landmarks()
        val handLandmarks = if (allLandmarks.isNotEmpty() && allLandmarks[0].isNotEmpty()) {
            allLandmarks[0].map { Point3D(it.x(), it.y(), it.z()) }
        } else {
            null
        }

        val currentSettings = settingsManager.settings.value
        val nowMs = SystemClock.uptimeMillis()

        val engineState = gestureEngine.process(
            landmarks = handLandmarks,
            screenWidth = cursorOverlay.screenWidth,
            screenHeight = cursorOverlay.screenHeight,
            settings = currentSettings,
            nowMs = nowMs
        )

        // Update overlay view on main thread
        cursorOverlay.updateState(engineState)
        _engineStateFlow.value = engineState

        // Dispatch detected actions to accessibility service
        engineState.triggeredAction?.let { action ->
            dispatchAction(action)
        }
    }

    private fun dispatchAction(action: GestureAction) {
        val accessibility = AirTouchAccessibilityService.instance
        if (accessibility == null) {
            Log.w(TAG, "Cannot dispatch action: Accessibility service not connected")
            return
        }

        when (action) {
            is GestureAction.Tap -> {
                Log.d(TAG, "Dispatching TAP at (${action.x}, ${action.y})")
                accessibility.performTap(action.x, action.y)
            }
            is GestureAction.Scroll -> {
                Log.d(TAG, "Dispatching SCROLL from (${action.startX}, ${action.startY}) to (${action.endX}, ${action.endY})")
                accessibility.performSwipe(action.startX, action.startY, action.endX, action.endY)
            }
            is GestureAction.Back -> {
                Log.d(TAG, "Dispatching BACK")
                accessibility.performBack()
            }
            is GestureAction.TogglePause -> {
                Log.d(TAG, "Gesture tracking pause state toggled to ${action.isPaused}")
            }
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
        _engineStateFlow.value = EngineState()

        Log.i(TAG, "GestureService stopped and clean up completed")
    }

    companion object {
        private const val TAG = "GestureService"
        private const val CHANNEL_ID = "airtouch_foreground_channel"
        private const val NOTIFICATION_ID = 2001
        const val ACTION_STOP = "com.example.airtouch.ACTION_STOP"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _engineStateFlow = MutableStateFlow(EngineState())
        val engineStateFlow: StateFlow<EngineState> = _engineStateFlow.asStateFlow()

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
