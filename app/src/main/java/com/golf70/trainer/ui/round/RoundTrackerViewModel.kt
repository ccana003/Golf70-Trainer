package com.golf70.trainer.ui.round

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.golf70.trainer.domain.HoleInput
import com.golf70.trainer.domain.RoundSummary
import com.golf70.trainer.repository.GolfRepository
import com.golf70.trainer.util.StatsCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RoundTrackerViewModel(
    private val repository: GolfRepository
) : ViewModel() {
    private val _holes = MutableStateFlow((1..18).map { hole ->
        HoleInput(hole, 4, false, false, 2, 0)
    })
    val holes: StateFlow<List<HoleInput>> = _holes.asStateFlow()

    fun updateHole(input: HoleInput) {
        _holes.value = _holes.value.map { if (it.holeNumber == input.holeNumber) input else it }
    }

    fun summary(): RoundSummary = StatsCalculator.summarizeRound(_holes.value)

    fun saveRound(course: String) {
        viewModelScope.launch {
            repository.saveRound(course, _holes.value)
        }
    }

    companion object {
        fun factory(repository: GolfRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RoundTrackerViewModel(repository) as T
                }
            }
    }
}
