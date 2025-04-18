package com.example.virtuallab

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class HorizontalJoystickView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val outerPaint = Paint().apply {
        color = Color.GRAY
        style = Paint.Style.FILL
    }

    private val innerPaint = Paint().apply {
        color = Color.DKGRAY
        style = Paint.Style.FILL
    }

    private var outerRadius = 100f
    private var innerRadius = 50f
    private var centerX = 0f
    private var centerY = 0f
    private var joystickX = 0f

    private var onMoveListener: ((Float) -> Unit)? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        joystickX = centerX
        outerRadius = w / 4f
        innerRadius = outerRadius / 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw outer circle
        canvas.drawCircle(centerX, centerY, outerRadius, outerPaint)
        // Draw inner circle (joystick)
        canvas.drawCircle(joystickX, centerY, innerRadius, innerPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val newX = event.x.coerceIn(centerX - outerRadius, centerX + outerRadius)
                joystickX = newX

                val strength = (joystickX - centerX) / outerRadius
                onMoveListener?.invoke(strength)

                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                joystickX = centerX
                onMoveListener?.invoke(0f)
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun setOnMoveListener(listener: (Float) -> Unit) {
        this.onMoveListener = listener
    }
}