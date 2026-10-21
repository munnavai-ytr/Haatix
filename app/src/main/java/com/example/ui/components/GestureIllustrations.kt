package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MoveIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cyan = Color(0xFF00E5FF)
        val paleCyan = Color(0x3300E5FF)

        // Draw motion concentric circles
        drawCircle(color = paleCyan, radius = w * 0.42f, center = Offset(w * 0.5f, h * 0.52f))
        drawCircle(
            color = cyan.copy(alpha = 0.4f),
            radius = w * 0.35f,
            center = Offset(w * 0.5f, h * 0.52f),
            style = Stroke(width = 2.dp.toPx())
        )

        // Draw 4 directional arrows (North, South, East, West)
        drawArrow(Offset(w * 0.5f, h * 0.18f), Offset(w * 0.5f, h * 0.06f), cyan)
        drawArrow(Offset(w * 0.5f, h * 0.86f), Offset(w * 0.5f, h * 0.98f), cyan)
        drawArrow(Offset(w * 0.18f, h * 0.52f), Offset(w * 0.06f, h * 0.52f), cyan)
        drawArrow(Offset(w * 0.82f, h * 0.52f), Offset(w * 0.94f, h * 0.52f), cyan)

        // Draw hand palm outline
        drawHandPalm(w, h, cyan)

        // Draw center cursor hologram dot
        drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(w * 0.5f, h * 0.55f))
        drawCircle(
            color = cyan,
            radius = 12.dp.toPx(),
            center = Offset(w * 0.5f, h * 0.55f),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
fun PinchTapIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cyan = Color(0xFF00E5FF)
        val white = Color.White

        // Draw tap ripple circles at pinch contact point (top-center)
        val pinchX = w * 0.46f
        val pinchY = h * 0.28f

        drawCircle(color = Color(0x3300E5FF), radius = w * 0.28f, center = Offset(pinchX, pinchY))
        drawCircle(
            color = cyan.copy(alpha = 0.6f),
            radius = w * 0.18f,
            center = Offset(pinchX, pinchY),
            style = Stroke(width = 2.dp.toPx())
        )
        drawCircle(color = white, radius = 6.dp.toPx(), center = Offset(pinchX, pinchY))

        // Draw pinch gesture thumb & index touching path
        val path = Path().apply {
            // Index finger bending in
            moveTo(w * 0.58f, h * 0.65f)
            quadraticTo(w * 0.62f, h * 0.38f, pinchX, pinchY)
            // Thumb meeting index
            quadraticTo(w * 0.28f, h * 0.38f, w * 0.34f, h * 0.68f)
            // Palm base
            quadraticTo(w * 0.46f, h * 0.88f, w * 0.58f, h * 0.65f)
        }
        drawPath(path = path, color = cyan.copy(alpha = 0.25f))
        drawPath(
            path = path,
            color = cyan,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Radiating tap spark lines
        for (angle in listOf(-60f, -30f, 0f, 30f, 60f)) {
            val rad = Math.toRadians((angle - 90).toDouble())
            val startR = w * 0.20f
            val endR = w * 0.28f
            val x1 = (pinchX + startR * Math.cos(rad)).toFloat()
            val y1 = (pinchY + startR * Math.sin(rad)).toFloat()
            val x2 = (pinchX + endR * Math.cos(rad)).toFloat()
            val y2 = (pinchY + endR * Math.sin(rad)).toFloat()
            drawLine(color = cyan, start = Offset(x1, y1), end = Offset(x2, y2), strokeWidth = 2.dp.toPx())
        }
    }
}

