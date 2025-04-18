package com.example.virtuallab

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class VerticalJoystickView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Updated colors to match the image
    private var bgColor = Color.parseColor("#4D000000")  // Light blue background
    private var thumbColor = Color.parseColor("#2196F3")  // Dark blue thumb
    private var borderColor = Color.parseColor("#1976D2")  // Dark blue border
    private var thumbRingColor = Color.parseColor("#FFFFFF")  // White ring around thumb

    private var borderWidth = 4f.dpToPx()  // Increased border width for visibility
    private var thumbRadius = 20f.dpToPx()
    private var thumbRingRadius = 24f.dpToPx()  // Slightly larger than thumb for the white ring

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = bgColor
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = borderWidth
        color = borderColor
    }

    private val thumbRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = thumbRingColor
    }

    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = thumbColor
    }

    private var centerX = 0f
    private var centerY = 0f
    private var thumbY = 0f
    private var movementHeight = 0f

    private var listener: ((strength: Float) -> Unit) = { _ -> }

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
        thumbRingRadius = thumbRadius + 4f.dpToPx()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        thumbY = centerY
        movementHeight = h / 2f - thumbRadius - borderWidth
    }

    override fun onDraw(canvas: Canvas) {
        // Draw background (light blue rounded rectangle)
        val rect = RectF(borderWidth, borderWidth, width.toFloat() - borderWidth, height.toFloat() - borderWidth)
        canvas.drawRoundRect(rect, width / 2f, width / 2f, bgPaint)

        // Draw border (dark blue outline)
        canvas.drawRoundRect(rect, width / 2f, width / 2f, borderPaint)

        // Draw thumb ring (white circle)
        canvas.drawCircle(centerX, thumbY, thumbRingRadius, thumbRingPaint)

        // Draw thumb (dark blue circle)
        canvas.drawCircle(centerX, thumbY, thumbRadius, thumbPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                updateThumbPosition(event.y)
                return true
            }
            MotionEvent.ACTION_UP -> {
                resetThumb()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateThumbPosition(y: Float) {
        thumbY = y.coerceIn(
            centerY - movementHeight,
            centerY + movementHeight
        )

        val strength = (centerY - thumbY) / movementHeight
        listener(strength)
        invalidate()
    }

    private fun resetThumb() {
        thumbY = centerY
        listener(0f)
        invalidate()
    }

    fun setOnMoveListener(listener: (strength: Float) -> Unit) {
        this.listener = listener
    }

    private fun Float.dpToPx(): Float = this * resources.displayMetrics.density
}