package com.golf70.trainer.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.golf70.trainer.session.PracticeSessionViewModel
import com.golf70.trainer.ui.navigation.Dependencies
import com.golf70.trainer.spotify.SpotifyController
import com.golf70.trainer.timer.DrillTimerViewModel

@Composable
fun PracticeSessionScreen(
    sessionViewModel: PracticeSessionViewModel = viewModel(
        factory = PracticeSessionViewModel.factory(Dependencies.repository(LocalContext.current))
    ),
    timerViewModel: DrillTimerViewModel = viewModel(),
    spotifyController: SpotifyController = SpotifyController()
) {
    val state by sessionViewModel.uiState.collectAsState()
    val remaining by timerViewModel.remainingSeconds.collectAsState()
    val running by timerViewModel.isRunning.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Practice Session", style = MaterialTheme.typography.headlineSmall)
        val drill = state.currentDrill
        if (drill == null) {
            Text("No drill loaded.")
            return@Column
        }

        Text("Current drill: ${drill.title}")
        Text(drill.instructions)
        Text("Next: ${state.nextDrill?.title ?: "Session complete"}")
        Text("Time remaining: ${remaining}s")

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
            Button(onClick = { sessionViewModel.nextDrill(); timerViewModel.reset() }) { Text("Skip") }
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

        Text("Spotify")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { spotifyController.connect() }) { Text("Connect") }
            Button(onClick = { spotifyController.playPlaylist() }) { Text("Play") }
            Button(onClick = { spotifyController.pause() }) { Text("Pause") }
            Button(onClick = { spotifyController.skip() }) { Text("Skip") }
        }
    }
}
