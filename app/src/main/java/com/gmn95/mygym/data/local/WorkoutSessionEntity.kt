package com.gmn95.mygym.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_sessions",
    indices = [Index(value = ["status"])],
)
data class WorkoutSessionEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "workout_group_id")
    val workoutGroupId: String,
    @ColumnInfo(name = "workout_name_snapshot")
    val workoutNameSnapshot: String,
    val status: String,
    @ColumnInfo(name = "started_at_epoch_millis")
    val startedAtEpochMillis: Long,
    @ColumnInfo(name = "ended_at_epoch_millis")
    val endedAtEpochMillis: Long?,
    @ColumnInfo(name = "active_accumulated_millis")
    val activeAccumulatedMillis: Long,
    @ColumnInfo(name = "active_anchor_elapsed_realtime")
    val activeAnchorElapsedRealtime: Long?,
    @ColumnInfo(name = "active_anchor_boot_count")
    val activeAnchorBootCount: Int?,
)
