package com.golf70.trainer.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.golf70.trainer.domain.WeeklyProgress
import com.golf70.trainer.repository.GolfRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProgressUiState(
    val loading: Boolean = true,
    val weeks: List<WeeklyProgress> = emptyList()
)

class ProgressViewModel(
    private val repository: GolfRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = ProgressUiState(loading = true)
            val nonEmptyWeeks = repository.weeklyProgress().filterNot { it.sessions == 0 && it.rounds == 0 }
            _uiState.value = ProgressUiState(loading = false, weeks = nonEmptyWeeks)
        }
    }

    companion object {
        fun factory(repository: GolfRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ProgressViewModel(repository) as T
                }
            }
    }
}
