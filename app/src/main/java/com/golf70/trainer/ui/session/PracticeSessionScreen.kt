package com.golf70.trainer.ui.session

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.golf70.trainer.session.PracticeSessionViewModel
import com.golf70.trainer.timer.DrillTimerViewModel
import com.golf70.trainer.ui.navigation.Dependencies

@Composable
fun PracticeSessionScreen(
    sessionViewModel: PracticeSessionViewModel = viewModel(
        factory = PracticeSessionViewModel.factory(Dependencies.repository(LocalContext.current))
    ),
    timerViewModel: DrillTimerViewModel = viewModel()
) {
    val state by sessionViewModel.uiState.collectAsState()
    val remaining by timerViewModel.remainingSeconds.collectAsState()
    val running by timerViewModel.isRunning.collectAsState()
    val finishedCount by timerViewModel.finishedCount.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.feedbackMessage) {
        state.feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            sessionViewModel.clearFeedback()
        }
    }

    LaunchedEffect(finishedCount) {
        if (finishedCount > 0) {
            ToneGenerator(AudioManager.STREAM_ALARM, 100).startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1200)
            snackbarHostState.showSnackbar("Timer complete")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SnackbarHost(hostState = snackbarHostState)
        Text("Practice Session", style = MaterialTheme.typography.headlineSmall)
        val drill = state.currentDrill
        if (drill == null) {
            Text("No drill loaded.")
            return@Column
        }

        Text("Current drill: ${drill.title}")
        Text(drill.instructions)
        Text("Next: ${state.nextDrill?.title ?: "Session complete"}")
        Text("Time remaining: ${formatTime(remaining)}")
        Text("Completed drills: ${state.completedDrills.size}/${state.drills.size}")

        LinearProgressIndicator(
            progress = {
                ((state.currentDrillIndex + 1).toFloat() / state.drills.size.coerceAtLeast(1))
            },
            modifier = Modifier.fillMaxWidth()
        )
        Text("Drill ${state.currentDrillIndex + 1} of ${state.drills.size}")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { timerViewModel.start(drill.timerSeconds) }) { Text("Start") }
            Button(onClick = { if (running) timerViewModel.pause() else timerViewModel.resume() }) {
                Text(if (running) "Pause" else "Resume")
            }
            Button(onClick = { sessionViewModel.previousDrill(); timerViewModel.reset() }) { Text("Previous") }
            Button(onClick = { sessionViewModel.nextDrill(); timerViewModel.reset() }) { Text("Next") }
        }

        Text("Quick logging")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { sessionViewModel.logMetric(direction = "left") }) { Text("Left") }
            Button(onClick = { sessionViewModel.logMetric(direction = "center") }) { Text("Center") }
            Button(onClick = { sessionViewModel.logMetric(direction = "right") }) { Text("Right") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { sessionViewModel.logMetric(success = true) }) { Text("Made") }
            Button(onClick = { sessionViewModel.logMetric(success = false) }) { Text("Missed") }
            Button(onClick = { sessionViewModel.completeCurrentDrill() }) { Text("Save Drill") }
        }
    }
}

private fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
