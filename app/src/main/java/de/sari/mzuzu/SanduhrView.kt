package de.sari.mzuzu

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class SanduhrView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {

    init {
        setWillNotDraw(false)
    }

    lateinit var path: Path


    override fun onSizeChanged(width: Int, height: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(width, height, oldw, oldh)

        val centerX = width / 2F
        val centerY = height / 2F
        path = Path().apply {
            addCircle(centerX, centerY, centerX / 2, Path.Direction.CW)
        }

    }

    val paint = Paint().apply {
        color = Color.BLUE
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 30F
        strokeCap = Paint.Cap.ROUND
    }

    private var sweepAngle: Float = 0F

    fun setFillPercentage(percentage: Float) {
        sweepAngle = percentage * 360F
        invalidate()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        canvas.drawArc(30F, 30F, canvas.width.toFloat() - 30F, canvas.height.toFloat() - 30F, -90F, sweepAngle, true, paint)
    }
}