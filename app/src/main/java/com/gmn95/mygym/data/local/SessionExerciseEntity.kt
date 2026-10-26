package com.gmn95.mygym.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "session_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["session_id"]),
        Index(value = ["session_id", "position"]),
    ],
)
data class SessionExerciseEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    @ColumnInfo(name = "workout_item_id")
    val workoutItemId: String,
    @ColumnInfo(name = "exercise_name_snapshot")
    val exerciseNameSnapshot: String,
    @ColumnInfo(name = "exercise_notes_snapshot")
    val exerciseNotesSnapshot: String?,
    val position: Int,
    val sets: Int,
    @ColumnInfo(name = "repetitions_min")
    val repetitionsMin: Int,
    @ColumnInfo(name = "repetitions_max")
    val repetitionsMax: Int?,
    @ColumnInfo(name = "rest_seconds")
    val restSeconds: Int,
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean,
)
