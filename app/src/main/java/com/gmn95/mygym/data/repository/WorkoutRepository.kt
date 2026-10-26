package com.gmn95.mygym.data.repository

import com.gmn95.mygym.domain.WorkoutItemConfiguration
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun observeWorkouts(): Flow<List<WorkoutSummary>>

    fun observeWorkout(id: String): Flow<Workout?>

    fun observeCustomExercises(): Flow<List<CustomExercise>>

    suspend fun createWorkout(name: String): String

    suspend fun renameWorkout(id: String, name: String)

    suspend fun duplicateWorkout(id: String, duplicateName: String): String?

    suspend fun deleteWorkout(id: String)

    suspend fun createAndAddExercise(
        workoutId: String,
        name: String,
        notes: String?,
        configuration: WorkoutItemConfiguration = DEFAULT_CONFIGURATION,
    )

    suspend fun addExistingExercise(
        workoutId: String,
        customExerciseId: String,
        configuration: WorkoutItemConfiguration = DEFAULT_CONFIGURATION,
    )

    suspend fun updateExerciseConfiguration(
        workoutId: String,
        itemId: String,
        configuration: WorkoutItemConfiguration,
    )

    suspend fun removeExercise(workoutId: String, itemId: String)

    suspend fun moveExercise(workoutId: String, itemId: String, offset: Int)

    companion object {
        val DEFAULT_CONFIGURATION = WorkoutItemConfiguration(
            sets = 3,
            repetitionsMin = 10,
            repetitionsMax = null,
            restSeconds = 60,
        )
    }
}
