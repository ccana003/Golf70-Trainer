package com.golf70.trainer.ui.session

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.golf70.trainer.R
import com.golf70.trainer.session.PracticeSessionViewModel
import com.golf70.trainer.ui.navigation.Dependencies

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeSessionScreen(
    sessionViewModel: PracticeSessionViewModel = viewModel(
        factory = PracticeSessionViewModel.factory(Dependencies.repository(LocalContext.current), LocalContext.current)
    )
) {
    val state by sessionViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                sessionViewModel.persistSessionState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(state.feedbackMessage) {
        state.feedbackMessage?.let {
            if (it == "Timer complete") {
                ToneGenerator(AudioManager.STREAM_ALARM, 100).startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1200)
            }
            snackbarHostState.showSnackbar(it)
            sessionViewModel.clearFeedback()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SnackbarHost(hostState = snackbarHostState) }

        item {
            Image(
                painter = painterResource(id = R.drawable.ic_golf70_logo),
                contentDescription = "Golf70 logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
            )
        }

        if (state.completed) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Session Complete", style = MaterialTheme.typography.headlineSmall)
                    Text("Week ${state.currentWeek}: ${state.phase}")
                    Text("Great work finishing today's structured training.")
                }
            }
            return@LazyColumn
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Week ${state.currentWeek} Training", style = MaterialTheme.typography.headlineSmall)
                Text("${state.phase} • ${state.focus}")
                Text("Choose session day:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.sessionLayouts.size) { index ->
                        val layout = state.sessionLayouts[index]
                        FilterChip(
                            selected = state.selectedLayoutIndex == index,
                            onClick = { sessionViewModel.selectLayout(index) },
                            label = { Text(layout.type) }
                        )
                    }
                }
                Button(
                    onClick = { sessionViewModel.advanceToNextWeek() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Move to Next Week")
                }
            }
        }

        val drill = state.currentDrill
        if (drill == null) {
            item { Text("No drill loaded.") }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Current drill: ${drill.title}", style = MaterialTheme.typography.titleMedium)
                    Text(drill.instructions)
                    Text("Next: ${state.nextDrill?.title ?: "Session complete"}", style = MaterialTheme.typography.bodySmall)
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Time remaining: ${formatTime(state.remainingSeconds)}", style = MaterialTheme.typography.titleLarge)
                    
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
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Drill ${state.currentDrillIndex + 1} of ${state.drills.size}", style = MaterialTheme.typography.labelSmall)
                    LinearProgressIndicator(
                        progress = { state.completedDrills.size.toFloat() / state.drills.size.coerceAtLeast(1) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = { sessionViewModel.startTimer() }, modifier = Modifier.weight(1f)) { Text("Start") }
                    Button(onClick = { if (state.timerRunning) sessionViewModel.pauseTimer() else sessionViewModel.resumeTimer() }, modifier = Modifier.weight(1.5f)) {
                        Text(if (state.timerRunning) "Pause" else "Resume")
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = { sessionViewModel.previousDrill() }, modifier = Modifier.weight(1f)) { Text("Back") }
                    if (!state.isLastDrill) {
                        Button(onClick = { sessionViewModel.nextDrill() }, modifier = Modifier.weight(1f)) { Text("Next") }
                    }
                }
            }

            item { Text("Quick logging", style = MaterialTheme.typography.titleSmall) }
            
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { sessionViewModel.logMetric(direction = "left") }, modifier = Modifier.weight(1f)) { Text("Left") }
                    Button(onClick = { sessionViewModel.logMetric(direction = "center") }, modifier = Modifier.weight(1f)) { Text("Center") }
                    Button(onClick = { sessionViewModel.logMetric(direction = "right") }, modifier = Modifier.weight(1f)) { Text("Right") }
                }
            }
            
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { sessionViewModel.logMetric(success = true) }, modifier = Modifier.weight(1f)) { Text("Made") }
                    Button(onClick = { sessionViewModel.logMetric(success = false) }, modifier = Modifier.weight(1f)) { Text("Missed") }
                }
            }

            item {
                Button(
                    onClick = {
                        if (state.isLastDrill) sessionViewModel.completeSession() else sessionViewModel.nextDrill()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(if (state.isLastDrill) "Complete Session" else "Save & Next Drill")
                }
            }
        }
    }
}

private fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
