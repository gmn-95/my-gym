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

data class SessionSeriesReference(
    val exerciseId: String,
    val exerciseName: String,
    val setId: String,
    val setPosition: Int,
    val totalSets: Int,
    val repetitionsMin: Int,
    val repetitionsMax: Int?,
)

data class SessionSeriesProgress(
    val current: SessionSeriesReference?,
    val previous: SessionSeriesReference?,
)

/** A série atual é derivada do snapshot persistido, sem estado visual paralelo. */
fun ActiveWorkoutSession.seriesProgress(): SessionSeriesProgress {
    val series = exercises.flatMap { exercise ->
        exercise.series.map { set ->
            SessionSeriesReference(
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                setId = set.id,
                setPosition = set.position,
                totalSets = exercise.sets,
                repetitionsMin = exercise.repetitionsMin,
                repetitionsMax = exercise.repetitionsMax,
            ) to set.isCompleted
        }
    }
    val currentIndex = series.indexOfFirst { (_, isCompleted) -> !isCompleted }
    return SessionSeriesProgress(
        current = series.getOrNull(currentIndex)?.first,
        previous = series.getOrNull(currentIndex - 1)?.first,
    )
}

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

data class CompletedWorkoutSession(
    val id: String,
    val workoutName: String,
    val endedAtEpochMillis: Long,
    val activeDurationMillis: Long,
    val exercises: List<CompletedSessionExercise>,
)

data class CompletedSessionExercise(
    val position: Int,
    val name: String,
    val notes: String?,
    val sets: Int,
    val repetitionsMin: Int,
    val repetitionsMax: Int?,
    val restSeconds: Int,
    val isCompleted: Boolean,
)
