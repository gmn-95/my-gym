package com.gmn95.mygym.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.gmn95.mygym.data.repository.CompletedWorkoutSessionSummary
import com.gmn95.mygym.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val isLoading: Boolean = true,
    val sessions: List<CompletedWorkoutSessionSummary> = emptyList(),
    val message: HistoryMessage? = null,
)

enum class HistoryMessage { OPERATION_FAILED }

class HistoryViewModel(
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val message = MutableStateFlow<HistoryMessage?>(null)

    val uiState: StateFlow<HistoryUiState> = combine(
        sessionRepository.observeCompletedSessions(),
        message,
    ) { sessions, currentMessage ->
        HistoryUiState(isLoading = false, sessions = sessions, message = currentMessage)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState(),
        )

    fun deleteCompletedSession(sessionId: String) {
        viewModelScope.launch {
            runCatching { sessionRepository.deleteCompletedSession(sessionId) }
                .onFailure { message.value = HistoryMessage.OPERATION_FAILED }
        }
    }

    fun clearCompletedSessions() {
        viewModelScope.launch {
            runCatching { sessionRepository.clearCompletedSessions() }
                .onFailure { message.value = HistoryMessage.OPERATION_FAILED }
        }
    }

    fun clearMessage() {
        message.value = null
    }

    companion object {
        fun factory(sessionRepository: SessionRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: CreationExtras,
                ): T = HistoryViewModel(sessionRepository) as T
            }
    }
}
