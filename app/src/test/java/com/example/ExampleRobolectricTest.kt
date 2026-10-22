package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.HaatixSettings
import com.example.engine.ActionSink
import com.example.engine.GestureEngine
import org.junit.Assert.assertEquals
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
        assertEquals("Haatix", appName)
    }

    @Test
    fun `gesture engine computes palm center and dispatches tap on release`() {
        var tapDispatched = false
        var tappedX = 0f
        var tappedY = 0f

        val testSink = object : ActionSink {
            override fun tap(x: Float, y: Float) {
                tapDispatched = true
                tappedX = x
                tappedY = y
            }
            override fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, ms: Long) {}
            override fun dragStart(x: Float, y: Float) {}
            override fun dragMove(x: Float, y: Float) {}
            override fun dragEnd() {}
            override fun back() {}
            override fun pauseChanged(paused: Boolean) {}
            override fun haptic(ms: Int) {}
        }

        val engine = GestureEngine(testSink)
        val settings = HaatixSettings(
            cursorSpeed = 1.0f,
            smoothness = 0.5f,
            pinchClickEnabled = true
        )

        // FloatArray(63) for 21 points
        val landmarks = FloatArray(63) { 0.5f }
        // Wrist (0)
        landmarks[0] = 0.5f
        landmarks[1] = 0.7f

        // Middle MCP (9) -> index 9*3 = 27, 28
        landmarks[27] = 0.5f
        landmarks[28] = 0.5f

        // Middle tip (12) extended
        landmarks[36] = 0.5f
        landmarks[37] = 0.2f

        // Ring tip (16) extended
        landmarks[48] = 0.55f
        landmarks[49] = 0.2f

        // Pinky tip (20) extended
        landmarks[60] = 0.58f
        landmarks[61] = 0.2f

        // Thumb tip (4: 12, 13) and Index tip (8: 24, 25) close together (pinch)
        landmarks[12] = 0.49f
        landmarks[13] = 0.45f
        landmarks[24] = 0.50f
        landmarks[25] = 0.45f

        // Frame 1: Approach pinch (t = 1000ms)
        engine.processFrame(landmarks, true, settings, 1000L)
        assertTrue(engine.hasHand)

        // Frame 2: Closed pinch frame 1 (t = 1040ms)
        engine.processFrame(landmarks, true, settings, 1040L)

        // Frame 3: Closed pinch frame 2 (t = 1080ms)
        engine.processFrame(landmarks, true, settings, 1080L)

        // Frame 4: Release pinch (t = 1200ms, closed duration = 120ms)
        landmarks[12] = 0.35f // thumb opened away
        landmarks[24] = 0.60f // index opened away
        engine.processFrame(landmarks, true, settings, 1200L)

        // Verify tap was dispatched at anchor
        assertTrue(tapDispatched)
        assertTrue(tappedX > 0f)
        assertTrue(tappedY > 0f)
    }
}
