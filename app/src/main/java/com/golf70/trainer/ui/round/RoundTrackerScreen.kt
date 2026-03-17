package com.golf70.trainer.ui.round

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.golf70.trainer.domain.HoleInput
import com.golf70.trainer.ui.navigation.Dependencies

@Composable
fun RoundTrackerScreen(
    onRoundSaved: () -> Unit = {},
    vm: RoundTrackerViewModel = viewModel(factory = RoundTrackerViewModel.factory(Dependencies.repository(LocalContext.current)))
) {
    var course by remember { mutableStateOf("Home Course") }
    val holes by vm.holes.collectAsState()
    val summary = vm.summary()
    val saveMessage by vm.saveMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(saveMessage) {
        saveMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearSaveMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Round Tracker", style = MaterialTheme.typography.headlineSmall)
                    OutlinedTextField(
                        value = course,
                        onValueChange = { course = it },
                        label = { Text("Course Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.loadCourseLayout(course) }, modifier = Modifier.weight(1f)) {
                            Text("Load Layout")
                        }
                        Button(onClick = { vm.saveCourseLayout(course) }, modifier = Modifier.weight(1f)) {
                            Text("Save Layout")
                        }
                    }
                }
            }

            items(holes, key = { it.holeNumber }) { hole ->
                HoleRow(hole = hole, onChange = vm::updateHole)
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Summary", style = MaterialTheme.typography.titleMedium)
                    Text("Total Score: ${summary.totalScore}", style = MaterialTheme.typography.titleLarge)
                    Text("Fairway %: ${summary.fairwayPercentage.toInt()}%  |  GIR %: ${summary.girPercentage.toInt()}%")
                    Text("Putts / Round: ${summary.puttsPerRound}")
                    Button(
                        onClick = { vm.saveRound(course, onRoundSaved) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Round")
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun HoleRow(hole: HoleInput, onChange: (HoleInput) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "H${hole.holeNumber}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(32.dp)
        )
        
        CompactNumberField(
            value = hole.par,
            onValueChange = { onChange(hole.copy(par = it)) },
            label = "Par"
        )
        CompactNumberField(
            value = hole.score,
            onValueChange = { onChange(hole.copy(score = it)) },
            label = "Score"
        )
        CompactNumberField(
            value = hole.putts,
            onValueChange = { onChange(hole.copy(putts = it)) },
            label = "Putts"
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("FW", style = MaterialTheme.typography.labelSmall)
            Checkbox(
                checked = hole.fairwayHit,
                onCheckedChange = { onChange(hole.copy(fairwayHit = it)) }
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("GIR", style = MaterialTheme.typography.labelSmall)
            Checkbox(
                checked = hole.gir,
                onCheckedChange = { onChange(hole.copy(gir = it)) }
            )
        }
    }
}

@Composable
private fun CompactNumberField(
    value: Int,
    onValueChange: (Int) -> Unit,
    label: String
) {
    var fieldValue by remember(value) {
        mutableStateOf(TextFieldValue(value.toString()))
    }

    OutlinedTextField(
        value = fieldValue,
        onValueChange = {
            fieldValue = it
            val numeric = it.text.toIntOrNull() ?: return@OutlinedTextField
            onValueChange(numeric)
        },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier
            .width(64.dp)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    fieldValue = fieldValue.copy(selection = TextRange(0, fieldValue.text.length))
                }
            },
        textStyle = MaterialTheme.typography.bodyMedium
    )
}
