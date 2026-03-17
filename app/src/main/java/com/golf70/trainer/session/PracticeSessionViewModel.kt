package com.golf70.trainer.session

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.golf70.trainer.data.local.GoalEntity
import com.golf70.trainer.domain.DrillDefinition
import com.golf70.trainer.domain.SessionDefinition
import com.golf70.trainer.domain.TrainingProgram
import com.golf70.trainer.repository.GolfRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class SessionUiState(
    val sessionId: Long? = null,
    val drills: List<DrillDefinition> = emptyList(),
    val currentDrillIndex: Int = 0,
    val attempts: Int = 0,
    val successes: Int = 0,
    val direction: String? = null,
    val completed: Boolean = false,
    val completedDrills: Set<Int> = emptySet(),
    val feedbackMessage: String? = null,
    val sessionSaved: Boolean = false,
    val savedDrillIds: List<Long> = emptyList(),
    val saveStatus: String = "Unsaved",
    val sessionLayouts: List<SessionDefinition> = emptyList(),
    val selectedLayoutIndex: Int = 0,
    val currentWeek: Int = 1,
    val phase: String = "",
    val focus: String = "",
    val remainingSeconds: Int = 0,
    val timerRunning: Boolean = false,
    val sessionStartTimestamp: Long = 0L,
    val resumedSession: Boolean = false
) {
    val currentDrill: DrillDefinition? = drills.getOrNull(currentDrillIndex)
    val nextDrill: DrillDefinition? = drills.getOrNull(currentDrillIndex + 1)
    val isLastDrill: Boolean = drills.isNotEmpty() && currentDrillIndex == drills.lastIndex
}

