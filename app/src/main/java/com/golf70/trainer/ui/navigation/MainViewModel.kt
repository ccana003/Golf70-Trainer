package com.golf70.trainer.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.golf70.trainer.data.local.PracticeSessionWithDrills
import com.golf70.trainer.data.local.RoundWithHoles
import com.golf70.trainer.domain.DashboardStats
import com.golf70.trainer.domain.WeeklyPlan
import com.golf70.trainer.repository.GolfRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WeeklyPlanUiState(
    val weekOffset: Int = 0,
    val loading: Boolean = true,
    val plan: WeeklyPlan? = null
)

class MainViewModel(
    private val repository: GolfRepository
) : ViewModel() {
    val dashboardStats: StateFlow<DashboardStats> = repository.dashboardStats.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardStats(0f, 0f, 0f, 0f)
    )

    val sessions: StateFlow<List<PracticeSessionWithDrills>> = repository.sessions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val rounds: StateFlow<List<RoundWithHoles>> = repository.rounds.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _weeklyPlanState = MutableStateFlow(WeeklyPlanUiState())
    val weeklyPlanState: StateFlow<WeeklyPlanUiState> = _weeklyPlanState.asStateFlow()

    init {
        loadWeeklyPlan()
    }

    fun changeWeek(offsetDelta: Int) {
        _weeklyPlanState.value = _weeklyPlanState.value.copy(
            weekOffset = _weeklyPlanState.value.weekOffset + offsetDelta,
            loading = true
        )
        loadWeeklyPlan()
    }

    private fun loadWeeklyPlan() {
        val offset = _weeklyPlanState.value.weekOffset
        viewModelScope.launch {
            val plan = repository.getWeeklyPlanWithProgress(offset)
            _weeklyPlanState.value = _weeklyPlanState.value.copy(loading = false, plan = plan)
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            loadWeeklyPlan()
        }
    }

    fun deleteRound(roundId: Long) {
        viewModelScope.launch {
            repository.deleteRound(roundId)
            loadWeeklyPlan()
        }
    }

    companion object {
        fun factory(repository: GolfRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(repository) as T
                }
            }
    }
}
