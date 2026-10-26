package com.gmn95.mygym.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

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

    @Insert
    suspend fun insert(session: WorkoutSessionEntity)

    @Update
    suspend fun update(session: WorkoutSessionEntity)
}
