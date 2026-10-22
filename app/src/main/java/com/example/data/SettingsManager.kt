package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PerformanceMode {
    AUTO,
    HIGH,
    POWER_SAVER
}

data class HaatixSettings(
    val cursorSpeed: Float = 1.15f,         // 0.8 .. 2.0
    val smoothness: Float = 0.6f,           // 0.0 .. 1.0 (maps minCutoff: 3.0 .. 0.8)
    val scrollSpeed: Float = 1.0f,          // 0.5 .. 2.0
    val scrollSensitivity: Float = 1.0f,    // 0.6 .. 1.6
    val reverseScroll: Boolean = false,
    val dwellClick: Boolean = false,
    val haptics: Boolean = true,
    val performanceMode: PerformanceMode = PerformanceMode.AUTO,
    val offsetX: Int = 0,                   // -80 .. 80 px calibration
    val offsetY: Int = 0,                   // -80 .. 80 px calibration
    val keepScreenOn: Boolean = true,
    val pinchClickEnabled: Boolean = true,
    val flickScrollEnabled: Boolean = true,
    val vScrollEnabled: Boolean = true,
    val fistBackEnabled: Boolean = true,
    val pauseEnabled: Boolean = true
)

class SettingsManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("haatix_settings_v2", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<HaatixSettings> = _settings.asStateFlow()

    private fun loadSettings(): HaatixSettings {
        val perfModeString = prefs.getString(KEY_PERF_MODE, PerformanceMode.AUTO.name) ?: PerformanceMode.AUTO.name
        val perfMode = try {
            PerformanceMode.valueOf(perfModeString)
        } catch (_: Exception) {
            PerformanceMode.AUTO
        }

        return HaatixSettings(
            cursorSpeed = prefs.getFloat(KEY_CURSOR_SPEED, 1.15f),
            smoothness = prefs.getFloat(KEY_SMOOTHNESS, 0.6f),
            scrollSpeed = prefs.getFloat(KEY_SCROLL_SPEED, 1.0f),
            scrollSensitivity = prefs.getFloat(KEY_SCROLL_SENSITIVITY, 1.0f),
            reverseScroll = prefs.getBoolean(KEY_REVERSE_SCROLL, false),
            dwellClick = prefs.getBoolean(KEY_DWELL_CLICK, false),
            haptics = prefs.getBoolean(KEY_HAPTICS, true),
            performanceMode = perfMode,
            offsetX = prefs.getInt(KEY_OFFSET_X, 0),
            offsetY = prefs.getInt(KEY_OFFSET_Y, 0),
            keepScreenOn = prefs.getBoolean(KEY_KEEP_SCREEN_ON, true),
            pinchClickEnabled = prefs.getBoolean(KEY_PINCH_CLICK, true),
            flickScrollEnabled = prefs.getBoolean(KEY_FLICK_SCROLL, true),
            vScrollEnabled = prefs.getBoolean(KEY_V_SCROLL, true),
            fistBackEnabled = prefs.getBoolean(KEY_FIST_BACK, true),
            pauseEnabled = prefs.getBoolean(KEY_PAUSE, true)
        )
    }

    fun updateCursorSpeed(value: Float) {
        prefs.edit().putFloat(KEY_CURSOR_SPEED, value).apply()
        _settings.value = _settings.value.copy(cursorSpeed = value)
    }

    fun updateSmoothness(value: Float) {
        prefs.edit().putFloat(KEY_SMOOTHNESS, value).apply()
        _settings.value = _settings.value.copy(smoothness = value)
    }

    fun updateScrollSpeed(value: Float) {
        prefs.edit().putFloat(KEY_SCROLL_SPEED, value).apply()
        _settings.value = _settings.value.copy(scrollSpeed = value)
    }

    fun updateScrollSensitivity(value: Float) {
        prefs.edit().putFloat(KEY_SCROLL_SENSITIVITY, value).apply()
        _settings.value = _settings.value.copy(scrollSensitivity = value)
    }

    fun updateReverseScroll(value: Boolean) {
        prefs.edit().putBoolean(KEY_REVERSE_SCROLL, value).apply()
        _settings.value = _settings.value.copy(reverseScroll = value)
    }

    fun updateDwellClick(value: Boolean) {
        prefs.edit().putBoolean(KEY_DWELL_CLICK, value).apply()
        _settings.value = _settings.value.copy(dwellClick = value)
    }

    fun updateHaptics(value: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTICS, value).apply()
        _settings.value = _settings.value.copy(haptics = value)
    }

    fun updatePerformanceMode(mode: PerformanceMode) {
        prefs.edit().putString(KEY_PERF_MODE, mode.name).apply()
        _settings.value = _settings.value.copy(performanceMode = mode)
    }

    fun updateOffsetX(value: Int) {
        prefs.edit().putInt(KEY_OFFSET_X, value).apply()
        _settings.value = _settings.value.copy(offsetX = value)
    }

    fun updateOffsetY(value: Int) {
        prefs.edit().putInt(KEY_OFFSET_Y, value).apply()
        _settings.value = _settings.value.copy(offsetY = value)
    }

    fun updateKeepScreenOn(value: Boolean) {
        prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, value).apply()
        _settings.value = _settings.value.copy(keepScreenOn = value)
    }

    fun updatePinchClickEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_PINCH_CLICK, value).apply()
        _settings.value = _settings.value.copy(pinchClickEnabled = value)
    }

    fun updateFlickScrollEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_FLICK_SCROLL, value).apply()
        _settings.value = _settings.value.copy(flickScrollEnabled = value)
    }

    fun updateVScrollEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_V_SCROLL, value).apply()
        _settings.value = _settings.value.copy(vScrollEnabled = value)
    }

    fun updateFistBackEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_FIST_BACK, value).apply()
        _settings.value = _settings.value.copy(fistBackEnabled = value)
    }

    fun updatePauseEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_PAUSE, value).apply()
        _settings.value = _settings.value.copy(pauseEnabled = value)
    }

    companion object {
        private const val KEY_CURSOR_SPEED = "cursor_speed"
        private const val KEY_SMOOTHNESS = "smoothness"
        private const val KEY_SCROLL_SPEED = "scroll_speed"
        private const val KEY_SCROLL_SENSITIVITY = "scroll_sensitivity"
        private const val KEY_REVERSE_SCROLL = "reverse_scroll"
        private const val KEY_DWELL_CLICK = "dwell_click"
        private const val KEY_HAPTICS = "haptics"
        private const val KEY_PERF_MODE = "perf_mode"
        private const val KEY_OFFSET_X = "offset_x"
        private const val KEY_OFFSET_Y = "offset_y"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_PINCH_CLICK = "pinch_click"
        private const val KEY_FLICK_SCROLL = "flick_scroll"
        private const val KEY_V_SCROLL = "v_scroll"
        private const val KEY_FIST_BACK = "fist_back"
        private const val KEY_PAUSE = "pause"

        @Volatile
        private var INSTANCE: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsManager(context).also { INSTANCE = it }
            }
        }
    }
}
