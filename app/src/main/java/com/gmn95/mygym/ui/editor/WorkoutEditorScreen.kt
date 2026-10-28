package com.gmn95.mygym.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gmn95.mygym.R
import com.gmn95.mygym.data.repository.WorkoutExercise
import com.gmn95.mygym.domain.WorkoutInputValidator
import com.gmn95.mygym.domain.WorkoutItemConfiguration
import com.gmn95.mygym.feature.editor.WorkoutEditorUiState
import com.gmn95.mygym.ui.components.BackNavigationHeader

@Composable
fun WorkoutEditorScreen(
    uiState: WorkoutEditorUiState,
    onBack: () -> Unit,
    onRenameWorkout: (String) -> Unit,
    onSearchCatalog: () -> Unit,
    onOpenSavedExercises: () -> Unit,
    onCreateAndAddExercise: (String, String?) -> Unit,
    onUpdateExercise: (String, WorkoutItemConfiguration) -> Unit,
    onRemoveExercise: (String) -> Unit,
    onMoveExercise: (String, Int) -> Unit,
    onOperationErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    var showAddExerciseDialog by rememberSaveable { mutableStateOf(false) }
    var exerciseToEdit by remember { mutableStateOf<WorkoutExercise?>(null) }
    var exerciseToRemove by remember { mutableStateOf<WorkoutExercise?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val operationError = stringResource(R.string.operation_error)

    LaunchedEffect(uiState.operationFailed) {
        if (uiState.operationFailed) {
            snackbarHostState.showSnackbar(operationError)
            onOperationErrorShown()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when {
            uiState.isLoading -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }

            uiState.workout == null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(R.string.workout_not_found))
                Button(
                    onClick = onBack,
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Text(stringResource(R.string.back))
                }
            }

            else -> {
                val workout = uiState.workout
                var workoutName by rememberSaveable(workout.id, workout.name) {
                    mutableStateOf(workout.name)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        BackNavigationHeader(
                            title = stringResource(R.string.edit_workout),
                            onBack = onBack,
                        )
                    }

                    item {
                        Text(
                            text = stringResource(R.string.workout_editor_description),
                            modifier = Modifier.padding(horizontal = 20.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    item {
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            OutlinedTextField(
                                value = workoutName,
                                onValueChange = { workoutName = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.workout_name)) },
                                singleLine = true,
                            )
                            Button(
                                onClick = { onRenameWorkout(workoutName.trim()) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                enabled = workoutName.isNotBlank() && workoutName.trim() != workout.name,
                            ) {
                                Text(stringResource(R.string.save_name))
                            }
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.exercises),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text = stringResource(R.string.exercises_description),
                                modifier = Modifier.padding(top = 4.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(
                                onClick = { showAddExerciseDialog = true },
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            ) {
                                Text(stringResource(R.string.add_exercise))
                            }
                        }
                    }

                    if (workout.exercises.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.no_exercises_description),
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = workout.exercises,
                            key = { _, exercise -> exercise.id },
                        ) { index, exercise ->
                            ExerciseCard(
                                exercise = exercise,
                                canMoveUp = index > 0,
                                canMoveDown = index < workout.exercises.lastIndex,
                                onMoveUp = { onMoveExercise(exercise.id, -1) },
                                onMoveDown = { onMoveExercise(exercise.id, 1) },
                                onEdit = { exerciseToEdit = exercise },
                                onRemove = { exerciseToRemove = exercise },
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                        }
                    }

                }
            }
        }
    }

    if (showAddExerciseDialog) {
        AddExerciseDialog(
            onDismiss = { showAddExerciseDialog = false },
            onSearchCatalog = {
                showAddExerciseDialog = false
                onSearchCatalog()
            },
            onOpenSavedExercises = {
                showAddExerciseDialog = false
                onOpenSavedExercises()
            },
            onCreate = { name, notes ->
                onCreateAndAddExercise(name, notes)
                showAddExerciseDialog = false
            },
        )
    }

    exerciseToEdit?.let { exercise ->
        EditConfigurationDialog(
            exercise = exercise,
            onDismiss = { exerciseToEdit = null },
            onConfirm = { configuration ->
                onUpdateExercise(exercise.id, configuration)
                exerciseToEdit = null
            },
        )
    }

    exerciseToRemove?.let { exercise ->
        AlertDialog(
            onDismissRequest = { exerciseToRemove = null },
            title = { Text(stringResource(R.string.remove_exercise_title)) },
            text = { Text(stringResource(R.string.remove_exercise_message, exercise.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveExercise(exercise.id)
                        exerciseToRemove = null
                    },
                ) {
                    Text(stringResource(R.string.remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToRemove = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun ExerciseCard(
    exercise: WorkoutExercise,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val configuration = exercise.configuration
    val repetitions = configuration.repetitionsMax?.let {
        stringResource(R.string.repetition_range, configuration.repetitionsMin, it)
    } ?: configuration.repetitionsMin.toString()
    val rest = if (configuration.restSeconds >= 60) {
        stringResource(
            R.string.rest_minutes_seconds,
            configuration.restSeconds / 60,
            configuration.restSeconds % 60,
        )
    } else {
        stringResource(R.string.rest_seconds, configuration.restSeconds)
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = exercise.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
            )
            exercise.notes?.let { notes ->
                Text(
                    text = notes,
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = stringResource(
                    R.string.exercise_configuration,
                    configuration.sets,
                    repetitions,
                    rest,
                ),
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.primary,
            )
            OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text(stringResource(R.string.edit))
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                TextButton(onClick = onMoveUp, modifier = Modifier.weight(1f), enabled = canMoveUp) {
                    Text(stringResource(R.string.move_up))
                }
                TextButton(onClick = onMoveDown, modifier = Modifier.weight(1f), enabled = canMoveDown) {
                    Text(stringResource(R.string.move_down))
                }
            }
            TextButton(onClick = onRemove, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.remove))
            }
        }
    }
}

@Composable
private fun AddExerciseDialog(
    onDismiss: () -> Unit,
    onSearchCatalog: () -> Unit,
    onOpenSavedExercises: () -> Unit,
    onCreate: (String, String?) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_exercise)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Button(onClick = onSearchCatalog, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.search_catalog))
                }
                OutlinedButton(
                    onClick = onOpenSavedExercises,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    Text(stringResource(R.string.choose_saved_exercise))
                }
                Text(
                    text = stringResource(R.string.or_create_exercise),
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.titleSmall,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    label = { Text(stringResource(R.string.exercise_name)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    label = { Text(stringResource(R.string.notes_optional)) },
                    minLines = 2,
                    maxLines = 4,
                )
                Text(
                    text = stringResource(R.string.default_configuration_hint),
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name.trim(), notes.trim().ifBlank { null }) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.create_and_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun EditConfigurationDialog(
    exercise: WorkoutExercise,
    onDismiss: () -> Unit,
    onConfirm: (WorkoutItemConfiguration) -> Unit,
) {
    val current = exercise.configuration
    var sets by rememberSaveable(exercise.id) { mutableStateOf(current.sets.toString()) }
    var repetitionsMin by rememberSaveable(exercise.id) {
        mutableStateOf(current.repetitionsMin.toString())
    }
    var repetitionsMax by rememberSaveable(exercise.id) {
        mutableStateOf(current.repetitionsMax?.toString().orEmpty())
    }
    var restMinutes by rememberSaveable(exercise.id) {
        mutableStateOf((current.restSeconds / 60).toString())
    }
    var restSeconds by rememberSaveable(exercise.id) {
        mutableStateOf((current.restSeconds % 60).toString())
    }

    val configuration = configurationOrNull(
        sets = sets,
        repetitionsMin = repetitionsMin,
        repetitionsMax = repetitionsMax,
        restMinutes = restMinutes,
        restSeconds = restSeconds,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_exercise, exercise.name)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                NumberField(
                    value = sets,
                    onValueChange = { sets = it },
                    label = stringResource(R.string.sets),
                )
                NumberField(
                    value = repetitionsMin,
                    onValueChange = { repetitionsMin = it },
                    label = stringResource(R.string.repetitions_fixed_or_minimum),
                )
                NumberField(
                    value = repetitionsMax,
                    onValueChange = { repetitionsMax = it },
                    label = stringResource(R.string.repetitions_maximum_optional),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NumberField(
                        value = restMinutes,
                        onValueChange = { restMinutes = it },
                        label = stringResource(R.string.rest_minutes),
                        modifier = Modifier.weight(1f),
                    )
                    NumberField(
                        value = restSeconds,
                        onValueChange = { restSeconds = it },
                        label = stringResource(R.string.rest_seconds_field),
                        modifier = Modifier.weight(1f),
                    )
                }
                if (configuration == null) {
                    Text(
                        text = stringResource(R.string.invalid_configuration),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { configuration?.let(onConfirm) },
                enabled = configuration != null,
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (input.all(Char::isDigit)) onValueChange(input)
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
    )
}

private fun configurationOrNull(
    sets: String,
    repetitionsMin: String,
    repetitionsMax: String,
    restMinutes: String,
    restSeconds: String,
): WorkoutItemConfiguration? {
    val parsedSets = sets.toIntOrNull() ?: return null
    val parsedMin = repetitionsMin.toIntOrNull() ?: return null
    val parsedMax = repetitionsMax.takeIf(String::isNotBlank)?.toIntOrNull()
        ?: if (repetitionsMax.isBlank()) null else return null
    val parsedMinutes = restMinutes.ifBlank { "0" }.toLongOrNull() ?: return null
    val parsedSeconds = restSeconds.ifBlank { "0" }.toLongOrNull() ?: return null
    if (parsedSeconds !in 0..59) return null
    val totalRest = parsedMinutes * 60 + parsedSeconds
    if (totalRest > Int.MAX_VALUE) return null

    return WorkoutItemConfiguration(
        sets = parsedSets,
        repetitionsMin = parsedMin,
        repetitionsMax = parsedMax,
        restSeconds = totalRest.toInt(),
    ).takeIf(WorkoutInputValidator::isValidConfiguration)
}
