package com.gmn95.mygym.ui.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gmn95.mygym.R
import com.gmn95.mygym.data.repository.ActiveRestTimer
import com.gmn95.mygym.data.repository.ActiveSessionExercise
import com.gmn95.mygym.data.repository.RestTimerStatus
import com.gmn95.mygym.data.repository.WorkoutSessionStatus
import com.gmn95.mygym.feature.session.SessionUiState

@Composable
fun SessionScreen(
    uiState: SessionUiState,
    onBack: () -> Unit,
    onPauseSession: () -> Unit,
    onResumeSession: () -> Unit,
    onSetSeriesCompleted: (String, String, Boolean) -> Unit,
    onSetExerciseCompleted: (String, Boolean) -> Unit,
    onPauseRest: () -> Unit,
    onResumeRest: () -> Unit,
    onResetRest: () -> Unit,
    onAdjustRest: (Long) -> Unit,
    onEndRest: () -> Unit,
    onCompleteSession: () -> Unit,
    onCancelSession: () -> Unit,
    onOperationErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    var showCompleteConfirmation by rememberSaveable { mutableStateOf(false) }
    var showCancelConfirmation by rememberSaveable { mutableStateOf(false) }
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

            uiState.session == null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(R.string.no_active_session))
                Button(
                    onClick = onBack,
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Text(stringResource(R.string.back))
                }
            }

            else -> {
                val session = uiState.session
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = onBack) {
                                Text(stringResource(R.string.back))
                            }
                            Text(
                                text = stringResource(R.string.workout_in_progress),
                                modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.headlineSmall,
                            )
                        }
                    }

                    item {
                        SessionTimerCard(
                            workoutName = session.workoutName,
                            status = session.status,
                            activeDurationMillis = uiState.activeDurationMillis,
                            onPause = onPauseSession,
                            onResume = onResumeSession,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }

                    item {
                        Text(
                            text = stringResource(R.string.planned_exercises),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }

                    items(session.exercises, key = ActiveSessionExercise::id) { exercise ->
                        SessionExerciseCard(
                            exercise = exercise,
                            onSeriesCompletedChange = { setId, completed ->
                                onSetSeriesCompleted(exercise.id, setId, completed)
                            },
                            onExerciseCompletedChange = { completed ->
                                onSetExerciseCompleted(exercise.id, completed)
                            },
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            OutlinedButton(
                                onClick = { showCancelConfirmation = true },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(R.string.cancel_workout))
                            }
                            Button(
                                onClick = { showCompleteConfirmation = true },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(R.string.finish_workout))
                            }
                        }
                    }
                }
            }
        }
    }

    uiState.session?.restTimer?.let { timer ->
        RestTimerDialog(
            timer = timer,
            remainingMillis = uiState.restRemainingMillis ?: 0,
            onPause = onPauseRest,
            onResume = onResumeRest,
            onReset = onResetRest,
            onAdjust = onAdjustRest,
            onEnd = onEndRest,
        )
    }

    if (showCompleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showCompleteConfirmation = false },
            title = { Text(stringResource(R.string.finish_workout_title)) },
            text = { Text(stringResource(R.string.finish_workout_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showCompleteConfirmation = false
                        onCompleteSession()
                    },
                ) {
                    Text(stringResource(R.string.finish))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteConfirmation = false }) {
                    Text(stringResource(R.string.keep_training))
                }
            },
        )
    }

    if (showCancelConfirmation) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmation = false },
            title = { Text(stringResource(R.string.cancel_workout_title)) },
            text = { Text(stringResource(R.string.cancel_workout_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelConfirmation = false
                        onCancelSession()
                    },
                ) {
                    Text(stringResource(R.string.cancel_workout))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmation = false }) {
                    Text(stringResource(R.string.keep_training))
                }
            },
        )
    }
}

@Composable
private fun SessionTimerCard(
    workoutName: String,
    status: WorkoutSessionStatus,
    activeDurationMillis: Long,
    onPause: () -> Unit,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(workoutName, style = MaterialTheme.typography.titleLarge)
            Text(
                text = formatSessionDuration(activeDurationMillis),
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.displayMedium,
            )
            Text(
                text = if (status == WorkoutSessionStatus.ACTIVE) {
                    stringResource(R.string.session_active)
                } else {
                    stringResource(R.string.session_paused)
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = if (status == WorkoutSessionStatus.ACTIVE) onPause else onResume,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Text(
                    if (status == WorkoutSessionStatus.ACTIVE) {
                        stringResource(R.string.pause_session)
                    } else {
                        stringResource(R.string.resume_session)
                    },
                )
            }
        }
    }
}

