package com.gmn95.mygym.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gmn95.mygym.R
import com.gmn95.mygym.data.repository.WorkoutSummary
import com.gmn95.mygym.feature.home.HomeUiState
import com.gmn95.mygym.feature.home.HomeMessage

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onCreateWorkout: (String) -> Unit,
    onOpenWorkout: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenSettings: () -> Unit,
    onDuplicateWorkout: (String, String) -> Unit,
    onDeleteWorkout: (String) -> Unit,
    onStartWorkout: (String) -> Unit,
    onResumeActiveSession: () -> Unit,
    onFinishActiveAndStart: (String) -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var workoutToDelete by remember { mutableStateOf<WorkoutSummary?>(null) }
    var workoutToStartWhenActive by remember { mutableStateOf<WorkoutSummary?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val currentMessage = uiState.message?.let { message ->
        when (message) {
            HomeMessage.OPERATION_FAILED -> stringResource(R.string.operation_error)
            HomeMessage.WORKOUT_HAS_NO_EXERCISES -> stringResource(R.string.empty_workout_cannot_start)
        }
    }

    LaunchedEffect(currentMessage) {
        if (currentMessage != null) {
            snackbarHostState.showSnackbar(currentMessage)
            onMessageShown()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
            ) {
                Text(stringResource(R.string.create_workout))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.my_workouts),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineMedium,
                )
                TextButton(onClick = onOpenSettings) {
                    Text(stringResource(R.string.settings_short))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onOpenHistory) { Text(stringResource(R.string.history)) }
                TextButton(onClick = onOpenCalendar) { Text(stringResource(R.string.calendar)) }
                TextButton(onClick = onOpenBackup) { Text(stringResource(R.string.backup_short)) }
            }
            Text(
                text = stringResource(R.string.home_description),
                modifier = Modifier.padding(bottom = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )

            uiState.activeSession?.let { activeSession ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.active_session_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = activeSession.workoutName,
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = onResumeActiveSession,
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = 8.dp),
                        ) {
                            Text(stringResource(R.string.resume_workout))
                        }
                    }
                }
            }

            when {
                uiState.isLoading -> LoadingContent()
                uiState.workouts.isEmpty() -> EmptyContent()
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.workouts, key = WorkoutSummary::id) { workout ->
                        val duplicateName = stringResource(R.string.workout_copy_name, workout.name)
                        WorkoutCard(
                            workout = workout,
                            onOpen = { onOpenWorkout(workout.id) },
                            onDuplicate = {
                                onDuplicateWorkout(workout.id, duplicateName)
                            },
                            onDelete = { workoutToDelete = workout },
                            onStart = {
                                if (uiState.activeSession == null) {
                                    onStartWorkout(workout.id)
                                } else {
                                    workoutToStartWhenActive = workout
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        WorkoutNameDialog(
            title = stringResource(R.string.create_workout),
            confirmLabel = stringResource(R.string.create),
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                onCreateWorkout(name)
                showCreateDialog = false
            },
        )
    }

    workoutToDelete?.let { workout ->
        AlertDialog(
            onDismissRequest = { workoutToDelete = null },
            title = { Text(stringResource(R.string.delete_workout_title)) },
            text = { Text(stringResource(R.string.delete_workout_message, workout.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteWorkout(workout.id)
                        workoutToDelete = null
                    },
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { workoutToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    workoutToStartWhenActive?.let { workout ->
        AlertDialog(
            onDismissRequest = { workoutToStartWhenActive = null },
            title = { Text(stringResource(R.string.active_session_exists_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.active_session_exists_message,
                        uiState.activeSession?.workoutName.orEmpty(),
                        workout.name,
                    ),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        workoutToStartWhenActive = null
                        onResumeActiveSession()
                    },
                ) {
                    Text(stringResource(R.string.resume_current))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { workoutToStartWhenActive = null }) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(
                        onClick = {
                            workoutToStartWhenActive = null
                            onFinishActiveAndStart(workout.id)
                        },
                    ) {
                        Text(stringResource(R.string.finish_and_start))
                    }
                }
            },
        )
    }
}

@Composable
private fun WorkoutCard(
    workout: WorkoutSummary,
    onOpen: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onStart: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = workout.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.exercise_count,
                    workout.exerciseCount,
                    workout.exerciseCount,
                ),
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val durationText = workout.averageDurationMillis?.let(::formatDuration) ?: stringResource(
                R.string.no_history,
            )
            Text(
                text = workout.averageDurationMillis?.let {
                    stringResource(R.string.average_duration, durationText)
                } ?: durationText,
                modifier = Modifier.padding(top = 2.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDuplicate) {
                    Text(stringResource(R.string.duplicate))
                }
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.delete))
                }
                Button(onClick = onOpen) {
                    Text(stringResource(R.string.edit))
                }
            }
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                Text(stringResource(R.string.start_workout))
            }
        }
    }
}

@Composable
private fun WorkoutNameDialog(
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.workout_name)) },
                singleLine = true,
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
            ) {
                Text(confirmLabel)
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
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.no_workouts_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.no_workouts_description),
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatDuration(durationMillis: Long): String {
    val totalSeconds = durationMillis.coerceAtLeast(0) / 1_000
    val hours = totalSeconds / 3_600
    val minutes = totalSeconds % 3_600 / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

