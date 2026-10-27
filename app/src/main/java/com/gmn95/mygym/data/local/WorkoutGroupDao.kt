package com.gmn95.mygym.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class WorkoutGroupSummaryRow(
    @Embedded
    val group: WorkoutGroupEntity,
    @ColumnInfo(name = "exercise_count")
    val exerciseCount: Int,
    @ColumnInfo(name = "average_duration_millis")
    val averageDurationMillis: Long?,
)

@Dao
interface WorkoutGroupDao {
    @Query(
        """
        SELECT
            workout_groups.*,
            COUNT(DISTINCT workout_items.id) AS exercise_count,
            CAST(AVG(workout_sessions.active_accumulated_millis) AS INTEGER)
                AS average_duration_millis
        FROM workout_groups
        LEFT JOIN workout_items
            ON workout_items.workout_group_id = workout_groups.id
        LEFT JOIN workout_sessions
            ON workout_sessions.workout_group_id = workout_groups.id
            AND workout_sessions.status = 'COMPLETED'
        GROUP BY workout_groups.id
        ORDER BY workout_groups.position, workout_groups.created_at_epoch_millis
        """,
    )
    fun observeSummaries(): Flow<List<WorkoutGroupSummaryRow>>

    @Query("SELECT * FROM workout_groups WHERE id = :id")
    fun observeById(id: String): Flow<WorkoutGroupEntity?>

    @Query("SELECT * FROM workout_groups WHERE id = :id")
    suspend fun getById(id: String): WorkoutGroupEntity?

    @Query("SELECT * FROM workout_groups ORDER BY position, created_at_epoch_millis")
    suspend fun getAll(): List<WorkoutGroupEntity>

    @Query("DELETE FROM workout_groups")
    suspend fun deleteAll()

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM workout_groups")
    suspend fun nextPosition(): Int

    @Insert
    suspend fun insert(group: WorkoutGroupEntity)

    @Insert
    suspend fun insertAll(groups: List<WorkoutGroupEntity>)

    @Delete
    suspend fun delete(group: WorkoutGroupEntity)

    @Query(
        """
        UPDATE workout_groups
        SET name = :name, updated_at_epoch_millis = :updatedAtEpochMillis
        WHERE id = :id
        """,
    )
    suspend fun rename(id: String, name: String, updatedAtEpochMillis: Long)

    @Query("UPDATE workout_groups SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: String, position: Int)

    @Query("UPDATE workout_groups SET updated_at_epoch_millis = :updatedAtEpochMillis WHERE id = :id")
    suspend fun touch(id: String, updatedAtEpochMillis: Long)
}
