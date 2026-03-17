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
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
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
    private var saveSessionJob: Deferred<Pair<Long, List<Long>>>? = null

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

    private fun buildSessionLayoutsForWeek(currentWeek: Int): Pair<String, List<SessionDefinition>> {
        val trainingWeek = TrainingProgram.weekPlan(currentWeek)
        val weekLabel = "Week ${trainingWeek.weekNumber}"
        val warmup = trainingWeek.drills.getOrNull(0)
        val fullSwing = trainingWeek.drills.getOrNull(1)
        val shortGame = trainingWeek.drills.getOrNull(2)
        val putting = trainingWeek.drills.getOrNull(3)
        val pressure = trainingWeek.drills.getOrNull(4)

        val day1 = listOfNotNull(warmup, fullSwing, shortGame, putting, pressure)
        val day2 = listOfNotNull(warmup, shortGame, putting, pressure)
        val day3 = listOfNotNull(warmup, fullSwing, shortGame, pressure)

        val layouts = listOf(
            SessionDefinition(type = "$weekLabel • Day 1", durationMinutes = 55, drills = day1),
            SessionDefinition(type = "$weekLabel • Day 2", durationMinutes = 45, drills = day2),
            SessionDefinition(type = "$weekLabel • Day 3", durationMinutes = 50, drills = day3)
        )
        return "${trainingWeek.phase} • ${trainingWeek.focus}" to layouts
    }

    private fun createNewWeekSession(selectedLayoutIndex: Int = 0) {
        val now = System.currentTimeMillis()
        val programStart = persistence.programStartTimestamp() ?: now.also { persistence.saveProgramStartTimestamp(it) }
        val startDate = Instant.ofEpochMilli(programStart).atZone(ZoneId.systemDefault()).toLocalDate()
        val trainingWeek = TrainingProgram.resolveWeek(programStartDate = startDate, today = LocalDate.now())
        val (phaseFocus, layouts) = buildSessionLayoutsForWeek(trainingWeek.weekNumber)
        val boundedLayoutIndex = selectedLayoutIndex.coerceIn(0, layouts.lastIndex)
        val selectedLayout = layouts[boundedLayoutIndex]
        val firstDuration = selectedLayout.drills.firstOrNull()?.timerSeconds ?: 0
        val phase = phaseFocus.substringBefore(" • ")
        val focus = phaseFocus.substringAfter(" • ", "")

        val newState = SessionUiState(
            drills = selectedLayout.drills,
            sessionLayouts = layouts,
            selectedLayoutIndex = boundedLayoutIndex,
            currentWeek = trainingWeek.weekNumber,
            phase = phase,
            focus = focus,
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
        val (phaseFocus, layouts) = buildSessionLayoutsForWeek(restored.currentWeek)
        val boundedLayoutIndex = restored.selectedSessionIndex.coerceIn(0, layouts.lastIndex)
        val selectedLayout = layouts[boundedLayoutIndex]
        val adjusted = adjustRemaining(
            previousRemaining = restored.remainingTime,
            savedTimestamp = restored.timerLastUpdatedTimestamp,
            running = restored.timerRunning
        )
        val boundedIndex = restored.currentDrillIndex.coerceIn(0, selectedLayout.drills.lastIndex)
        val phase = phaseFocus.substringBefore(" • ")
        val focus = phaseFocus.substringAfter(" • ", "")

        val state = SessionUiState(
            sessionId = restored.savedSessionId,
            sessionSaved = restored.savedSessionId != null,
            savedDrillIds = restored.savedDrillIds,
            drills = selectedLayout.drills,
            sessionLayouts = layouts,
            selectedLayoutIndex = boundedLayoutIndex,
            currentDrillIndex = boundedIndex,
            completedDrills = restored.completedDrills,
            currentWeek = restored.currentWeek,
            phase = phase,
            focus = focus,
            remainingSeconds = adjusted.first,
            timerRunning = adjusted.second,
            sessionStartTimestamp = restored.sessionStartTimestamp,
            resumedSession = true,
            feedbackMessage = "Resumed active session from week ${restored.currentWeek}",
            saveStatus = if (restored.savedSessionId != null) "Saved" else "Unsaved"
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
                selectedSessionIndex = current.selectedLayoutIndex,
                currentDrillIndex = current.currentDrillIndex,
                remainingTime = current.remainingSeconds,
                timerRunning = current.timerRunning,
                timerLastUpdatedTimestamp = System.currentTimeMillis(),
                sessionStartTimestamp = current.sessionStartTimestamp,
                completedDrills = current.completedDrills,
                savedSessionId = current.sessionId,
                savedDrillIds = current.savedDrillIds
            )
        )
    }

    private suspend fun ensureSessionSavedInternal(): Boolean {
        val current = _uiState.value
        if (current.sessionSaved && current.sessionId != null && current.savedDrillIds.isNotEmpty()) return true

        val existingJob = saveSessionJob
        if (existingJob != null && existingJob.isActive) {
            val result = existingJob.await()
            _uiState.value = _uiState.value.copy(
                sessionId = result.first,
                sessionSaved = true,
                savedDrillIds = result.second,
                saveStatus = "Saved"
            )
            persistSessionState()
            return true
        }

        _uiState.value = _uiState.value.copy(saveStatus = "Saving…")
        val activeState = _uiState.value
        val definition = SessionDefinition(
            type = activeState.sessionLayouts.getOrNull(activeState.selectedLayoutIndex)?.type
                ?: "Week ${activeState.currentWeek} Session",
            durationMinutes = 55,
            drills = activeState.drills
        )

        val createdJob = viewModelScope.async {
            val sessionId = repository.saveSession(definition)
            val drillIds = repository.getDrillIdsForSession(sessionId)
            sessionId to drillIds
        }
        saveSessionJob = createdJob

        val (sessionId, drillIds) = createdJob.await()
        saveSessionJob = null

        _uiState.value = _uiState.value.copy(
            sessionId = sessionId,
            sessionSaved = true,
            savedDrillIds = drillIds,
            saveStatus = "Saved",
            feedbackMessage = "Session autosaved"
        )
        persistSessionState()
        return true
    }

    private fun ensureSessionSaved() {
        viewModelScope.launch {
            ensureSessionSavedInternal()
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
        viewModelScope.launch {
            val hasSession = ensureSessionSavedInternal()
            if (!hasSession) {
                _uiState.value = _uiState.value.copy(feedbackMessage = "Unable to save session")
                return@launch
            }

            val current = _uiState.value
            val drillId = current.savedDrillIds.getOrNull(current.currentDrillIndex)
            if (drillId == null) {
                _uiState.value = current.copy(feedbackMessage = "Unable to find saved drill for this index")
                return@launch
            }

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
            persistCurrentDrill(advanceAfterSave = true)
            return
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
        val current = _uiState.value
        if (current.timerRunning) {
            _uiState.value = current.copy(feedbackMessage = "Pause timer before switching sessions")
            return
        }
        if (current.sessionSaved || current.completedDrills.isNotEmpty()) {
            _uiState.value = current.copy(feedbackMessage = "Finish current session before switching day")
            return
        }
        if (index == current.selectedLayoutIndex) return
        createNewWeekSession(selectedLayoutIndex = index)
        _uiState.value = _uiState.value.copy(feedbackMessage = "Loaded ${_uiState.value.sessionLayouts[index].type}")
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
