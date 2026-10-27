package com.gmn95.mygym.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.gmn95.mygym.data.repository.WorkoutRepository
import com.gmn95.mygym.data.repository.WorkoutFolderSummary
import com.gmn95.mygym.data.repository.ActiveSessionSummary
import com.gmn95.mygym.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val folders: List<WorkoutFolderSummary> = emptyList(),
    val activeSession: ActiveSessionSummary? = null,
    val message: HomeMessage? = null,
)

enum class HomeMessage {
    OPERATION_FAILED,
}

class HomeViewModel(
    private val workoutRepository: WorkoutRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val message = MutableStateFlow<HomeMessage?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        workoutRepository.observeFolders(),
        sessionRepository.observeActiveSessionSummary(),
        message,
    ) { folders, activeSession, currentMessage ->
        HomeUiState(
            isLoading = false,
            folders = folders,
            activeSession = activeSession,
            message = currentMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun createFolder(name: String, onCreated: (String) -> Unit) {
        launchOperation {
            onCreated(workoutRepository.createFolder(name))
        }
    }

    fun deleteFolder(id: String) {
        launchOperation {
            workoutRepository.deleteFolder(id)
        }
    }

    fun duplicateFolder(id: String, duplicateName: String) {
        launchOperation {
            workoutRepository.duplicateFolder(id, duplicateName)
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
