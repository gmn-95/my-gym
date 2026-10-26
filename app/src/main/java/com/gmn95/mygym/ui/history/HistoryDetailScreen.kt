package com.gmn95.mygym.ui.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gmn95.mygym.R
import com.gmn95.mygym.data.repository.CompletedSessionExercise
import com.gmn95.mygym.feature.history.HistoryDetailUiState
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryDetailScreen(
    uiState: HistoryDetailUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    when {
        uiState.isLoading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        uiState.session == null -> Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(stringResource(R.string.history_session_not_found))
            TextButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(R.string.back))
            }
        }

        else -> {
            val session = uiState.session
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.back))
                    }
                    Text(session.workoutName, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = DateFormat.getDateTimeInstance(
                            DateFormat.MEDIUM,
                            DateFormat.SHORT,
                        ).format(Date(session.endedAtEpochMillis)),
                        modifier = Modifier.padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(
                            R.string.history_duration,
                            formatDuration(session.activeDurationMillis),
                        ),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        text = stringResource(R.string.history_exercises),
                        modifier = Modifier.padding(top = 20.dp),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                items(session.exercises, key = CompletedSessionExercise::position) { exercise ->
                    CompletedExerciseCard(exercise)
                }
            }
        }
    }
}

@Composable
private fun CompletedExerciseCard(exercise: CompletedSessionExercise) {
    val repetitions = exercise.repetitionsMax?.let {
        stringResource(R.string.repetition_range, exercise.repetitionsMin, it)
    } ?: exercise.repetitionsMin.toString()
    val rest = if (exercise.restSeconds >= 60) {
        stringResource(R.string.rest_minutes_seconds, exercise.restSeconds / 60, exercise.restSeconds % 60)
    } else {
        stringResource(R.string.rest_seconds, exercise.restSeconds)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = exercise.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(
                    R.string.exercise_configuration,
                    exercise.sets,
                    repetitions,
                    rest,
                ),
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            exercise.notes?.let { notes ->
                Text(
                    text = notes,
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                text = stringResource(
                    if (exercise.isCompleted) R.string.history_exercise_completed
                    else R.string.history_exercise_not_completed,
                ),
                modifier = Modifier.padding(top = 8.dp),
                color = if (exercise.isCompleted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
