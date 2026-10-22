package com.example.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AirTouchAccessibilityService : AccessibilityService() {

    private var injector: TouchInjector? = null

    val isDragging: Boolean
        get() = injector?.isDragging == true

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        injector = TouchInjector(this)
        _isConnected.value = true
        Log.i(TAG, "AirTouch/Haatix Accessibility Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // canRetrieveWindowContent is false. No screen content is ever inspected.
    }

    override fun onInterrupt() {
        Log.w(TAG, "AirTouch/Haatix Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        injector?.dragEnd()
        injector = null
        if (instance == this) {
            instance = null
            _isConnected.value = false
        }
        Log.i(TAG, "AirTouch/Haatix Accessibility Service Destroyed")
    }

    fun tap(x: Float, y: Float) {
        injector?.tap(x, y)
    }

    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long) {
        injector?.swipe(x1, y1, x2, y2, durationMs)
    }

    fun dragStart(x: Float, y: Float) {
        injector?.dragStart(x, y)
    }

    fun dragMove(x: Float, y: Float) {
        injector?.dragMove(x, y)
    }

    fun dragEnd() {
        injector?.dragEnd()
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
