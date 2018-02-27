package de.sari.mzuzu

object TimeUtils {
    fun toMinutes(seconds: Int): Int = ((seconds - 1) / 60) + 1

    fun toSeconds(minutes: Int) = minutes * 60
}