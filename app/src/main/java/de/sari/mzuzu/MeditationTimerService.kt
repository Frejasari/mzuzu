package de.sari.mzuzu

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import io.reactivex.disposables.Disposable


class MeditationTimerService : Service() {

    val timer: AbstractTimer = MeditationTimer()
    private val music by lazy { resources.assets.openFd("music.mp3") }
    lateinit var mediaPlayer: MeditationMediaPlayer
    private var musicDisposable: Disposable? = null

    inner class Binder : android.os.Binder() {
        fun getTimer() = timer
    }

    override fun onCreate() {
        super.onCreate()
        mediaPlayer = MeditationMediaPlayer(music)
        musicDisposable = timer.stateSubject.subscribe {
            when (it) {
                TimerState.COMPLETED -> mediaPlayer.start()
                else -> mediaPlayer.pause()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        musicDisposable?.dispose()
    }

    override fun onBind(intent: Intent?): IBinder {
        return Binder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pendingIntent = PendingIntent.getActivity(this, 13, Intent(this, MainActivity::class.java), FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(applicationContext, "YOOOO ")
                .setContentTitle("Meditating")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build()
        startForeground(12, notification)
        return START_STICKY
    }
}