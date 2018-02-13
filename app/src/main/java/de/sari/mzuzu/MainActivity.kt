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
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*


fun ImageButton.setImageDrawable(id: Int) {
    setImageDrawable(ContextCompat.getDrawable(this.context, id))
}

class MainActivity : AppCompatActivity() {
    var binder: MeditationTimerService.Binder? = null
    private var timeDisposable: Disposable? = null
    private var stateDisposable: Disposable? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, iBinder: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
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
        timePicker.maxValue = 120
        timePicker.minValue = 5

        playButton.setOnClickListener {
            getTimer()!!.setDuration(timePicker.value)
            getTimer()!!.toggleTimer()
        }

        timePicker.setOnValueChangedListener { picker, oldVal, selectedMinutes -> getTimer()?.setDuration(selectedMinutes) }

        plusButton.setOnClickListener {
            getTimer()?.snooze(10)
        }

        stopButton.setOnClickListener {
            getTimer()?.stop()
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, MeditationTimerService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_ABOVE_CLIENT)
    }

    override fun onStop() {
        super.onStop()
        timeDisposable?.dispose()
        stateDisposable?.dispose()
        unbindService(serviceConnection)
    }

    fun getTimer() = binder?.getTimer()

    private fun onTimerTick(timeRemaining: Int) {
        timeBar.max = getTimer()!!.getTotalTime()
        timePicker.value
        textView.text = timeRemaining.toString()
        timeBar.progress = timeRemaining
    }

    private fun synchronizeInterface(state: TimerState) {
        with(playButton) {
            setImageDrawable(when (state) {
                TimerState.RUNNING -> R.drawable.ic_pause
                TimerState.COMPLETED -> R.drawable.ic_replay
                else -> R.drawable.ic_play_arrow
            })
        }
        plusButton.isEnabled = (state != TimerState.STOPPED)
        timePicker.isEnabled = state == TimerState.STOPPED
        stopButton.isEnabled = (state != TimerState.STOPPED)
    }
}