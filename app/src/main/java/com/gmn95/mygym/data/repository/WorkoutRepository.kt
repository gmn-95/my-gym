package com.gmn95.mygym.data.repository

import com.gmn95.mygym.domain.WorkoutItemConfiguration
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun observeFolders(): Flow<List<WorkoutFolderSummary>>

    fun observeFolder(id: String): Flow<WorkoutFolder?>

    fun observeWorkout(id: String): Flow<Workout?>

    fun observeCustomExercises(): Flow<List<CustomExercise>>

    suspend fun createFolder(name: String): String

    suspend fun renameFolder(id: String, name: String)

    /** Duplica os treinos e suas configurações, sem copiar sessões históricas. */
    suspend fun duplicateFolder(id: String, duplicateName: String): String?

    suspend fun deleteFolder(id: String)

    suspend fun createWorkout(folderId: String, name: String): String

    suspend fun renameWorkout(id: String, name: String)

    suspend fun duplicateWorkout(id: String, duplicateName: String): String?

    suspend fun deleteWorkout(id: String)

    suspend fun moveWorkout(folderId: String, workoutId: String, offset: Int)

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
