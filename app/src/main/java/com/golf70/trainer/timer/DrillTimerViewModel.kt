package com.golf70.trainer.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DrillTimerViewModel : ViewModel() {
    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _finishedCount = MutableStateFlow(0)
    val finishedCount: StateFlow<Int> = _finishedCount.asStateFlow()

    private var timerJob: Job? = null

    fun start(durationSeconds: Int, onFinished: () -> Unit = {}) {
        timerJob?.cancel()
        _remainingSeconds.value = durationSeconds
        _isRunning.value = true
        timerJob = viewModelScope.launch {
            while (_remainingSeconds.value > 0 && _isRunning.value) {
                delay(1_000)
                _remainingSeconds.value = (_remainingSeconds.value - 1).coerceAtLeast(0)
            }
            if (_remainingSeconds.value == 0) {
                _isRunning.value = false
                _finishedCount.value += 1
                onFinished()
            }
        }
    }

    fun pause() {
        _isRunning.value = false
        timerJob?.cancel()
    }

    fun resume(onFinished: () -> Unit = {}) {
        if (_remainingSeconds.value <= 0) return
        if (_isRunning.value) return
        _isRunning.value = true
        timerJob = viewModelScope.launch {
            while (_remainingSeconds.value > 0 && _isRunning.value) {
                delay(1_000)
                _remainingSeconds.value = (_remainingSeconds.value - 1).coerceAtLeast(0)
            }
            if (_remainingSeconds.value == 0) {
                _isRunning.value = false
                _finishedCount.value += 1
                onFinished()
            }
        }
    }

    fun reset() {
        _isRunning.value = false
        timerJob?.cancel()
        _remainingSeconds.value = 0
    }
}
