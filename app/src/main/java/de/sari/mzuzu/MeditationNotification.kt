package de.sari.mzuzu

import android.app.Notification
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import android.util.Log

/**
 * Created by sari on 11.02.18.
 */
const val ID_MEDITATION_CHANNEL = "Meditation Channel ID"
const val ACTION_ADD = "Action add"
const val ACTION_PAUSE = "Action pause"
const val ACTION_PLAY = "Action play"
const val ACTION_REPEAT = "Action repeat"
const val ACTION_STOP = "Action stop"
const val ACTION_ENABLE = "Action enable"


class MeditationNotification : NotificationCompat() {

    companion object {

        fun getNotification(status: TimerState, remainingMinutes: Int, context: Context): Notification {
            Log.i("Notification", "Notification Update, state: $status")
            val notificationBuilder = getNotificationBuilder(context).setContentTitle(status.toString())
            return when (status) {
                TimerState.RUNNING -> {
                    notificationBuilder
                            .setContentText("remaining meditation time: $remainingMinutes minutes")
                            .addAction(R.drawable.ic_stop, "", getActionIntent(context, ACTION_STOP))
                            .addAction(R.drawable.ic_pause, "", getActionIntent(context, ACTION_PAUSE))
                            .addAction(R.drawable.ic_add_2, "", getActionIntent(context, ACTION_ADD))
                            .build()
                            .apply { flags = NotificationCompat.FLAG_ONGOING_EVENT }
                }
                TimerState.PAUSED -> {
                    notificationBuilder
                            .setContentText("remaining meditation time: $remainingMinutes minutes")
                            .addAction(R.drawable.ic_stop, "", getActionIntent(context, ACTION_STOP))
                            .addAction(R.drawable.ic_play, "", getActionIntent(context, ACTION_PLAY))
                            .addAction(R.drawable.ic_add_2, "", getActionIntent(context, ACTION_ADD))
                            .build()
                            .apply { flags = NotificationCompat.FLAG_ONGOING_EVENT }
                }
                TimerState.STOPPED -> {
                    notificationBuilder
                            .setContentText("Start meditating?")
                            .addAction(R.drawable.ic_stop, "", getActionIntent(context, ACTION_ENABLE))
                            .addAction(R.drawable.ic_play, "", getActionIntent(context, ACTION_PLAY))
                            .addAction(R.drawable.ic_add_2, "", getActionIntent(context, ACTION_ENABLE))
                            .build()
                }
                TimerState.COMPLETED -> {
                    notificationBuilder
                            .setContentText("Start meditating?")
                            .addAction(R.drawable.ic_stop, "", getActionIntent(context, ACTION_STOP))
                            .addAction(R.drawable.ic_replay, "", getActionIntent(context, ACTION_REPEAT))
                            .addAction(R.drawable.ic_add_2, "", getActionIntent(context, ACTION_ADD))
                            .build()
                            .apply { flags = NotificationCompat.FLAG_ONGOING_EVENT }
                }
            }
        }

        private fun getActionIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(context, MeditationTimerService::class.java).apply {
                this.action = action
            }
            return PendingIntent.getService(context, 0, intent, FLAG_UPDATE_CURRENT)
        }

        private fun getNotificationBuilder(context: Context): NotificationCompat.Builder {
            val notificationIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, FLAG_UPDATE_CURRENT)
            return NotificationCompat.Builder(context, "YOOOO ")
                    .setContentTitle("Start Meditating?")
                    .setSmallIcon(R.drawable.ic_launcher_bw)
                    .setOngoing(true)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setContentIntent(pendingIntent)
        }
    }
}