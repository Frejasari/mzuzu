package de.sari.mzuzu

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.widget.RemoteViews
import android.widget.Toast
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Observables

const val NOTIFICATION_ID = 8890

class MeditationTimerService : Service() {

    private val timer: AbstractTimer = MeditationTimer()
    private val music by lazy { resources.assets.openFd("music.mp3") }
    private lateinit var mediaPlayer: MeditationMediaPlayer
    private var timerStateDisposable: Disposable? = null
    private var timeDisposable: Disposable? = null
    private var timerDataDisposable: Disposable? = null
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private var notificationBuilder by lazy { MeditationNotification.getNotificationBuilder(this) }


    inner class Binder : android.os.Binder() {
        fun getTimer() = timer
    }

    private var notificationDisposable: Disposable? = null

    override fun onCreate() {
        super.onCreate()
        mediaPlayer = MeditationMediaPlayer(music)

        val timerDataObservable = Observables
                .combineLatest(timer.stateSubject, timer.timeSubject
//                        .filter { timeRemaining -> timeRemaining % 60 == 0 }
                ) { timerState, remainingTime ->
                    TimerData(timerState, remainingTime)
                }

        timerDataDisposable = timerDataObservable.subscribe { timerData ->
            when (timerData.state) {
                TimerState.COMPLETED -> mediaPlayer.start()
                else -> mediaPlayer.pause()
            }
            updateNotification(timerData.state, timerData.remainingSeconds)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timeDisposable?.dispose()
        timerStateDisposable?.dispose()
        timerDataDisposable?.dispose()
        mediaPlayer.release()
    }

    // A client is binding to the service with bindService()
    override fun onBind(intent: Intent?): IBinder {
        return Binder()
    }

    // The service is starting, due to a call to startService()
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pendingIntent = PendingIntent.getActivity(this, 13, Intent(this, MainActivity::class.java), FLAG_UPDATE_CURRENT)
//        val playPendingIntent = PendingIntent.getService(this, )
        return START_STICKY
    }

    private fun updateNotification(status: TimerState, remainingMinutes: Int) {
        if (status == TimerState.RUNNING || status == TimerState.PAUSED) {
            notificationBuilder
                    .setContentText("remaining meditation time: $remainingMinutes minutes")
                    .setContentTitle(status.toString())
            var notification = notificationBuilder.build()
            when (status) {
                TimerState.RUNNING ->
            }
            if (status == TimerState.STOPPED) stopForeground(true)
            else startForeground(NOTIFICATION_ID, notificationBuilder.build())

        }
    }

    private fun updatePlayer(intent: Intent){
        when {
            intent.action == ACTION_PLAY -> {
                Toast.makeText(this, "Clicked Play", Toast.LENGTH_SHORT).show()
            }
            intent.action == ACTION_PAUSE -> {
                Toast.makeText(this, "Clicked Pause", Toast.LENGTH_SHORT).show()
            }
            intent.action == ACTION_ADD -> {
                Toast.makeText(this, "Clicked Add", Toast.LENGTH_SHORT).show()
            }
            intent.action == ACTION_REPEAT -> {
                Toast.makeText(this, "Clicked Repeat", Toast.LENGTH_SHORT).show()
            }
            intent.action == ACTION_STOP -> {
                Toast.makeText(this, "Clicked Stop", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun configureNotification(status: TimerState, remainingMinutes: Int) {
        val views = RemoteViews(packageName, R.layout.med_notification)
        var notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.action = ACTION_MAIN
        notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK; Intent.FLAG_ACTIVITY_CLEAR_TASK
        var pendingIntent = PendingIntent.getService(this, 0, notificationIntent, 0)

        var playIntent = Intent(this, NotificationIntentService::class.java).apply {
            action = ACTION_PLAY
        }
        var pPlayIntent = PendingIntent.getService(this, 0, playIntent, 0)
        var pauseIntent = Intent(this, NotificationIntentService::class.java).apply {
            action = ACTION_PAUSE
        }
        var pPauseIntent = PendingIntent.getService(this, 0, pauseIntent, 0)

        var addIntent = Intent(this, NotificationIntentService::class.java).apply {
            action = ACTION_ADD
        }
        var pAddIntent = PendingIntent.getService(this, 0, addIntent, 0)

        var repeatIntent = Intent(this, NotificationIntentService::class.java).apply {
            action = ACTION_REPEAT
        }
        var pRepeatIntent = PendingIntent.getService(this, 0, repeatIntent, 0)

        var stopIntent = Intent(this, NotificationIntentService::class.java).apply {
            action = ACTION_STOP
        }
        var pStopIntent = PendingIntent.getService(this, 0, stopIntent, 0)
        views.apply {
            //            setOnClickPendingIntent(playButton, pPlayIntent)
            setImageViewResource(R.id.playButton, R.drawable.ic_play_arrow)
            setImageViewResource(R.id.stopButton, R.drawable.ic_stop)
            setImageViewResource(R.id.plusButton, R.drawable.ic_exposure_plus_2)
            setImageViewResource(R.id.appIcon, R.drawable.ic_launcher)
            setOnClickPendingIntent(R.id.playButton, pPauseIntent)
            setOnClickPendingIntent(R.id.stopButton, pStopIntent)
            setOnClickPendingIntent(R.id.plusButton, pAddIntent)
//            setOnClickPendingIntent(playButton, pPlayIntent)
//            setOnClickPendingIntent(playButton, pPlayIntent)
        }

        var status = NotificationCompat.Builder(this, "TEST ID ")
                .setContentTitle("HELLO TEST")
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentText("Text test - meditating!")
                .setCustomContentView(views)
                .setContentIntent(pendingIntent)
                .build()
                .apply { flags = NotificationCompat.FLAG_ONGOING_EVENT }

        startForeground(NOTIFICATION_ID_FOREGROUND_SERVICE, status)
    }
}