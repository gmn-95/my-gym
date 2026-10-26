package com.gmn95.mygym.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "session_exercise_sets",
    foreignKeys = [
        ForeignKey(
            entity = SessionExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["session_exercise_id"]),
        Index(value = ["session_exercise_id", "position"]),
    ],
)
data class SessionExerciseSetEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "session_exercise_id")
    val sessionExerciseId: String,
    val position: Int,
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean,
)
