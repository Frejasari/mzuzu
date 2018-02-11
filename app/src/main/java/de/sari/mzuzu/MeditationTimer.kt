package de.sari.mzuzu

import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit


interface AbstractTimer {
    val stateSubject: BehaviorSubject<TimerState>
    val timeSubject: BehaviorSubject<Int>
    fun toggleTimer()
    fun stop()
    fun setDuration(duration: Int)
    fun snooze(duration: Int)
    fun getTimeRemaining(): Int
    fun isRunning(): Boolean
    fun getTotalTime(): Int
}

enum class TimerState {
    RUNNING, PAUSED, STOPPED, COMPLETED
}


class MeditationTimer : AbstractTimer {

    private var meditationTime: Int = 300
    private var secondsPassed: Int = 0
    private var addedSeconds: Int = 0

    private var state = TimerState.STOPPED
        set(value) {
            field = value
            onStateChangeEmitter.onNext(value)
        }

    private val timerObservable = Observable.interval(1, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .filter { state == TimerState.RUNNING }
            .map { it.toInt() }
            .map {
                secondsPassed++
                getTimeRemaining()
            }

    private val timeObservable: Observable<Int> = Observable.create<Int> { emitter -> timeEmitter = emitter }
    private lateinit var timeEmitter: ObservableEmitter<Int> //Emitter gehoert zum Observable
    private var timerDisposable: Disposable? = null
    override val timeSubject: BehaviorSubject<Int> = BehaviorSubject.create<Int>().apply { timeObservable.subscribe(this) }

    private lateinit var onStateChangeEmitter: ObservableEmitter<TimerState>
    private val stateObservable = Observable.create<TimerState> { emitter -> onStateChangeEmitter = emitter }

    override val stateSubject: BehaviorSubject<TimerState> = BehaviorSubject.create<TimerState>().apply { stateObservable.subscribe(this) }

    private fun start() {
        state = TimerState.RUNNING
        if (!timerRunning()) {
            timeEmitter.onNext(getTimeRemaining())
            timerDisposable = timerObservable.subscribe {
                timeEmitter.onNext(it)
                if (it <= 0) onCompleted()
            }
        }
    }

    private fun pause() {
        state = TimerState.PAUSED
    }

    override fun toggleTimer() {
        if (state == TimerState.RUNNING) {
            pause()
        } else {
            if (state == TimerState.COMPLETED) stop()
            start()
        }
    }

    override fun stop() {
        state = TimerState.STOPPED
        if (timerRunning()) timerDisposable!!.dispose()
        resetTimer()
    }

    private fun onCompleted() {
        state = TimerState.COMPLETED
        resetTimer()
    }

    override fun snooze(duration: Int) {
        addedSeconds += duration
        if (state != TimerState.PAUSED) state = TimerState.RUNNING
        timeEmitter.onNext(getTimeRemaining())
    }

    override fun isRunning() = state == TimerState.RUNNING || state == TimerState.PAUSED

    override fun setDuration(duration: Int) {
        meditationTime = duration
    }

    private fun resetTimer() {
        setDuration(0)
        secondsPassed = 0
        addedSeconds = 0
        timeEmitter.onNext(0)
    }

    private fun timerRunning() = timerDisposable != null && !timerDisposable!!.isDisposed

    override fun getTotalTime() = meditationTime + addedSeconds

    override fun getTimeRemaining() = getTotalTime().minus(secondsPassed)
}