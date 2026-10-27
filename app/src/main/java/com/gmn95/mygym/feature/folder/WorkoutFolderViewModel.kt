package com.gmn95.mygym.feature.folder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.gmn95.mygym.data.repository.ActiveSessionSummary
import com.gmn95.mygym.data.repository.SessionRepository
import com.gmn95.mygym.data.repository.WorkoutFolder
import com.gmn95.mygym.data.repository.WorkoutHasNoExercisesException
import com.gmn95.mygym.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WorkoutFolderUiState(
    val isLoading: Boolean = true,
    val folder: WorkoutFolder? = null,
    val activeSession: ActiveSessionSummary? = null,
    val message: WorkoutFolderMessage? = null,
)

enum class WorkoutFolderMessage { OPERATION_FAILED, WORKOUT_HAS_NO_EXERCISES }

class WorkoutFolderViewModel(
    private val folderId: String,
    private val workoutRepository: WorkoutRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val message = MutableStateFlow<WorkoutFolderMessage?>(null)

    val uiState: StateFlow<WorkoutFolderUiState> = combine(
        workoutRepository.observeFolder(folderId),
        sessionRepository.observeActiveSessionSummary(),
        message,
    ) { folder, activeSession, currentMessage ->
        WorkoutFolderUiState(false, folder, activeSession, currentMessage)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutFolderUiState())

    fun createWorkout(name: String, onCreated: (String) -> Unit) = launchOperation {
        onCreated(workoutRepository.createWorkout(folderId, name))
    }

    fun duplicateWorkout(id: String, name: String) = launchOperation {
        workoutRepository.duplicateWorkout(id, name)
    }

    fun deleteWorkout(id: String) = launchOperation { workoutRepository.deleteWorkout(id) }

    fun startWorkout(id: String, onStarted: () -> Unit) = startOperation(onStarted) {
        sessionRepository.startSession(id)
    }

    fun finishActiveAndStart(id: String, onStarted: () -> Unit) = startOperation(onStarted) {
        sessionRepository.completeActiveAndStartSession(id)
    }

    fun clearMessage() { message.value = null }

    private fun startOperation(onSuccess: () -> Unit, action: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { action() }.onSuccess { onSuccess() }.onFailure { error ->
                message.value = if (error is WorkoutHasNoExercisesException) {
                    WorkoutFolderMessage.WORKOUT_HAS_NO_EXERCISES
                } else {
                    WorkoutFolderMessage.OPERATION_FAILED
                }
            }
        }
    }

    private fun launchOperation(action: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { action() }.onFailure { message.value = WorkoutFolderMessage.OPERATION_FAILED }
        }
    }

    companion object {
        fun factory(
            folderId: String,
            workoutRepository: WorkoutRepository,
            sessionRepository: SessionRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                WorkoutFolderViewModel(folderId, workoutRepository, sessionRepository) as T
        }
    }
}
