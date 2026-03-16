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
    val completed: Boolean = false
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
            val sessionId = repository.saveSession(definition)
            _uiState.value = _uiState.value.copy(sessionId = sessionId)
            repository.saveGoal(GoalEntity())
        }
    }

    fun logMetric(direction: String? = null, success: Boolean? = null) {
        val current = _uiState.value
        _uiState.value = current.copy(
            attempts = current.attempts + 1,
            successes = current.successes + if (success == true) 1 else 0,
            direction = direction ?: current.direction
        )
    }

    fun completeCurrentDrill() {
        // Persisting by drill id is simplified in this starter implementation.
        val current = _uiState.value
        viewModelScope.launch {
            repository.saveDrillResult(
                drillId = current.currentDrillIndex + 1L,
                attempts = current.attempts,
                successes = current.successes,
                direction = current.direction,
                distance = null
            )
        }
        nextDrill()
    }

    fun nextDrill() {
        val current = _uiState.value
        val newIndex = current.currentDrillIndex + 1
        _uiState.value = if (newIndex >= current.drills.size) {
            current.copy(completed = true)
        } else {
            current.copy(
                currentDrillIndex = newIndex,
                attempts = 0,
                successes = 0,
                direction = null
            )
        }
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
