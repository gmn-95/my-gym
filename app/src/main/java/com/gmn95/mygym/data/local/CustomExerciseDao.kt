package com.gmn95.mygym.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomExerciseDao {
    @Query("SELECT * FROM custom_exercises ORDER BY name COLLATE NOCASE, created_at_epoch_millis")
    fun observeAll(): Flow<List<CustomExerciseEntity>>

    @Query("SELECT * FROM custom_exercises WHERE id = :id")
    suspend fun getById(id: String): CustomExerciseEntity?

    @Query("SELECT * FROM custom_exercises")
    suspend fun getAll(): List<CustomExerciseEntity>

    @Query("DELETE FROM custom_exercises")
    suspend fun deleteAll()

    @Insert
    suspend fun insert(exercise: CustomExerciseEntity)

    @Insert
    suspend fun insertAll(exercises: List<CustomExerciseEntity>)
}
