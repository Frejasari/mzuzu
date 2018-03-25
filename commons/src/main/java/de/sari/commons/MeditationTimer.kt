package de.sari.commons

import android.util.Log
import io.reactivex.Observable
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
 *
 */

interface AbstractTimer {
    fun stateObservable(): Observable<TimerState>
    fun timeObservable(): Observable<Long>
    fun timerDataObservable(): Observable<TimerData>
    fun timeSelectedObservable(): PublishSubject<Long>
    fun snoozeObservable(): Observable<TimerData>
    fun timerDataStateObservable(): Observable<TimerData>
    fun timerStartedObservable(): PublishSubject<Long>

    fun setDuration(millis: Long)
    fun setDuration(millis: Int)
    fun toggleTimer()
    fun stop()
    fun snooze(millis: Long)
    fun snooze(millis: Int)
    fun getTotalMillis(): Long
    fun getSelectedMillis(): Long
    fun getRemainingMillis(): Long
}

enum class TimerState {
    RUNNING, PAUSED, STOPPED, COMPLETED
}

data class TimerData(var state: TimerState = TimerState.STOPPED, var remainingMillis: Long = 0)


class MeditationTimer(timeUnit: TimeUnit) : AbstractTimer {

    private var addedMillis: Long = 0
    private var startTime: LocalDateTime? = null
    private var pauseStartTime: LocalDateTime? = null
    private var pausedMillis: Long = 0
    private var selectedMillis: Long = 60000
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

    private fun passedMillis(): Long = startTime?.let { Duration.between(startTime, LocalDateTime.now()).toMillis() }
            ?: 0

    var rem = 0L
    override fun getRemainingMillis(): Long {
        val pause = getPausedMillis()
        rem = getTotalMillis().plus(pause).minus(passedMillis())
        Log.i("SanduhrDebug", "getRemainingMillis(), total sec: ${getTotalMillis()/60}, paused sec ${pause/60}, passed sec ${passedMillis()/60}, remaining time: ${rem/60}")
        return rem
    }

    override fun getTotalMillis() = selectedMillis.plus(addedMillis)

    override fun getSelectedMillis() = selectedMillis

    /**
     * Emits the remaining time when the timer is started
     */
    private val timerStartedSubject: PublishSubject<Long> = PublishSubject.create()

    override fun timerStartedObservable() = timerStartedSubject

    /**
     * Emits the selected time when setDuration is called
     */
    private val timeSelectedObservable: PublishSubject<Long> = PublishSubject.create<Long>()

    override fun timeSelectedObservable() = timeSelectedObservable

    /**
     * Emits the timerData at the time when snooze is invoked
     */
    private val snoozeSubject = PublishSubject.create<TimerData>()

    override fun snoozeObservable() = snoozeSubject

    private val timerObservable = Observable.interval(1, timeUnit)
            .observeOn(AndroidSchedulers.mainThread())
            .filter { state == TimerState.RUNNING }
            .map {
                Log.i("observables", "timerObservable onNext called, remainingMillis: ${rem}")
                getRemainingMillis()
            }

    private var timerDisposable: Disposable? = null

    private val timeSubject: BehaviorSubject<Long> = BehaviorSubject.createDefault<Long>(getRemainingMillis())

    /**
     * Emits the remainingMillis when it changes,
     */
    override fun timeObservable() = timeSubject.mergeWith(snoozeSubject.map { timerData -> timerData.remainingMillis })
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
        Log.i("observables", "timerDataObservable onNext called, timerState: $timerState, remainingMillis: $remainingSeconds")
        TimerData(timerState, remainingSeconds)
    }

    override fun timerDataObservable() = timerDataObservable

    /**
     * Emits TimerData when state changes
     */
    private val timerDataStateObservable = stateSubject.withLatestFrom(timeObservable())
    { state, remainingSeconds ->
        Log.i("observables", "timerDataStateObservable onNext called, timerState: $state, remainingMillis: $remainingSeconds")
        TimerData(state, remainingSeconds)
    }

    override fun timerDataStateObservable() = timerDataStateObservable

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

    override fun toggleTimer() {
        Log.i("observables", "toggleTimer: $state, remainingMillis: ${rem}")
        when (state) {
            TimerState.RUNNING -> pause()
            TimerState.COMPLETED -> restart()
            TimerState.STOPPED -> startTimerFromStoppedState()
            TimerState.PAUSED -> continueTimer()
        }
    }

    /**
     * Adds a selected amount of time to the total time.
     * Can only get called from paused, running and completed state
     * Will not reset the passed time and the selected time.
     * Let the snoozeSubject emit the remaining millis
     * Continues the timer when in completed state
     */
    override fun snooze(millis: Long) {
        if (state != TimerState.STOPPED) {
            addedMillis += millis
//            initTime() TODO emit item when snooze subject emits item!
            if (state == TimerState.COMPLETED) {
                continueTimer()
            }
            snoozeSubject.onNext(TimerData(state, getRemainingMillis()))
        }
    }

    override fun snooze(millis: Int) {
        snooze(millis.toLong())
    }

    override fun setDuration(millis: Long) {
        selectedMillis = millis
    }

    override fun setDuration(millis: Int) {
        setDuration(millis.toLong())
    }

    private fun start() {
        state = TimerState.RUNNING
        timerStartedSubject.onNext(getRemainingMillis())

        if (!timerRunning()) {
            timerDisposable = timerObservable.subscribe { remainingSeconds ->
                Log.i("observables", "timeSubject onNext called, remainingMillis: ${getRemainingMillis()}")
                timeSubject.onNext(remainingSeconds)
                if (remainingSeconds <= 0) onCompleted()
            }
        }
    }

    /**
     * starts the pause-period of the timer
     * sets the pause start time to current time
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
        Log.i("SanduhrDebug", "continue called, paused sec : ${pausedMillis/60}, remaining sec: ${getRemainingMillis()/60}")
        pausedMillis += Duration.between(pauseStartTime, LocalDateTime.now()).toMillis()
        Log.i("SanduhrDebug", "continue called, paused sec : ${pausedMillis/60}, remAining sec: ${getRemainingMillis()/60}")
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
        startTimerFromStoppedState()
    }

    private fun getPausedMillis(): Long {
        if (state == TimerState.PAUSED || state == TimerState.COMPLETED) {
            var pMillis = pausedMillis + Duration.between(pauseStartTime, LocalDateTime.now()).toMillis().toInt()
            Log.i("SanduhrDebug", "gePausedMillis() called, state pause or completed - paused seconds: ${pMillis/60}")
            return (pMillis)
        }
        return pausedMillis
    }

    private fun pauseTimer() {
        pauseStartTime = LocalDateTime.now()
    }

    /**
     * Pauses the timer and sets the state to onCompleted
     * Will not reset passed time, added time, paused time or total time!
     */
    private fun onCompleted() {
        state = TimerState.COMPLETED
        pauseTimer()
    }

    private fun resetTimer() {
        pausedMillis = 0
        addedMillis = 0
        startTime = null
    }

    private fun timerRunning() = timerDisposable != null && !timerDisposable!!.isDisposed
}