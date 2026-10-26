package com.gmn95.mygym.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.gmn95.mygym.data.repository.WorkoutRepository
import com.gmn95.mygym.data.repository.WorkoutSummary
import com.gmn95.mygym.data.repository.ActiveSessionSummary
import com.gmn95.mygym.data.repository.SessionRepository
import com.gmn95.mygym.data.repository.WorkoutHasNoExercisesException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val workouts: List<WorkoutSummary> = emptyList(),
    val activeSession: ActiveSessionSummary? = null,
    val message: HomeMessage? = null,
)

enum class HomeMessage {
    OPERATION_FAILED,
    WORKOUT_HAS_NO_EXERCISES,
}

class HomeViewModel(
    private val workoutRepository: WorkoutRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val message = MutableStateFlow<HomeMessage?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        workoutRepository.observeWorkouts(),
        sessionRepository.observeActiveSessionSummary(),
        message,
    ) { workouts, activeSession, currentMessage ->
        HomeUiState(
            isLoading = false,
            workouts = workouts,
            activeSession = activeSession,
            message = currentMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun createWorkout(name: String, onCreated: (String) -> Unit) {
        launchOperation {
            onCreated(workoutRepository.createWorkout(name))
        }
    }

    fun duplicateWorkout(id: String, duplicateName: String) {
        launchOperation {
            workoutRepository.duplicateWorkout(id, duplicateName)
        }
    }

    fun deleteWorkout(id: String) {
        launchOperation {
            workoutRepository.deleteWorkout(id)
        }
    }

    fun startWorkout(id: String, onStarted: () -> Unit) {
        viewModelScope.launch {
            runCatching { sessionRepository.startSession(id) }
                .onSuccess { onStarted() }
                .onFailure { error ->
                    message.value = if (error is WorkoutHasNoExercisesException) {
                        HomeMessage.WORKOUT_HAS_NO_EXERCISES
                    } else {
                        HomeMessage.OPERATION_FAILED
                    }
                }
        }
    }

    fun finishActiveAndStart(id: String, onStarted: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                sessionRepository.completeActiveAndStartSession(id)
            }.onSuccess {
                onStarted()
            }.onFailure { error ->
                message.value = if (error is WorkoutHasNoExercisesException) {
                    HomeMessage.WORKOUT_HAS_NO_EXERCISES
                } else {
                    HomeMessage.OPERATION_FAILED
                }
            }
        }
    }

    fun clearMessage() {
        message.value = null
    }

    private fun launchOperation(action: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { action() }
                .onFailure { message.value = HomeMessage.OPERATION_FAILED }
        }
    }

    companion object {
        fun factory(
            workoutRepository: WorkoutRepository,
            sessionRepository: SessionRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: CreationExtras,
                ): T = HomeViewModel(workoutRepository, sessionRepository) as T
            }
    }
}
