package de.sari.mzuzu

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.TextView

class SanduhrView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {
    val TAG = "Sanduhr"

    var shouldInterceptTouch = true
    var stopWithFullCircle = true

    fun setFillPercentage(percentage: Float) {
        sweepAngle = percentage * 360F
        invalidate()
    }

    fun setText(text: String) {
        timeTextView.text = text
        invalidate()
    }

    private var onRotationListener: OnRotationListener? = null
    private val timeTextView = TextView(context)
    private val bigCircleDiameter by lazy { width.toFloat() }
    private val bigCircleRadius by lazy { bigCircleDiameter.toRadius() }
    private val smallCircleDiameter by lazy { bigCircleDiameter / 27 }
    private val smallCircleRadius by lazy { smallCircleDiameter.toRadius() }
    private val imaginarySmallCircleRadius by lazy { smallCircleRadius * 2.5F }
    private val offsetSmallCircle by lazy {
        Math.toDegrees(Math.acos(-((smallCircleRadius * smallCircleRadius) /
                (2 * bigCircleRadius * bigCircleRadius)).toDouble())).toFloat()
    }
    private val bigCircleStrokeWidth by lazy { 10F }
    private var sweepAngle: Float = 260F
        set(value) {
            field = if (stopWithFullCircle) {
                if (value > 0) Math.min(value, 360F)
                else 0F
            } else value % 360
        }
    private val paint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = bigCircleStrokeWidth
        strokeCap = Paint.Cap.ROUND
    }
    private val paintSmallCircle = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    init {
        setWillNotDraw(false)
        addView(timeTextView)
        val textViewParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT)
        textViewParams.gravity = Gravity.CENTER
        timeTextView.layoutParams = textViewParams
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        Log.v(TAG, "[Sanduhr] onMeasure w: " + MeasureSpec.toString(widthMeasureSpec))
        Log.v(TAG, "[Sanduhr] onMeasure h: " + MeasureSpec.toString(heightMeasureSpec))
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val size: Int
        size = if (widthMode == MeasureSpec.EXACTLY && widthSize > 0) {
            widthSize
        } else if (heightMode == MeasureSpec.EXACTLY && heightSize > 0) {
            heightSize
        } else {
            if (widthSize < heightSize) widthSize else heightSize
        }

        val finalMeasureSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
        super.onMeasure(finalMeasureSpec, finalMeasureSpec)
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        val bigCircleOffset = bigCircleStrokeWidth / 2
        canvas.drawArc(bigCircleOffset, bigCircleOffset, bigCircleDiameter - bigCircleOffset,
                bigCircleDiameter - bigCircleOffset,
                -90F, sweepAngle, false, paint)
        canvas.drawCircle(getCircleCenterX(sweepAngle - offsetSmallCircle),
                getCircleCenterY(sweepAngle - offsetSmallCircle), smallCircleRadius, paintSmallCircle)
    }

    private fun getCircleCenterX(angle: Float): Float {
        return (bigCircleRadius - imaginarySmallCircleRadius) *
                (1 + Math.cos(Math.toRadians(angle.toDouble()))).toFloat() + imaginarySmallCircleRadius
    }

    private fun getCircleCenterY(angle: Float): Float {
        return (bigCircleRadius - imaginarySmallCircleRadius) *
                (1 + Math.sin(Math.toRadians(angle.toDouble()))).toFloat() + imaginarySmallCircleRadius
    }

    private var startX = 0F
    private var startY = 0F
    private var lastMoveX = 0F
    private var lastMoveY = 0F
    private val touchSlop = ViewConfiguration.get(this.context).scaledTouchSlop
    private var touchSweepAngle = 0F

    private val QUADRANT_TOP_LEFT = 1
    private val QUADRANT_TOP_RIGHT = 2
    private val QUADRANT_BOTTOM_LEFT = 3
    private val QUADRANT_BOTTOM_RIGHT = 4

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        Log.i(TAG, "onInterceptTouchEvent called")
        return when (shouldInterceptTouch) {
            false -> super.onInterceptTouchEvent(event)
        // if false is returned TouchEvent is propagated to ChildView
            true -> true
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        val action = event.actionMasked
        val distanceX: Float
        val distanceY: Float
        val thisMoveX: Float
        val thisMoveY: Float
        Log.i(TAG, "onTouchEvent called, action: $action")

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                touchSweepAngle = sweepAngle
                startX = event.x
                startY = event.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                Log.i(TAG, "actionMove called = $sweepAngle")

                thisMoveX = event.x
                thisMoveY = event.y
                distanceX = startX - thisMoveX
                distanceY = startY - thisMoveY
                if (Math.abs(distanceX) > touchSlop || Math.abs(distanceY) > touchSlop) {
                    val arc = getRotationArc(lastMoveX, lastMoveY, thisMoveX, thisMoveY)
                    rotateCircles(arc)
                }
                lastMoveX = thisMoveX
                lastMoveY = thisMoveY
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private val center by lazy { width / 2 }

    private fun getGradientToCenter(x: Float, y: Float): Float {
        return (y - center) / (x - center)
    }

    private fun getRotationArc(x0: Float, y0: Float, x1: Float, y1: Float): Float {
        val m0 = getGradientToCenter(x0, y0).toDouble()
        val m1 = getGradientToCenter(x1, y1).toDouble()
        val rad = Math.atan((m0 - m1) / (1 + m0 * m1))
        return Math.toDegrees(rad).toFloat()
    }

    private fun rotateCircles(arc: Float) {
        touchSweepAngle -= arc
        sweepAngle = touchSweepAngle
        onRotationListener?.onRotation(sweepAngle)
        Log.i(TAG, "rotateCircles called sweepAngle = $sweepAngle")
        invalidate()
    }

    fun getQuadrant(x: Float, y: Float): Int {
        if (x <= center && y <= center) return QUADRANT_TOP_LEFT
        if (x > center && y <= center) return QUADRANT_TOP_RIGHT
        return if (x <= center && y > center) QUADRANT_BOTTOM_LEFT
        else QUADRANT_BOTTOM_RIGHT
    }

    fun getTotalDistance(x0: Float, y0: Float, x1: Float, y1: Float): Double {
        val distanceX = x1 - x0
        val distanceY = y1 - y0
        return Math.sqrt((distanceX * distanceX + distanceY * distanceY).toDouble())
    }

    fun setRotationListener(onRotationListener: OnRotationListener) {
        this.onRotationListener = onRotationListener
    }

    fun removeRotationListener() {
        this.onRotationListener = null
    }

    interface OnRotationListener {
        fun onRotation(arc: Float)
    }
}

fun Float.toRadius() = this / 2