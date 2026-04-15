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
        HoleInput(hole, 4, 0, false, false, 0, 0)
    })
    val holes: StateFlow<List<HoleInput>> = _holes.asStateFlow()

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()
    private val _savedLayoutNames = MutableStateFlow<List<String>>(emptyList())
    val savedLayoutNames: StateFlow<List<String>> = _savedLayoutNames.asStateFlow()

    private val _selectedRange = MutableStateFlow(HoleRange.WHOLE_18)
    val selectedRange: StateFlow<HoleRange> = _selectedRange.asStateFlow()

    init {
        seedAlwaysAvailableCourseLayout()
    }

    private fun seedAlwaysAvailableCourseLayout() {
        viewModelScope.launch {
            repository.saveCourseLayout(ALWAYS_AVAILABLE_COURSE_NAME, ALWAYS_AVAILABLE_COURSE_PARS)
            _holes.value = _holes.value.mapIndexed { index, hole ->
                hole.copy(par = ALWAYS_AVAILABLE_COURSE_PARS[index])
            }
            refreshSavedLayoutNames()
        }
    }

    fun updateSelectedRange(range: HoleRange) {
        _selectedRange.value = range
    }

    fun displayedHoles(): List<HoleInput> {
        val range = _selectedRange.value
        return _holes.value.filter { it.holeNumber in range.holes }
    }

    fun updateHole(input: HoleInput) {
        _holes.value = _holes.value.map { if (it.holeNumber == input.holeNumber) input else it }
    }

    fun summary(): RoundSummary = StatsCalculator.summarizeRound(displayedHoles())

    fun saveRound(course: String, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            val holesToSave = displayedHoles()
            repository.saveRound(course, holesToSave)
            _saveMessage.value = "Round saved successfully"
            _holes.value = _holes.value.map { hole ->
                if (hole.holeNumber in _selectedRange.value.holes) {
                    hole.copy(score = 0, fairwayHit = false, gir = false, putts = 0, penalty = 0)
                } else {
                    hole
                }
            }
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
            refreshSavedLayoutNames()
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

    fun refreshSavedLayoutNames() {
        viewModelScope.launch {
            _savedLayoutNames.value = repository.getSavedCourseLayoutNames()
        }
    }

    enum class HoleRange(val label: String, val holes: IntRange) {
        FRONT_9("Front 9", 1..9),
        BACK_9("Back 9", 10..18),
        WHOLE_18("Whole 18", 1..18)
    }

    companion object {
        const val ALWAYS_AVAILABLE_COURSE_NAME = "Dolphin/Marlin"
        val ALWAYS_AVAILABLE_COURSE_PARS = listOf(
            5, 3, 4, 4, 4, 4, 5, 3, 4,
            5, 5, 4, 3, 5, 4, 4, 3, 3
        )

        fun factory(repository: GolfRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RoundTrackerViewModel(repository) as T
                }
            }
    }
}
