package com.gmn95.mygym.ui.folder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gmn95.mygym.R
import com.gmn95.mygym.data.repository.WorkoutSummary
import com.gmn95.mygym.feature.folder.WorkoutFolderMessage
import com.gmn95.mygym.feature.folder.WorkoutFolderUiState

@Composable
fun WorkoutFolderScreen(
    uiState: WorkoutFolderUiState,
    onBack: () -> Unit,
    onRenameFolder: (String) -> Unit,
    onCreateWorkout: (String) -> Unit,
    onOpenWorkout: (String) -> Unit,
    onDuplicateWorkout: (String, String) -> Unit,
    onDeleteWorkout: (String) -> Unit,
    onMoveWorkout: (String, Int) -> Unit,
    onStartWorkout: (String) -> Unit,
    onResumeActiveSession: () -> Unit,
    onFinishActiveAndStart: (String) -> Unit,
    onMessageShown: () -> Unit,
) {
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var showRenameDialog by rememberSaveable { mutableStateOf(false) }
    var workoutToDelete by remember { mutableStateOf<WorkoutSummary?>(null) }
    var workoutToStart by remember { mutableStateOf<WorkoutSummary?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val message = when (uiState.message) {
        WorkoutFolderMessage.OPERATION_FAILED -> stringResource(R.string.operation_error)
        WorkoutFolderMessage.WORKOUT_HAS_NO_EXERCISES -> stringResource(R.string.empty_workout_cannot_start)
        null -> null
    }
    LaunchedEffect(message) { if (message != null) { snackbar.showSnackbar(message); onMessageShown() } }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = { showCreateDialog = true }) { Text(stringResource(R.string.add_workout)) } },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                Text(uiState.folder?.name.orEmpty(), Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                TextButton(onClick = { showRenameDialog = true }, enabled = uiState.folder != null) {
                    Text(stringResource(R.string.edit))
                }
            }
            Text(
                stringResource(R.string.workouts),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                stringResource(R.string.folder_workouts_description),
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                uiState.folder == null -> Text(stringResource(R.string.workout_not_found))
                uiState.folder.workouts.isEmpty() -> Text(stringResource(R.string.no_trainings_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(uiState.folder.workouts, key = WorkoutSummary::id) { workout ->
                        val workoutIndex = uiState.folder.workouts.indexOf(workout)
                        val duplicateName = stringResource(R.string.workout_copy_name, workout.name)
                        TrainingCard(
                            workout = workout,
                            onOpen = { onOpenWorkout(workout.id) },
                            onDuplicate = { onDuplicateWorkout(workout.id, duplicateName) },
                            onDelete = { workoutToDelete = workout },
                            canMoveUp = workoutIndex > 0,
                            canMoveDown = workoutIndex < uiState.folder.workouts.lastIndex,
                            onMoveUp = { onMoveWorkout(workout.id, -1) },
                            onMoveDown = { onMoveWorkout(workout.id, 1) },
                            onStart = {
                                if (uiState.activeSession == null) onStartWorkout(workout.id) else workoutToStart = workout
                            },
                        )
                    }
                }
            }
        }
    }
    if (showCreateDialog) {
        TrainingNameDialog(onDismiss = { showCreateDialog = false }) { name ->
            onCreateWorkout(name)
            showCreateDialog = false
        }
    }
    if (showRenameDialog) {
        FolderNameDialog(
            initialName = uiState.folder?.name.orEmpty(),
            onDismiss = { showRenameDialog = false },
        ) { name ->
            onRenameFolder(name)
            showRenameDialog = false
        }
    }
    workoutToDelete?.let { workout ->
        AlertDialog(
            onDismissRequest = { workoutToDelete = null },
            title = { Text(stringResource(R.string.delete_workout_title)) },
            text = { Text(stringResource(R.string.delete_workout_message, workout.name)) },
            confirmButton = { Button(onClick = { onDeleteWorkout(workout.id); workoutToDelete = null }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { workoutToDelete = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    workoutToStart?.let { workout ->
        AlertDialog(
            onDismissRequest = { workoutToStart = null },
            title = { Text(stringResource(R.string.active_session_exists_title)) },
            text = { Text(stringResource(R.string.active_session_exists_message, uiState.activeSession?.workoutName.orEmpty(), workout.name)) },
            confirmButton = { Button(onClick = { workoutToStart = null; onResumeActiveSession() }) { Text(stringResource(R.string.resume_current)) } },
            dismissButton = {
                Row {
                    TextButton(onClick = { workoutToStart = null }) { Text(stringResource(R.string.cancel)) }
                    TextButton(onClick = { workoutToStart = null; onFinishActiveAndStart(workout.id) }) { Text(stringResource(R.string.finish_and_start)) }
                }
            },
        )
    }
}

@Composable
private fun TrainingCard(
    workout: WorkoutSummary,
    onOpen: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onStart: () -> Unit,
) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Column(Modifier.padding(16.dp)) {
            Text(workout.name, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(pluralStringResource(R.plurals.exercise_count, workout.exerciseCount, workout.exerciseCount), Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            workout.averageDurationMillis?.let { duration ->
                Text(stringResource(R.string.average_duration, duration.formatDuration()), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text(stringResource(R.string.start_workout))
            }
            OutlinedButton(onClick = onOpen, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(stringResource(R.string.edit))
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onMoveUp, enabled = canMoveUp) { Text(stringResource(R.string.move_up)) }
                TextButton(onClick = onMoveDown, enabled = canMoveDown) { Text(stringResource(R.string.move_down)) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDuplicate) { Text(stringResource(R.string.duplicate)) }
                TextButton(onClick = onDelete) { Text(stringResource(R.string.delete)) }
            }
        }
    }
}

@Composable
private fun FolderNameDialog(initialName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_folder)) },
        text = { OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.folder_name)) }, singleLine = true) },
        confirmButton = { Button(onClick = { onConfirm(name) }, enabled = name.trim().isNotEmpty()) { Text(stringResource(R.string.save_name)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun TrainingNameDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_workout)) },
        text = { OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.training_name)) }, singleLine = true) },
        confirmButton = { Button(onClick = { onConfirm(name) }, enabled = name.trim().isNotEmpty()) { Text(stringResource(R.string.create)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

private fun Long.formatDuration(): String {
    val minutes = this / 60_000
    val seconds = (this / 1_000) % 60
    return "%d:%02d".format(minutes, seconds)
}
