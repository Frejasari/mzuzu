package de.sari.mzuzu

import junit.framework.Assert
import org.junit.Test

class MathTest {

    val center = 3
    private fun getGradientToCenter(x: Float, y: Float): Float {
        return (y - center) / (x - center)
    }

    fun getRotationArc(x0: Float, y0: Float, x1: Float, y1: Float): Float {
        val m0 = getGradientToCenter(x0, y0).toDouble()
        val m1 = getGradientToCenter(x1, y1).toDouble()
        val rad = Math.atan((m0 - m1) / (1 + m0 * m1))
        return Math.toDegrees(rad).toFloat()
    }

    @Test
    fun negativeArcBetweenTwoPoints() {
        Assert.assertEquals(180F, getRotationArc(1F, 4F, 4F, 3F))
    }

    @Test
    fun positiveArcBetweenTwoPoints() {
        Assert.assertEquals(10F, getRotationArc(4F, 3F, 1F, 4F))
    }

    @Test
    fun negativeArcBetweenTwoPoints2() {
        Assert.assertEquals(10F, getRotationArc(1F, 4F, 2F, 2F))
    }

    @Test
    fun positiveArcBetweenTwoPoints2() {
        Assert.assertEquals(10F, getRotationArc(4F, 2F, 1F, 2F))
    }

    @Test
    fun positiveArcBetweenTwoPoints0() {
        Assert.assertEquals(10F, getRotationArc(1F, 3F, 2F, 3F))
    }

    @Test
    fun positiveArcBetweenTwoPoints180() {
        Assert.assertEquals(10F, getRotationArc(1F, 3F, 4F, 3F))
    }
}