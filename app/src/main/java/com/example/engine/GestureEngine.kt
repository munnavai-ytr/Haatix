package com.example.engine

import com.example.data.AirTouchSettings
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

data class Point3D(
    val x: Float,
    val y: Float,
    val z: Float = 0f
)

sealed interface GestureAction {
    data class Tap(val x: Float, val y: Float) : GestureAction
    data class Scroll(val startX: Float, val startY: Float, val endX: Float, val endY: Float) : GestureAction
    object Back : GestureAction
    data class TogglePause(val isPaused: Boolean) : GestureAction
}

data class EngineState(
    val cursorX: Float = 0f,
    val cursorY: Float = 0f,
    val isHandDetected: Boolean = false,
    val isPaused: Boolean = false,
    val isPinching: Boolean = false,
    val isFist: Boolean = false,
    val isOpenPalm: Boolean = false,
    val fistProgress: Float = 0f,       // 0f .. 1f
    val pauseProgress: Float = 0f,      // 0f .. 1f
    val statusText: String = "No Hand Detected",
    val triggeredAction: GestureAction? = null
)

class GestureEngine {

    var isPaused: Boolean = false
        private set

    private var smoothCursorX: Float = 0f
    private var smoothCursorY: Float = 0f
    private var isInitialized: Boolean = false

    // Pinch detection state
    private var isPinching: Boolean = false
    private var pinchStartTime: Long = 0L
    private var tapTriggeredForCurrentPinch: Boolean = false

    // Fist detection state
    private var fistStartTime: Long = 0L

    // Still palm pause detection state
    private var stillStartTime: Long = 0L

    // History for velocity calculation (timestampMs, rawX, rawY)
    private val positionHistory = ArrayDeque<Pair<Long, Pair<Float, Float>>>()

    // Global action cooldown (600ms between discrete actions)
    private var lastActionTime: Long = 0L
    private val actionCooldownMs: Long = 600L

    fun reset() {
        isInitialized = false
        isPinching = false
        pinchStartTime = 0L
        tapTriggeredForCurrentPinch = false
        fistStartTime = 0L
        stillStartTime = 0L
        positionHistory.clear()
        isPaused = false
    }

    fun setPauseState(paused: Boolean) {
        isPaused = paused
    }

