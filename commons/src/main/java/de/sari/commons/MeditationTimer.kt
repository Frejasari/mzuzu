package de.sari.commons

import android.util.Log
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import org.threeten.bp.Duration
import org.threeten.bp.LocalDateTime
import java.util.concurrent.TimeUnit


interface AbstractTimer {
    fun stateObservable(): Observable<TimerState>
    fun timeObservable(): Observable<Int>
    fun timerDataObservable(): Observable<TimerData>
    fun timeSelectedObservable(): Observable<Int>
    fun toggleTimer()
    fun stop()
    fun setDuration(seconds: Int)
    fun snooze(seconds: Int)
    fun getSecondsRemaining(): Int
    fun getTotalTime(): Int
    fun getTimerDuration(): Int
    fun getMeditationTime(): Single<Int>
}

enum class TimerState {
    RUNNING, PAUSED, STOPPED, COMPLETED
}

data class TimerData(var state: TimerState = TimerState.STOPPED, var remainingSeconds: Int = 0)


class MeditationTimer : AbstractTimer {

    private var meditationTime: Int = 5
    private var addedSeconds: Int = 0
    private var startTime: LocalDateTime? = null
    private var pauseStartTime: LocalDateTime? = null
    private var pauseEndTime: LocalDateTime? = null
    private var pausedSeconds: Int = 0

    private var state = TimerState.STOPPED
        set(value) {
            field = value
            stateSubject.onNext(value)
        }

    private val timeSelectedObservable: PublishSubject<Int> = PublishSubject.create<Int>()

    override fun timeSelectedObservable() = timeSelectedObservable

    private val timerObservable = Observable.interval(1, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .filter { state == TimerState.RUNNING }
            .map {
                Log.i("sync", "timerObservable remaining seconds ${getSecondsRemaining()}")
                getSecondsRemaining()
            }

    private var timerDisposable: Disposable? = null
    private val timeSubject: BehaviorSubject<Int> = BehaviorSubject.create<Int>()
    override fun timeObservable() = timeSubject

    private val stateSubject: BehaviorSubject<TimerState> = BehaviorSubject.createDefault<TimerState>(TimerState.STOPPED)
    override fun stateObservable() = stateSubject

    private val timerDataObservable = Observables.combineLatest(stateSubject, timeSubject) { timerState, remainingSeconds ->
        TimerData(timerState, remainingSeconds)
    }

    override fun timerDataObservable() = timerDataObservable

    private fun start() {
        if (state == TimerState.COMPLETED || state == TimerState.STOPPED) startTimerFromStoppedState()
        if (state == TimerState.PAUSED) startTimerFromPausedState()
        state = TimerState.RUNNING
        initTime()
        if (!timerRunning()) {
            timerDisposable = timerObservable.subscribe { remainingSeconds ->
                timeSubject.onNext(remainingSeconds)
                if (remainingSeconds <= 0) onCompleted()
            }
        }
    }

    private fun pause() {
        pauseTimer()
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
//        resetTimer()
    }

    override fun snooze(seconds: Int) {
        addedSeconds += seconds
        if (state != TimerState.PAUSED && state != TimerState.STOPPED) {
            state = TimerState.RUNNING
            Log.i("sync", "seconds Passed ${passedSeconds()}, meditation Time $meditationTime, added Seconds $addedSeconds")
        }
        initTime()
    }

    override fun setDuration(seconds: Int) {
        Log.i("sync", "setDuration called duration $seconds")
        meditationTime = seconds
        timeSelectedObservable.onNext(seconds)
    }

    override fun getTotalTime() = meditationTime.plus(addedSeconds)

    override fun getSecondsRemaining() = getTotalTime().plus(pausedSeconds).minus(passedSeconds())

    override fun getTimerDuration(): Int = meditationTime

    override fun getMeditationTime(): Single<Int> = Single.create { emitter -> emitter.onSuccess(meditationTime) }

    private fun resetTimer() {
        pausedSeconds = 0
        addedSeconds = 0
        initTime()
    }

    private fun passedSeconds() = Duration.between(startTime, LocalDateTime.now()).seconds.toInt()

    private fun startTimerFromStoppedState() {
        startTime = LocalDateTime.now()
        resetTimer()
    }

    private fun startTimerFromPausedState() {
        pauseEndTime = LocalDateTime.now()
        pausedSeconds += Duration.between(pauseStartTime, pauseEndTime).seconds.toInt()
    }

    private fun pauseTimer() {
        pauseStartTime = LocalDateTime.now()
    }

    private fun initTime() {
        timeSubject.onNext(getSecondsRemaining())
    }

    private fun timerRunning() = timerDisposable != null && !timerDisposable!!.isDisposed
}