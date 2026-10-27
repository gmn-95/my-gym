package com.gmn95.mygym.ui.exercise

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gmn95.mygym.R
import com.gmn95.mygym.data.repository.CustomExercise
import com.gmn95.mygym.feature.exercise.SavedExercisesUiState

@Composable
fun SavedExercisesScreen(
    uiState: SavedExercisesUiState,
    onBack: () -> Unit,
    onAddExercise: (String) -> Unit,
    onOperationErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    var query by rememberSaveable { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val operationError = stringResource(R.string.operation_error)
    val filteredExercises = remember(uiState.exercises, query) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) {
            uiState.exercises
        } else {
            uiState.exercises.filter { exercise ->
                exercise.name.contains(normalizedQuery, ignoreCase = true) ||
                    exercise.notes?.contains(normalizedQuery, ignoreCase = true) == true
            }
        }
    }

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
            uiState.isLoading -> LoadingState(Modifier.padding(innerPadding))

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.padding(start = 12.dp, top = 8.dp),
                    ) {
                        Text(stringResource(R.string.back))
                    }
                }
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text(
                            text = stringResource(R.string.saved_exercises),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = stringResource(R.string.saved_exercises_description),
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            label = { Text(stringResource(R.string.saved_exercises_search)) },
                            singleLine = true,
                        )
                    }
                }
                when {
                    uiState.exercises.isEmpty() -> item {
                        EmptySavedExercises(
                            title = stringResource(R.string.no_saved_exercises_title),
                            description = stringResource(R.string.no_saved_exercises_description),
                        )
                    }

                    filteredExercises.isEmpty() -> item {
                        EmptySavedExercises(
                            title = stringResource(R.string.no_search_results),
                            description = stringResource(R.string.no_search_results_description),
                        )
                    }

                    else -> items(filteredExercises, key = CustomExercise::id) { exercise ->
                        SavedExerciseCard(
                            exercise = exercise,
                            onAdd = { onAddExercise(exercise.id) },
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptySavedExercises(title: String, description: String) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = description,
            modifier = Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SavedExerciseCard(
    exercise: CustomExercise,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
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
            Button(
                onClick = onAdd,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Text(stringResource(R.string.add_to_workout))
            }
        }
    }
}
