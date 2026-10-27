package com.gmn95.mygym.data.local

import androidx.room.Dao
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class CompletedWorkoutSessionSummaryRow(
    @Embedded
    val session: WorkoutSessionEntity,
    @ColumnInfo(name = "exercise_count")
    val exerciseCount: Int,
    @ColumnInfo(name = "completed_exercise_count")
    val completedExerciseCount: Int,
)

@Dao
interface WorkoutSessionDao {
    @Query(
        """
        SELECT * FROM workout_sessions
        WHERE status IN ('ACTIVE', 'PAUSED')
        ORDER BY started_at_epoch_millis DESC
        LIMIT 1
        """,
    )
    fun observeActive(): Flow<WorkoutSessionEntity?>

    @Query(
        """
        SELECT * FROM workout_sessions
        WHERE status IN ('ACTIVE', 'PAUSED')
        ORDER BY started_at_epoch_millis DESC
        LIMIT 1
        """,
    )
    suspend fun getActive(): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getById(id: String): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions ORDER BY started_at_epoch_millis")
    suspend fun getAll(): List<WorkoutSessionEntity>

    @Query("DELETE FROM workout_sessions")
    suspend fun deleteAll()

    @Query("DELETE FROM workout_sessions WHERE id = :sessionId AND status = 'COMPLETED'")
    suspend fun deleteCompletedById(sessionId: String)

    @Query(
        """
        SELECT
            workout_sessions.*,
            COUNT(session_exercises.id) AS exercise_count,
            COALESCE(
                SUM(CASE WHEN session_exercises.is_completed THEN 1 ELSE 0 END),
                0
            ) AS completed_exercise_count
        FROM workout_sessions
        LEFT JOIN session_exercises
            ON session_exercises.session_id = workout_sessions.id
        WHERE workout_sessions.status = 'COMPLETED'
        GROUP BY workout_sessions.id
        ORDER BY workout_sessions.ended_at_epoch_millis DESC
        """,
    )
    fun observeCompletedSummaries(): Flow<List<CompletedWorkoutSessionSummaryRow>>

    @Query(
        "SELECT * FROM workout_sessions WHERE id = :id AND status = 'COMPLETED'",
    )
    fun observeCompletedById(id: String): Flow<WorkoutSessionEntity?>

    @Insert
    suspend fun insert(session: WorkoutSessionEntity)

    @Insert
    suspend fun insertAll(sessions: List<WorkoutSessionEntity>)

    @Update
    suspend fun update(session: WorkoutSessionEntity)
}
