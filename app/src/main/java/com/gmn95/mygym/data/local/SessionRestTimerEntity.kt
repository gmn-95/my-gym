package com.gmn95.mygym.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "session_rest_timers",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SessionExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["session_exercise_id"])],
)
data class SessionRestTimerEntity(
    @PrimaryKey
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    @ColumnInfo(name = "session_exercise_id")
    val sessionExerciseId: String,
    val status: String,
    @ColumnInfo(name = "duration_millis")
    val durationMillis: Long,
    @ColumnInfo(name = "remaining_at_anchor_millis")
    val remainingAtAnchorMillis: Long,
    @ColumnInfo(name = "anchor_elapsed_realtime")
    val anchorElapsedRealtime: Long?,
    @ColumnInfo(name = "anchor_boot_count")
    val anchorBootCount: Int?,
)
