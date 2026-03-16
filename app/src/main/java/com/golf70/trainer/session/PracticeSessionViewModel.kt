package com.golf70.trainer.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.golf70.trainer.data.local.GoalEntity
import com.golf70.trainer.domain.DrillDefinition
import com.golf70.trainer.domain.SeedSessions
import com.golf70.trainer.repository.GolfRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    val saveStatus: String = "Unsaved"
) {
    val currentDrill: DrillDefinition? = drills.getOrNull(currentDrillIndex)
    val nextDrill: DrillDefinition? = drills.getOrNull(currentDrillIndex + 1)
}

class PracticeSessionViewModel(
    private val repository: GolfRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    init {
        loadTodaySession()
    }

    fun loadTodaySession() {
        val definition = SeedSessions.weeklyPlan.first()
        _uiState.value = SessionUiState(
            drills = definition.drills,
            currentDrillIndex = savedStateHandle["drillIndex"] ?: 0,
            attempts = savedStateHandle["attempts"] ?: 0,
            successes = savedStateHandle["successes"] ?: 0,
            direction = savedStateHandle["direction"],
            saveStatus = "Unsaved"
        )
        viewModelScope.launch {
            repository.saveGoal(GoalEntity())
        }
    }

    private fun cacheState(state: SessionUiState) {
        savedStateHandle["drillIndex"] = state.currentDrillIndex
        savedStateHandle["attempts"] = state.attempts
        savedStateHandle["successes"] = state.successes
        savedStateHandle["direction"] = state.direction
    }

    private fun ensureSessionSaved() {
        val current = _uiState.value
        if (current.sessionSaved) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saveStatus = "Saving…")
            val definition = SeedSessions.weeklyPlan.first()
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
        }
    }

    fun completeCurrentDrill() {
        ensureSessionSaved()
        persistCurrentDrill(advanceAfterSave = true)
    }

    fun nextDrill(skipSave: Boolean = false) {
        if (!skipSave) {
            ensureSessionSaved()
            persistCurrentDrill(advanceAfterSave = false)
        }
        val current = _uiState.value
        val newIndex = (current.currentDrillIndex + 1).coerceAtMost(current.drills.lastIndex)
        val updated = current.copy(
            currentDrillIndex = newIndex,
            completed = current.completedDrills.size == current.drills.size,
            attempts = 0,
            successes = 0,
            direction = null
        )
        _uiState.value = updated
        cacheState(updated)
    }

    fun previousDrill() {
        val current = _uiState.value
        val newIndex = (current.currentDrillIndex - 1).coerceAtLeast(0)
        val updated = current.copy(
            currentDrillIndex = newIndex,
            attempts = 0,
            successes = 0,
            direction = null
        )
        _uiState.value = updated
        cacheState(updated)
    }

    fun completeSession() {
        ensureSessionSaved()
        _uiState.value = _uiState.value.copy(feedbackMessage = "Session finalized and saved", saveStatus = "Saved")
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(feedbackMessage = null)
    }

    companion object {
        fun factory(repository: GolfRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: androidx.lifecycle.viewmodel.CreationExtras): T {
                    val savedStateHandle = androidx.lifecycle.SavedStateHandle()
                    return PracticeSessionViewModel(repository, savedStateHandle) as T
                }
            }
    }
}
