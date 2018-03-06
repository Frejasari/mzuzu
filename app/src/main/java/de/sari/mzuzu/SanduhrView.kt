package de.sari.mzuzu

import android.animation.TimeAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView

class SanduhrView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    val anim = TimeAnimator()
    val timeTextView = TextView(context)

    private val bigCircleDiameter by lazy { width.toFloat() }
    private val bigCircleRadius by lazy { bigCircleDiameter.toRadius() }
    private val smallCircleDiameter by lazy { bigCircleDiameter / 27 }
    private val smallCircleRadius by lazy { smallCircleDiameter.toRadius() }
    private val imaginarySmallCircleRadius by lazy { smallCircleRadius * 2.5F }
    private val offsetSmallCircle by lazy {
        Math.toDegrees(Math.acos(-((smallCircleRadius * smallCircleRadius) /
                (2 * bigCircleRadius * bigCircleRadius)).toDouble())).toFloat()
    }

    private var sweepAngle: Float = 260F

    private val paint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 10F
        strokeCap = Paint.Cap.ROUND
    }

    private val paintSmallCircle = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    fun setFillPercentage(percentage: Float) {
        sweepAngle = percentage * 360F
        invalidate()
    }

    fun setText(text: String) {
        timeTextView.text = text
        invalidate()
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
        canvas.drawArc(5F, 5F, bigCircleDiameter - 5F, bigCircleDiameter - 5F,
                -90F, sweepAngle, false, paint)
        canvas.drawCircle(getCircleCenterX(sweepAngle - offsetSmallCircle),
                getCircleCenterY(sweepAngle - offsetSmallCircle), smallCircleRadius, paintSmallCircle)
    }


    fun getCircleCenterX(angle: Float): Float {
        return (bigCircleRadius - imaginarySmallCircleRadius) * (1 + Math.cos(Math.toRadians(angle.toDouble()))).toFloat() + imaginarySmallCircleRadius
    }

    fun getCircleCenterY(angle: Float): Float {
        return (bigCircleRadius - imaginarySmallCircleRadius) * (1 + Math.sin(Math.toRadians(angle.toDouble()))).toFloat() + imaginarySmallCircleRadius
    }
}

fun Float.toRadius() = this / 2

fun Canvas.getMinimum(): Float {
    return if (width >= height) height.toFloat()
    else width.toFloat()
}