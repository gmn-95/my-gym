package com.gmn95.mygym.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.gmn95.mygym.data.repository.CompletedWorkoutSession
import com.gmn95.mygym.data.repository.SessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HistoryDetailUiState(
    val isLoading: Boolean = true,
    val session: CompletedWorkoutSession? = null,
)

class HistoryDetailViewModel(
    sessionId: String,
    sessionRepository: SessionRepository,
) : ViewModel() {
    val uiState: StateFlow<HistoryDetailUiState> = sessionRepository.observeCompletedSession(sessionId)
        .map { session -> HistoryDetailUiState(isLoading = false, session = session) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryDetailUiState(),
        )

    companion object {
        fun factory(
            sessionId: String,
            sessionRepository: SessionRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras,
            ): T = HistoryDetailViewModel(sessionId, sessionRepository) as T
        }
    }
}
