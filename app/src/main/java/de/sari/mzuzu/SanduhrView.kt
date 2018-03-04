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

    init {
        setWillNotDraw(false)
        addView(timeTextView)
        val textViewParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT)
        textViewParams.gravity = Gravity.CENTER
        timeTextView.layoutParams = textViewParams
        timeTextView.text = "Test"

        anim.setTimeListener { animation, totalTime, deltaTime ->
            sweepAngle = (sweepAngle + 360F / 1000F * deltaTime * 0.25F) % 360F
            invalidate()
        }
        anim.start()
    }

//    lateinit var path: Path
//
//    override fun onSizeChanged(width: Int, height: Int, oldw: Int, oldh: Int) {
//        super.onSizeChanged(width, height, oldw, oldh)
//
//        val centerX = width / 2F
//        val centerY = height / 2F
////        path = Path().apply {
////            addCircle(centerX, centerY, centerX / 2, Path.Direction.CW)
////        }
//
//    }

    val paint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 10F
        strokeCap = Paint.Cap.ROUND
    }

    val paintSmallCircle = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private var sweepAngle: Float = 260F

    fun setFillPercentage(percentage: Float) {
        sweepAngle = percentage * 360F
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val squareLength = getMinimum(widthMeasureSpec, heightMeasureSpec)
        super.onMeasure(squareLength, squareLength)
    }


    val bigCircleDiameter by lazy { width.toFloat() }
    val bigCircleRadius by lazy { bigCircleDiameter.toRadius() }
    val smallCircleDiameter by lazy { bigCircleDiameter / 27 }
    val smallCircleRadius by lazy { smallCircleDiameter.toRadius() }
    val imaginarySmallCircleRadius by lazy { smallCircleRadius * 2F }
    val offsetSmallCircle by lazy { Math.toDegrees(Math.acos(-((smallCircleRadius * smallCircleRadius) /
            (2 * bigCircleRadius * bigCircleRadius)).toDouble())).toFloat() }


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

    fun getMinimum(width: Int, height: Int): Int {
        return if (width >= height) height
        else width
    }
}

fun Float.toRadius() = this / 2

fun Canvas.getMinimum(): Float {
    return if (width >= height) height.toFloat()
    else width.toFloat()
}