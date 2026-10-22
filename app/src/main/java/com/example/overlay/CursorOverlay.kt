package com.example.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.example.data.HaatixSettings
import kotlin.math.max
import kotlin.math.min

enum class CursorMode {
    MOVE,
    PINCH_READY,
    DRAGGING,
    SCROLL,
    PAUSED,
    NO_HAND
}

class CursorOverlay(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var cursorView: CursorView? = null
    private var keepScreenOnView: View? = null

    val realSize: android.util.Size
        get() = ScreenMetrics.realSize(context)

    fun show() {
        if (!Settings.canDrawOverlays(context)) {
            Log.w(TAG, "Cannot show overlay: SYSTEM_ALERT_WINDOW permission missing")
            return
        }

        if (cursorView == null) {
            cursorView = CursorView(context)
            val s = ScreenMetrics.realSize(context)
            val params = WindowManager.LayoutParams(
                s.width,
                s.height,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
            try {
                windowManager.addView(cursorView, params)
                Log.i(TAG, "Cursor overlay added: ${s.width}x${s.height}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add cursor view to WindowManager", e)
            }
        }
    }

    fun update(
        cursorX: Float,
        cursorY: Float,
        mode: CursorMode,
        fistProgress: Float,
        dwellProgress: Float,
        scrollVelocityNorm: Float,
        scrollIsHorizontal: Boolean,
        hasHand: Boolean,
        statusLabel: String,
        hintLabel: String,
        settings: HaatixSettings,
        nowMs: Long
    ) {
        cursorView?.update(
            cursorX, cursorY, mode, fistProgress, dwellProgress,
            scrollVelocityNorm, scrollIsHorizontal, hasHand,
            statusLabel, hintLabel, settings, nowMs
        )
    }

    fun triggerClickRipple(nowMs: Long) {
        cursorView?.triggerRipple(nowMs)
    }

    fun setKeepScreenOn(enabled: Boolean) {
        if (!Settings.canDrawOverlays(context)) return

        if (enabled && keepScreenOnView == null) {
            val view = View(context).apply {
                keepScreenOn = true
                alpha = 0f
            }
            val params = WindowManager.LayoutParams(
                1,
                1,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
            }
            try {
                windowManager.addView(view, params)
                keepScreenOnView = view
                Log.i(TAG, "KeepScreenOn overlay enabled")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add keepScreenOnView", e)
            }
        } else if (!enabled && keepScreenOnView != null) {
            try {
                windowManager.removeView(keepScreenOnView)
                keepScreenOnView = null
                Log.i(TAG, "KeepScreenOn overlay removed")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove keepScreenOnView", e)
            }
        }
    }

    fun hide() {
        cursorView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove cursorView", e)
            }
            cursorView = null
        }
        keepScreenOnView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove keepScreenOnView", e)
            }
            keepScreenOnView = null
        }
    }

    companion object {
        private const val TAG = "CursorOverlay"
    }
}

class CursorView(context: Context) : View(context) {

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    // State
    private var curX = 0f
    private var curY = 0f
    private var mode = CursorMode.NO_HAND
    private var prevMode = CursorMode.NO_HAND
    private var fistProg = 0f
    private var dwellProg = 0f
    private var scrollVelNorm = 0f
    private var scrollIsHoriz = false
    private var hasHand = false
    private var lastHandSeenMs = 0L
    private var rippleStartMs = 0L
    private var stateChangeMs = 0L
    private var status = ""
    private var hint = ""
    private var hintStartMs = 0L

    private var offsetX = 0
    private var offsetY = 0

    // Coordinates mapping
    private val loc = IntArray(2)

    // Reusable bounds for dirty rect invalidation
    private var oldL = 0
    private var oldT = 0
    private var oldR = 0
    private var oldB = 0
    private val dirtyRect = Rect()

