package de.sari.commons

import android.util.Log
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import org.threeten.bp.Duration
import org.threeten.bp.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 *  The remaining time in paused state is not accurate
 */


interface AbstractTimer {
    fun stateObservable(): Observable<TimerState>
    fun timeObservable(): Observable<Int>
    fun timerDataSecondsObservable(): Observable<TimerData>
    fun timeSelectedObservable(): Observable<Int>
    fun snoozeObservable(): Observable<TimerData>
    fun toggleTimer()
    fun stop()
    fun setDuration(seconds: Int)
    fun snooze(seconds: Int)
    fun getTotalTime(): Int
    fun getTimerDuration(): Int
    fun getMeditationTime(): Single<Int>
    fun timerDataStateObservable(): Observable<TimerData>
}

enum class TimerState {
    RUNNING, PAUSED, STOPPED, COMPLETED
}

data class TimerData(var state: TimerState = TimerState.STOPPED, var remainingSeconds: Int = 0)


class MeditationTimer(private var selectedTime: Int = 60) : AbstractTimer {

    private var addedSeconds: Int = 0
    //    TODO NullPointer, when no Times are set!
    private var startTime: LocalDateTime? = null
    private var pauseStartTime: LocalDateTime? = null
    private var pauseEndTime: LocalDateTime? = null
    private var pausedSeconds: Int = 0

    private var state = TimerState.STOPPED
        set(value) {
            field = value
            Log.i("observables", "stateSubject onNext called, timerState: ${value}")
            stateSubject.onNext(value)
        }

    /**
     * Emits the selected time when setDuration is called
     */
    private val timeSelectedObservable: PublishSubject<Int> = PublishSubject.create<Int>()

    override fun timeSelectedObservable() = timeSelectedObservable

    /**
     * Emits the timerData at the time when snooze is invoked
     */
    private val snoozeSubject = PublishSubject.create<Int>()

    override fun snoozeObservable() = snoozeSubject.withLatestFrom(timerDataSecondsObservable) { _, timerData -> timerData }

    private val timerObservable = Observable.interval(1, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .filter { state == TimerState.RUNNING }
            .map {
                Log.i("observables", "timerObservable onNext called, remainingSeconds: ${getSecondsRemaining()}")
                getSecondsRemaining()
            }
    private var timerDisposable: Disposable? = null

    /**
     * Emits the remainingTime when the time changes, emits the most recently emitted item when an observer subscribes
     */
    private val timeSubject: BehaviorSubject<Int> = BehaviorSubject.create<Int>()

    override fun timeObservable() = timeSubject

    /**
     * Emits the TimerState when it changes, emits the most recently emitted item
     * (or - if non exists - TimerState.STOPPED) when an observer subscribes
     */
    private val stateSubject: BehaviorSubject<TimerState> = BehaviorSubject.createDefault<TimerState>(TimerState.STOPPED)

    override fun stateObservable() = stateSubject

    /**
     * Emits TimerData with current status, when a second passes
     */
    private val timerDataSecondsObservable = Observables.combineLatest(stateSubject, timeSubject) { timerState, remainingSeconds ->
        // TODO Why is this called 2 times?
        Log.i("observables", "timerDataSecondsObservable onNext called, timerState: $timerState, remainingSeconds: $remainingSeconds")
        TimerData(timerState, remainingSeconds)
    }

    override fun timerDataSecondsObservable() = timerDataSecondsObservable

    /**
     * Emits TimerData with current time, when the state changes
     */
    private val timerDataStateObservable = stateSubject.withLatestFrom(timeSubject)
    { state, remainingSeconds ->
        Log.i("observables", "timerDataStateObservable onNext called, timerState: $state, remainingSeconds: $remainingSeconds")

        TimerData(state, remainingSeconds)
    }

    override fun timerDataStateObservable() = timerDataStateObservable

    private fun start() {
        if (state == TimerState.COMPLETED || state == TimerState.STOPPED) startTimerFromStoppedState()
        if (state == TimerState.PAUSED) startTimerFromPausedState()
        state = TimerState.RUNNING
        initTime()
        if (!timerRunning()) {
            timerDisposable = timerObservable.subscribe { remainingSeconds ->
                Log.i("observables", "timeSubject onNext called, remainingSeconds: ${getSecondsRemaining()}")
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
        Log.i("observables", "snoozeSubject onNext called, added seconds: $seconds remaining seconds: ${getSecondsRemaining()}")

        addedSeconds += seconds
        if (state == TimerState.COMPLETED) {
            state = TimerState.RUNNING
            Log.i("sync", "seconds Passed ${passedSeconds()}, meditation Time $selectedTime, added Seconds $addedSeconds")
        }
        initTime()
        snoozeSubject.onNext(getSecondsRemaining())
    }

    override fun setDuration(seconds: Int) {
        Log.i("sync", "setDuration called duration $seconds")
        selectedTime = seconds
        Log.i("observables", "timeSelectedObservable onNext called, remainingSeconds: ${getSecondsRemaining()}")
        timeSelectedObservable.onNext(seconds)
    }

    override fun getTotalTime() = selectedTime.plus(addedSeconds)

    private fun getSecondsRemaining() = getTotalTime().plus(pausedSeconds).minus(passedSeconds())

    override fun getTimerDuration(): Int = selectedTime

    override fun getMeditationTime(): Single<Int> = Single.create { emitter -> emitter.onSuccess(selectedTime) }

    private fun resetTimer() {
        pausedSeconds = 0
        addedSeconds = 0
        initTime()
    }

    private fun passedSeconds(): Int {
        return startTime?.let { Duration.between(startTime, LocalDateTime.now()).seconds.toInt() }
                ?: 0
    }

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
        Log.i("observables", "timeSubject onNext called, remainingSeconds: ${getSecondsRemaining()}")
        timeSubject.onNext(getSecondsRemaining())
    }

    private fun timerRunning() = timerDisposable != null && !timerDisposable!!.isDisposed
}