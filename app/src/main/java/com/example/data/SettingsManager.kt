package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AirTouchSettings(
    val sensitivity: Float = 1.3f,       // 0.5f .. 2.5f
    val smoothing: Float = 0.35f,       // EMA alpha: 0.1f (smooth) .. 0.8f (responsive)
    val targetFps: Int = 20,             // 15, 20, 30
    val keepScreenOn: Boolean = true,
    val pinchTapEnabled: Boolean = true,
    val swipeScrollEnabled: Boolean = true,
    val fistBackEnabled: Boolean = true,
    val pauseResumeEnabled: Boolean = true
)

class SettingsManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("air_touch_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AirTouchSettings> = _settings.asStateFlow()

    private fun loadSettings(): AirTouchSettings {
        return AirTouchSettings(
            sensitivity = prefs.getFloat(KEY_SENSITIVITY, 1.3f),
            smoothing = prefs.getFloat(KEY_SMOOTHING, 0.35f),
            targetFps = prefs.getInt(KEY_TARGET_FPS, 20),
            keepScreenOn = prefs.getBoolean(KEY_KEEP_SCREEN_ON, true),
            pinchTapEnabled = prefs.getBoolean(KEY_PINCH_TAP, true),
            swipeScrollEnabled = prefs.getBoolean(KEY_SWIPE_SCROLL, true),
            fistBackEnabled = prefs.getBoolean(KEY_FIST_BACK, true),
            pauseResumeEnabled = prefs.getBoolean(KEY_PAUSE_RESUME, true)
        )
    }

    fun updateSensitivity(value: Float) {
        prefs.edit().putFloat(KEY_SENSITIVITY, value).apply()
        _settings.value = _settings.value.copy(sensitivity = value)
    }

    fun updateSmoothing(value: Float) {
        prefs.edit().putFloat(KEY_SMOOTHING, value).apply()
        _settings.value = _settings.value.copy(smoothing = value)
    }

    fun updateTargetFps(value: Int) {
        prefs.edit().putInt(KEY_TARGET_FPS, value).apply()
        _settings.value = _settings.value.copy(targetFps = value)
    }

    fun updateKeepScreenOn(value: Boolean) {
        prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, value).apply()
        _settings.value = _settings.value.copy(keepScreenOn = value)
    }

    fun updatePinchTapEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_PINCH_TAP, value).apply()
        _settings.value = _settings.value.copy(pinchTapEnabled = value)
    }

    fun updateSwipeScrollEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_SWIPE_SCROLL, value).apply()
        _settings.value = _settings.value.copy(swipeScrollEnabled = value)
    }

    fun updateFistBackEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_FIST_BACK, value).apply()
        _settings.value = _settings.value.copy(fistBackEnabled = value)
    }

    fun updatePauseResumeEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_PAUSE_RESUME, value).apply()
        _settings.value = _settings.value.copy(pauseResumeEnabled = value)
    }

    companion object {
        private const val KEY_SENSITIVITY = "sensitivity"
        private const val KEY_SMOOTHING = "smoothing"
        private const val KEY_TARGET_FPS = "target_fps"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_PINCH_TAP = "pinch_tap"
        private const val KEY_SWIPE_SCROLL = "swipe_scroll"
        private const val KEY_FIST_BACK = "fist_back"
        private const val KEY_PAUSE_RESUME = "pause_resume"

        @Volatile
        private var INSTANCE: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsManager(context).also { INSTANCE = it }
            }
        }
    }
}