class PracticeSessionViewModel(
    private val repository: GolfRepository,
    private val persistence: SessionPersistence,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadTodaySession()
    }

    fun loadTodaySession() {
        viewModelScope.launch {
            val restored = persistence.load()
            if (restored != null) {
                restorePersistedSession(restored)
            } else {
                createNewWeekSession()
            }
            repository.saveGoal(GoalEntity())
        }
    }

    private fun createNewWeekSession() {
        val now = System.currentTimeMillis()
        val programStart = persistence.programStartTimestamp() ?: now.also { persistence.saveProgramStartTimestamp(it) }
        val startDate = Instant.ofEpochMilli(programStart).atZone(ZoneId.systemDefault()).toLocalDate()
        val trainingWeek = TrainingProgram.resolveWeek(programStartDate = startDate, today = LocalDate.now())
        val drills = trainingWeek.drills
        val firstDuration = drills.firstOrNull()?.timerSeconds ?: 0

        val newState = SessionUiState(
            drills = drills,
            currentWeek = trainingWeek.weekNumber,
            phase = trainingWeek.phase,
            focus = trainingWeek.focus,
            remainingSeconds = firstDuration,
            timerRunning = false,
            sessionStartTimestamp = now,
            resumedSession = false,
            saveStatus = "Unsaved"
        )
        _uiState.value = newState
        cacheState(newState)
        persistSessionState()
    }

    private fun restorePersistedSession(restored: PersistedSessionState) {
        val trainingWeek = TrainingProgram.weekPlan(restored.currentWeek)
        val adjusted = adjustRemaining(
            previousRemaining = restored.remainingTime,
            savedTimestamp = restored.timerLastUpdatedTimestamp,
            running = restored.timerRunning
        )
        val boundedIndex = restored.currentDrillIndex.coerceIn(0, trainingWeek.drills.lastIndex)
        val state = SessionUiState(
            drills = trainingWeek.drills,
            currentDrillIndex = boundedIndex,
            completedDrills = restored.completedDrills,
            currentWeek = restored.currentWeek,
            phase = trainingWeek.phase,
            focus = trainingWeek.focus,
            remainingSeconds = adjusted.first,
            timerRunning = adjusted.second,
            sessionStartTimestamp = restored.sessionStartTimestamp,
            resumedSession = true,
            feedbackMessage = "Resumed active session from week ${restored.currentWeek}"
        )
        _uiState.value = state
        if (state.timerRunning) {
            runTimer()
        }
        if (restored.timerRunning && adjusted.first == 0) {
            onTimerFinished()
        }
    }

    private fun adjustRemaining(previousRemaining: Int, savedTimestamp: Long, running: Boolean): Pair<Int, Boolean> {
        if (!running) return previousRemaining.coerceAtLeast(0) to false
        val elapsed = ((System.currentTimeMillis() - savedTimestamp).coerceAtLeast(0L) / 1000L).toInt()
        val remaining = (previousRemaining - elapsed).coerceAtLeast(0)
        return remaining to (remaining > 0)
    }

    private fun cacheState(state: SessionUiState) {
        savedStateHandle["drillIndex"] = state.currentDrillIndex
        savedStateHandle["attempts"] = state.attempts
        savedStateHandle["successes"] = state.successes
        savedStateHandle["direction"] = state.direction
    }

    fun persistSessionState() {
        val current = _uiState.value
        if (current.completed || current.drills.isEmpty()) return
        persistence.save(
            PersistedSessionState(
                currentWeek = current.currentWeek,
                currentDrillIndex = current.currentDrillIndex,
                remainingTime = current.remainingSeconds,
                timerRunning = current.timerRunning,
                timerLastUpdatedTimestamp = System.currentTimeMillis(),
                sessionStartTimestamp = current.sessionStartTimestamp,
                completedDrills = current.completedDrills
            )
        )
    }

    private fun ensureSessionSaved() {
        val current = _uiState.value
        if (current.sessionSaved) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saveStatus = "Saving…")
            val definition = SessionDefinition(
                type = "Week ${current.currentWeek} Session",
                durationMinutes = 55,
                drills = current.drills
            )
            val sessionId = repository.saveSession(definition)
            val drillIds = repository.getDrillIdsForSession(sessionId)
            _uiState.value = _uiState.value.copy(
                sessionId = sessionId,
                sessionSaved = true,
                savedDrillIds = drillIds,
                saveStatus = "Saved",
                feedbackMessage = "Session autosaved"
            )
        }
    }

    fun logMetric(direction: String? = null, success: Boolean? = null) {
        ensureSessionSaved()
        val current = _uiState.value
        val updated = current.copy(
            attempts = current.attempts + 1,
            successes = current.successes + if (success == true) 1 else 0,
            direction = direction ?: current.direction,
            feedbackMessage = when {
                direction != null -> "Logged direction: ${direction.replaceFirstChar { it.uppercase() }}"
                success == true -> "Logged result: Made"
                success == false -> "Logged result: Missed"
                else -> "Logged"
            },
            saveStatus = "Unsaved"
        )
        _uiState.value = updated
        cacheState(updated)
        persistSessionState()
    }

    private fun persistCurrentDrill(advanceAfterSave: Boolean) {
        val current = _uiState.value
        if (!current.sessionSaved || current.sessionId == null) {
            ensureSessionSaved()
            _uiState.value = current.copy(feedbackMessage = "Preparing session save...")
            return
        }

        val drillId = current.savedDrillIds.getOrNull(current.currentDrillIndex)
        if (drillId == null) {
            _uiState.value = current.copy(feedbackMessage = "Unable to find saved drill for this index")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saveStatus = "Saving…")
            repository.saveDrillResult(
                drillId = drillId,
                attempts = current.attempts,
                successes = current.successes,
                direction = current.direction,
                distance = null
            )
            _uiState.value = _uiState.value.copy(
                completedDrills = _uiState.value.completedDrills + current.currentDrillIndex,
                feedbackMessage = "Drill saved",
                saveStatus = "Saved"
            )
            if (advanceAfterSave) {
                nextDrill(skipSave = true)
            }
            persistSessionState()
        }
    }

    fun completeCurrentDrill() {
        ensureSessionSaved()
        persistCurrentDrill(advanceAfterSave = true)
    }

    fun startTimer() {
        val current = _uiState.value
        if (current.remainingSeconds <= 0) return
        _uiState.value = current.copy(timerRunning = true)
        runTimer()
        persistSessionState()
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(timerRunning = false)
        persistSessionState()
    }

    fun resumeTimer() {
        val current = _uiState.value
        val adjusted = adjustRemaining(current.remainingSeconds, System.currentTimeMillis(), current.timerRunning)
        if (adjusted.first <= 0) {
            onTimerFinished()
            return
        }
        _uiState.value = current.copy(remainingSeconds = adjusted.first, timerRunning = true)
        runTimer()
        persistSessionState()
    }

    fun resetTimerForCurrentDrill() {
        timerJob?.cancel()
        val duration = _uiState.value.currentDrill?.timerSeconds ?: 0
        _uiState.value = _uiState.value.copy(remainingSeconds = duration, timerRunning = false)
        persistSessionState()
    }

    private fun runTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.timerRunning && _uiState.value.remainingSeconds > 0) {
                delay(1_000)
                _uiState.value = _uiState.value.copy(remainingSeconds = (_uiState.value.remainingSeconds - 1).coerceAtLeast(0))
                persistSessionState()
            }
            if (_uiState.value.timerRunning && _uiState.value.remainingSeconds == 0) {
                onTimerFinished()
            }
        }
    }

    private fun onTimerFinished() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(timerRunning = false, remainingSeconds = 0, feedbackMessage = "Timer complete")
        if (_uiState.value.isLastDrill) {
            persistSessionState()
            return
        }
        nextDrill()
    }

    fun nextDrill(skipSave: Boolean = false) {
        if (!skipSave) {
            ensureSessionSaved()
            persistCurrentDrill(advanceAfterSave = false)
        }
        timerJob?.cancel()
        val current = _uiState.value
        val newIndex = (current.currentDrillIndex + 1).coerceAtMost(current.drills.lastIndex)
        val newDrillDuration = current.drills.getOrNull(newIndex)?.timerSeconds ?: 0
        val updated = current.copy(
            currentDrillIndex = newIndex,
            completed = current.completedDrills.size == current.drills.size,
            attempts = 0,
            successes = 0,
            direction = null,
            remainingSeconds = newDrillDuration,
            timerRunning = false
        )
        _uiState.value = updated
        cacheState(updated)
        persistSessionState()
    }

    fun previousDrill() {
        timerJob?.cancel()
        val current = _uiState.value
        val newIndex = (current.currentDrillIndex - 1).coerceAtLeast(0)
        val updated = current.copy(
            currentDrillIndex = newIndex,
            attempts = 0,
            successes = 0,
            direction = null,
            remainingSeconds = current.drills.getOrNull(newIndex)?.timerSeconds ?: 0,
            timerRunning = false
        )
        _uiState.value = updated
        cacheState(updated)
        persistSessionState()
    }

    fun selectLayout(index: Int) {
        _uiState.value = _uiState.value.copy(feedbackMessage = "Weekly progression is fixed. Resume the active session.")
    }

    fun completeSession() {
        timerJob?.cancel()
        ensureSessionSaved()
        _uiState.value = _uiState.value.copy(
            completed = true,
            timerRunning = false,
            feedbackMessage = "Session complete. Great work.",
            saveStatus = "Saved"
        )
        persistence.clearSession()
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(feedbackMessage = null)
    }

    companion object {
        fun factory(repository: GolfRepository, context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: androidx.lifecycle.viewmodel.CreationExtras): T {
                    val savedStateHandle = androidx.lifecycle.SavedStateHandle()
                    return PracticeSessionViewModel(repository, SessionPersistence(context.applicationContext), savedStateHandle) as T
                }
            }
    }
}