@Composable
private fun RestTimerDialog(
    timer: ActiveRestTimer,
    remainingMillis: Long,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onAdjust: (Long) -> Unit,
    onEnd: () -> Unit,
) {
    val isFinished = remainingMillis <= 0
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                text = stringResource(R.string.rest_for_exercise, timer.exerciseName),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatRestDuration(remainingMillis),
                color = if (isFinished) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.displaySmall,
            )
            Text(
                text = when {
                    isFinished -> stringResource(R.string.rest_finished)
                    timer.status == RestTimerStatus.RUNNING -> stringResource(R.string.rest_running)
                    else -> stringResource(R.string.rest_paused)
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { onAdjust(-10_000) },
                    modifier = Modifier.weight(1f),
                    enabled = !isFinished,
                ) {
                    Text(stringResource(R.string.rest_minus_ten_seconds))
                }
                Button(onClick = { onAdjust(10_000) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.rest_plus_ten_seconds))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                if (!isFinished) {
                    TextButton(
                        onClick = if (timer.status == RestTimerStatus.RUNNING) onPause else onResume,
                    ) {
                        Text(
                            if (timer.status == RestTimerStatus.RUNNING) {
                                stringResource(R.string.pause)
                            } else {
                                stringResource(R.string.continue_action)
                            },
                        )
                    }
                }
                TextButton(onClick = onReset) {
                    Text(stringResource(R.string.restart))
                }
            }
            }
        },
        confirmButton = {
            Button(onClick = onEnd) {
                Text(stringResource(R.string.end_rest))
            }
        },
    )
}

@Composable
private fun SessionExerciseCard(
    exercise: ActiveSessionExercise,
    onSeriesCompletedChange: (String, Boolean) -> Unit,
    onExerciseCompletedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by rememberSaveable(exercise.id) { mutableStateOf(false) }
    val completedSeries = exercise.series.count { it.isCompleted }
    val seriesProgress = if (exercise.series.isEmpty()) {
        0f
    } else {
        completedSeries.toFloat() / exercise.series.size
    }
    val repetitions = exercise.repetitionsMax?.let {
        stringResource(R.string.repetition_range, exercise.repetitionsMin, it)
    } ?: exercise.repetitionsMin.toString()
    val rest = if (exercise.restSeconds >= 60) {
        stringResource(R.string.rest_minutes_seconds, exercise.restSeconds / 60, exercise.restSeconds % 60)
    } else {
        stringResource(R.string.rest_seconds, exercise.restSeconds)
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = exercise.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium.copy(
                    textDecoration = if (exercise.isCompleted) {
                        TextDecoration.LineThrough
                    } else {
                        TextDecoration.None
                    },
                ),
            )
            Text(
                text = stringResource(
                    R.string.exercise_configuration,
                    exercise.sets,
                    repetitions,
                    rest,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    R.string.series_progress,
                    completedSeries,
                    exercise.series.size,
                ),
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.labelLarge,
            )
            LinearProgressIndicator(
                progress = { seriesProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = exercise.isCompleted,
                    onCheckedChange = onExerciseCompletedChange,
                )
                Text(
                    text = stringResource(R.string.exercise_completed),
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { isExpanded = !isExpanded }) {
                    Text(
                        stringResource(
                            if (isExpanded) R.string.collapse_sets else R.string.expand_sets,
                        ),
                    )
                }
            }
            if (isExpanded) {
                exercise.notes?.let { notes ->
                    Text(
                        text = notes,
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    text = stringResource(R.string.series_completion_hint),
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                exercise.series.forEach { set ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = set.isCompleted,
                            onCheckedChange = { completed ->
                                onSeriesCompletedChange(set.id, completed)
                            },
                        )
                        Text(stringResource(R.string.series_number, set.position))
                    }
                }
            }
        }
    }
}

private fun formatSessionDuration(durationMillis: Long): String {
    val totalSeconds = durationMillis.coerceAtLeast(0) / 1_000
    val hours = totalSeconds / 3_600
    val minutes = totalSeconds % 3_600 / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

private fun formatRestDuration(durationMillis: Long): String {
    val totalSeconds = if (durationMillis <= 0) 0 else (durationMillis + 999) / 1_000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
