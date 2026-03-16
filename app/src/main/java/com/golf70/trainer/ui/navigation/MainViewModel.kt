package com.golf70.trainer.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.golf70.trainer.domain.DashboardStats
import com.golf70.trainer.repository.GolfRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MainViewModel(
    repository: GolfRepository
) : ViewModel() {
    val dashboardStats: StateFlow<DashboardStats> = repository.dashboardStats.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardStats(0f, 0f, 0f, 0f)
    )

    companion object {
        fun factory(repository: GolfRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(repository) as T
                }
            }
    }
}
