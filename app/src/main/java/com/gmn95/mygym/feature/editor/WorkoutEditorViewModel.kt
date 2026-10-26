package com.gmn95.mygym.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.gmn95.mygym.data.repository.CustomExercise
import com.gmn95.mygym.data.repository.Workout
import com.gmn95.mygym.data.repository.WorkoutRepository
import com.gmn95.mygym.domain.WorkoutItemConfiguration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WorkoutEditorUiState(
    val isLoading: Boolean = true,
    val workout: Workout? = null,
    val customExercises: List<CustomExercise> = emptyList(),
    val operationFailed: Boolean = false,
)

class WorkoutEditorViewModel(
    private val workoutId: String,
    private val repository: WorkoutRepository,
) : ViewModel() {
    private val operationFailed = MutableStateFlow(false)

    val uiState: StateFlow<WorkoutEditorUiState> = combine(
        repository.observeWorkout(workoutId),
        repository.observeCustomExercises(),
        operationFailed,
    ) { workout, customExercises, failed ->
        WorkoutEditorUiState(
            isLoading = false,
            workout = workout,
            customExercises = customExercises,
            operationFailed = failed,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WorkoutEditorUiState(),
    )

    fun renameWorkout(name: String) {
        launchOperation {
            repository.renameWorkout(workoutId, name)
        }
    }

    fun createAndAddExercise(name: String, notes: String?) {
        launchOperation {
            repository.createAndAddExercise(workoutId, name, notes)
        }
    }

    fun addExistingExercise(customExerciseId: String) {
        launchOperation {
            repository.addExistingExercise(workoutId, customExerciseId)
        }
    }

    fun updateExercise(itemId: String, configuration: WorkoutItemConfiguration) {
        launchOperation {
            repository.updateExerciseConfiguration(workoutId, itemId, configuration)
        }
    }

    fun removeExercise(itemId: String) {
        launchOperation {
            repository.removeExercise(workoutId, itemId)
        }
    }

    fun moveExercise(itemId: String, offset: Int) {
        launchOperation {
            repository.moveExercise(workoutId, itemId, offset)
        }
    }

    fun clearOperationError() {
        operationFailed.value = false
    }

    private fun launchOperation(action: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { action() }
                .onFailure { operationFailed.value = true }
        }
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
            ): T = WorkoutEditorViewModel(workoutId, repository) as T
        }
    }
}
