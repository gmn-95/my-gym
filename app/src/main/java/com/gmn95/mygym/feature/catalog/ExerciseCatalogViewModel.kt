package com.gmn95.mygym.feature.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.gmn95.mygym.data.remote.CatalogExercise
import com.gmn95.mygym.data.remote.ExerciseCatalogException
import com.gmn95.mygym.data.remote.ExerciseCatalogRepository
import com.gmn95.mygym.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExerciseCatalogUiState(
    val query: String = "",
    val exercises: List<CatalogExercise> = emptyList(),
    val nextPageUrl: String? = null,
    val isLoading: Boolean = false,
    val selectedExerciseId: String? = null,
    val message: CatalogMessage? = null,
)

enum class CatalogMessage { ENTER_SEARCH_TERM, NETWORK_ERROR, RATE_LIMITED, ADD_ERROR }

class ExerciseCatalogViewModel(
    private val workoutId: String,
    private val catalogRepository: ExerciseCatalogRepository,
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ExerciseCatalogUiState())
    val uiState: StateFlow<ExerciseCatalogUiState> = mutableUiState.asStateFlow()

    fun updateQuery(query: String) {
        mutableUiState.value = mutableUiState.value.copy(query = query, message = null)
    }

    fun search() {
        val name = mutableUiState.value.query.trim()
        if (name.length < 2) {
            mutableUiState.value = mutableUiState.value.copy(message = CatalogMessage.ENTER_SEARCH_TERM)
            return
        }
        loadPage(name, nextPageUrl = null, append = false)
    }

    fun loadNextPage() {
        val state = mutableUiState.value
        val nextPageUrl = state.nextPageUrl ?: return
        loadPage(state.query.trim(), nextPageUrl, append = true)
    }

    fun selectExercise(id: String) {
        val selected = mutableUiState.value.selectedExerciseId
        mutableUiState.value = mutableUiState.value.copy(selectedExerciseId = if (selected == id) null else id)
    }

    fun addExercise(exercise: CatalogExercise, onAdded: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                // Apenas o nome escolhido entra no banco local; os dados e a mídia continuam remotos.
                workoutRepository.createAndAddExercise(workoutId, exercise.name, null)
            }.onSuccess { onAdded() }
                .onFailure { mutableUiState.value = mutableUiState.value.copy(message = CatalogMessage.ADD_ERROR) }
        }
    }

    fun clearMessage() {
        mutableUiState.value = mutableUiState.value.copy(message = null)
    }

    private fun loadPage(name: String, nextPageUrl: String?, append: Boolean) {
        if (mutableUiState.value.isLoading) return
        viewModelScope.launch {
            mutableUiState.value = mutableUiState.value.copy(isLoading = true, message = null)
            runCatching { catalogRepository.search(name, nextPageUrl) }
                .onSuccess { page ->
                    val current = mutableUiState.value
                    mutableUiState.value = current.copy(
                        exercises = if (append) current.exercises + page.exercises else page.exercises,
                        nextPageUrl = page.nextPageUrl,
                        isLoading = false,
                    )
                }
                .onFailure { error ->
                    val message = if ((error as? ExerciseCatalogException)?.reason ==
                        ExerciseCatalogException.Reason.RATE_LIMITED
                    ) {
                        CatalogMessage.RATE_LIMITED
                    } else {
                        CatalogMessage.NETWORK_ERROR
                    }
                    mutableUiState.value = mutableUiState.value.copy(isLoading = false, message = message)
                }
        }
    }

    companion object {
        fun factory(
            workoutId: String,
            catalogRepository: ExerciseCatalogRepository,
            workoutRepository: WorkoutRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                ExerciseCatalogViewModel(workoutId, catalogRepository, workoutRepository) as T
        }
    }
}
