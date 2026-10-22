package com.example.engine

class OneEuroFilter(
    var minCutoff: Float = 1.7f,
    var beta: Float = 6f,
    private val dCutoff: Float = 1f
) {
    private var xPrev = 0f
    private var dxPrev = 0f
    private var tPrevMs = 0L
    private var init = false

    private fun alpha(cutoff: Float, dt: Float): Float {
        val tau = 1f / (2f * Math.PI.toFloat() * cutoff)
        return 1f / (1f + tau / dt)
    }

    fun filter(x: Float, tMs: Long): Float {
        if (!init) {
            init = true
            xPrev = x
            dxPrev = 0f
            tPrevMs = tMs
            return x
        }
        val dt = ((tMs - tPrevMs) / 1000f).coerceIn(0.005f, 0.2f)
        tPrevMs = tMs
        val dx = (x - xPrev) / dt
        val aD = alpha(dCutoff, dt)
        val dxHat = aD * dx + (1f - aD) * dxPrev
        val cutoff = minCutoff + beta * kotlin.math.abs(dxHat)
        val a = alpha(cutoff, dt)
        val xHat = a * x + (1f - a) * xPrev
        xPrev = xHat
        dxPrev = dxHat
        return xHat
    }

    fun reset() {
        init = false
    }
}
