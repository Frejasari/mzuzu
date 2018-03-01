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
    fun timerDataObservable(): Observable<TimerData>
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
    fun timerStartedObservable(): Observable<Int>
}

enum class TimerState {
    RUNNING, PAUSED, STOPPED, COMPLETED
}

data class TimerData(var state: TimerState = TimerState.STOPPED, var remainingSeconds: Int = 0)


class MeditationTimer : AbstractTimer {

    private var addedSeconds: Int = 0
    private var startTime: LocalDateTime? = null
    private var pauseStartTime: LocalDateTime? = null
    private var pausedSeconds: Int = 0
    private var selectedSeconds: Int = 60
        set(value) {
            field = value
            Log.i("observables", "timeSelected onNext called, timerState: ${value}")
            timeSelectedObservable.onNext(value)
        }
    private var state = TimerState.STOPPED
        set(value) {
            field = value
            Log.i("observables", "stateSubject onNext called, timerState: ${value}")
            stateSubject.onNext(value)
        }

    private fun passedSeconds() = startTime?.let { Duration.between(startTime, LocalDateTime.now()).seconds.toInt() }
            ?: 0

    /**
     * Emits the remaining time when the timer is started
     */
    private val timerStartedSubject: PublishSubject<Int> = PublishSubject.create()

    override fun timerStartedObservable() = timerStartedSubject

    /**
     * Emits the selected time when setDuration is called
     */
    private val timeSelectedObservable: PublishSubject<Int> = PublishSubject.create<Int>()

    override fun timeSelectedObservable() = timeSelectedObservable

    /**
     * Emits the timerData at the time when snooze is invoked
     */
    private val snoozeSubject = PublishSubject.create<TimerData>()

    override fun snoozeObservable() = snoozeSubject

