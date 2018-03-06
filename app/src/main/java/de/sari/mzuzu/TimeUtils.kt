package de.sari.mzuzu

object TimeUtils {
    //    fun secondsToMinutes(seconds: Int): Int = seconds
    fun secondsToMinutes(seconds: Long): Int = ((seconds.toInt() - 1) / 60) + 1

    fun secondsToMinutes(seconds: Int): Int = ((seconds - 1) / 60) + 1

    fun minToSeconds(minutes: Int): Int = minutes * 60
    fun minToSeconds(minutes: Long): Int = minutes.toInt() * 60
    //    fun minToSeconds(minutes: Int) = minutes
    fun minToMillis(minutes: Int): Long = minutes * 60 * 1000L

    fun secondsToMillis(seconds: Int) = seconds.toLong() * 1000L

    fun millisToSeconds(millis: Long): Int = (millis / 1000).toInt()
    fun millisToMinutes(millis: Long): Int = (millis / 60000).toInt()
}