package com.golf70.trainer.ui.swing

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun SwingAnalysisScreen(
    swingViewModel: SwingAnalysisViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by swingViewModel.uiState.collectAsState()

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = false
        }
    }

    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            swingViewModel.setVideo(it)
            player.setMediaItem(MediaItem.fromUri(it))
            player.prepare()
        }
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    DisposableEffect(state.speed) {
        player.playbackParameters = PlaybackParameters(state.speed)
        onDispose { }
    }

    DisposableEffect(state.isPlaying) {
        if (state.isPlaying) player.play() else player.pause()
        onDispose { }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Swing Analysis", style = MaterialTheme.typography.headlineSmall)
            Text("Set camera view, draw swing plane, and review in slow motion.")
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CameraSetupType.entries.forEach { type ->
                    FilterChip(
                        selected = state.setupType == type,
                        onClick = { swingViewModel.setSetupType(type) },
                        label = { Text(type.title) }
                    )
                }
            }
        }

        item {
            Column {
                Text("Camera setup tips", style = MaterialTheme.typography.titleMedium)
                state.setupType.tips.forEach { tip ->
                    Text("• $tip")
                }
            }
        }

        item {
            Button(
                onClick = {
                    pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Import swing video")
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .background(Color.Black)
            ) {
                if (state.videoUri != null) {
                    AndroidView(
                        factory = {
                            PlayerView(it).apply {
                                useController = false
                                this.player = player
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = "Import a swing video to start analysis",
                        color = Color.White,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                var dragStart by remember { mutableStateOf<Offset?>(null) }
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(state.isDrawingSwingPath) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    dragStart = offset
                                },
                                onDragEnd = {
                                    dragStart = null
                                }
                            ) { change, _ ->
                                val start = dragStart ?: change.position
                                val end = change.position
                                if (state.isDrawingSwingPath) {
                                    swingViewModel.addSwingSegment(start, end)
                                } else {
                                    swingViewModel.setBaseline(start, end)
                                }
                                dragStart = end
                            }
                        }
                ) {
                    state.baseline?.let { base ->
                        drawLine(
                            color = Color(0xFF31D843),
                            start = base.start,
                            end = base.end,
                            strokeWidth = 6f,
                            cap = StrokeCap.Round
                        )
                    }

                    state.swingLines.forEach { line ->
                        val deviationColor = baselineDeviationColor(state.baseline, line)
                        drawLine(
                            color = deviationColor,
                            start = line.start,
                            end = line.end,
                            strokeWidth = 5f,
                            style = Stroke(width = 5f)
                        )
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { swingViewModel.setIsPlaying(!state.isPlaying) }) {
                    Text(if (state.isPlaying) "Pause" else "Play")
                }
                listOf(1f, 0.5f, 0.25f, 0.1f).forEach { speed ->
                    FilterChip(
                        selected = state.speed == speed,
                        onClick = { swingViewModel.setSpeed(speed) },
                        label = { Text("${speed}x") }
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { swingViewModel.toggleDrawMode() }, modifier = Modifier.weight(1f)) {
                    Text(if (state.isDrawingSwingPath) "Drawing: Swing Path" else "Drawing: Baseline")
                }
                Button(onClick = { swingViewModel.clearOverlays() }, modifier = Modifier.weight(1f)) {
                    Text("Clear lines")
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 4.dp)) {
                ColorKey(Color(0xFF31D843), "Ideal plane")
                ColorKey(Color(0xFFFDD835), "Close to plane")
                ColorKey(Color(0xFFE53935), "Off plane")
            }
        }
    }
}

@Composable
private fun ColorKey(color: Color, label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(12.dp).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun baselineDeviationColor(baseline: SwingLine?, swingLine: SwingLine): Color {
    if (baseline == null) return Color(0xFFFDD835)

    val baselineVector = baseline.end - baseline.start
    val swingVector = swingLine.end - swingLine.start
    val baselineAngle = kotlin.math.atan2(baselineVector.y, baselineVector.x)
    val swingAngle = kotlin.math.atan2(swingVector.y, swingVector.x)
    val diffDegrees = kotlin.math.abs((swingAngle - baselineAngle) * 180f / Math.PI.toFloat())

    return when {
        diffDegrees <= 10f -> Color(0xFFFDD835)
        else -> Color(0xFFE53935)
    }
}

private operator fun Offset.minus(other: Offset): Offset = Offset(x - other.x, y - other.y)
