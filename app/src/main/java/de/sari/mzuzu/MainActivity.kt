package de.sari.mzuzu

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.os.Bundle
import android.os.IBinder
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.ImageButton
import de.sari.commons.TimerState
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*

fun ImageButton.setImageDrawable(id: Int) {
    setImageDrawable(ContextCompat.getDrawable(this.context, id))
}

const val MEDITATION_TIMER_SETTINGS = "de.sari.mzuzu.meditation.timer.settings.exit.com"
const val MEDITATION_TIME = "de.sari.mzuzu.meditation.time.exit.com"

class MainActivity : AppCompatActivity() {
    private lateinit var exitPunktCom: SharedPreferences
    var binder: MeditationTimerService.Binder? = null
    private var timeDisposable: Disposable? = null
    private var stateDisposable: Disposable? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, iBinder: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalServices instance
            binder = iBinder as MeditationTimerService.Binder
            timeDisposable = getTimer()!!.timeSubject.subscribe { onTimerTick(it) }
            stateDisposable = getTimer()!!.stateSubject.subscribe { synchronizeInterface(it) }
        }

        override fun onServiceDisconnected(className: ComponentName) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        exitPunktCom = getSharedPreferences(MEDITATION_TIMER_SETTINGS, Context.MODE_PRIVATE)

        timePicker.maxValue = 120
        timePicker.minValue = 1
        timePicker.value = (exitPunktCom.getInt(MEDITATION_TIME, 300)) / 60
        setSupportActionBar(toolbar)

        playButton.setOnClickListener {
            val time = timePicker.value * 60
//            exitPunktCom.edit().putInt(MEDITATION_TIME, time).apply()
            getTimer()!!.setDuration(time)
            getTimer()!!.toggleTimer()
        }

        timePicker.setOnValueChangedListener { picker, oldVal, selectedMinutes -> getTimer()?.setDuration(selectedMinutes * 60) }

        plusButton.setOnClickListener {
            getTimer()?.snooze(resources.getInteger(R.integer.snooze_duration))
        }

        stopButton.setOnClickListener {
            getTimer()?.stop()
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = getMeditationTimerServiceIntent(this)
        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_ABOVE_CLIENT)
    }

    override fun onStop() {
        super.onStop()
        timeDisposable?.dispose()
        stateDisposable?.dispose()
        unbindService(serviceConnection)
    }

    override fun onDestroy() {
        super.onDestroy()
        exitPunktCom.edit().putInt(MEDITATION_TIME, timePicker.value * 60).apply()
    }

    fun getTimer() = binder?.getTimer()

    private fun onTimerTick(secondsRemaining: Int) {
        val totalTime = getTimer()!!.getTotalTime()
        timeBar.max = totalTime
        timeBar.progress = secondsRemaining
        toolbar.subtitle = getString(R.string.notification_remaining_time, TimeUtils.toMinutes(secondsRemaining))
        Log.i("timertick", "time remaining: $secondsRemaining")
    }

    private fun synchronizeInterface(state: TimerState) {
        with(playButton) {
            setImageDrawable(when (state) {
                TimerState.RUNNING -> R.drawable.ic_pause
                TimerState.COMPLETED -> R.drawable.ic_replay
                else -> R.drawable.ic_play
            })
        }
        with(toolbar) {
            if (state == TimerState.STOPPED) subtitle = null
            if (state == TimerState.COMPLETED) subtitle = getString(R.string.notification_meditate_again)
        }
        if (state == TimerState.STOPPED) {
            plusButton.visibility = View.INVISIBLE
            stopButton.visibility = View.INVISIBLE
        } else {
            plusButton.visibility = View.VISIBLE
            stopButton.visibility = View.VISIBLE
        }
        plusButton.isEnabled = (state != TimerState.STOPPED)
        timePicker.isEnabled = (state == TimerState.STOPPED || state == TimerState.COMPLETED)
        stopButton.isEnabled = (state != TimerState.STOPPED)
    }
}