package com.gmn95.mygym.feature.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.gmn95.mygym.data.repository.CustomExercise
import com.gmn95.mygym.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SavedExercisesUiState(
    val isLoading: Boolean = true,
    val exercises: List<CustomExercise> = emptyList(),
    val operationFailed: Boolean = false,
)

class SavedExercisesViewModel(
    private val workoutId: String,
    private val repository: WorkoutRepository,
) : ViewModel() {
    private val operationFailed = MutableStateFlow(false)

    val uiState: StateFlow<SavedExercisesUiState> = combine(
        repository.observeCustomExercises(),
        operationFailed,
    ) { exercises, failed ->
        SavedExercisesUiState(
            isLoading = false,
            exercises = exercises,
            operationFailed = failed,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SavedExercisesUiState(),
    )

    fun addExercise(customExerciseId: String, onAdded: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                repository.addExistingExercise(workoutId, customExerciseId)
            }.onSuccess {
                onAdded()
            }.onFailure {
                operationFailed.value = true
            }
        }
    }

    fun clearOperationError() {
        operationFailed.value = false
    }

    companion object {
        fun factory(
            workoutId: String,
            repository: WorkoutRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras,
            ): T = SavedExercisesViewModel(workoutId, repository) as T
        }
    }
}
