package de.sari.mzuzu

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat

/**
 * Created by sari on 11.02.18.
 */
const val ID_MEDITATION_CHANNEL = "Meditation Channel ID"
const val ACTION_ADD = "Meditation Channel ID"
const val ACTION_PAUSE = "Meditation Channel ID"
const val ACTION_PLAY = "Meditation Channel ID"
const val ACTION_REPEAT = "Meditation Channel ID"
const val ACTION_STOP = "Meditation Channel ID"


object MeditationNotification {
    lateinit var notificationIntent: Intent

    lateinit var pendingIntent: PendingIntent

    private lateinit var playIntent: Intent

    private lateinit var pPlayIntent: PendingIntent

    private lateinit var pauseIntent: Intent

    private lateinit var pPauseIntent: PendingIntent

    private lateinit var addIntent: Intent

    private lateinit var pAddIntent: PendingIntent

    private lateinit var repeatIntent: Intent

    private lateinit var pRepeatIntent: PendingIntent

    private lateinit var stopIntent: Intent

    private lateinit var pStopIntent: PendingIntent

    fun createIntents(context: Context) {
         notificationIntent = Intent(context, MainActivity::class.java)
//        notificationIntent.action = ACTION_MAIN
        notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK; Intent.FLAG_ACTIVITY_CLEAR_TASK
         pendingIntent = PendingIntent.getService(context, 0, notificationIntent, 0)

         playIntent = Intent(context, MeditationTimerService::class.java).apply {
            action = ACTION_PLAY
        }
         pPlayIntent = PendingIntent.getService(context, 0, playIntent, 0)
        pauseIntent = Intent(context, MeditationTimerService::class.java).apply {
            action = ACTION_PAUSE
        }
         pPauseIntent = PendingIntent.getService(context, 0, pauseIntent, 0)

         addIntent = Intent(context, MeditationTimerService::class.java).apply {
            action = ACTION_ADD
        }
         pAddIntent = PendingIntent.getService(context, 0, addIntent, 0)

         repeatIntent = Intent(context, MeditationTimerService::class.java).apply {
            action = ACTION_REPEAT
        }
         pRepeatIntent = PendingIntent.getService(context, 0, repeatIntent, 0)

         stopIntent = Intent(context, MeditationTimerService::class.java).apply {
            action = ACTION_STOP
        }
         pStopIntent = PendingIntent.getService(context, 0, stopIntent, 0)
    }

    private fun getNotificationBuilder(context: Context, pendingIntent: PendingIntent) : NotificationCompat.Builder{
       return NotificationCompat.Builder(context, "YOOOO ")
                .setContentTitle("Start Meditating?")
                .setSmallIcon(R.drawable.ic_launcher)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent)
    }

    fun getRunningNotification(, ){

    }

    // Add media control buttons that invoke intents in your media service
    .addAction(R.drawable.ic_stop, "", stopPendingIntent) // #0
    .addAction(R.drawable.ic_play_arrow, "", playPendingIntent)  // #1
    .addAction(R.drawable.ic_exposure_plus_2, "", plusPendingIntent) // #2

}