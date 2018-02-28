package de.sari.mzuzu

object TimeUtils {
    //    fun toMinutes(seconds: Int): Int = seconds
    fun toMinutes(seconds: Int): Int = ((seconds - 1) / 60) + 1

    fun toSeconds(minutes: Int) = minutes * 60
//    fun toSeconds(minutes: Int) = minutes

    fun toMillis(seconds: Int) = seconds.toLong() * 1000L
}