    fun process(
        landmarks: List<Point3D>?,
        screenWidth: Int,
        screenHeight: Int,
        settings: AirTouchSettings,
        nowMs: Long
    ): EngineState {
        if (landmarks == null || landmarks.size < 21 || screenWidth <= 0 || screenHeight <= 0) {
            isPinching = false
            pinchStartTime = 0L
            tapTriggeredForCurrentPinch = false
            fistStartTime = 0L
            stillStartTime = 0L
            positionHistory.clear()

            return EngineState(
                cursorX = smoothCursorX,
                cursorY = smoothCursorY,
                isHandDetected = false,
                isPaused = isPaused,
                statusText = if (isPaused) "Paused" else "No Hand Detected",
                triggeredAction = null
            )
        }

        // 1. Palm Center = Average of landmarks 0, 5, 9, 13, 17
        val rawPalmX = (landmarks[0].x + landmarks[5].x + landmarks[9].x + landmarks[13].x + landmarks[17].x) / 5f
        val rawPalmY = (landmarks[0].y + landmarks[5].y + landmarks[9].y + landmarks[13].y + landmarks[17].y) / 5f

        // 2. Mirror X for front camera
        val mirroredX = 1f - rawPalmX
        val rawY = rawPalmY

        // 3. Margin mapping (margins = 0.16f so screen edges are easily reachable)
        val margin = 0.16f
        val clampedNormX = ((mirroredX - margin) / (1f - 2f * margin)).coerceIn(0f, 1f)
        val clampedNormY = ((rawY - margin) / (1f - 2f * margin)).coerceIn(0f, 1f)

        // Sensitivity scaling from screen center (0.5)
        val sensX = ((clampedNormX - 0.5f) * settings.sensitivity + 0.5f).coerceIn(0f, 1f)
        val sensY = ((clampedNormY - 0.5f) * settings.sensitivity + 0.5f).coerceIn(0f, 1f)

        val targetScreenX = sensX * screenWidth
        val targetScreenY = sensY * screenHeight

        // 4. Dead zone + EMA Smoothing
        if (!isInitialized) {
            smoothCursorX = targetScreenX
            smoothCursorY = targetScreenY
            isInitialized = true
        } else {
            val dist = hypot(targetScreenX - smoothCursorX, targetScreenY - smoothCursorY)
            val deadZonePx = 4f
            if (dist > deadZonePx) {
                val alpha = settings.smoothing.coerceIn(0.08f, 0.85f)
                smoothCursorX += alpha * (targetScreenX - smoothCursorX)
                smoothCursorY += alpha * (targetScreenY - smoothCursorY)
            }
        }

        // Update velocity history (retain last 250ms)
        positionHistory.addLast(nowMs to (rawPalmX to rawPalmY))
        while (positionHistory.isNotEmpty() && nowMs - positionHistory.first().first > 250L) {
            positionHistory.removeFirst()
        }

        // 5. Palm scale reference (distance between wrist 0 and middle MCP 9)
        val wrist = landmarks[0]
        val middleMcp = landmarks[9]
        val palmSize = max(hypot(middleMcp.x - wrist.x, middleMcp.y - wrist.y), 0.04f)

        // 6. Check finger extension for Open Palm
        // Compare fingertip distance to wrist vs PIP distance to wrist
        val indexExtended = hypot(landmarks[8].x - wrist.x, landmarks[8].y - wrist.y) >
                hypot(landmarks[6].x - wrist.x, landmarks[6].y - wrist.y) * 1.1f
        val middleExtended = hypot(landmarks[12].x - wrist.x, landmarks[12].y - wrist.y) >
                hypot(landmarks[10].x - wrist.x, landmarks[10].y - wrist.y) * 1.1f
        val ringExtended = hypot(landmarks[16].x - wrist.x, landmarks[16].y - wrist.y) >
                hypot(landmarks[14].x - wrist.x, landmarks[14].y - wrist.y) * 1.1f
        val pinkyExtended = hypot(landmarks[20].x - wrist.x, landmarks[20].y - wrist.y) >
                hypot(landmarks[18].x - wrist.x, landmarks[18].y - wrist.y) * 1.1f

        val extendedCount = (if (indexExtended) 1 else 0) +
                (if (middleExtended) 1 else 0) +
                (if (ringExtended) 1 else 0) +
                (if (pinkyExtended) 1 else 0)
        val isOpenPalm = extendedCount >= 3

        // 7. Check for Closed Fist
        // All four fingertips are curled close to MCP joints
        val indexCurled = hypot(landmarks[8].x - landmarks[5].x, landmarks[8].y - landmarks[5].y) < palmSize * 0.65f
        val middleCurled = hypot(landmarks[12].x - landmarks[9].x, landmarks[12].y - landmarks[9].y) < palmSize * 0.65f
        val ringCurled = hypot(landmarks[16].x - landmarks[13].x, landmarks[16].y - landmarks[13].y) < palmSize * 0.65f
        val pinkyCurled = hypot(landmarks[20].x - landmarks[17].x, landmarks[20].y - landmarks[17].y) < palmSize * 0.65f
        val isFist = indexCurled && middleCurled && ringCurled && pinkyCurled

        // 8. Pinch Detection (Thumb tip 4 to Index tip 8)
        val thumbTip = landmarks[4]
        val indexTip = landmarks[8]
        val pinchDist = hypot(thumbTip.x - indexTip.x, thumbTip.y - indexTip.y)
        val pinchRatio = pinchDist / palmSize

        val enterPinchThreshold = 0.38f
        val exitPinchThreshold = 0.52f

        if (!isPinching && pinchRatio < enterPinchThreshold) {
            isPinching = true
            pinchStartTime = nowMs
            tapTriggeredForCurrentPinch = false
        } else if (isPinching && pinchRatio > exitPinchThreshold) {
            isPinching = false
            pinchStartTime = 0L
            tapTriggeredForCurrentPinch = false
        }

        var triggeredAction: GestureAction? = null
        var currentStatus = if (isPaused) "Paused" else "Active"
        var fistProgress = 0f
        var pauseProgress = 0f

        val canPerformAction = (nowMs - lastActionTime) >= actionCooldownMs

        // GESTURE 1: Open palm held still 2s = PAUSE / RESUME (safety switch)
        if (isOpenPalm && settings.pauseResumeEnabled) {
            val oldest = positionHistory.firstOrNull()
            val movementDist = if (oldest != null) {
                hypot(rawPalmX - oldest.second.first, rawPalmY - oldest.second.second)
            } else 0f

            if (movementDist < 0.05f) { // Hand held relatively still
                if (stillStartTime == 0L) stillStartTime = nowMs
                val heldMs = nowMs - stillStartTime
                pauseProgress = (heldMs / 2000f).coerceIn(0f, 1f)

                if (heldMs >= 2000L && canPerformAction) {
                    isPaused = !isPaused
                    triggeredAction = GestureAction.TogglePause(isPaused)
                    lastActionTime = nowMs
                    stillStartTime = 0L
                    currentStatus = if (isPaused) "Gestures Paused" else "Gestures Resumed"
                }
            } else {
                stillStartTime = 0L
                pauseProgress = 0f
            }
        } else {
            stillStartTime = 0L
            pauseProgress = 0f
        }

        // Only process navigation/touch gestures if not paused
        if (!isPaused && triggeredAction == null) {

            // GESTURE 2: Fist held 0.7s = BACK
            if (isFist && settings.fistBackEnabled) {
                if (fistStartTime == 0L) fistStartTime = nowMs
                val heldMs = nowMs - fistStartTime
                fistProgress = (heldMs / 700f).coerceIn(0f, 1f)

                if (heldMs >= 700L && canPerformAction) {
                    triggeredAction = GestureAction.Back
                    lastActionTime = nowMs
                    fistStartTime = 0L
                    currentStatus = "Back Triggered"
                } else {
                    currentStatus = "Holding Fist (Back...)"
                }
            } else {
                fistStartTime = 0L
                fistProgress = 0f

                // GESTURE 3: Pinch (thumb 4 to index 8 < threshold with 150ms debounce) = TAP
                if (isPinching && settings.pinchTapEnabled) {
                    val heldPinchMs = nowMs - pinchStartTime
                    if (heldPinchMs >= 150L && !tapTriggeredForCurrentPinch && canPerformAction) {
                        triggeredAction = GestureAction.Tap(smoothCursorX, smoothCursorY)
                        tapTriggeredForCurrentPinch = true
                        lastActionTime = nowMs
                        currentStatus = "Tap Triggered"
                    } else if (!tapTriggeredForCurrentPinch) {
                        currentStatus = "Pinching..."
                    }
                }

                // GESTURE 4: Open palm moving fast up/down = SCROLL (velocity-based swipe)
                if (triggeredAction == null && isOpenPalm && settings.swipeScrollEnabled && canPerformAction) {
                    val oldest = positionHistory.firstOrNull()
                    if (oldest != null) {
                        val dtMs = nowMs - oldest.first
                        if (dtMs in 100L..250L) {
                            val dy = rawPalmY - oldest.second.second
                            val dtSec = dtMs / 1000f
                            val velocityY = dy / dtSec // Normalized units per second

                            // If moving fast UP (dy < 0), user swipes up -> scrolls down
                            // If moving fast DOWN (dy > 0), user swipes down -> scrolls up
                            val minSwipeVelocity = 1.1f
                            val minDisplacement = 0.12f

                            if (dy < -minDisplacement && velocityY < -minSwipeVelocity) {
                                // Swipe up gesture: swipe from bottom towards top
                                val startX = screenWidth * 0.5f
                                val startY = screenHeight * 0.75f
                                val endY = screenHeight * 0.25f
                                triggeredAction = GestureAction.Scroll(startX, startY, startX, endY)
                                lastActionTime = nowMs
                                currentStatus = "Scroll Down (Swipe Up)"
                                positionHistory.clear()
                            } else if (dy > minDisplacement && velocityY > minSwipeVelocity) {
                                // Swipe down gesture: swipe from top towards bottom
                                val startX = screenWidth * 0.5f
                                val startY = screenHeight * 0.25f
                                val endY = screenHeight * 0.75f
                                triggeredAction = GestureAction.Scroll(startX, startY, startX, endY)
                                lastActionTime = nowMs
                                currentStatus = "Scroll Up (Swipe Down)"
                                positionHistory.clear()
                            }
                        }
                    }
                }
            }
        }

        return EngineState(
            cursorX = smoothCursorX,
            cursorY = smoothCursorY,
            isHandDetected = true,
            isPaused = isPaused,
            isPinching = isPinching,
            isFist = isFist,
            isOpenPalm = isOpenPalm,
            fistProgress = fistProgress,
            pauseProgress = pauseProgress,
            statusText = currentStatus,
            triggeredAction = triggeredAction
        )
    }
}
