package de.sari.mzuzu

import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit


interface AbstractTimer {
    val stateSubject: BehaviorSubject<TimerState>
    val timeSubject: BehaviorSubject<Int>
    val timerDataObservable: Observable<TimerData>
    fun toggleTimer()
    fun stop()
    fun setDuration(duration: Int)
    fun snooze(duration: Int)
    fun getSecondsRemaining(): Int
    fun getTotalTime(): Int
}

enum class TimerState {
    RUNNING, PAUSED, STOPPED, COMPLETED
}

data class TimerData(var state: TimerState = TimerState.STOPPED, var remainingSeconds: Int = 0)


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
                getSecondsRemaining()
            }

    private lateinit var timeEmitter: ObservableEmitter<Int> //Emitter gehoert zum Observable
    private val timeObservable: Observable<Int> = Observable.create<Int> { emitter -> timeEmitter = emitter }
    private var timerDisposable: Disposable? = null
    override val timeSubject: BehaviorSubject<Int> = BehaviorSubject.create<Int>().apply { timeObservable.subscribe(this) }

    private lateinit var onStateChangeEmitter: ObservableEmitter<TimerState>
    private val stateObservable = Observable.create<TimerState> { emitter -> onStateChangeEmitter = emitter }
    override val stateSubject: BehaviorSubject<TimerState> = BehaviorSubject.create<TimerState>().apply { stateObservable.subscribe(this) }

    override val timerDataObservable = Observables.combineLatest(stateSubject, timeSubject) { timerState, remainingSeconds ->
        TimerData(timerState, remainingSeconds)
    }

    private fun start() {
        state = TimerState.RUNNING
        initTime()
        if (!timerRunning()) {
            timerDisposable = timerObservable.subscribe { remainingSeconds ->
                timeEmitter.onNext(remainingSeconds)
                if (remainingSeconds <= 0) onCompleted()
            }
        }
    }

    private fun pause() {
        state = TimerState.PAUSED
    }

    override fun toggleTimer() {
        if (state == TimerState.RUNNING) pause()
        else start()
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
        if (state != TimerState.PAUSED) {
            state = TimerState.RUNNING
        }
        initTime()
    }

    override fun setDuration(duration: Int) {
        meditationTime = duration
    }

    override fun getTotalTime() = meditationTime.plus(addedSeconds)

    override fun getSecondsRemaining() = getTotalTime().minus(secondsPassed)

    private fun resetTimer() {
        setDuration(0)
        secondsPassed = 0
        addedSeconds = 0
        initTime()
    }

    private fun initTime() {
        timeEmitter.onNext(getSecondsRemaining())
    }

    private fun timerRunning() = timerDisposable != null && !timerDisposable!!.isDisposed
}