package com.gmn95.mygym.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionRestTimerDao {
    @Query("SELECT * FROM session_rest_timers WHERE session_id = :sessionId")
    fun observeBySession(sessionId: String): Flow<SessionRestTimerEntity?>

    @Query("SELECT * FROM session_rest_timers WHERE session_id = :sessionId")
    suspend fun getBySession(sessionId: String): SessionRestTimerEntity?

    @Query("SELECT * FROM session_rest_timers")
    suspend fun getAll(): List<SessionRestTimerEntity>

    @Query("DELETE FROM session_rest_timers")
    suspend fun deleteAll()

    @Upsert
    suspend fun upsert(timer: SessionRestTimerEntity)

    @Delete
    suspend fun delete(timer: SessionRestTimerEntity)
}
