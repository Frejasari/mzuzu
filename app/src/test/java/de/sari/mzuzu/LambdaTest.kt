package de.sari.mzuzu

import junit.framework.Assert.assertEquals
import org.junit.Test

class LambdaTest {


    inline fun higherOrderFunctionInline(f: () -> String): String {
        f()
        return "inline higherOrderFunction am Ende"
    }

    val wo = { "wo return" }

    @Test
    fun inlineTest() {
        fun someFunction(): String {
            higherOrderFunctionInline(wo)
            return "someFunction Ende"
        }

        val string = someFunction()
        assertEquals("someFunction Ende", string)
    }

    @Test
    fun inlineTest2() {

        fun someFunction(): String {
            higherOrderFunctionInline {
                return "wo return"
            }
            return "someFunction Ende"
        }

        val string = someFunction()
        assertEquals("wo return", string)
    }

    @Test
    fun inlineTest3() {
        fun someFunction(): String {
            higherOrderFunctionInline {
                val z = "heeloooo"
                return@higherOrderFunctionInline "wo return"
            }
            return "someFunction Ende"
        }

        val string = someFunction()

        assertEquals("someFunction Ende", string)
    }
}