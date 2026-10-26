package com.gmn95.mygym.data.repository

enum class WorkoutSessionStatus {
    ACTIVE,
    PAUSED,
    COMPLETED,
    CANCELLED,
}

enum class RestTimerStatus {
    RUNNING,
    PAUSED,
}

data class ActiveWorkoutSession(
    val id: String,
    val workoutGroupId: String,
    val workoutName: String,
    val status: WorkoutSessionStatus,
    val startedAtEpochMillis: Long,
    val activeAccumulatedMillis: Long,
    val activeAnchorElapsedRealtime: Long?,
    val activeAnchorBootCount: Int?,
    val exercises: List<ActiveSessionExercise>,
    val restTimer: ActiveRestTimer?,
)

data class ActiveSessionExercise(
    val id: String,
    val name: String,
    val notes: String?,
    val position: Int,
    val sets: Int,
    val repetitionsMin: Int,
    val repetitionsMax: Int?,
    val restSeconds: Int,
    val isCompleted: Boolean,
    val series: List<ActiveExerciseSet>,
)

data class ActiveExerciseSet(
    val id: String,
    val position: Int,
    val isCompleted: Boolean,
)

data class ActiveRestTimer(
    val sessionExerciseId: String,
    val exerciseName: String,
    val status: RestTimerStatus,
    val durationMillis: Long,
    val remainingAtAnchorMillis: Long,
    val anchorElapsedRealtime: Long?,
    val anchorBootCount: Int?,
)

data class ActiveSessionSummary(
    val id: String,
    val workoutName: String,
    val status: WorkoutSessionStatus,
)

data class CompletedWorkoutSessionSummary(
    val id: String,
    val workoutName: String,
    val endedAtEpochMillis: Long,
    val activeDurationMillis: Long,
    val exerciseCount: Int,
    val completedExerciseCount: Int,
)
