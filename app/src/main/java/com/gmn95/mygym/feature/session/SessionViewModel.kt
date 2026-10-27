package com.gmn95.mygym.feature.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.gmn95.mygym.data.repository.ActiveWorkoutSession
import com.gmn95.mygym.data.repository.SessionSeriesProgress
import com.gmn95.mygym.data.repository.SessionRepository
import com.gmn95.mygym.data.repository.WorkoutSessionStatus
import com.gmn95.mygym.data.repository.seriesProgress
import com.gmn95.mygym.domain.session.SessionClock
import com.gmn95.mygym.domain.session.TimerMath
import com.gmn95.mygym.domain.WorkoutItemConfiguration
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SessionUiState(
    val isLoading: Boolean = true,
    val session: ActiveWorkoutSession? = null,
    val activeDurationMillis: Long = 0,
    val restRemainingMillis: Long? = null,
    val seriesProgress: SessionSeriesProgress = SessionSeriesProgress(null, null),
    val operationFailed: Boolean = false,
)

class SessionViewModel(
    private val repository: SessionRepository,
    private val clock: SessionClock,
) : ViewModel() {
    private val operationFailed = MutableStateFlow(false)
    private val ticker = flow {
        while (currentCoroutineContext().isActive) {
            emit(Unit)
            delay(1_000)
        }
    }.onStart { emit(Unit) }

    val uiState: StateFlow<SessionUiState> = combine(
        repository.observeActiveSession(),
        ticker,
        operationFailed,
    ) { session, _, failed ->
        val elapsedRealtime = clock.elapsedRealtimeMillis()
        val bootCount = clock.bootCount()
        SessionUiState(
            isLoading = false,
            session = session,
            activeDurationMillis = session?.let {
                TimerMath.activeDurationMillis(
                    accumulatedMillis = it.activeAccumulatedMillis,
                    anchorElapsedRealtime = if (it.status == WorkoutSessionStatus.ACTIVE) {
                        it.activeAnchorElapsedRealtime
                    } else {
                        null
                    },
                    anchorBootCount = it.activeAnchorBootCount,
                    nowElapsedRealtime = elapsedRealtime,
                    currentBootCount = bootCount,
                )
            } ?: 0,
            restRemainingMillis = session?.restTimer?.let {
                TimerMath.remainingMillis(
                    remainingAtAnchorMillis = it.remainingAtAnchorMillis,
                    anchorElapsedRealtime = it.anchorElapsedRealtime,
                    anchorBootCount = it.anchorBootCount,
                    nowElapsedRealtime = elapsedRealtime,
                    currentBootCount = bootCount,
                )
            },
            seriesProgress = session?.seriesProgress() ?: SessionSeriesProgress(null, null),
            operationFailed = failed,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SessionUiState(),
    )

    init {
        viewModelScope.launch {
            runCatching { repository.recoverAfterRebootIfNeeded() }
                .onFailure { operationFailed.value = true }
        }
    }

    fun pauseSession() = launchOperation(repository::pauseSession)

    fun resumeSession() = launchOperation(repository::resumeSession)

    fun setExerciseSetCompleted(
        exerciseId: String,
        setId: String,
        completed: Boolean,
    ) = launchOperation(
        action = { repository.setExerciseSetCompleted(exerciseId, setId, completed) },
    )

    fun setExerciseCompleted(exerciseId: String, completed: Boolean) = launchOperation(
        action = { repository.setExerciseCompleted(exerciseId, completed) },
    )

    fun updateActiveExercise(
        exerciseId: String,
        configuration: WorkoutItemConfiguration,
    ) = launchOperation(
        action = { repository.updateActiveExerciseConfiguration(exerciseId, configuration) },
    )

    fun pauseRest() = launchOperation(repository::pauseRest)

    fun resumeRest() = launchOperation(repository::resumeRest)

    fun resetRest() = launchOperation(repository::resetRest)

    fun adjustRestBy(deltaMillis: Long) = launchOperation(
        action = { repository.adjustRestBy(deltaMillis) },
    )

    fun endRest() = launchOperation(repository::endRest)

    fun completeSession(onCompleted: () -> Unit) {
        launchOperation(onSuccess = onCompleted, action = repository::completeActiveSession)
    }

    fun cancelSession(onCancelled: () -> Unit) {
        launchOperation(onSuccess = onCancelled, action = repository::cancelActiveSession)
    }

    fun clearOperationError() {
        operationFailed.value = false
    }

    private fun launchOperation(
        action: suspend () -> Unit,
        onSuccess: () -> Unit = {},
    ) {
        viewModelScope.launch {
            runCatching { action() }
                .onSuccess { onSuccess() }
                .onFailure { operationFailed.value = true }
        }
    }

    companion object {
        fun factory(
            repository: SessionRepository,
            clock: SessionClock,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras,
            ): T = SessionViewModel(repository, clock) as T
        }
    }
}
