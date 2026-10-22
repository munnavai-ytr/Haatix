package com.example.engine

import com.example.data.HaatixSettings
import com.example.overlay.CursorMode
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class GestureEngine(private val sink: ActionSink) {

    var isPaused: Boolean = false
        private set

    // One Euro Filters for X and Y
    private val filterX = OneEuroFilter()
    private val filterY = OneEuroFilter()

    // Screen dimensions
    var screenWidth: Int = 1080
    var screenHeight: Int = 2400

    // Current output state
    var cursorX: Float = 540f
        private set
    var cursorY: Float = 1200f
        private set
    var mode: CursorMode = CursorMode.NO_HAND
        private set
    var fistProgress: Float = 0f
        private set
    var dwellProgress: Float = 0f
        private set
    var scrollVelocityNorm: Float = 0f
        private set
    var scrollIsHorizontal: Boolean = false
        private set
    var hasHand: Boolean = false
        private set
    var statusLabel: String = "No Hand"
        private set
    var hintLabel: String = ""
        private set

    // Cursor position ring buffer (for anchor lookups during pinch & flick)
    // 40 entries: timeMs, xPx, yPx
    private val bufSize = 40
    private val cBufTime = LongArray(bufSize)
    private val cBufX = FloatArray(bufSize)
    private val cBufY = FloatArray(bufSize)
    private var cBufIdx = 0
    private var cBufCount = 0

    // Flick palm ring buffer: timeMs, normX, normY
    private val fBufTime = LongArray(bufSize)
    private val fBufX = FloatArray(bufSize)
    private val fBufY = FloatArray(bufSize)
    private var fBufIdx = 0
    private var fBufCount = 0

    // Pinch & Tap/Drag State Machine
    private var isFrozen = false
    private var anchorX = 0f
    private var anchorY = 0f
    private var prevPinchRatio = 1.0f
    private var closedConsecutiveFrames = 0
    private var closedStartMs = 0L
    private var isDragging = false
    private var dragStartCursorX = 0f
    private var dragStartCursorY = 0f
    private var lastTapTimeMs = 0L

    // Open Palm & Flick State
    private var openPalmStartMs = 0L
    private var lastFlickTimeMs = 0L
    private var lastFlickSpeed = 0f
    private var lastFlickDirSign = 0 // +1 or -1

    // V-Sign Continuous Scroll State
    private var vSignStartMs = 0L
    private var inVScrollMode = false
    private var vAnchorNormX = 0f
    private var vAnchorNormY = 0f
    private var vVirtualX = 0f
    private var vVirtualY = 0f
    private var vLockedAxis = 0 // 0 = none, 1 = vertical, 2 = horizontal
    private var lastFrameTimeMs = 0L

    // Fist State (Back / Pause)
    private var fistStartMs = 0L
    private var fistTriggeredPause = false

    // Dwell Click State
    private var dwellStartMs = 0L
    private var dwellAnchorX = 0f
    private var dwellAnchorY = 0f

    // Hand Presence & Grace Period
    private var lastHandSeenMs = 0L
    private val handLostGraceMs = 200L

    fun setPauseState(paused: Boolean) {
        isPaused = paused
        if (paused) {
            cancelActiveInteractions()
        }
    }

    fun reset() {
        filterX.reset()
        filterY.reset()
        isFrozen = false
        isDragging = false
        inVScrollMode = false
        closedConsecutiveFrames = 0
        closedStartMs = 0L
        openPalmStartMs = 0L
        vSignStartMs = 0L
        fistStartMs = 0L
        fistTriggeredPause = false
        dwellStartMs = 0L
        cBufCount = 0
        fBufCount = 0
        isPaused = false
    }

    private fun cancelActiveInteractions() {
        if (isDragging) {
            sink.dragEnd()
            isDragging = false
        }
        if (inVScrollMode) {
            sink.dragEnd()
            inVScrollMode = false
        }
        isFrozen = false
        fistStartMs = 0L
        dwellStartMs = 0L
    }

    /**
     * Hot-path per-frame processing.
     * Takes landmarks array: FloatArray(63) representing 21 landmarks (x, y, z).
     * If hand is not detected, landmarks can be null or count < 63.
     */
    fun processFrame(
        landmarks: FloatArray?,
        hasLandmarks: Boolean,
        settings: HaatixSettings,
        nowMs: Long
    ) {
        val dtSec = if (lastFrameTimeMs > 0L) {
            ((nowMs - lastFrameTimeMs) / 1000f).coerceIn(0.005f, 0.1f)
        } else 0.033f
        lastFrameTimeMs = nowMs

        if (!hasLandmarks || landmarks == null || landmarks.size < 63) {
            // Check grace period
            if (nowMs - lastHandSeenMs > handLostGraceMs) {
                hasHand = false
                cancelActiveInteractions()
                mode = if (isPaused) CursorMode.PAUSED else CursorMode.NO_HAND
                statusLabel = if (isPaused) "Paused" else "No Hand"
                hintLabel = ""
                fistProgress = 0f
                dwellProgress = 0f
                scrollVelocityNorm = 0f
            }
            return
        }

        hasHand = true
        lastHandSeenMs = nowMs

        // 1. Calculate Poses and Distances
        // Wrist is index 0 (indices: x=0, y=1, z=2)
        val wX = landmarks[0]
        val wY = landmarks[1]

        // Palm size: distance between wrist (0) and middle MCP (9)
        val m9X = landmarks[27]
        val m9Y = landmarks[28]
        val palmSize = max(hypot(m9X - wX, m9Y - wY), 0.04f)

        // Check finger extensions (tip vs PIP from wrist)
        // Index: 8 vs 6 (8: 24,25; 6: 18,19)
        val indexExt = hypot(landmarks[24] - wX, landmarks[25] - wY) >
                hypot(landmarks[18] - wX, landmarks[19] - wY) * 1.1f
        // Middle: 12 vs 10 (12: 36,37; 10: 30,31)
        val midExt = hypot(landmarks[36] - wX, landmarks[37] - wY) >
                hypot(landmarks[30] - wX, landmarks[31] - wY) * 1.1f
        // Ring: 16 vs 14 (16: 48,49; 14: 42,43)
        val ringExt = hypot(landmarks[48] - wX, landmarks[49] - wY) >
                hypot(landmarks[42] - wX, landmarks[43] - wY) * 1.1f
        // Pinky: 20 vs 18 (20: 60,61; 18: 54,55)
        val pinkyExt = hypot(landmarks[60] - wX, landmarks[61] - wY) >
                hypot(landmarks[54] - wX, landmarks[55] - wY) * 1.1f

        var extendedCount = 0
        if (indexExt) extendedCount++
        if (midExt) extendedCount++
        if (ringExt) extendedCount++
        if (pinkyExt) extendedCount++

        // Pinch Ratio: thumb tip (4: 12,13) to index tip (8: 24,25)
        val pinchDist = hypot(landmarks[12] - landmarks[24], landmarks[13] - landmarks[25])
        val pinchRatio = pinchDist / palmSize

        // Pose classification
        val isPinchPrioritized = pinchRatio < 0.45f
        val isFist = (extendedCount == 0 && pinchRatio > 0.50f && !isPinchPrioritized)
        val isVSign = (indexExt && midExt && !ringExt && !pinkyExt)
        val isOpenPalm = (extendedCount >= 3)

        // Hand distance & frame hints (Bengali + English)
        hintLabel = when {
            palmSize < 0.07f -> "Hand ektu kache anun / Bring hand closer"
            palmSize > 0.32f -> "Hand ektu pichone / Move hand back"
            isLandmarkNearEdge(landmarks) -> "Pura hat camera-y rakhun / Keep full hand in view"
            else -> ""
        }

        // 2. FIST Gestures: Back & Pause/Resume
        // Fist hold works even if paused (to unpause)
        if (isFist && settings.fistBackEnabled) {
            if (fistStartMs == 0L) {
                fistStartMs = nowMs
                fistTriggeredPause = false
            }
            val heldDuration = nowMs - fistStartMs
            fistProgress = (heldDuration / 1500f).coerceIn(0f, 1f)

            if (heldDuration >= 1500L && !fistTriggeredPause && settings.pauseEnabled) {
                // Toggle pause
                isPaused = !isPaused
                fistTriggeredPause = true
                sink.pauseChanged(isPaused)
                sink.haptic(40)
            }
        } else {
            // Fist released
            if (fistStartMs > 0L) {
                val held = nowMs - fistStartMs
                fistStartMs = 0L
                fistProgress = 0f
                if (!isPaused && held in 450L..1400L && !fistTriggeredPause) {
                    sink.back()
                    sink.haptic(25)
                }
            }
            fistProgress = 0f
        }

        // If paused, only the Fist 1.5s gesture can be processed
        if (isPaused) {
            mode = CursorMode.PAUSED
            statusLabel = "Paused (Hold Fist 1.5s to Resume)"
            return
        }

        // 3. Pointer calculation (Palm Center: landmarks 0, 5, 9, 13, 17)
        val rawPalmX = (landmarks[0] + landmarks[15] + landmarks[27] + landmarks[39] + landmarks[51]) / 5f
        val rawPalmY = (landmarks[1] + landmarks[16] + landmarks[28] + landmarks[40] + landmarks[52]) / 5f

        // Mirror X for front camera
        val mirrorX = 1f - rawPalmX
        val margin = 0.14f
        val normX = ((mirrorX - margin) / (1f - 2f * margin)).coerceIn(0f, 1f)
        val normY = ((rawPalmY - margin) / (1f - 2f * margin)).coerceIn(0f, 1f)

        // Speed gain around center
        val speed = settings.cursorSpeed
        val gainX = ((normX - 0.5f) * speed + 0.5f).coerceIn(0f, 1f)
        val gainY = ((normY - 0.5f) * speed + 0.5f).coerceIn(0f, 1f)

        // One Euro Filter (Smoothness setting maps 0..1 to minCutoff 3.0 .. 0.8)
        val minCutoff = lerp(3.0f, 0.8f, settings.smoothness.coerceIn(0f, 1f))
        filterX.minCutoff = minCutoff
        filterY.minCutoff = minCutoff

        val filteredX = filterX.filter(gainX, nowMs)
        val filteredY = filterY.filter(gainY, nowMs)

        val curPixelX = filteredX * screenWidth
        val curPixelY = filteredY * screenHeight

        // Push to cursor ring buffer
        pushCursorBuffer(nowMs, curPixelX, curPixelY)

        // 4. V-SIGN Continuous Scroll Mode
        if (isVSign && settings.vScrollEnabled) {
            if (vSignStartMs == 0L) {
                vSignStartMs = nowMs
            } else if (!inVScrollMode && nowMs - vSignStartMs >= 150L) {
                // Engage continuous scroll mode
                inVScrollMode = true
                vAnchorNormX = filteredX
                vAnchorNormY = filteredY
                vVirtualX = screenWidth * 0.5f
                vVirtualY = screenHeight * 0.58f
                vLockedAxis = 0
                sink.dragStart(vVirtualX + settings.offsetX, vVirtualY + settings.offsetY)
            }

            if (inVScrollMode) {
                mode = CursorMode.SCROLL
                statusLabel = "Two-Finger Scroll"

                val ox = filteredX - vAnchorNormX
                val oy = filteredY - vAnchorNormY
                val deadZone = 0.035f

                // Determine dominant axis
                if (vLockedAxis == 0) {
                    if (abs(oy) > deadZone && abs(oy) > abs(ox) * 1.5f) {
                        vLockedAxis = 1 // Vertical
                    } else if (abs(ox) > deadZone && abs(ox) > abs(oy) * 1.5f) {
                        vLockedAxis = 2 // Horizontal
                    }
                } else {
                    // Reset lock if hand returns inside deadzone
                    if (hypot(ox, oy) < deadZone) {
                        vLockedAxis = 0
                    }
                }

                scrollIsHorizontal = (vLockedAxis == 2)
                var vRate = 0f

                if (vLockedAxis == 1) { // Vertical
                    val mag = abs(oy)
                    if (mag > deadZone) {
                        val t = ((mag - deadZone) / (0.28f - deadZone)).coerceIn(0f, 1f)
                        val vMax = 2.2f * screenHeight * settings.scrollSpeed
                        val speedMag = vMax * t.pow(1.6f)
                        val sign = if (oy < 0) -1f else 1f // Hand up = negative oy
                        val dirSign = if (settings.reverseScroll) -sign else sign
                        // Hand moves UP -> virtual finger moves UP (content moves up)
                        vRate = dirSign * speedMag
                        scrollVelocityNorm = (sign * t).coerceIn(-1f, 1f)
                    } else {
                        scrollVelocityNorm = 0f
                    }

                    vVirtualY += vRate * dtSec

                    // Ratchet lane [0.30, 0.85] of screen height
                    val laneTop = screenHeight * 0.30f
                    val laneBottom = screenHeight * 0.85f
                    if (vVirtualY < laneTop || vVirtualY > laneBottom) {
                        sink.dragEnd()
                        vVirtualY = screenHeight * 0.58f
                        sink.dragStart(vVirtualX + settings.offsetX, vVirtualY + settings.offsetY)
                    } else {
                        sink.dragMove(vVirtualX + settings.offsetX, vVirtualY + settings.offsetY)
                    }

                } else if (vLockedAxis == 2) { // Horizontal
                    val mag = abs(ox)
                    if (mag > deadZone) {
                        val t = ((mag - deadZone) / (0.28f - deadZone)).coerceIn(0f, 1f)
                        val vMax = 2.2f * screenWidth * settings.scrollSpeed
                        val speedMag = vMax * t.pow(1.6f)
                        val sign = if (ox < 0) -1f else 1f
                        val dirSign = if (settings.reverseScroll) -sign else sign
                        vRate = dirSign * speedMag
                        scrollVelocityNorm = (sign * t).coerceIn(-1f, 1f)
                    } else {
                        scrollVelocityNorm = 0f
                    }

                    vVirtualX += vRate * dtSec

                    // Ratchet lane [0.20, 0.80] of screen width
                    val laneLeft = screenWidth * 0.20f
                    val laneRight = screenWidth * 0.80f
                    if (vVirtualX < laneLeft || vVirtualX > laneRight) {
                        sink.dragEnd()
                        vVirtualX = screenWidth * 0.50f
                        sink.dragStart(vVirtualX + settings.offsetX, vVirtualY + settings.offsetY)
                    } else {
                        sink.dragMove(vVirtualX + settings.offsetX, vVirtualY + settings.offsetY)
                    }
                } else {
                    scrollVelocityNorm = 0f
                }

                // Keep cursor at anchor while in scroll mode
                cursorX = curPixelX
                cursorY = curPixelY
                return
            }
        } else {
            if (inVScrollMode) {
                sink.dragEnd()
                inVScrollMode = false
                vLockedAxis = 0
            }
            vSignStartMs = 0L
            scrollVelocityNorm = 0f
        }

        // 5. Open Palm Wave (FLICK)
        if (isOpenPalm && settings.flickScrollEnabled) {
            pushFlickBuffer(nowMs, filteredX, filteredY)
            if (openPalmStartMs == 0L) {
                openPalmStartMs = nowMs
            } else if (nowMs - openPalmStartMs >= 120L) {
                // Check for flick in recent history
                val flickSample = getOldestFlickSample(nowMs, 80L, 350L)
                if (flickSample != null) {
                    val dt = (nowMs - flickSample.timeMs) / 1000f
                    if (dt > 0.05f) {
                        val dx = filteredX - flickSample.x
                        val dy = filteredY - flickSample.y
                        val disp = hypot(dx, dy)
                        val flickSpeed = disp / dt

                        val s = settings.scrollSensitivity
                        val minDisp = 0.08f / s
                        val minSpeed = 0.7f * (1f / s)

                        if (disp >= minDisp && flickSpeed >= minSpeed) {
                            // Determine axis
                            val isVertical = abs(dy) > 1.4f * abs(dx)
                            val isHorizontal = abs(dx) > 1.4f * abs(dy)

                            if (isVertical || isHorizontal) {
                                val dirSign = if (isVertical) (if (dy < 0) -1 else 1) else (if (dx < 0) -1 else 1)
                                val isOpposite = (dirSign != lastFlickDirSign && (nowMs - lastFlickTimeMs < 600L))

                                // Return-stroke filter
                                val acceptFlick = if (isOpposite) {
                                    (flickSpeed >= lastFlickSpeed && disp >= 1.3f * minDisp)
                                } else {
                                    (nowMs - lastFlickTimeMs >= 350L)
                                }

                                if (acceptFlick) {
                                    // Lookup anchor from start of flick window
                                    val startAnchor = getCursorSampleAt(flickSample.timeMs)
                                    val aX = startAnchor?.first ?: curPixelX
                                    val aY = startAnchor?.second ?: curPixelY

                                    dispatchFlick(isVertical, dirSign, aX, aY, flickSpeed, settings)
                                    lastFlickTimeMs = nowMs
                                    lastFlickSpeed = flickSpeed
                                    lastFlickDirSign = dirSign
                                    fBufCount = 0 // Clear buffer after flick
                                }
                            }
                        }
                    }
                }
            }
        } else {
            openPalmStartMs = 0L
        }

        // 6. ACCURATE PINCH / TAP / DRAG STATE MACHINE
        if (settings.pinchClickEnabled) {
            val isFalling = pinchRatio < prevPinchRatio

            // APPROACH: pinchRatio < 0.80 and falling -> freeze cursor at anchor from 120ms ago
            if (!isFrozen && pinchRatio < 0.80f && isFalling) {
                isFrozen = true
                val pastAnchor = getCursorSampleAt(nowMs - 120L)
                anchorX = pastAnchor?.first ?: curPixelX
                anchorY = pastAnchor?.second ?: curPixelY
            } else if (isFrozen && !isDragging && closedStartMs == 0L && pinchRatio > 0.95f) {
                // False alarm unfreeze
                isFrozen = false
            }

            // CLOSED: pinchRatio < 0.40 for 2 consecutive frames
            if (pinchRatio < 0.40f) {
                closedConsecutiveFrames++
                if (closedConsecutiveFrames == 2 && closedStartMs == 0L) {
                    closedStartMs = nowMs
                }
            } else {
                closedConsecutiveFrames = 0
            }

            // HOLD: closed for >= 350ms -> start real drag
            if (closedStartMs > 0L && (nowMs - closedStartMs >= 350L) && !isDragging) {
                isDragging = true
                dragStartCursorX = curPixelX
                dragStartCursorY = curPixelY
                sink.dragStart(anchorX + settings.offsetX, anchorY + settings.offsetY)
                sink.haptic(20)
            }

            // WHILE DRAGGING: move virtual finger
            if (isDragging) {
                val dragX = (anchorX + (curPixelX - dragStartCursorX)).coerceIn(0f, screenWidth.toFloat())
                val dragY = (anchorY + (curPixelY - dragStartCursorY)).coerceIn(0f, screenHeight.toFloat())
                sink.dragMove(dragX + settings.offsetX, dragY + settings.offsetY)
            }

            // RELEASE: pinch opens back up (> 0.55)
            if (pinchRatio > 0.55f && isFrozen) {
                val closedDuration = if (closedStartMs > 0L) (nowMs - closedStartMs) else 0L

                if (isDragging) {
                    sink.dragEnd()
                    isDragging = false
                } else if (closedDuration in 40L..350L && (nowMs - lastTapTimeMs >= 200L)) {
                    // Tap on release at locked anchor position!
                    sink.tap(anchorX + settings.offsetX, anchorY + settings.offsetY)
                    sink.haptic(12)
                    lastTapTimeMs = nowMs
                }

                isFrozen = false
                closedStartMs = 0L
                closedConsecutiveFrames = 0
            }

            prevPinchRatio = pinchRatio
        }

        // 7. Optional Dwell Click (1.0s still in MOVE mode)
        if (settings.dwellClick && !isFrozen && !isDragging && !inVScrollMode) {
            val dwellDist = hypot(curPixelX - dwellAnchorX, curPixelY - dwellAnchorY)
            val maxDwellRadius = screenWidth * 0.03f // 3% of screen width

            if (dwellStartMs == 0L || dwellDist > maxDwellRadius) {
                dwellStartMs = nowMs
                dwellAnchorX = curPixelX
                dwellAnchorY = curPixelY
                dwellProgress = 0f
            } else {
                val dwellHeld = nowMs - dwellStartMs
                dwellProgress = (dwellHeld / 1000f).coerceIn(0f, 1f)
                if (dwellHeld >= 1000L && (nowMs - lastTapTimeMs >= 200L)) {
                    sink.tap(curPixelX + settings.offsetX, curPixelY + settings.offsetY)
                    sink.haptic(12)
                    lastTapTimeMs = nowMs
                    dwellStartMs = 0L
                    dwellProgress = 0f
                }
            }
        } else {
            dwellStartMs = 0L
            dwellProgress = 0f
        }

        // 8. Update Current Visual Cursor Position and Mode
        if (isDragging) {
            cursorX = (anchorX + (curPixelX - dragStartCursorX)).coerceIn(0f, screenWidth.toFloat())
            cursorY = (anchorY + (curPixelY - dragStartCursorY)).coerceIn(0f, screenHeight.toFloat())
            mode = CursorMode.DRAGGING
            statusLabel = "Dragging"
        } else if (isFrozen) {
            cursorX = anchorX
            cursorY = anchorY
            mode = CursorMode.PINCH_READY
            statusLabel = "Click Ready"
        } else {
            cursorX = curPixelX
            cursorY = curPixelY
            mode = CursorMode.MOVE
            statusLabel = "Move"
        }
    }

    private fun dispatchFlick(
        isVertical: Boolean,
        dirSign: Int,
        anchorX: Float,
        anchorY: Float,
        flickSpeed: Float,
        settings: HaatixSettings
    ) {
        val speedFactor = (flickSpeed / 1.2f).coerceIn(0.7f, 1.4f) * settings.scrollSpeed
        val effectiveDir = if (settings.reverseScroll) -dirSign else dirSign

        if (isVertical) {
            val x = anchorX.coerceIn(0.2f * screenWidth, 0.8f * screenWidth)
            val length = 0.44f * screenHeight * speedFactor
            val halfL = length / 2f
            val midY = screenHeight * 0.5f

            // Hand moves UP (dirSign = -1) -> swipe from bottom to top (advances feed)
            val (startY, endY) = if (effectiveDir < 0) {
                (midY + halfL) to (midY - halfL)
            } else {
                (midY - halfL) to (midY + halfL)
            }
            sink.swipe(
                x + settings.offsetX, startY + settings.offsetY,
                x + settings.offsetX, endY + settings.offsetY,
                140L
            )
        } else {
            val y = anchorY.coerceIn(0.2f * screenHeight, 0.8f * screenHeight)
            val length = 0.55f * screenWidth * speedFactor
            val halfL = length / 2f
            val midX = screenWidth * 0.5f

            // Hand moves LEFT (dirSign = -1) -> swipe right to left (next page)
            val (startX, endX) = if (effectiveDir < 0) {
                (midX + halfL) to (midX - halfL)
            } else {
                (midX - halfL) to (midX + halfL)
            }
            sink.swipe(
                startX + settings.offsetX, y + settings.offsetY,
                endX + settings.offsetX, y + settings.offsetY,
                140L
            )
        }
    }

    private fun pushCursorBuffer(timeMs: Long, x: Float, y: Float) {
        cBufTime[cBufIdx] = timeMs
        cBufX[cBufIdx] = x
        cBufY[cBufIdx] = y
        cBufIdx = (cBufIdx + 1) % bufSize
        if (cBufCount < bufSize) cBufCount++
    }

    private fun getCursorSampleAt(targetTimeMs: Long): Pair<Float, Float>? {
        if (cBufCount == 0) return null
        var bestIdx = -1
        var minDiff = Long.MAX_VALUE

        for (i in 0 until cBufCount) {
            val diff = abs(cBufTime[i] - targetTimeMs)
            if (diff < minDiff) {
                minDiff = diff
                bestIdx = i
            }
        }
        return if (bestIdx != -1) cBufX[bestIdx] to cBufY[bestIdx] else null
    }

    private fun pushFlickBuffer(timeMs: Long, x: Float, y: Float) {
        fBufTime[fBufIdx] = timeMs
        fBufX[fBufIdx] = x
        fBufY[fBufIdx] = y
        fBufIdx = (fBufIdx + 1) % bufSize
        if (fBufCount < bufSize) fBufCount++
    }

    private data class Sample(val timeMs: Long, val x: Float, val y: Float)

    private fun getOldestFlickSample(nowMs: Long, minAgeMs: Long, maxAgeMs: Long): Sample? {
        if (fBufCount == 0) return null
        var oldestIdx = -1
        var oldestTime = Long.MAX_VALUE

        for (i in 0 until fBufCount) {
            val age = nowMs - fBufTime[i]
            if (age in minAgeMs..maxAgeMs) {
                if (fBufTime[i] < oldestTime) {
                    oldestTime = fBufTime[i]
                    oldestIdx = i
                }
            }
        }
        return if (oldestIdx != -1) Sample(fBufTime[oldestIdx], fBufX[oldestIdx], fBufY[oldestIdx]) else null
    }

    private fun isLandmarkNearEdge(landmarks: FloatArray): Boolean {
        // Check wrist (0), thumb tip (4), index tip (8), middle tip (12), pinky tip (20)
        val checkIndices = intArrayOf(0, 4, 8, 12, 20)
        for (idx in checkIndices) {
            val x = landmarks[idx * 3]
            val y = landmarks[idx * 3 + 1]
            if (x < 0.02f || x > 0.98f || y < 0.02f || y > 0.98f) return true
        }
        return false
    }

    private fun lerp(start: Float, stop: Float, fraction: Float): Float {
        return start + (stop - start) * fraction
    }
}
