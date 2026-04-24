package com.golf70.trainer.ui.swing

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@Composable
fun SwingAnalysisScreen(
    swingViewModel: SwingAnalysisViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by swingViewModel.uiState.collectAsState()

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = false
            volume = 0f
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

    LaunchedEffect(player, state.videoUri, state.isPlaying) {
        while (true) {
            val duration = player.duration.takeIf { it > 0 } ?: 0L
            val position = player.currentPosition.coerceAtLeast(0L)
            swingViewModel.updatePlaybackPosition(positionMs = position, durationMs = duration)
            delay(100L)
        }
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
                        .pointerInput(state.isDrawingSwingPath, state.durationMs, state.videoUri, state.isScrubGestureEnabled) {
                            if (!state.isScrubGestureEnabled || state.videoUri == null) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        dragStart = offset.clampTo(size.width.toFloat(), size.height.toFloat())
                                    },
                                    onDragEnd = {
                                        dragStart = null
                                    }
                                ) { change, _ ->
                                    val start = (dragStart ?: change.position).clampTo(
                                        maxWidth = size.width.toFloat(),
                                        maxHeight = size.height.toFloat()
                                    )
                                    val end = change.position.clampTo(
                                        maxWidth = size.width.toFloat(),
                                        maxHeight = size.height.toFloat()
                                    )
                                    if (state.isDrawingSwingPath) {
                                        swingViewModel.addSwingSegment(start, end)
                                    } else {
                                        swingViewModel.setBaseline(start, end)
                                    }
                                    dragStart = null
                                }
                            } else {
                                detectHorizontalDragGestures { _, dragAmount ->
                                    if (state.durationMs <= 0L) return@detectHorizontalDragGestures
                                    val deltaMs = (dragAmount * 12f).toLong()
                                    val target = (player.currentPosition + deltaMs).coerceIn(0L, state.durationMs)
                                    player.seekTo(target)
                                    swingViewModel.updatePlaybackPosition(target, state.durationMs)
                                }
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
                            cap = StrokeCap.Round
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
                Button(onClick = { seekBy(player, state, -1000L, swingViewModel) }) { Text("-1s") }
                Button(onClick = { seekBy(player, state, 1000L, swingViewModel) }) { Text("+1s") }
                Button(onClick = {
                    player.seekTo(0L)
                    swingViewModel.setIsPlaying(false)
                    swingViewModel.updatePlaybackPosition(0L, state.durationMs)
                }) { Text("Restart") }
            }
        }

        item {
            Text(
                text = if (state.isScrubGestureEnabled) {
                    "Drag left/right on video to scrub"
                } else {
                    "Gesture mode is Draw (switch to Scrub to drag video)"
                },
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Button(onClick = { swingViewModel.toggleGestureMode() }, modifier = Modifier.weight(1f)) {
                    Text(if (state.isScrubGestureEnabled) "Gesture: Scrub" else "Gesture: Draw")
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

private fun Offset.clampTo(maxWidth: Float, maxHeight: Float): Offset =
    Offset(x = x.coerceIn(0f, maxWidth), y = y.coerceIn(0f, maxHeight))

private fun seekBy(
    player: ExoPlayer,
    state: SwingAnalysisUiState,
    deltaMs: Long,
    swingViewModel: SwingAnalysisViewModel
) {
    val duration = state.durationMs.takeIf { it > 0 } ?: return
    val target = (player.currentPosition + deltaMs).coerceIn(0L, duration)
    player.seekTo(target)
    swingViewModel.updatePlaybackPosition(target, duration)
}
