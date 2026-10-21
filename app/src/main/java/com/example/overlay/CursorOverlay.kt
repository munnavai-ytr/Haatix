package com.example.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import com.example.engine.EngineState

class CursorOverlay(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var cursorView: CursorView? = null
    private var keepScreenOnView: View? = null

    val screenWidth: Int
        get() = context.resources.displayMetrics.widthPixels

    val screenHeight: Int
        get() = context.resources.displayMetrics.heightPixels

    fun show() {
        if (!Settings.canDrawOverlays(context)) {
            Log.w(TAG, "Cannot show overlay: SYSTEM_ALERT_WINDOW permission missing")
            return
        }

        if (cursorView == null) {
            cursorView = CursorView(context)
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )
            try {
                windowManager.addView(cursorView, params)
                Log.i(TAG, "Cursor overlay added to WindowManager")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add cursor view to WindowManager", e)
            }
        }
    }

    fun updateState(state: EngineState) {
        cursorView?.updateState(state)
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
            )
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

    private var currentState = EngineState()

    // Paints for Iron-Man hologram style cyan glowing cursor
    private val outerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
        color = Color.parseColor("#00E5FF")
    }

    private val outerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        color = Color.parseColor("#4400E5FF")
    }

    private val innerCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#00E5FF")
    }

    private val innerCoreGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#6600E5FF")
    }

    private val reticlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#8000E5FF")
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
    }

    private val badgeBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#DD08121E")
    }

    private val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Color.parseColor("#6600E5FF")
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0F7FA")
        textSize = 28f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val badgeRect = RectF()
    private val progressRect = RectF()

    fun updateState(state: EngineState) {
        this.currentState = state
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val x = currentState.cursorX
        val y = currentState.cursorY

        // If hand is not detected and not initialized, don't draw off-screen
        if (x <= 0f && y <= 0f && !currentState.isHandDetected) {
            return
        }

        val baseCyan = if (currentState.isPaused) Color.parseColor("#FF9100") else Color.parseColor("#00E5FF")
        val glowCyan = if (currentState.isPaused) Color.parseColor("#44FF9100") else Color.parseColor("#4400E5FF")

        outerRingPaint.color = baseCyan
        outerGlowPaint.color = glowCyan
        innerCorePaint.color = if (currentState.isPinching) Color.WHITE else baseCyan
        reticlePaint.color = if (currentState.isPaused) Color.parseColor("#80FF9100") else Color.parseColor("#8000E5FF")
        badgeBorderPaint.color = if (currentState.isPaused) Color.parseColor("#80FF9100") else Color.parseColor("#6600E5FF")

        val baseRadius = if (currentState.isPinching) 20f else 32f

        // Draw outer glow and ring
        canvas.drawCircle(x, y, baseRadius + 4f, outerGlowPaint)
        canvas.drawCircle(x, y, baseRadius, outerRingPaint)

        // Draw reticle crosshair ticks (Iron-Man hologram aesthetic)
        val tickLen = 8f
        val gap = baseRadius + 3f
        canvas.drawLine(x - gap - tickLen, y, x - gap, y, reticlePaint)
        canvas.drawLine(x + gap, y, x + gap + tickLen, y, reticlePaint)
        canvas.drawLine(x, y - gap - tickLen, x, y - gap, reticlePaint)
        canvas.drawLine(x, y + gap, x, y + gap + tickLen, reticlePaint)

        // Draw center core
        canvas.drawCircle(x, y, 9f, innerCoreGlowPaint)
        canvas.drawCircle(x, y, 4.5f, innerCorePaint)

        // Draw Action Progress Rings if user is holding fist or holding still
        if (currentState.fistProgress > 0f) {
            progressPaint.color = Color.parseColor("#FF5252") // Red/Orange for Back action
            progressRect.set(x - baseRadius - 10f, y - baseRadius - 10f, x + baseRadius + 10f, y + baseRadius + 10f)
            canvas.drawArc(progressRect, -90f, currentState.fistProgress * 360f, false, progressPaint)
        } else if (currentState.pauseProgress > 0f) {
            progressPaint.color = Color.parseColor("#FFD600") // Yellow for Pause toggle
            progressRect.set(x - baseRadius - 10f, y - baseRadius - 10f, x + baseRadius + 10f, y + baseRadius + 10f)
            canvas.drawArc(progressRect, -90f, currentState.pauseProgress * 360f, false, progressPaint)
        }

        // Draw status badge below cursor
        val status = when {
            !currentState.isHandDetected -> "No Hand"
            currentState.isPaused -> "Paused"
            currentState.isPinching -> "Pinch (Tap)"
            currentState.isFist -> "Fist (Back)"
            currentState.isOpenPalm -> "Open Palm"
            else -> "Active"
        }

        val textWidth = textPaint.measureText(status)
        val badgePaddingHorizontal = 18f
        val badgeHeight = 44f
        val badgeTop = y + baseRadius + 22f
        val badgeBottom = badgeTop + badgeHeight
        val badgeLeft = (x - (textWidth / 2f) - badgePaddingHorizontal).coerceAtLeast(16f)
        val badgeRight = (x + (textWidth / 2f) + badgePaddingHorizontal).coerceAtMost(width.toFloat() - 16f)

        badgeRect.set(badgeLeft, badgeTop, badgeRight, badgeBottom)
        canvas.drawRoundRect(badgeRect, 22f, 22f, badgeBackgroundPaint)
        canvas.drawRoundRect(badgeRect, 22f, 22f, badgeBorderPaint)

        val textY = badgeTop + (badgeHeight / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(status, badgeRect.centerX(), textY, textPaint)
    }
}
