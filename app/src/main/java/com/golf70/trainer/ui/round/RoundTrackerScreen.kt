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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoundTrackerScreen(
    onRoundSaved: () -> Unit = {},
    vm: RoundTrackerViewModel = viewModel(factory = RoundTrackerViewModel.factory(Dependencies.repository(LocalContext.current)))
) {
    var selectedCourseName by remember { mutableStateOf(RoundTrackerViewModel.ALWAYS_AVAILABLE_COURSE_NAME) }
    var courseDropdownExpanded by remember { mutableStateOf(false) }
    var holeRangeDropdownExpanded by remember { mutableStateOf(false) }
    val holes by vm.holes.collectAsState()
    val selectedRange by vm.selectedRange.collectAsState()
    val savedLayoutNames by vm.savedLayoutNames.collectAsState()
    val displayedHoles = remember(holes, selectedRange) { vm.displayedHoles() }
    val summary = remember(displayedHoles) { vm.summary() }
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
                    ExposedDropdownMenuBox(
                        expanded = courseDropdownExpanded,
                        onExpandedChange = { courseDropdownExpanded = !courseDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCourseName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Course Name") },
                            placeholder = { Text("Select a course") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        DropdownMenu(
                            expanded = courseDropdownExpanded,
                            onDismissRequest = { courseDropdownExpanded = false }
                        ) {
                            savedLayoutNames.forEach { name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        selectedCourseName = name
                                        courseDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = holeRangeDropdownExpanded,
                        onExpandedChange = { holeRangeDropdownExpanded = !holeRangeDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedRange.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Holes to Play") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = holeRangeDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        DropdownMenu(
                            expanded = holeRangeDropdownExpanded,
                            onDismissRequest = { holeRangeDropdownExpanded = false }
                        ) {
                            RoundTrackerViewModel.HoleRange.entries.forEach { range ->
                                DropdownMenuItem(
                                    text = { Text(range.label) },
                                    onClick = {
                                        vm.updateSelectedRange(range)
                                        holeRangeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { vm.loadCourseLayout(selectedCourseName) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Load Layout")
                        }
                        Button(onClick = { vm.saveCourseLayout(selectedCourseName) }, modifier = Modifier.weight(1f)) {
                            Text("Save Layout")
                        }
                    }
                }
            }

            items(displayedHoles, key = { it.holeNumber }) { hole ->
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
                    Text("${selectedRange.label} Score: ${summary.totalScore}", style = MaterialTheme.typography.titleLarge)
                    Text("Fairway %: ${summary.fairwayPercentage.toInt()}%  |  GIR %: ${summary.girPercentage.toInt()}%")
                    Text("Putts / Round: ${summary.puttsPerRound}")
                    Button(
                        onClick = { vm.saveRound(selectedCourseName, onRoundSaved) },
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
            label = "Score",
            showBlankWhenZero = true
        )
        CompactNumberField(
            value = hole.putts,
            onValueChange = { onChange(hole.copy(putts = it)) },
            label = "Putts",
            showBlankWhenZero = true
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
    label: String,
    showBlankWhenZero: Boolean = false
) {
    var fieldValue by remember(value) {
        val initialText = if (showBlankWhenZero && value == 0) "" else value.toString()
        mutableStateOf(TextFieldValue(initialText))
    }

    OutlinedTextField(
        value = fieldValue,
        onValueChange = {
            fieldValue = it
            if (it.text.isBlank()) {
                onValueChange(0)
                return@OutlinedTextField
            }
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
