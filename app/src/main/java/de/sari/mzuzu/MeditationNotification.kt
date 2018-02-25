package de.sari.mzuzu

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationCompat.PRIORITY_MAX
import android.support.v4.content.ContextCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.app.NotificationCompat.MediaStyle
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import de.sari.commons.TimerState


const val ACTION_ADD = "Action add"
const val ACTION_PAUSE = "Action pause"
const val ACTION_PLAY = "Action play"
const val ACTION_REPEAT = "Action repeat"
const val ACTION_STOP = "Action stop"
const val ACTION_ENABLE = "Action enable"
const val CHANNEL_ID_COMPLETED = "notification channel for completion"
const val CHANNEL_ID_MEDITATING = "notification channel for meditation"
const val MEDIA_SESSION_ID = "media session id sari"

object MeditationNotification {

    fun getNotification(status: TimerState, remainingMinutes: Int, context: Context, notificationManager: NotificationManager): Notification {
        Log.i("Notification", "Notification Update, state: $status")
        createNotificationBuilder(context, notificationManager).let { notificationBuilder ->
            notificationBuilder.setContentTitle(status.toString())
            return when (status) {
                TimerState.RUNNING -> {
                    notificationBuilder
                            .setContentText(context.getString(R.string.notification_remaining_time, remainingMinutes))
                            .addAction(R.drawable.ic_stop, context.getString(R.string.stop_meditation), getActionIntent(context, ACTION_STOP))
                            .addAction(R.drawable.ic_pause, context.getString(R.string.action_pause), getActionIntent(context, ACTION_PAUSE))
                            .addAction(R.drawable.ic_add_2, context.getString(R.string.add_2_minutes), getActionIntent(context, ACTION_ADD))
                            .build()
                            .apply { flags = NotificationCompat.FLAG_ONGOING_EVENT }
                }
                TimerState.PAUSED -> {
                    notificationBuilder
                            .setContentText(context.getString(R.string.notification_remaining_time, remainingMinutes))
                            .addAction(R.drawable.ic_stop, context.getString(R.string.stop_meditation), getActionIntent(context, ACTION_STOP))
                            .addAction(R.drawable.ic_play, context.getString(R.string.action_play), getActionIntent(context, ACTION_PLAY))
                            .addAction(R.drawable.ic_add_2, context.getString(R.string.add_2_minutes), getActionIntent(context, ACTION_ADD))
                            .build()
                            .apply { flags = NotificationCompat.FLAG_ONGOING_EVENT }
                }
                TimerState.STOPPED -> {
                    notificationBuilder
                            .setContentText(context.getString(R.string.notification_start_meditation))
                            .addAction(R.drawable.ic_stop, context.getString(R.string.stop_meditation), getActionIntent(context, ACTION_ENABLE))
                            .addAction(R.drawable.ic_play, context.getString(R.string.action_play), getActionIntent(context, ACTION_PLAY))
                            .addAction(R.drawable.ic_add_2, context.getString(R.string.add_2_minutes), getActionIntent(context, ACTION_ENABLE))
//                        .setOngoing(false)
                            .build()
                }
                TimerState.COMPLETED -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        createCompletedChannel(context, notificationManager)
                    }
                    notificationBuilder
                            .setContentText(context.getString(R.string.notification_meditate_again))
                            .addAction(R.drawable.ic_stop, context.getString(R.string.stop_meditation), getActionIntent(context, ACTION_STOP))
                            .addAction(R.drawable.ic_replay, context.getString(R.string.action_play), getActionIntent(context, ACTION_REPEAT))
                            .addAction(R.drawable.ic_add_2, context.getString(R.string.add_2_minutes), getActionIntent(context, ACTION_ADD))
                            .setPriority(PRIORITY_MAX)
                            .setChannelId(CHANNEL_ID_COMPLETED)
                            .build()
                            .apply { flags = NotificationCompat.FLAG_INSISTENT }
                }
            }
        }
    }

    private fun getActionIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, MeditationTimerService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(context, 22, intent, FLAG_UPDATE_CURRENT)
    }

    private fun createNotificationBuilder(context: Context, notificationManager: NotificationManager): NotificationCompat.Builder {
        //if (notificationBuilder == null) {
        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 23, notificationIntent, FLAG_UPDATE_CURRENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createMeditatingChannel(context, notificationManager)
        }
        return NotificationCompat.Builder(context, CHANNEL_ID_MEDITATING)
                .setContentTitle("Start Meditating?")
                .setContentText("")
                .setSmallIcon(R.drawable.ic_launcher_bw)
                .setOngoing(true)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent)
                .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
                .setStyle(MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2)
                        .setMediaSession(createMediaSession(context).sessionToken)
                )
    }
    //return notificationBuilder!!

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createMeditatingChannel(context: Context, notificationManager: NotificationManager) {
        val channel = NotificationChannel(CHANNEL_ID_MEDITATING, context.getString(R.string.notification_channel_name_ongoing),
                NotificationManager.IMPORTANCE_LOW)
        // Configure the notification channel.
        channel.description = ""
        channel.enableLights(false)
        channel.enableVibration(false)
        channel.setShowBadge(false)
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        notificationManager.createNotificationChannel(channel)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createCompletedChannel(context: Context, notificationManager: NotificationManager) {
        val channel = NotificationChannel(CHANNEL_ID_COMPLETED, context.getString(R.string.notification_channel_name_completed),
                NotificationManager.IMPORTANCE_HIGH)
        // Configure the notification channel.
        channel.description = ""
        channel.setShowBadge(false)
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        notificationManager.createNotificationChannel(channel)
    }

    private fun createMediaSession(context: Context): MediaSessionCompat{
        return MediaSessionCompat(context, MEDIA_SESSION_ID).apply {
            .
        }
    }
}
