package com.kotlinsun.aideskhub.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class IdleLineArtView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.SQUARE
        strokeWidth = 7f
    }

    private data class LineSpec(
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        val color: Int,
        val alpha: Int,
        val width: Float = 7f,
    )

    private val lines = listOf(
        LineSpec(0.04f, 0.38f, 0.28f, 0.18f, Color.rgb(41, 155, 255), 70),
        LineSpec(0.04f, 0.52f, 0.33f, 0.28f, Color.rgb(41, 155, 255), 120),
        LineSpec(0.04f, 0.66f, 0.39f, 0.38f, Color.rgb(41, 155, 255), 210),
        LineSpec(0.36f, 0.10f, 0.48f, 0.36f, Color.WHITE, 235, 8f),
        LineSpec(0.45f, 0.10f, 0.56f, 0.30f, Color.WHITE, 140),
        LineSpec(0.54f, 0.10f, 0.64f, 0.27f, Color.WHITE, 80),
        LineSpec(0.39f, 0.70f, 0.49f, 0.92f, Color.rgb(255, 211, 39), 80),
        LineSpec(0.48f, 0.62f, 0.61f, 0.94f, Color.rgb(255, 211, 39), 235, 8f),
        LineSpec(0.62f, 0.46f, 0.96f, 0.12f, Color.rgb(32, 226, 139), 235, 8f),
        LineSpec(0.68f, 0.58f, 0.96f, 0.30f, Color.rgb(32, 226, 139), 150),
        LineSpec(0.73f, 0.71f, 0.96f, 0.48f, Color.rgb(32, 226, 139), 85),
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        lines.forEach { line ->
            paint.color = line.color
            paint.alpha = line.alpha
            paint.strokeWidth = line.width
            canvas.drawLine(
                line.startX * w,
                line.startY * h,
                line.endX * w,
                line.endY * h,
                paint,
            )
        }
    }
}
