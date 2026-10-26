package com.gmn95.mygym.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_items",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["workout_group_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CustomExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["custom_exercise_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["workout_group_id"]),
        Index(value = ["custom_exercise_id"]),
        Index(value = ["workout_group_id", "position"]),
    ],
)
data class WorkoutItemEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "workout_group_id")
    val workoutGroupId: String,
    @ColumnInfo(name = "custom_exercise_id")
    val customExerciseId: String,
    val position: Int,
    val sets: Int,
    @ColumnInfo(name = "repetitions_min")
    val repetitionsMin: Int,
    @ColumnInfo(name = "repetitions_max")
    val repetitionsMax: Int?,
    @ColumnInfo(name = "rest_seconds")
    val restSeconds: Int,
)
