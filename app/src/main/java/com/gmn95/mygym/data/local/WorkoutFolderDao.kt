package com.gmn95.mygym.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class WorkoutFolderSummaryRow(
    @Embedded
    val folder: WorkoutFolderEntity,
    @ColumnInfo(name = "workout_count")
    val workoutCount: Int,
)

@Dao
interface WorkoutFolderDao {
    @Query(
        """
        SELECT workout_folders.*, COUNT(workout_groups.id) AS workout_count
        FROM workout_folders
        LEFT JOIN workout_groups ON workout_groups.workout_folder_id = workout_folders.id
        GROUP BY workout_folders.id
        ORDER BY workout_folders.position, workout_folders.created_at_epoch_millis
        """,
    )
    fun observeSummaries(): Flow<List<WorkoutFolderSummaryRow>>

    @Query("SELECT * FROM workout_folders WHERE id = :id")
    fun observeById(id: String): Flow<WorkoutFolderEntity?>

    @Query("SELECT * FROM workout_folders WHERE id = :id")
    suspend fun getById(id: String): WorkoutFolderEntity?

    @Query("SELECT * FROM workout_folders ORDER BY position, created_at_epoch_millis")
    suspend fun getAll(): List<WorkoutFolderEntity>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM workout_folders")
    suspend fun nextPosition(): Int

    @Insert
    suspend fun insert(folder: WorkoutFolderEntity)

    @Insert
    suspend fun insertAll(folders: List<WorkoutFolderEntity>)

    @Delete
    suspend fun delete(folder: WorkoutFolderEntity)

    @Query("DELETE FROM workout_folders")
    suspend fun deleteAll()

    @Query("UPDATE workout_folders SET name = :name, updated_at_epoch_millis = :updatedAt WHERE id = :id")
    suspend fun rename(id: String, name: String, updatedAt: Long)

    @Query("UPDATE workout_folders SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: String, position: Int)
}
