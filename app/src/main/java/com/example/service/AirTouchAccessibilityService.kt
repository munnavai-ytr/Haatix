package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AirTouchAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isConnected.value = true
        Log.i(TAG, "AirTouch Accessibility Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // canRetrieveWindowContent is false. No screen content is inspected.
    }

    override fun onInterrupt() {
        Log.w(TAG, "AirTouch Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
            _isConnected.value = false
        }
        Log.i(TAG, "AirTouch Accessibility Service Destroyed")
    }

    fun performTap(x: Float, y: Float): Boolean {
        return try {
            val path = Path().apply {
                moveTo(x, y)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0, 50)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            dispatchGesture(gesture, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch tap", e)
            false
        }
    }

    fun performSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 280
    ): Boolean {
        return try {
            val path = Path().apply {
                moveTo(startX, startY)
                lineTo(endX, endY)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            dispatchGesture(gesture, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch swipe", e)
            false
        }
    }

    fun performBack(): Boolean {
        return try {
            performGlobalAction(GLOBAL_ACTION_BACK)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform BACK action", e)
            false
        }
    }

    companion object {
        private const val TAG = "AirTouchAccessibility"

        @Volatile
        var instance: AirTouchAccessibilityService? = null
            private set

        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    }
}
