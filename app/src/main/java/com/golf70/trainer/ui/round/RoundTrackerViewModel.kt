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
        HoleInput(hole, 4, 4, false, false, 2, 0)
    })
    val holes: StateFlow<List<HoleInput>> = _holes.asStateFlow()

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

    fun updateHole(input: HoleInput) {
        _holes.value = _holes.value.map { if (it.holeNumber == input.holeNumber) input else it }
    }

    fun summary(): RoundSummary = StatsCalculator.summarizeRound(_holes.value)

    fun saveRound(course: String, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveRound(course, _holes.value)
            _saveMessage.value = "Round saved successfully"
            _holes.value = (1..18).map { hole -> HoleInput(hole, 4, 4, false, false, 2, 0) }
            onSaved()
        }
    }

    fun saveCourseLayout(course: String) {
        val normalized = course.trim()
        if (normalized.isBlank()) {
            _saveMessage.value = "Enter a course name first"
            return
        }
        viewModelScope.launch {
            repository.saveCourseLayout(normalized, _holes.value.map { it.par })
            _saveMessage.value = "Saved layout for $normalized"
        }
    }

    fun loadCourseLayout(course: String) {
        val normalized = course.trim()
        if (normalized.isBlank()) {
            _saveMessage.value = "Enter a course name first"
            return
        }
        viewModelScope.launch {
            val pars = repository.getCourseLayout(normalized)
            if (pars == null) {
                _saveMessage.value = "No saved layout for $normalized"
                return@launch
            }
            _holes.value = _holes.value.mapIndexed { index, hole ->
                hole.copy(par = pars[index])
            }
            _saveMessage.value = "Loaded layout for $normalized"
        }
    }

    fun clearSaveMessage() {
        _saveMessage.value = null
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
