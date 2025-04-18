package com.example.virtuallab

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

class CircularJoystickView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Updated colors to match the image
    private var bgColor = Color.parseColor("#4D000000")  // Light blue background
    private var thumbColor = Color.parseColor("#2196F3")  // Dark blue thumb
    private var borderColor = Color.parseColor("#1976D2")  // Dark blue border
    private var innerCircleColor = Color.parseColor("#FFFFFF")  // White inner circle

    private var borderWidth = 4f.dpToPx()
    private var thumbRadius = 20f.dpToPx()
    private var innerCircleRadius = 0f

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = bgColor
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = borderWidth
        color = borderColor
    }

    private val innerCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = innerCircleColor
    }

    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = thumbColor
    }

    private var centerX = 0f
    private var centerY = 0f
    private var thumbX = 0f
    private var thumbY = 0f
    private var movementRadius = 0f

    private var listener: ((Float, Float) -> Unit) = { _: Float, _: Float -> }

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.JoystickView,
            defStyleAttr,
            0
        ).apply {
            try {
                bgColor = getColor(R.styleable.JoystickView_bgColor, bgColor)
                thumbColor = getColor(R.styleable.JoystickView_thumbColor, thumbColor)
                borderWidth = getDimension(R.styleable.JoystickView_borderWidth, borderWidth)
                thumbRadius = getDimension(R.styleable.JoystickView_thumbRadius, thumbRadius)
            } finally {
                recycle()
            }
        }

        bgPaint.color = bgColor
        thumbPaint.color = thumbColor
        borderPaint.strokeWidth = borderWidth
        borderPaint.color = borderColor
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        thumbX = centerX
        thumbY = centerY
        movementRadius = min(w, h) / 2f - borderWidth
        innerCircleRadius = movementRadius * 0.5f  // Inner circle is 50% of the movement radius
    }

    override fun onDraw(canvas: Canvas) {
        // Draw background (light blue)
        canvas.drawCircle(centerX, centerY, movementRadius, bgPaint)

        // Draw inner white circle
        canvas.drawCircle(centerX, centerY, innerCircleRadius, innerCirclePaint)

        // Draw border (dark blue)
        canvas.drawCircle(centerX, centerY, movementRadius, borderPaint)

        // Draw thumb (dark blue)
        canvas.drawCircle(thumbX, thumbY, thumbRadius, thumbPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                updateThumbPosition(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_UP -> {
                resetThumb()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateThumbPosition(x: Float, y: Float) {
        val dx = x - centerX
        val dy = y - centerY
        val distance = sqrt(dx * dx + dy * dy)

        if (distance > movementRadius - thumbRadius) {
            thumbX = centerX + (dx / distance) * (movementRadius - thumbRadius)
            thumbY = centerY + (dy / distance) * (movementRadius - thumbRadius)
        } else {
            thumbX = x
            thumbY = y
        }

        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        val strength = (distance / movementRadius).coerceAtMost(1f)
        listener(angle, strength)
        invalidate()
    }

    private fun resetThumb() {
        thumbX = centerX
        thumbY = centerY
        listener(0f, 0f)
        invalidate()
    }

    fun setOnMoveListener(listener: (angle: Float, strength: Float) -> Unit) {
        this.listener = listener
    }

    private fun Float.dpToPx(): Float = this * resources.displayMetrics.density
}