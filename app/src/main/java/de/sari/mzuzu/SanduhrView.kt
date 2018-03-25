package de.sari.mzuzu

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.support.v4.content.ContextCompat
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

    fun setPercentageOfBigCircel(percentage: Float) {
        sweepAngle = percentage * 360F
        invalidate()
    }

    fun setFillPercentage(percentage: Float) {
        fillPercentageOfBigCircle = percentage
        invalidate()
    }

    fun setText(text: String) {
        this.textViewText = text
        invalidate()
    }

    private var onRotationListener: OnRotationListener? = null
    private val timeTextView = TextView(context)
    private val fullCircleDiameter by lazy { width.toFloat() - bigCircleStrokeWidth }
    private val fullCircleRadius by lazy { fullCircleDiameter.toRadius() }
    private val bigCircleRadius by lazy { bigCircleDiameter /2}
    private val bigCircleDiameter by lazy { (fullCircleDiameter * 0.95).toFloat()}
    private val smallCircleDiameter by lazy { fullCircleDiameter / 27 }
    private val smallCircleRadius by lazy { smallCircleDiameter.toRadius() }
    private val imaginarySmallCircleRadius by lazy { smallCircleRadius * 2.5F }
    private val offsetSmallCircle by lazy {
        Math.toDegrees(Math.acos(-((smallCircleRadius * smallCircleRadius) /
                (2 * fullCircleRadius * fullCircleRadius)).toDouble())).toFloat()
    }
    private val bigCircleStrokeWidth by lazy { 10F }
    private var sweepAngle: Float = 260F
        set(value) {
            field = if (stopWithFullCircle) {
                if (value > 0) Math.min(value, 360F)
                else 0F
            } else value % 360
        }
    private var fillPercentageOfBigCircle = 0.7F
        set(value) {
            if (value >= 1) field = 1F
            field = if (value <= 0) 0F
            else value
        }
    private var textViewText = ""
    private val paint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = bigCircleStrokeWidth
        strokeCap = Paint.Cap.ROUND
    }

    private val paintFullCircle = Paint().apply {
        color = ContextCompat.getColor(context, R.color.colorAccent)
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

    private val paintFillCircle = Paint().apply {
        color = ContextCompat.getColor(context, R.color.colorAccent)
        style = Paint.Style.FILL
        alpha = 100
    }
    private val fontSize = 70F
    private val paintText = Paint().apply {
        color = ContextCompat.getColor(context, R.color.colorAccentPrimary)
        isAntiAlias = true
        strokeWidth = 2F
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.FILL_AND_STROKE
        textSize = fontSize
        textAlign = Paint.Align.CENTER
        // TODO  And change the color / style
    }

    init {
        setWillNotDraw(false)
        addView(timeTextView) // TODO Remove View or get to know how to bring it in front of the filling circle
        val textViewParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT)
        textViewParams.gravity = Gravity.CENTER
        timeTextView.layoutParams = textViewParams
        timeTextView.translationZ = 1F
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
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
        canvas.translate(width / 2F, width / 2F)
        canvas.save()
//        defines new drawing area - everything outside of this area will not draw
        canvas.clipRect(-fullCircleRadius, (fullCircleRadius - fillPercentageOfBigCircle * fullCircleDiameter), fullCircleRadius, fullCircleRadius)
        canvas.drawCircle(0F, 0F, fullCircleRadius, paintFillCircle)
        canvas.restore()
        canvas.drawCircle(0F, 0F, fullCircleRadius, paintFullCircle)
        canvas.drawArc(-bigCircleRadius, -bigCircleRadius, bigCircleRadius, bigCircleRadius,
                -90F, sweepAngle, false, paint)
        canvas.drawCircle(getCircleCenterX(sweepAngle - offsetSmallCircle),
                getCircleCenterY(sweepAngle - offsetSmallCircle), smallCircleRadius, paintSmallCircle)
        canvas.drawText(textViewText, 0F, fontSize / 2, paintText)
    }

    private fun getCircleCenterX(angle: Float): Float {
        return (bigCircleRadius - imaginarySmallCircleRadius) * Math.cos(Math.toRadians(angle.toDouble())).toFloat()
    }

    private fun getCircleCenterY(angle: Float): Float {
        return (bigCircleRadius - imaginarySmallCircleRadius) * Math.sin(Math.toRadians(angle.toDouble())).toFloat()
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
        Log.i(TAG, "onInterceptTouchEvent called. shouldInterceptTouch: $shouldInterceptTouch")
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
                return shouldInterceptTouch
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