package com.gmn95.mygym.ui.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.net.Uri
import android.widget.VideoView
import coil3.compose.AsyncImage
import com.gmn95.mygym.R
import com.gmn95.mygym.data.remote.CatalogExercise
import com.gmn95.mygym.feature.catalog.CatalogMessage
import com.gmn95.mygym.feature.catalog.ExerciseCatalogUiState

@Composable
fun ExerciseCatalogScreen(
    uiState: ExerciseCatalogUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onLoadNextPage: () -> Unit,
    onSelectExercise: (String) -> Unit,
    onAddExercise: (CatalogExercise) -> Unit,
    onMessageShown: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    val message = when (uiState.message) {
        CatalogMessage.ENTER_SEARCH_TERM -> stringResource(R.string.catalog_enter_search_term)
        CatalogMessage.NETWORK_ERROR -> stringResource(R.string.catalog_network_error)
        CatalogMessage.RATE_LIMITED -> stringResource(R.string.catalog_rate_limited)
        CatalogMessage.ADD_ERROR -> stringResource(R.string.operation_error)
        null -> null
    }
    LaunchedEffect(message) { if (message != null) { snackbar.showSnackbar(message); onMessageShown() } }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                    Text(stringResource(R.string.catalog_title), style = MaterialTheme.typography.headlineSmall)
                }
            }
            item {
                Text(
                    stringResource(R.string.catalog_attribution),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = onQueryChange,
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.catalog_search_label)) },
                        singleLine = true,
                    )
                    Button(onClick = onSearch, modifier = Modifier.padding(start = 8.dp), enabled = !uiState.isLoading) {
                        Text(stringResource(R.string.search))
                    }
                }
            }
            if (uiState.isLoading && uiState.exercises.isEmpty()) {
                item { CircularProgressIndicator(Modifier.padding(top = 24.dp)) }
            }
            if (
                !uiState.isLoading &&
                uiState.hasSuccessfulSearch &&
                uiState.exercises.isEmpty()
            ) {
                item {
                    Text(
                        stringResource(R.string.catalog_no_results),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(uiState.exercises, key = CatalogExercise::id) { exercise ->
                CatalogExerciseCard(
                    exercise = exercise,
                    isExpanded = uiState.selectedExerciseId == exercise.id,
                    onToggle = { onSelectExercise(exercise.id) },
                    onAdd = { onAddExercise(exercise) },
                )
            }
            if (uiState.isLoading && uiState.exercises.isNotEmpty()) {
                item { CircularProgressIndicator(Modifier.padding(16.dp)) }
            } else if (uiState.nextPageUrl != null) {
                item {
                    TextButton(onClick = onLoadNextPage, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.catalog_load_more))
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogExerciseCard(
    exercise: CatalogExercise,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onAdd: () -> Unit,
) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onToggle)) {
        Column(Modifier.padding(16.dp)) {
            Text(exercise.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (isExpanded) {
                CatalogExerciseMedia(exercise)
                exercise.mediaAttribution?.let { attribution ->
                    Text(
                        stringResource(R.string.catalog_media_attribution, attribution),
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Text(stringResource(R.string.catalog_add_exercise))
                }
            } else {
                TextButton(onClick = onToggle, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.catalog_show_details))
                }
            }
        }
    }
}

@Composable
private fun CatalogExerciseMedia(exercise: CatalogExercise) {
    when {
        exercise.videoUrl != null -> AndroidView(
            factory = { context ->
                VideoView(context).apply {
                    setOnPreparedListener { player ->
                        player.isLooping = true
                        start()
                    }
                }
            },
            update = { view ->
                if (view.tag != exercise.videoUrl) {
                    view.tag = exercise.videoUrl
                    view.setVideoURI(Uri.parse(exercise.videoUrl))
                    view.start()
                }
            },
            modifier = Modifier.fillMaxWidth().height(240.dp).padding(top = 12.dp),
        )

        exercise.imageUrl != null -> AsyncImage(
            model = exercise.imageUrl,
            contentDescription = stringResource(R.string.catalog_media_description, exercise.name),
            modifier = Modifier.fillMaxWidth().height(240.dp).padding(top = 12.dp),
            contentScale = ContentScale.Fit,
        )

        else -> Text(
            stringResource(R.string.catalog_media_not_available),
            modifier = Modifier.padding(top = 12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
