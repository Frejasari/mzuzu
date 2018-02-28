package de.sari.mzuzu

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.NumberPicker
import de.sari.commons.TimerState
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*

fun ImageButton.setImageDrawable(id: Int) {
    setImageDrawable(ContextCompat.getDrawable(this.context, id))
}

const val MEDITATION_TIMER_SETTINGS = "de.sari.mzuzu.meditation.timer.settings.exit.com"
const val MEDITATION_TIME = "de.sari.mzuzu.meditation.time.exit.com"

class MainActivity : AppCompatActivity(), NumberPicker.OnValueChangeListener {

    var binder: MeditationTimerService.Binder? = null
    private var timeDisposable: Disposable? = null
    private var stateDisposable: Disposable? = null
    private val serviceConnection = object : ServiceConnection {
//         onServiceConntected is always called when the activity binds to the service
//         in comparision onBind is only called the first time a client binds to the service
//          - the service connection channel is cached and the system returns the same binder to any further clients
        override fun onServiceConnected(className: ComponentName, iBinder: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalServices instance
            Log.i("sync", "onServiceConnected called")
            binder = iBinder as MeditationTimerService.Binder
            timeDisposable = getTimer()!!.timeObservable().subscribe { onTimerTick(it) }
            stateDisposable = getTimer()!!.stateObservable().subscribe { synchronizeInterface(it) }
        }

        override fun onServiceDisconnected(className: ComponentName) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timePicker.maxValue = 120
        timePicker.minValue = 1
        setSupportActionBar(toolbar)

        playButton.setOnClickListener {
            getTimer()!!.toggleTimer()
        }

        timePicker.setOnValueChangedListener(this)

        plusButton.setOnClickListener {
            getTimer()?.snooze(resources.getInteger(R.integer.snooze_duration))
        }

        stopButton.setOnClickListener {
            getTimer()?.stop()
        }
    }

    override fun onStart() {
        super.onStart()
        Log.i("sync", "onStart activity called")
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

    override fun onValueChange(picker: NumberPicker?, oldVal: Int, selectedMinutes: Int) {
        getTimer()?.setDuration(TimeUtils.toSeconds(selectedMinutes))
    }

    fun getTimer() = binder?.getTimer()

    private fun onTimerTick(secondsRemaining: Int) {
        val totalTime = getTimer()!!.getTotalTime()
        timeBar.max = totalTime
        timeBar.progress = secondsRemaining
        timePicker.value = TimeUtils.toMinutes(secondsRemaining)
    }

    private fun synchronizeInterface(state: TimerState) {
        Log.i("sync", "synchronizeInterface called, state: $state")
        with(playButton) {
            setImageDrawable(when (state) {
                TimerState.RUNNING -> R.drawable.ic_pause
                TimerState.COMPLETED -> R.drawable.ic_replay
                else -> R.drawable.ic_play
            })
        }
        with(plusButton) {
            isEnabled = (state != TimerState.STOPPED)
            visibility = when (state) {
                TimerState.STOPPED -> View.INVISIBLE
                else -> View.VISIBLE
            }
        }
        with(stopButton) {
            isEnabled = (state != TimerState.STOPPED)
            visibility = when (state) {
                TimerState.STOPPED -> View.INVISIBLE
                else -> View.VISIBLE
            }
        }
        with(toolbar) {
            subtitle = when (state) {
                TimerState.RUNNING -> getString(R.string.notification_running_state)
                TimerState.PAUSED -> getString(R.string.notification_paused_state)
                TimerState.STOPPED -> getString(R.string.notification_stopped_state)
                TimerState.COMPLETED -> getString(R.string.notification_meditate_again)
            }
        }
        timePicker.also {
            it.isEnabled = (state == TimerState.STOPPED || state == TimerState.COMPLETED)
            when (state) {
                TimerState.RUNNING -> it.setOnValueChangedListener(null)
                TimerState.PAUSED -> it.setOnValueChangedListener(null)
                else -> {
                    it.value = TimeUtils.toMinutes(getTimer()?.getTimerDuration()!!)
                    it.setOnValueChangedListener(this)
                }
            }
        }
    }
}