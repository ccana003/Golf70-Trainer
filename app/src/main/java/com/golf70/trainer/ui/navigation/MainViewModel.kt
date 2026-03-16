package com.golf70.trainer.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.golf70.trainer.data.local.PracticeSessionWithDrills
import com.golf70.trainer.data.local.RoundWithHoles
import com.golf70.trainer.domain.DashboardStats
import com.golf70.trainer.repository.GolfRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

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

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
        }
    }

    fun deleteRound(roundId: Long) {
        viewModelScope.launch {
            repository.deleteRound(roundId)
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
