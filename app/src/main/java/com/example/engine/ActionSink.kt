package com.example.engine

interface ActionSink {
    fun tap(x: Float, y: Float)
    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, ms: Long)
    fun dragStart(x: Float, y: Float)
    fun dragMove(x: Float, y: Float)
    fun dragEnd()
    fun back()
    fun pauseChanged(paused: Boolean)
    fun haptic(ms: Int)
}
