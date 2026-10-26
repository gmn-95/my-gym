package com.gmn95.mygym.data.repository

import com.gmn95.mygym.domain.WorkoutItemConfiguration

data class WorkoutSummary(
    val id: String,
    val name: String,
    val exerciseCount: Int,
)

data class Workout(
    val id: String,
    val name: String,
    val exercises: List<WorkoutExercise>,
)

data class WorkoutExercise(
    val id: String,
    val customExerciseId: String,
    val name: String,
    val notes: String?,
    val position: Int,
    val configuration: WorkoutItemConfiguration,
)

data class CustomExercise(
    val id: String,
    val name: String,
    val notes: String?,
)
