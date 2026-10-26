package com.gmn95.mygym.data.repository

import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun observeActiveSession(): Flow<ActiveWorkoutSession?>

    fun observeActiveSessionSummary(): Flow<ActiveSessionSummary?>

    fun observeCompletedSessions(): Flow<List<CompletedWorkoutSessionSummary>>

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

    suspend fun pauseRest()

    suspend fun resumeRest()

    suspend fun resetRest()

    suspend fun endRest()

    suspend fun completeActiveSession()

    suspend fun cancelActiveSession()

    suspend fun checkpointActiveTimers()

    suspend fun recoverAfterRebootIfNeeded()
}

class ActiveSessionAlreadyExistsException : IllegalStateException()

class WorkoutHasNoExercisesException : IllegalStateException()
