package com.gmn95.mygym.domain

data class WorkoutItemConfiguration(
    val sets: Int,
    val repetitionsMin: Int,
    val repetitionsMax: Int?,
    val restSeconds: Int,
)

object WorkoutInputValidator {
    fun isValidName(name: String): Boolean = name.isNotBlank()

    fun isValidConfiguration(configuration: WorkoutItemConfiguration): Boolean =
        configuration.sets > 0 &&
            configuration.repetitionsMin > 0 &&
            (configuration.repetitionsMax == null ||
                configuration.repetitionsMax >= configuration.repetitionsMin) &&
            configuration.restSeconds >= 0
}
