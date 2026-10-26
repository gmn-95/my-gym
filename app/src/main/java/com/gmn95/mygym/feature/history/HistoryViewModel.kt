package com.gmn95.mygym.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.gmn95.mygym.data.repository.CompletedWorkoutSessionSummary
import com.gmn95.mygym.data.repository.SessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HistoryUiState(
    val isLoading: Boolean = true,
    val sessions: List<CompletedWorkoutSessionSummary> = emptyList(),
)

class HistoryViewModel(
    sessionRepository: SessionRepository,
) : ViewModel() {
    val uiState: StateFlow<HistoryUiState> = sessionRepository.observeCompletedSessions()
        .map { sessions -> HistoryUiState(isLoading = false, sessions = sessions) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState(),
        )

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
