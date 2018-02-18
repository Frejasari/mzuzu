package de.sari.mzuzu

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Observables

const val NOTIFICATION_ID = 8890

class MeditationTimerService : Service() {

    private val timer: AbstractTimer = MeditationTimer()
    private val music by lazy { resources.assets.openFd("music.mp3") }
    private lateinit var mediaPlayer: MeditationMediaPlayer
    private var timerDataDisposable: Disposable? = null
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    inner class Binder : android.os.Binder() {
        fun getTimer() = timer
    }

    @SuppressLint("NewApi")
    override fun onCreate() {
        super.onCreate()
        mediaPlayer = MeditationMediaPlayer(music)
        val timerDataObservable = Observables
                .combineLatest(timer.stateSubject, timer.timeSubject
                ) { timerState, remainingSeconds ->
                    TimerData(timerState, remainingSeconds)
                }

        timerDataDisposable = timerDataObservable.subscribe { timerData ->
            Log.i("Notification", "TimerDataDisposable onNext called: state: ${timerData.state}")
            when (timerData.state) {
                TimerState.COMPLETED -> mediaPlayer.start()
                else -> mediaPlayer.pause()
            }
            updateNotification(timerData.state, timerData.remainingSeconds)
        }
    }

    private fun updateNotification(state: TimerState, remainingSeconds: Int) {
        val notification = MeditationNotification.getNotification(state, TimeUtils.toMinutes(remainingSeconds), this)
        if (state == TimerState.STOPPED) {
            stopForeground(false)
            notificationManager.notify(NOTIFICATION_ID, notification)
        } else startForeground(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        timerDataDisposable?.dispose()
        mediaPlayer.release()
    }

    // A client is binding to the service with bindService()
    override fun onBind(intent: Intent?): IBinder {
        return Binder()
    }

    // The service is starting, due to a call to startService()
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        updateTimer(intent)
        return START_NOT_STICKY
    }

    private fun updateTimer(intent: Intent) {
        Log.i("Notification", "Update Timer called, action: $intent.action")

        when (intent.action) {
            ACTION_PLAY -> {
                timer.toggleTimer()
            }
            ACTION_PAUSE -> {
                timer.toggleTimer()
            }
            ACTION_ADD -> {
                timer.snooze(resources.getInteger(R.integer.snooze_duration))
            }
            ACTION_REPEAT -> {
                timer.toggleTimer()
            }
            ACTION_STOP -> {
                timer.stop()
            }
        }
    }
}