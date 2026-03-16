package com.golf70.trainer.timer

data class TimerState(val remaining: Int, val running: Boolean)

class DrillTimerEngine(initialSeconds: Int) {
    private var remainingSeconds: Int = initialSeconds
    private var running: Boolean = false

    fun start(seconds: Int): TimerState {
        remainingSeconds = seconds
        running = true
        return TimerState(remainingSeconds, running)
    }

    fun tick(): TimerState {
        if (running && remainingSeconds > 0) remainingSeconds--
        if (remainingSeconds == 0) running = false
        return TimerState(remainingSeconds, running)
    }

    fun pause(): TimerState {
        running = false
        return TimerState(remainingSeconds, running)
    }

    fun resume(): TimerState {
        if (remainingSeconds > 0) running = true
        return TimerState(remainingSeconds, running)
    }

    fun reset(): TimerState {
        remainingSeconds = 0
        running = false
        return TimerState(remainingSeconds, running)
    }
}