    private val timerObservable = Observable.interval(1, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .filter { state == TimerState.RUNNING }
            .map {
                Log.i("observables", "timerObservable onNext called, remainingSeconds: ${getSecondsRemaining()}")
                getSecondsRemaining()
            }
    private var timerDisposable: Disposable? = null


    private val timeSubject: BehaviorSubject<Int> = BehaviorSubject.createDefault<Int>(getSecondsRemaining())
    /**
     * Emits the remainingTime when it changes,
     */
    override fun timeObservable() = timeSubject.mergeWith(snoozeSubject.map { timerData -> timerData.remainingSeconds })
            .mergeWith(timerStartedSubject).mergeWith(timeSelectedObservable)

    /**
     * Emits the TimerState when it changes, emits the most recently emitted item
     * (or - if non exists - TimerState.STOPPED) when an observer subscribes
     */
    private val stateSubject: BehaviorSubject<TimerState> = BehaviorSubject.createDefault<TimerState>(TimerState.STOPPED)

    override fun stateObservable() = stateSubject

    /**
     * Emits TimerData when time or state changes
     */
    private val timerDataObservable = Observables.combineLatest(stateSubject, timeObservable()) { timerState, remainingSeconds ->
        Log.i("observables", "timerDataObservable onNext called, timerState: $timerState, remainingSeconds: $remainingSeconds")
        TimerData(timerState, remainingSeconds)
    }

    override fun timerDataObservable() = timerDataObservable

    /**
     * Emits TimerData when state changes
     */
    private val timerDataStateObservable = stateSubject.withLatestFrom(timeObservable())
    { state, remainingSeconds ->
        Log.i("observables", "timerDataStateObservable onNext called, timerState: $state, remainingSeconds: $remainingSeconds")
        TimerData(state, remainingSeconds)
    }

    override fun timerDataStateObservable() = timerDataStateObservable

    private fun start() {
        timerStartedSubject.onNext(getSecondsRemaining())
//        initTime() TODO emit item when state is changed!
        Log.i("observables", "start called")

        state = TimerState.RUNNING
        if (!timerRunning()) {
            timerDisposable = timerObservable.subscribe { remainingSeconds ->
                Log.i("observables", "timeSubject onNext called, remainingSeconds: ${getSecondsRemaining()}")
                timeSubject.onNext(remainingSeconds)
                if (remainingSeconds <= 0) onCompleted()
            }
        }
    }

    /**
     * starts the pause-period of the timer
     * sets the start time for pause to current time
     */
    private fun pause() {
        Log.i("observables", "pause called")
        pauseTimer()
        state = TimerState.PAUSED
    }

    /**
     * continues the timer after the pause-period of the timer
     * sets paused seconds with start time and current time
     */
    private fun continueTimer() {
        Log.i("observables", "continue called")
        pausedSeconds += Duration.between(pauseStartTime, LocalDateTime.now()).seconds.toInt()
        start()
    }

    /**
     * sets the startTime to current time
     * and starts the timer from an already clean state
     */
    private fun startTimerFromStoppedState() {
        Log.i("observables", "startTimerFromStoppedState called")
        startTime = LocalDateTime.now()
        start()
    }

    /**
     * Resets the timer for a fresh start.
     * sets the startTime to current time
     * Will not reset the selected duration of the timer!
     */
    private fun restart() {
        Log.i("observables", "restart called")
        resetTimer()
        startTime = LocalDateTime.now()
        start()
    }

    override fun toggleTimer() {
        when (state) {
            TimerState.RUNNING -> pause()
            TimerState.COMPLETED -> restart()
            TimerState.STOPPED -> startTimerFromStoppedState()
            TimerState.PAUSED -> continueTimer()
        }
    }

    private fun getPausedSeconds(): Int {
        if (state == TimerState.PAUSED || state == TimerState.COMPLETED) {
            return pausedSeconds + Duration.between(pauseStartTime, LocalDateTime.now()).seconds.toInt()
        }
        return pausedSeconds
    }

    // Has always to be followed by stop() or continueTimer!
    private fun pauseTimer() {
        pauseStartTime = LocalDateTime.now()
    }

    /**
     * Stops timer and resets the paused and added time to 0.
     * Resets the start time to null.
     * Will not reset the selected duration of the timer!
     */
    override fun stop() {
        resetTimer() // do not change order with following line -> state Subject will emit after Time is set!
        state = TimerState.STOPPED
        if (timerRunning()) timerDisposable!!.dispose()
    }

    /**
     * Pauses the timer and sets the state to onCompleted
     * Will not reset passed time, added time, paused time or total time!
     */
    private fun onCompleted() {
        state = TimerState.COMPLETED
        pauseTimer()
    }

    /**
     * Adds a selected amount of time to the total time.
     * Can only get called from paused, running and completed state
     * Will not reset the passed time and the selected time.
     * Let the snoozeSubject emit the remaining seconds
     * Continues the timer when in completed state
     */
    override fun snooze(seconds: Int) {
        if (state != TimerState.STOPPED) {
            addedSeconds += seconds
//            initTime() TODO emit item when snooze subject emits item!
            if (state == TimerState.COMPLETED) {
                continueTimer()
            }
            snoozeSubject.onNext(TimerData(state, getSecondsRemaining()))
        }
    }

    override fun setDuration(seconds: Int) {
        selectedSeconds = seconds
    }

    override fun getTotalTime() = selectedSeconds.plus(addedSeconds)

    private fun getSecondsRemaining(): Int {
        getPausedSeconds()
        Log.i("observables", "getSecondsRemaining called, pausedSeconds: ${pausedSeconds}, passed seconds: ${passedSeconds()} totalTime: ${getTotalTime()}, selectedSeconds: $selectedSeconds, addedSeconds: $addedSeconds, start time: $startTime")
        return getTotalTime().plus(pausedSeconds).minus(passedSeconds())
    }

    override fun getTimerDuration(): Int = selectedSeconds

    override fun getMeditationTime(): Single<Int> = Single.create { emitter -> emitter.onSuccess(selectedSeconds) }

    private fun resetTimer() {
        pausedSeconds = 0
        addedSeconds = 0
        startTime = null
//        initTime()
    }

//    private fun initTime() {
//        Log.i("observables", "initTime timerSubject onNext called, remainingSeconds: ${getSecondsRemaining()}")
//        timeSubject.onNext(getSecondsRemaining())
//    }

    private fun timerRunning() = timerDisposable != null && !timerDisposable!!.isDisposed
}