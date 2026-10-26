package com.gmn95.mygym.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class WorkoutItemDetailsRow(
    @Embedded
    val item: WorkoutItemEntity,
    @ColumnInfo(name = "exercise_name")
    val exerciseName: String,
    @ColumnInfo(name = "exercise_notes")
    val exerciseNotes: String?,
)

@Dao
interface WorkoutItemDao {
    @Query(
        """
        SELECT workout_items.*,
               custom_exercises.name AS exercise_name,
               custom_exercises.notes AS exercise_notes
        FROM workout_items
        INNER JOIN custom_exercises
            ON custom_exercises.id = workout_items.custom_exercise_id
        WHERE workout_items.workout_group_id = :workoutGroupId
        ORDER BY workout_items.position, workout_items.id
        """,
    )
    fun observeDetails(workoutGroupId: String): Flow<List<WorkoutItemDetailsRow>>

    @Query(
        """
        SELECT workout_items.*,
               custom_exercises.name AS exercise_name,
               custom_exercises.notes AS exercise_notes
        FROM workout_items
        INNER JOIN custom_exercises
            ON custom_exercises.id = workout_items.custom_exercise_id
        WHERE workout_items.workout_group_id = :workoutGroupId
        ORDER BY workout_items.position, workout_items.id
        """,
    )
    suspend fun getDetailsByWorkoutGroup(workoutGroupId: String): List<WorkoutItemDetailsRow>

    @Query(
        """
        SELECT * FROM workout_items
        WHERE workout_group_id = :workoutGroupId
        ORDER BY position, id
        """,
    )
    suspend fun getByWorkoutGroup(workoutGroupId: String): List<WorkoutItemEntity>

    @Query("SELECT * FROM workout_items WHERE id = :id")
    suspend fun getById(id: String): WorkoutItemEntity?

    @Query(
        """
        SELECT COALESCE(MAX(position), -1) + 1 FROM workout_items
        WHERE workout_group_id = :workoutGroupId
        """,
    )
    suspend fun nextPosition(workoutGroupId: String): Int

    @Insert
    suspend fun insert(item: WorkoutItemEntity)

    @Insert
    suspend fun insertAll(items: List<WorkoutItemEntity>)

    @Update
    suspend fun update(item: WorkoutItemEntity)

    @Delete
    suspend fun delete(item: WorkoutItemEntity)

    @Query("UPDATE workout_items SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: String, position: Int)
}
