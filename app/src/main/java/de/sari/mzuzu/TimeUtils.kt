package de.sari.mzuzu

object TimeUtils {
    fun toMinutes(seconds: Int): Int = ((seconds - 1) / 60) + 1
}