package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper

class TouchInjector(private val service: AccessibilityService) {
    private val main = Handler(Looper.getMainLooper())
    private var stroke: GestureDescription.StrokeDescription? = null
    private var curX = 0f
    private var curY = 0f
    private var tgtX = 0f
    private var tgtY = 0f
    private var down = false
    private var busy = false
    private var endRequested = false

    val isDragging: Boolean
        get() = down

    fun tap(x: Float, y: Float) {
        if (down) return
        val p = Path().apply { moveTo(x, y) }
        service.dispatchGesture(
            GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(p, 0, 45)).build(),
            null,
            null
        )
    }

    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long) {
        if (down) return
        val p = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        service.dispatchGesture(
            GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(p, 0, durationMs.coerceAtLeast(60))).build(),
            null,
            null
        )
    }

    fun dragStart(x: Float, y: Float) {
        if (down) return
        curX = x
        curY = y
        tgtX = x
        tgtY = y
        endRequested = false
        val s = GestureDescription.StrokeDescription(Path().apply { moveTo(x, y) }, 0, 40, true)
        stroke = s
        down = true
        busy = true
        dispatch(s)
    }

    fun dragMove(x: Float, y: Float) {
        tgtX = x
        tgtY = y
    }

    fun dragEnd() {
        if (down) endRequested = true
    }

    private fun dispatch(s: GestureDescription.StrokeDescription) {
        val ok = service.dispatchGesture(
            GestureDescription.Builder().addStroke(s).build(),
            object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    busy = false
                    pump()
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    reset()
                }
            },
            main
        )
        if (!ok) reset()
    }

    private fun pump() {
        val prev = stroke ?: return
        if (!down) return
        val moved = (tgtX - curX) * (tgtX - curX) + (tgtY - curY) * (tgtY - curY) >= 1f
        val p = Path().apply {
            moveTo(curX, curY)
            if (moved) lineTo(tgtX, tgtY)
        }
        val last = endRequested
        val next = prev.continueStroke(p, 0, 40, !last)
        if (moved) {
            curX = tgtX
            curY = tgtY
        }
        if (last) {
            down = false
            stroke = null
        } else {
            stroke = next
        }
        busy = true
        dispatch(next)
    }

    private fun reset() {
        down = false
        busy = false
        stroke = null
        endRequested = false
    }
}
