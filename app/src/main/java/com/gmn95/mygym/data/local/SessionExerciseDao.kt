package com.gmn95.mygym.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionExerciseDao {
    @Query(
        """
        SELECT * FROM session_exercises
        WHERE session_id = :sessionId
        ORDER BY position, id
        """,
    )
    fun observeBySession(sessionId: String): Flow<List<SessionExerciseEntity>>

    @Query("SELECT * FROM session_exercises WHERE id = :id")
    suspend fun getById(id: String): SessionExerciseEntity?

    @Query("SELECT * FROM session_exercises ORDER BY session_id, position")
    suspend fun getAll(): List<SessionExerciseEntity>

    @Query("DELETE FROM session_exercises")
    suspend fun deleteAll()

    @Query("DELETE FROM session_exercises WHERE session_id = :sessionId")
    suspend fun deleteBySession(sessionId: String)

    @Query(
        """
        DELETE FROM session_exercises
        WHERE session_id IN (
            SELECT id FROM workout_sessions WHERE status = 'COMPLETED'
        )
        """,
    )
    suspend fun deleteAllForCompletedSessions()

    @Insert
    suspend fun insertAll(exercises: List<SessionExerciseEntity>)

    @Query("UPDATE session_exercises SET is_completed = :isCompleted WHERE id = :id")
    suspend fun setCompleted(id: String, isCompleted: Boolean)
}
