package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AirTouchSettings
import com.example.engine.GestureAction
import com.example.engine.GestureEngine
import com.example.engine.Point3D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AirTouch", appName)
    }

    @Test
    fun `gesture engine computes palm center and detects pinch`() {
        val engine = GestureEngine()
        val settings = AirTouchSettings(
            sensitivity = 1.0f,
            smoothing = 0.5f,
            pinchTapEnabled = true
        )

        // Create 21 landmarks where thumb tip (4) and index tip (8) are pinched close together
        // and middle, ring, pinky are extended so it is not a fist
        val landmarks = MutableList(21) { Point3D(0.5f, 0.5f, 0f) }
        landmarks[0] = Point3D(0.5f, 0.7f, 0f)   // wrist
        landmarks[5] = Point3D(0.48f, 0.5f, 0f)  // index MCP
        landmarks[9] = Point3D(0.52f, 0.5f, 0f)  // middle MCP (palm size = 0.2)
        landmarks[13] = Point3D(0.55f, 0.5f, 0f) // ring MCP
        landmarks[17] = Point3D(0.58f, 0.5f, 0f) // pinky MCP

        landmarks[4] = Point3D(0.49f, 0.45f, 0f) // thumb tip
        landmarks[8] = Point3D(0.50f, 0.45f, 0f) // index tip (very close to thumb = pinch)
        landmarks[12] = Point3D(0.52f, 0.25f, 0f) // middle tip extended
        landmarks[16] = Point3D(0.55f, 0.25f, 0f) // ring tip extended
        landmarks[20] = Point3D(0.58f, 0.28f, 0f) // pinky tip extended

        // Process frame 1 at t=1000ms
        val state1 = engine.process(landmarks, 1080, 2400, settings, 1000L)
        assertTrue(state1.isHandDetected)
        assertTrue(state1.isPinching)

        // Process frame 2 at t=1200ms (>150ms debounce)
        val state2 = engine.process(landmarks, 1080, 2400, settings, 1200L)
        assertNotNull(state2.triggeredAction)
        assertTrue(state2.triggeredAction is GestureAction.Tap)
    }
}
