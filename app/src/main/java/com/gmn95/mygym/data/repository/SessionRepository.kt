package com.gmn95.mygym.data.repository

import com.gmn95.mygym.domain.WorkoutItemConfiguration
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun observeActiveSession(): Flow<ActiveWorkoutSession?>

    fun observeActiveSessionSummary(): Flow<ActiveSessionSummary?>

    fun observeCompletedSessions(): Flow<List<CompletedWorkoutSessionSummary>>

    fun observeCompletedSession(id: String): Flow<CompletedWorkoutSession?>

    suspend fun startSession(workoutGroupId: String): String

    suspend fun completeActiveAndStartSession(workoutGroupId: String): String

    suspend fun pauseSession()

    suspend fun resumeSession()

    suspend fun setExerciseSetCompleted(
        sessionExerciseId: String,
        sessionExerciseSetId: String,
        isCompleted: Boolean,
    )

    suspend fun setExerciseCompleted(sessionExerciseId: String, isCompleted: Boolean)

    suspend fun updateActiveExerciseConfiguration(
        sessionExerciseId: String,
        configuration: WorkoutItemConfiguration,
    )

    suspend fun pauseRest()

    suspend fun resumeRest()

    suspend fun resetRest()

    /** Ajusta o descanso restante sem alterar a configuração do exercício. */
    suspend fun adjustRestBy(deltaMillis: Long)

    suspend fun endRest()

    suspend fun completeActiveSession()

    suspend fun cancelActiveSession()

    /** Remove somente uma sessão concluída e seus snapshots históricos associados. */
    suspend fun deleteCompletedSession(sessionId: String)

    /** Remove todas as sessões concluídas e seus snapshots, preservando a sessão ativa. */
    suspend fun clearCompletedSessions()

    suspend fun checkpointActiveTimers()

    suspend fun recoverAfterRebootIfNeeded()
}

class ActiveSessionAlreadyExistsException : IllegalStateException()

class WorkoutHasNoExercisesException : IllegalStateException()
