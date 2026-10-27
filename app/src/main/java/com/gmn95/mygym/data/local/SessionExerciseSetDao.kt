package com.gmn95.mygym.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionExerciseSetDao {
    @Query(
        """
        SELECT session_exercise_sets.*
        FROM session_exercise_sets
        INNER JOIN session_exercises
            ON session_exercises.id = session_exercise_sets.session_exercise_id
        WHERE session_exercises.session_id = :sessionId
        ORDER BY session_exercise_sets.session_exercise_id, session_exercise_sets.position
        """,
    )
    fun observeBySession(sessionId: String): Flow<List<SessionExerciseSetEntity>>

    @Query("SELECT * FROM session_exercise_sets WHERE id = :id")
    suspend fun getById(id: String): SessionExerciseSetEntity?

    @Query("SELECT * FROM session_exercise_sets ORDER BY session_exercise_id, position")
    suspend fun getAll(): List<SessionExerciseSetEntity>

    @Query("DELETE FROM session_exercise_sets")
    suspend fun deleteAll()

    @Query(
        """
        SELECT * FROM session_exercise_sets
        WHERE session_exercise_id = :sessionExerciseId
        ORDER BY position
        """,
    )
    suspend fun getByExercise(sessionExerciseId: String): List<SessionExerciseSetEntity>

    @Query(
        "UPDATE session_exercise_sets SET is_completed = :isCompleted " +
            "WHERE session_exercise_id = :sessionExerciseId",
    )
    suspend fun setCompletedForExercise(sessionExerciseId: String, isCompleted: Boolean)

    @Insert
    suspend fun insertAll(sets: List<SessionExerciseSetEntity>)

    @Update
    suspend fun update(set: SessionExerciseSetEntity)
}
