package com.golf70.trainer.ui.swing

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

enum class CameraSetupType(val title: String, val tips: List<String>) {
    DOWN_THE_LINE(
        title = "Down-the-line",
        tips = listOf(
            "Place the camera at hand height.",
            "Aim parallel to the target line.",
            "Keep feet, club, and ball in frame."
        )
    ),
    FACE_ON(
        title = "Face-on",
        tips = listOf(
            "Place the camera chest high.",
            "Center your sternum in frame.",
            "Keep full club motion visible."
        )
    )
}

data class SwingLine(
    val start: Offset,
    val end: Offset
)

data class SwingAnalysisUiState(
    val videoUri: Uri? = null,
    val isPlaying: Boolean = false,
    val speed: Float = 1f,
    val setupType: CameraSetupType = CameraSetupType.DOWN_THE_LINE,
    val baseline: SwingLine? = null,
    val swingLines: List<SwingLine> = emptyList(),
    val isDrawingSwingPath: Boolean = false
)

class SwingAnalysisViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SwingAnalysisUiState())
    val uiState: StateFlow<SwingAnalysisUiState> = _uiState

    fun setVideo(uri: Uri) {
        _uiState.update { it.copy(videoUri = uri, isPlaying = false) }
    }

    fun setSetupType(type: CameraSetupType) {
        _uiState.update { it.copy(setupType = type) }
    }

    fun setSpeed(speed: Float) {
        _uiState.update { it.copy(speed = speed) }
    }

    fun setIsPlaying(isPlaying: Boolean) {
        _uiState.update { it.copy(isPlaying = isPlaying) }
    }

    fun toggleDrawMode() {
        _uiState.update { it.copy(isDrawingSwingPath = !it.isDrawingSwingPath) }
    }

    fun setBaseline(start: Offset, end: Offset) {
        _uiState.update { it.copy(baseline = SwingLine(start, end)) }
    }

    fun addSwingSegment(start: Offset, end: Offset) {
        _uiState.update { state ->
            state.copy(swingLines = state.swingLines + SwingLine(start, end))
        }
    }

    fun clearOverlays() {
        _uiState.update { it.copy(baseline = null, swingLines = emptyList()) }
    }
}
