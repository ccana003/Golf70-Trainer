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
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.golf70.trainer.session.PracticeSessionViewModel
import com.golf70.trainer.timer.DrillTimerViewModel
import com.golf70.trainer.ui.navigation.Dependencies

@OptIn(ExperimentalMaterial3Api::class)
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
    var expanded by remember { mutableStateOf(false) }

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

        Text("Session Layout", style = MaterialTheme.typography.labelLarge)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            val selectedLayout = state.sessionLayouts.getOrNull(state.selectedLayoutIndex)?.type ?: "Default Layout"
            OutlinedTextField(
                value = selectedLayout,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                val options = if (state.sessionLayouts.isEmpty()) listOf("Default Layout") else state.sessionLayouts.map { it.type }
                options.forEachIndexed { index, label ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            sessionViewModel.selectLayout(index)
                            timerViewModel.reset()
                            expanded = false
                        }
                    )
                }
            }
        }

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

        val badgeColor = if (state.sessionSaved) Color(0xFF2E7D32) else Color(0xFFEF6C00)
        FilterChip(
            selected = state.sessionSaved,
            onClick = {},
            enabled = false,
            label = { Text("● ${if (state.sessionSaved) "Saved" else "Unsaved"}") },
            colors = FilterChipDefaults.filterChipColors(
                disabledContainerColor = badgeColor.copy(alpha = 0.15f),
                disabledLabelColor = badgeColor
            )
        )

        Text("Drill ${state.currentDrillIndex + 1} of ${state.drills.size}")
        LinearProgressIndicator(
            progress = { state.completedDrills.size.toFloat() / state.drills.size.coerceAtLeast(1) },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
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
            Button(onClick = { sessionViewModel.completeCurrentDrill() }) { Text("Save Drill Now") }
        }

        Button(
            onClick = { sessionViewModel.completeSession() },
            enabled = !state.sessionSaved,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.sessionSaved) "Finalize Session" else "Save + Finalize")
        }
    }
}

private fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
