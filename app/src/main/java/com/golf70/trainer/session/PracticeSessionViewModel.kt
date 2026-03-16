package com.golf70.trainer.session

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
    val savedDrillIds: List<Long> = emptyList()
) {
    val currentDrill: DrillDefinition? = drills.getOrNull(currentDrillIndex)
    val nextDrill: DrillDefinition? = drills.getOrNull(currentDrillIndex + 1)
}

class PracticeSessionViewModel(
    private val repository: GolfRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    init {
        loadTodaySession()
    }

    fun loadTodaySession() {
        val definition = SeedSessions.weeklyPlan.first()
        _uiState.value = SessionUiState(drills = definition.drills)
        viewModelScope.launch {
            repository.saveGoal(GoalEntity())
        }
    }

    fun logMetric(direction: String? = null, success: Boolean? = null) {
        val current = _uiState.value
        _uiState.value = current.copy(
            attempts = current.attempts + 1,
            successes = current.successes + if (success == true) 1 else 0,
            direction = direction ?: current.direction,
            feedbackMessage = when {
                direction != null -> "Logged direction: ${direction.replaceFirstChar { it.uppercase() }}"
                success == true -> "Logged result: Made"
                success == false -> "Logged result: Missed"
                else -> "Logged"
            }
        )
    }

    fun completeCurrentDrill() {
        val current = _uiState.value
        if (!current.sessionSaved || current.sessionId == null) {
            _uiState.value = current.copy(feedbackMessage = "Complete the session first to save drills")
            return
        }

        val drillId = current.savedDrillIds.getOrNull(current.currentDrillIndex)
        if (drillId == null) {
            _uiState.value = current.copy(feedbackMessage = "Unable to find saved drill for this index")
            return
        }

        viewModelScope.launch {
            repository.saveDrillResult(
                drillId = drillId,
                attempts = current.attempts,
                successes = current.successes,
                direction = current.direction,
                distance = null
            )
            _uiState.value = _uiState.value.copy(
                completedDrills = _uiState.value.completedDrills + current.currentDrillIndex,
                feedbackMessage = "Drill saved"
            )
            nextDrill()
        }
    }

    fun nextDrill() {
        val current = _uiState.value
        val newIndex = (current.currentDrillIndex + 1).coerceAtMost(current.drills.lastIndex)
        _uiState.value = current.copy(
            currentDrillIndex = newIndex,
            completed = current.completedDrills.size == current.drills.size,
            attempts = 0,
            successes = 0,
            direction = null
        )
    }

    fun previousDrill() {
        val current = _uiState.value
        val newIndex = (current.currentDrillIndex - 1).coerceAtLeast(0)
        _uiState.value = current.copy(
            currentDrillIndex = newIndex,
            attempts = 0,
            successes = 0,
            direction = null
        )
    }



    fun completeSession() {
        val current = _uiState.value
        if (current.sessionSaved) {
            _uiState.value = current.copy(feedbackMessage = "Session already saved")
            return
        }
        val definition = SeedSessions.weeklyPlan.first()
        viewModelScope.launch {
            val sessionId = repository.saveSession(definition)
            val drillIds = repository.getDrillIdsForSession(sessionId)
            _uiState.value = _uiState.value.copy(
                sessionId = sessionId,
                sessionSaved = true,
                savedDrillIds = drillIds,
                feedbackMessage = "Session complete and saved"
            )
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(feedbackMessage = null)
    }

    companion object {
        fun factory(repository: GolfRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PracticeSessionViewModel(repository) as T
                }
            }
    }
}