    // Paints (reused)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4.5f
        strokeCap = Paint.Cap.ROUND
    }
    private val gaugePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
        strokeCap = Paint.Cap.ROUND
    }
    private val chipBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#E6070F1E")
    }
    private val chipBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
        color = Color.parseColor("#6600E5FF")
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F0F6FC")
        textSize = 26f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val hintTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFB703")
        textSize = 24f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val arcRect = RectF()
    private val chipRect = RectF()

    fun update(
        cursorX: Float,
        cursorY: Float,
        newMode: CursorMode,
        fistProgress: Float,
        dwellProgress: Float,
        scrollVelocityNorm: Float,
        scrollIsHorizontal: Boolean,
        handDetected: Boolean,
        statusLabel: String,
        hintLabel: String,
        settings: HaatixSettings,
        nowMs: Long
    ) {
        val modeChanged = newMode != mode
        if (modeChanged) {
            prevMode = mode
            mode = newMode
            stateChangeMs = nowMs
        }

        if (handDetected) {
            lastHandSeenMs = nowMs
        }
        hasHand = handDetected

        if (hintLabel.isNotEmpty() && hintLabel != hint) {
            hint = hintLabel
            hintStartMs = nowMs
        } else if (hintLabel.isEmpty() && nowMs - hintStartMs > 1200L) {
            hint = ""
        }

        curX = cursorX
        curY = cursorY
        fistProg = fistProgress
        dwellProg = dwellProgress
        scrollVelNorm = scrollVelocityNorm
        scrollIsHoriz = scrollIsHorizontal
        status = statusLabel
        offsetX = settings.offsetX
        offsetY = settings.offsetY

        // Compute new bounding box around cursor for dirty rect
        getLocationOnScreen(loc)
        val cx = (curX - loc[0] + offsetX).toInt()
        val cy = (curY - loc[1] + offsetY).toInt()

        val pad = 120 // covers ring (28dp), label chips, glow, and progress arcs
        val newL = max(0, cx - pad)
        val newT = max(0, cy - pad)
        val newR = min(width, cx + pad)
        val newB = min(height, cy + pad + 60)

        if (oldR > 0 && oldB > 0) {
            val unionL = min(oldL, newL)
            val unionT = min(oldT, newT)
            val unionR = max(oldR, newR)
            val unionB = max(oldB, newB)
            postInvalidate(unionL, unionT, unionR, unionB)
        } else {
            postInvalidate(newL, newT, newR, newB)
        }

        oldL = newL
        oldT = newT
        oldR = newR
        oldB = newB
    }

    fun triggerRipple(nowMs: Long) {
        rippleStartMs = nowMs
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        getLocationOnScreen(loc)
        val cx = curX - loc[0] + offsetX
        val cy = curY - loc[1] + offsetY

        val nowMs = SystemClock.uptimeMillis()
        val isHandLostLong = !hasHand && (nowMs - lastHandSeenMs > 1000L)

        // Alpha calculation: 30% if no hand for 1s
        val globalAlpha = if (isHandLostLong) 0.30f else 1.0f

        val baseRadius = when (mode) {
            CursorMode.PINCH_READY -> 20f
            CursorMode.DRAGGING -> 24f
            else -> 28f
        }

        // Color selection based on state
        val (primaryColor, glowColor) = when (mode) {
            CursorMode.PAUSED -> Color.parseColor("#90A4AE") to Color.parseColor("#3390A4AE")
            CursorMode.PINCH_READY -> Color.parseColor("#FFD600") to Color.parseColor("#44FFD600")
            CursorMode.DRAGGING -> Color.parseColor("#00E5FF") to Color.parseColor("#5500E5FF")
            CursorMode.SCROLL -> Color.parseColor("#00F5D4") to Color.parseColor("#4400F5D4")
            else -> Color.parseColor("#00E5FF") to Color.parseColor("#4400E5FF")
        }

        ringPaint.color = applyAlpha(primaryColor, globalAlpha)
        glowPaint.color = applyAlpha(glowColor, globalAlpha)
        fillPaint.color = applyAlpha(primaryColor, globalAlpha)

        // 1. Draw Click Ripple animation if recently tapped
        if (rippleStartMs > 0L) {
            val dt = (nowMs - rippleStartMs).toFloat()
            if (dt < 250f) {
                val ripFrac = dt / 250f
                val ripRadius = baseRadius + (ripFrac * 36f)
                val ripAlpha = (1f - ripFrac) * globalAlpha
                val ripPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 4f * (1f - ripFrac)
                    color = applyAlpha(Color.WHITE, ripAlpha)
                }
                canvas.drawCircle(cx, cy, ripRadius, ripPaint)
                postInvalidate() // continue ripple animation
            } else {
                rippleStartMs = 0L
            }
        }

        // 2. Draw Ring and Glow
        if (mode == CursorMode.DRAGGING) {
            // Solid filled ring for dragging
            canvas.drawCircle(cx, cy, baseRadius, fillPaint)
            canvas.drawCircle(cx, cy, baseRadius + 4f, glowPaint)
        } else {
            canvas.drawCircle(cx, cy, baseRadius + 3f, glowPaint)
            canvas.drawCircle(cx, cy, baseRadius, ringPaint)
            // Center reticle dot
            canvas.drawCircle(cx, cy, 3.5f, fillPaint)
        }

        // 3. Fist Progress Arc (0..1.5s)
        // 0..0.45s: cyan arming
        // 0.45..1.4s: green Back ready
        // 1.5s: amber Pause
        if (fistProg > 0f) {
            val arcColor = when {
                fistProg >= 1.0f -> Color.parseColor("#FFB703") // Pause
                fistProg >= 0.30f -> Color.parseColor("#00F5D4") // Back Ready
                else -> Color.parseColor("#00E5FF")             // Arming
            }
            progressPaint.color = applyAlpha(arcColor, globalAlpha)
            arcRect.set(cx - baseRadius - 8f, cy - baseRadius - 8f, cx + baseRadius + 8f, cy + baseRadius + 8f)
            val sweep = (fistProg * 360f).coerceIn(0f, 360f)
            canvas.drawArc(arcRect, -90f, sweep, false, progressPaint)
        }

        // 4. Dwell Progress Arc if active
        if (dwellProg > 0f) {
            progressPaint.color = applyAlpha(Color.parseColor("#FFD600"), globalAlpha)
            arcRect.set(cx - baseRadius - 8f, cy - baseRadius - 8f, cx + baseRadius + 8f, cy + baseRadius + 8f)
            val sweep = (dwellProg * 360f).coerceIn(0f, 360f)
            canvas.drawArc(arcRect, -90f, sweep, false, progressPaint)
        }

        // 5. Scroll Mode Indicators (Arrows + Velocity Gauge)
        if (mode == CursorMode.SCROLL) {
            gaugePaint.color = applyAlpha(Color.parseColor("#00F5D4"), globalAlpha)
            if (!scrollIsHoriz) {
                // Vertical gauge
                val gaugeLen = scrollVelNorm * 35f
                canvas.drawLine(cx, cy - 14f, cx, cy + 14f, glowPaint)
                canvas.drawLine(cx + baseRadius + 8f, cy, cx + baseRadius + 8f, cy - gaugeLen, gaugePaint)
                // Small indicator tick
                canvas.drawCircle(cx + baseRadius + 8f, cy, 3f, fillPaint)
            } else {
                // Horizontal gauge
                val gaugeLen = scrollVelNorm * 35f
                canvas.drawLine(cx - 14f, cy, cx + 14f, cy, glowPaint)
                canvas.drawLine(cx, cy + baseRadius + 8f, cx + gaugeLen, cy + baseRadius + 8f, gaugePaint)
                canvas.drawCircle(cx, cy + baseRadius + 8f, 3f, fillPaint)
            }
        }

        // 6. State Change Chip (shows only for 1.2s after state change)
        val showStateChip = (nowMs - stateChangeMs < 1200L) && status.isNotEmpty()
        if (showStateChip) {
            drawChip(canvas, cx, cy + baseRadius + 22f, status, textPaint, chipBgPaint, chipBorderPaint, globalAlpha)
        }

        // 7. Hand Hints Chip (shows under ring if triggered)
        val showHint = hint.isNotEmpty() && (nowMs - hintStartMs < 1200L)
        if (showHint) {
            val hintY = if (showStateChip) cy + baseRadius + 60f else cy + baseRadius + 22f
            drawChip(canvas, cx, hintY, hint, hintTextPaint, chipBgPaint, chipBorderPaint, globalAlpha)
        }
    }

    private fun drawChip(
        canvas: Canvas,
        cx: Float,
        topY: Float,
        text: String,
        tPaint: Paint,
        bgPaint: Paint,
        bPaint: Paint,
        alpha: Float
    ) {
        val textWidth = tPaint.measureText(text)
        val padH = 18f
        val chipH = 38f
        val chipW = textWidth + padH * 2f

        val left = (cx - chipW / 2f).coerceAtLeast(16f)
        val right = (left + chipW).coerceAtMost(width.toFloat() - 16f)
        val top = topY
        val bottom = top + chipH

        chipRect.set(left, top, right, bottom)
        canvas.drawRoundRect(chipRect, 18f, 18f, bgPaint)
        canvas.drawRoundRect(chipRect, 18f, 18f, bPaint)

        val fontMetrics = tPaint.fontMetrics
        val baseline = top + (chipH - fontMetrics.bottom - fontMetrics.top) / 2f
        canvas.drawText(text, chipRect.centerX(), baseline, tPaint)
    }

    private fun applyAlpha(color: Int, factor: Float): Int {
        val a = ((color ushr 24) * factor).toInt().coerceIn(0, 255)
        return (color and 0x00FFFFFF) or (a shl 24)
    }
}
