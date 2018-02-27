package de.sari.mzuzu

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.support.v4.content.ContextCompat
import android.util.Log
import de.sari.commons.AbstractTimer
import de.sari.commons.MeditationTimer
import de.sari.commons.TimerData
import de.sari.commons.TimerState
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Observables

const val NOTIFICATION_ID = 8890

fun getMeditationTimerServiceIntent(context: Context) = Intent(context, MeditationTimerService::class.java)

class MeditationTimerService : Service() {

    private lateinit var sharedPreferences: SharedPreferences

    private val timer: AbstractTimer = MeditationTimer()
    private val music by lazy { resources.assets.openFd("music.mp3") }
    private lateinit var mediaPlayer: MeditationMediaPlayer
    private var timerDataDisposable: Disposable? = null
    private var timeSelectedDisposable: Disposable? = null
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    inner class Binder : android.os.Binder() {
        fun getTimer() = timer
    }

    @SuppressLint("NewApi")
    override fun onCreate() {
        super.onCreate()
        Log.i("sync", "onCreate service called")
        sharedPreferences = getSharedPreferences(MEDITATION_TIMER_SETTINGS, Context.MODE_PRIVATE)
        timer.setDuration(sharedPreferences.getInt(MEDITATION_TIME, 300))
        mediaPlayer = MeditationMediaPlayer(music)

        val timerDataObservable = Observables
                .combineLatest(timer.stateSubject, timer.timeSubject
                ) { timerState, remainingSeconds ->
                    TimerData(timerState, remainingSeconds)
                }

        timeSelectedDisposable = timer.timeSelectedObservable.subscribe { seconds ->
            Log.i("sync", "timeSelectedDisposable $seconds s")
            sharedPreferences.edit().putInt(MEDITATION_TIME, seconds).apply()
        }

        timerDataDisposable = timerDataObservable.subscribe { timerData ->
            //            Log.i("service", "TimerDataDisposable onNext called: state: ${timerData.state}, remaining seconds: ${timerData.remainingSeconds}")
            when (timerData.state) {
                TimerState.COMPLETED -> mediaPlayer.start()
                else -> mediaPlayer.pause()
            }
            updateNotification(timerData.state, timerData.remainingSeconds)
        }
    }

    // A client is binding to the service with bindService()
// the system caches the IBinder service communication channel. In other words,
// the system calls the service's onBind() method to generate the IBinder only when the first client binds.
// The system then delivers that same IBinder to all additional clients that bind to that same service,
// without calling onBind() again.
    override fun onBind(intent: Intent?): IBinder {
        Log.i("sync", "onBind called")
        return Binder()
    }

    // The service is starting, due to a call to startService()
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        updateTimer(intent)
        return START_NOT_STICKY
    }

    override fun onDestroy() { // todo never called?
        super.onDestroy()
        notificationManager.cancel(NOTIFICATION_ID)
        timerDataDisposable?.dispose()
        timeSelectedDisposable?.dispose()
        mediaPlayer.release()
    }

    private fun updateNotification(state: TimerState, remainingSeconds: Int) {
        val notification = MeditationNotification.getNotification(state, TimeUtils.toMinutes(remainingSeconds), this, notificationManager)
        if (state == TimerState.STOPPED || state == TimerState.PAUSED) {
            stopForeground(false)
            notificationManager.notify(NOTIFICATION_ID, notification)
        } else {
            ContextCompat.startForegroundService(this, getMeditationTimerServiceIntent(this))
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateTimer(intent: Intent) {
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