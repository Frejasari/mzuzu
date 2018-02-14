package de.sari.mzuzu

import android.app.Notification
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationCompat.PRIORITY_MAX
import android.support.v4.media.app.NotificationCompat.MediaStyle
import android.util.Log


const val CHANNEL_ID_MEDITATING = "Meditation Channel ID"
const val ACTION_ADD = "Action add"
const val ACTION_PAUSE = "Action pause"
const val ACTION_PLAY = "Action play"
const val ACTION_REPEAT = "Action repeat"
const val ACTION_STOP = "Action stop"
const val ACTION_ENABLE = "Action enable"
const val CHANNEL_ID_COMPLETED = "notification channel for completion"

//fun NotificationCompat.Builder.addAction(context: Context, icon : Int, title: Int, pendingIntent: PendingIntent){
//    NotificationCompat.Builder.addAction
//}
object MeditationNotification {


    fun getNotification(status: TimerState, remainingMinutes: Int, context: Context): Notification {
        Log.i("Notification", "Notification Update, state: $status")
        val notificationBuilder = getNotificationBuilder(context).setContentTitle(status.toString())
        return when (status) {
            TimerState.RUNNING -> {
                notificationBuilder
                        .setContentText(context.getString(R.string.notification_remaining_time, remainingMinutes))
                        .addAction(R.drawable.ic_stop, context.getString(R.string.action_stop), getActionIntent(context, ACTION_STOP))
                        .addAction(R.drawable.ic_pause, context.getString(R.string.action_pause), getActionIntent(context, ACTION_PAUSE))
                        .addAction(R.drawable.ic_add_2, context.getString(R.string.action_add), getActionIntent(context, ACTION_ADD))
                        .build()
                        .apply { flags = NotificationCompat.FLAG_ONGOING_EVENT }
            }
            TimerState.PAUSED -> {
                notificationBuilder
                        .setContentText(context.getString(R.string.notification_remaining_time, remainingMinutes))
                        .addAction(R.drawable.ic_stop, context.getString(R.string.action_stop), getActionIntent(context, ACTION_STOP))
                        .addAction(R.drawable.ic_play, context.getString(R.string.action_play), getActionIntent(context, ACTION_PLAY))
                        .addAction(R.drawable.ic_add_2, context.getString(R.string.action_add), getActionIntent(context, ACTION_ADD))
                        .build()
                        .apply { flags = NotificationCompat.FLAG_ONGOING_EVENT }
            }
            TimerState.STOPPED -> {
                notificationBuilder
                        .setContentText(context.getString(R.string.notification_start_meditation))
                        .addAction(R.drawable.ic_stop, context.getString(R.string.action_stop), getActionIntent(context, ACTION_ENABLE))
                        .addAction(R.drawable.ic_play, context.getString(R.string.action_play), getActionIntent(context, ACTION_PLAY))
                        .addAction(R.drawable.ic_add_2, context.getString(R.string.action_add), getActionIntent(context, ACTION_ENABLE))
                        .setOngoing(false)
                        .build()
            }
            TimerState.COMPLETED -> {
                notificationBuilder
                        .setContentText(context.getString(R.string.notification_meditate_again))
                        .addAction(R.drawable.ic_stop, context.getString(R.string.action_stop), getActionIntent(context, ACTION_STOP))
                        .addAction(R.drawable.ic_replay, context.getString(R.string.action_play), getActionIntent(context, ACTION_REPEAT))
                        .addAction(R.drawable.ic_add_2, context.getString(R.string.action_add), getActionIntent(context, ACTION_ADD))
                        .setPriority(PRIORITY_MAX)
                        .setChannelId(CHANNEL_ID_COMPLETED)
                        .build()
                        .apply { flags = NotificationCompat.FLAG_INSISTENT }
            }
        }
    }

    private fun getActionIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, MeditationTimerService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(context, 22, intent, FLAG_UPDATE_CURRENT)
    }

    private fun getNotificationBuilder(context: Context): NotificationCompat.Builder {
        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 23, notificationIntent, FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(context, "CHANNEL_ID_MEDITATING")
                .setContentTitle("Start Meditating?")
                .setContentText("")
                .setSmallIcon(R.drawable.ic_launcher_bw)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent)
                .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
                .setStyle(MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2)
                )
    }
}