@Composable
fun ScrollIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cyan = Color(0xFF00E5FF)
        val teal = Color(0xFF00F5D4)

        // Draw hand
        drawHandPalm(w, h, cyan.copy(alpha = 0.4f))

        // Vertical motion swipe indicator on the right side
        val barX = w * 0.82f
        drawLine(
            color = teal,
            start = Offset(barX, h * 0.2f),
            end = Offset(barX, h * 0.8f),
            strokeWidth = 3.5.dp.toPx(),
            cap = StrokeCap.Round
        )
        // Up arrow head
        drawArrow(Offset(barX, h * 0.32f), Offset(barX, h * 0.15f), teal)
        // Down arrow head
        drawArrow(Offset(barX, h * 0.68f), Offset(barX, h * 0.85f), teal)

        // Speed wave trails
        drawLine(
            color = cyan.copy(alpha = 0.6f),
            start = Offset(w * 0.25f, h * 0.35f),
            end = Offset(w * 0.65f, h * 0.35f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = cyan.copy(alpha = 0.6f),
            start = Offset(w * 0.25f, h * 0.65f),
            end = Offset(w * 0.65f, h * 0.65f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun FistBackIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val red = Color(0xFFFF5252)
        val cyan = Color(0xFF00E5FF)

        // Draw timer countdown arc (0.7s indicator)
        drawArc(
            color = red,
            startAngle = -90f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(w * 0.08f, h * 0.08f),
            size = Size(w * 0.84f, h * 0.84f),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw compact fist shape
        val fistPath = Path().apply {
            moveTo(w * 0.32f, h * 0.72f)
            // Thumb folded over
            quadraticTo(w * 0.24f, h * 0.52f, w * 0.35f, h * 0.44f)
            // Curled knuckles
            lineTo(w * 0.45f, h * 0.36f)
            lineTo(w * 0.55f, h * 0.36f)
            lineTo(w * 0.65f, h * 0.38f)
            quadraticTo(w * 0.74f, h * 0.48f, w * 0.68f, h * 0.72f)
            close()
        }
        drawPath(path = fistPath, color = cyan.copy(alpha = 0.25f))
        drawPath(path = fistPath, color = cyan, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

        // Draw "Back" return arrow
        val arrowPath = Path().apply {
            moveTo(w * 0.75f, h * 0.22f)
            quadraticTo(w * 0.5f, h * 0.12f, w * 0.26f, h * 0.22f)
        }
        drawPath(path = arrowPath, color = red, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        drawArrow(Offset(w * 0.34f, h * 0.18f), Offset(w * 0.22f, h * 0.24f), red)
    }
}

@Composable
fun PauseResumeIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val amber = Color(0xFFFFB703)
        val cyan = Color(0xFF00E5FF)

        // Draw 2-second hold circular progress ring
        drawCircle(
            color = amber.copy(alpha = 0.3f),
            radius = w * 0.44f,
            center = Offset(w * 0.5f, h * 0.5f),
            style = Stroke(width = 3.dp.toPx())
        )
        drawArc(
            color = amber,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(w * 0.06f, h * 0.06f),
            size = Size(w * 0.88f, h * 0.88f),
            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw hand palm outline
        drawHandPalm(w, h, cyan.copy(alpha = 0.35f))

        // Draw Pause Bars (||) at center
        val barW = 5.dp.toPx()
        val barH = 22.dp.toPx()
        drawRect(
            color = amber,
            topLeft = Offset(w * 0.44f - barW, h * 0.5f - (barH / 2f)),
            size = Size(barW, barH)
        )
        drawRect(
            color = amber,
            topLeft = Offset(w * 0.56f, h * 0.5f - (barH / 2f)),
            size = Size(barW, barH)
        )
    }
}

private fun DrawScope.drawArrow(start: Offset, end: Offset, color: Color) {
    drawLine(color = color, start = start, end = end, strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
    val angle = Math.atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
    val headLen = 12f
    val headAngle = Math.PI / 6.0 // 30 deg

    val x1 = (end.x - headLen * Math.cos(angle - headAngle)).toFloat()
    val y1 = (end.y - headLen * Math.sin(angle - headAngle)).toFloat()
    val x2 = (end.x - headLen * Math.cos(angle + headAngle)).toFloat()
    val y2 = (end.y - headLen * Math.sin(angle + headAngle)).toFloat()

    val path = Path().apply {
        moveTo(end.x, end.y)
        lineTo(x1, y1)
        moveTo(end.x, end.y)
        lineTo(x2, y2)
    }
    drawPath(path = path, color = color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
}

private fun DrawScope.drawHandPalm(w: Float, h: Float, color: Color) {
    val path = Path().apply {
        // Wrist
        moveTo(w * 0.42f, h * 0.86f)
        // Thumb
        quadraticTo(w * 0.22f, h * 0.72f, w * 0.24f, h * 0.52f)
        quadraticTo(w * 0.26f, h * 0.44f, w * 0.34f, h * 0.52f)
        // Index
        lineTo(w * 0.38f, h * 0.26f)
        quadraticTo(w * 0.41f, h * 0.22f, w * 0.44f, h * 0.26f)
        // Middle
        lineTo(w * 0.48f, h * 0.18f)
        quadraticTo(w * 0.52f, h * 0.14f, w * 0.56f, h * 0.18f)
        // Ring
        lineTo(w * 0.60f, h * 0.24f)
        quadraticTo(w * 0.63f, h * 0.20f, w * 0.66f, h * 0.24f)
        // Pinky
        lineTo(w * 0.70f, h * 0.36f)
        quadraticTo(w * 0.73f, h * 0.32f, w * 0.76f, h * 0.38f)
        // Outer palm edge to wrist
        quadraticTo(w * 0.75f, h * 0.72f, w * 0.58f, h * 0.86f)
        close()
    }
    drawPath(path = path, color = color.copy(alpha = 0.2f))
    drawPath(path = path, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
}
