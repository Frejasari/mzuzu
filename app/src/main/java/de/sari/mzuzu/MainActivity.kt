package de.sari.mzuzu

import android.animation.TimeAnimator
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
import kotlin.math.roundToInt

fun ImageButton.setImageDrawable(id: Int) {
    setImageDrawable(ContextCompat.getDrawable(this.context, id))
}

const val MEDITATION_TIMER_SETTINGS = "de.sari.mzuzu.meditation.timer.settings.exit.com"
const val MEDITATION_TIME = "de.sari.mzuzu.meditation.time.exit.com"

class MainActivity : AppCompatActivity(), NumberPicker.OnValueChangeListener {
    var selectedMinutes = 5
    private val anim = TimeAnimator()
    var binder: MeditationTimerService.Binder? = null
    private var timeDisposable: Disposable? = null
    private var stateDisposable: Disposable? = null
    private var firstDisposable: Disposable? = null
    private val serviceConnection = object : ServiceConnection {
        //         onServiceConnected is always called when the activity binds to the service
//         in comparision onBind is only called the first time a client binds to the service
//          - the service connection channel is cached and the system returns the same binder to any further clients
        override fun onServiceConnected(className: ComponentName, iBinder: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalServices instance
            Log.i("sync", "onServiceConnected called")
            binder = iBinder as MeditationTimerService.Binder
            timeDisposable = getTimer()!!.timeObservable().subscribe { onTimerTick(TimeUtils.millisToSeconds(it)) }
            firstDisposable = getTimer()!!.timeObservable().firstElement().subscribe { initSanduhr(TimeUtils.millisToSeconds(it)) }
            stateDisposable = getTimer()!!.stateObservable().subscribe { synchronizeInterface(it) }
        }

        override fun onServiceDisconnected(className: ComponentName) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        playButton.setOnClickListener {
            getTimer()!!.toggleTimer()
        }

        plusButton.setOnClickListener {
            getTimer()?.snooze(resources.getInteger(R.integer.snooze_duration))
        }

        stopButton.setOnClickListener {
            getTimer()?.stop()
        }
        sanduhrView.stopWithFullCircle = false
        sanduhrView.setRotationListener(object : SanduhrView.OnRotationListener {
            override fun onRotation(arc: Float) {
                val newSelectedMinutes: Int = Math.max(resources.getInteger(R.integer.timer_maximum_in_minutes) * arc.roundToInt() / 360, 1)
                if (newSelectedMinutes != selectedMinutes) {
                    selectedMinutes = newSelectedMinutes
                    getTimer()?.setDuration(TimeUtils.minToMillis(selectedMinutes))
                }
            }
        })

        anim.setTimeListener { animation, totalTime, deltaTime ->
            val percentage: Float = getTimer()?.run { getRemainingMillis().toFloat() / getTotalMillis().toFloat() }
                    ?: 1F
            sanduhrView.setFillPercentage(percentage)
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
        firstDisposable?.dispose()
        unbindService(serviceConnection)
    }

    override fun onValueChange(picker: NumberPicker?, oldVal: Int, selectedMinutes: Int) {
        getTimer()?.setDuration(TimeUtils.minToMillis(selectedMinutes))
    }

    fun getTimer() = binder?.getTimer()

    private fun initSanduhr(secondsRemaining: Int) {
        val totalTime = TimeUtils.millisToSeconds(getTimer()!!.getTotalMillis())
        sanduhrView.setPercentageOfBigCircle(totalTime.toFloat() / (resources.getInteger(R.integer.timer_maximum_in_minutes).toFloat() * 60))
    }

    private fun onTimerTick(secondsRemaining: Int) {

        val totalTime = TimeUtils.millisToSeconds(getTimer()!!.getTotalMillis())
        timeBar.max = totalTime
        timeBar.progress = secondsRemaining
        sanduhrView.setText("${TimeUtils.secondsToMinutes(secondsRemaining)}")
    }

    private fun synchronizeInterface(state: TimerState) {
        Log.i("sync", "synchronizeInterface called, state: $state")
        if (state == TimerState.RUNNING) anim.start()
        else anim.cancel()

        with(sanduhrView) {
            shouldInterceptTouch = when (state) {
                TimerState.COMPLETED -> {
                    setFillPercentage(0F)
                    setText("0")
                    true
                }
                TimerState.STOPPED -> {
                    setFillPercentage(0F)
                    val totalTime = TimeUtils.millisToMinutes(getTimer()!!.getTotalMillis())
                    setText("$totalTime")
                    true
                }
                else -> false
            }
        }

        with(timeBar) {
            if (state == TimerState.STOPPED) {
                val timerDuration = TimeUtils.millisToSeconds(getTimer()!!.getSelectedMillis())
                timeBar.progress = timerDuration
                timeBar.max = timerDuration
            }
        }
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
    }
}

