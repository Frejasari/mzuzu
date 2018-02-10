package de.sari.mzuzu

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.widget.ImageButton
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit


fun ImageButton.setImageDrawable(id: Int) {
    setImageDrawable(ContextCompat.getDrawable(this.context, id))
}

const val KEY_SECONDS_PASSED = "seconds passed"
const val KEY_IS_RUNNING = "meditation timer is running"
const val KEY_ADDED_SECONDS = "Added seconds"
const val KEY_ADDED_AFTER_COMPLETION = "added after completion"

enum class TimerState {
    RUNNING, PAUSED, STOPPED, COMPLETED
}

class MainActivity : AppCompatActivity() {
    var secondsPassed: Int = 0
    var addedSeconds: Int = 0
    var isAddedAfterCompletion = false
    var state = TimerState.STOPPED
        set(value) {
            field = value
            synchronizeInterface()
        }
    val timerObservable = Observable.interval(1, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .map { it.toInt() }
            .map {
                secondsPassed++
                getTimeRemaining()
            }
    var timerDisposable: Disposable? = null

    val music by lazy { resources.assets.openFd("music.mp3") }
    val player by lazy { MeditationMediaPlayer(music).apply { setOnCompletionListener { onMusicCompleted() } } }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        savedInstanceState?.apply {
            if (containsKey(KEY_IS_RUNNING)) {
//                isRunning = savedInstanceState.getBoolean(KEY_IS_RUNNING)
            }
            state = TimerState.values()[(savedInstanceState.getInt(KEY_IS_RUNNING))]
            secondsPassed = savedInstanceState.getInt(KEY_SECONDS_PASSED, 0)
            addedSeconds = savedInstanceState.getInt(KEY_ADDED_SECONDS, 0)
            isAddedAfterCompletion = savedInstanceState.getBoolean(KEY_ADDED_AFTER_COMPLETION, false)
        }

        timePicker.maxValue = 120
        timePicker.minValue = 5
        if (!isRunning()) timeBar.progress = 100
        if (state == TimerState.RUNNING) startTimer()
        synchronizeInterface()

        playButton.setOnClickListener {
            toggleTimer()
        }

        plusButton.setOnClickListener {
            addTime()
        }

        stopButton.setOnClickListener {
            when (state) {
                TimerState.COMPLETED -> stopMusic()
                else -> stopTimer()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, MeditationTimerService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        unbindService(serviceConnection)
    }

    var meditationTimerService: MeditationTimerService? = null
    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName,
                                        iBinder: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = iBinder as MeditationTimerService.MeditationTimerBinder
            meditationTimerService = binder.getService()
        }

        override fun onServiceDisconnected(className: ComponentName) {
        }
    }


    private fun toggleTimer() {
        if (state == TimerState.RUNNING) {
            pauseTimer()
        } else {
            if (state == TimerState.COMPLETED) stopTimer()
            startTimer()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.apply {
            putInt(KEY_IS_RUNNING, state.ordinal)
            putInt(KEY_SECONDS_PASSED, secondsPassed)
            putInt(KEY_ADDED_SECONDS, addedSeconds)
            putBoolean(KEY_ADDED_AFTER_COMPLETION, isAddedAfterCompletion)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerDisposable?.dispose()
    }

    override fun onPause() {
        super.onPause()
        stopMusic()
    }

    fun startTimer() {
        initTimer()
        state = TimerState.RUNNING
        if (timerDisposable == null || timerDisposable!!.isDisposed) {
            timerDisposable = timerObservable.subscribe {
                onTimerTick(it)
                if (it <= 0) onTimerCompleted()
            }
        }
    }

    fun initTimer() {
        timeBar.max = getMeditationSeconds()
        onTimerTick(getTimeRemaining())
    }

    fun onTimerTick(timeRemaining: Int) {
        textView.text = timeRemaining.toString()
        timeBar.progress = timeRemaining
    }

    fun pauseTimer() {
        state = TimerState.PAUSED
        pauseCountdown()
    }

    fun stopTimer() {
        isAddedAfterCompletion = false
        state = TimerState.STOPPED
        pauseCountdown()
        stopMusic()
        resetTimer()
    }

    fun resetTimer() {
        secondsPassed = 0
        addedSeconds = 0
    }

    fun pauseCountdown() {
        timerDisposable!!.dispose()
    }

    fun onTimerCompleted() {
        isAddedAfterCompletion = false
        state = TimerState.COMPLETED
        pauseCountdown()
        resetTimer()
        player.start()
    }

    fun addTime() {
        if (state == TimerState.COMPLETED) {
            stopMusic()
            isAddedAfterCompletion = true
        }
        addedSeconds += 10
        startTimer()
    }

    fun isRunning() = state == TimerState.RUNNING || state == TimerState.PAUSED

    fun getMeditationSeconds(): Int {
        return if (isAddedAfterCompletion) addedSeconds
        else timePicker.value + addedSeconds
    }

    fun getTimeRemaining() = getMeditationSeconds().minus(secondsPassed)

    //    ------------ Music -------------------

    fun stopMusic() {
        player.pause()
    }

    fun onMusicCompleted() {
        state = TimerState.STOPPED
    }

    private fun synchronizeInterface() {
        when (state) {
            TimerState.RUNNING -> playButton.setImageDrawable(R.drawable.ic_pause)
            else -> playButton.setImageDrawable(R.drawable.ic_play_arrow)
        }
        timePicker.isEnabled = state == TimerState.STOPPED
        stopButton.isEnabled = (state != TimerState.STOPPED)
    }
}