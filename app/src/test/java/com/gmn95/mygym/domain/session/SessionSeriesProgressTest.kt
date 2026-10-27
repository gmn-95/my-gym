package com.gmn95.mygym.domain.session

import com.gmn95.mygym.data.repository.ActiveExerciseSet
import com.gmn95.mygym.data.repository.ActiveSessionExercise
import com.gmn95.mygym.data.repository.ActiveWorkoutSession
import com.gmn95.mygym.data.repository.WorkoutSessionStatus
import com.gmn95.mygym.data.repository.seriesProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionSeriesProgressTest {
    @Test
    fun selectsFirstUncompletedSeriesAndItsPreviousSeries() {
        val progress = sessionWithSeries(
            ActiveExerciseSet("set-1", 1, true),
            ActiveExerciseSet("set-2", 2, false),
        ).seriesProgress()

        assertEquals("set-2", progress.current?.setId)
        assertEquals(2, progress.current?.setPosition)
        assertEquals("set-1", progress.previous?.setId)
    }

    @Test
    fun hasNoCurrentSeriesWhenEverySeriesIsCompleted() {
        val progress = sessionWithSeries(
            ActiveExerciseSet("set-1", 1, true),
            ActiveExerciseSet("set-2", 2, true),
        ).seriesProgress()

        assertNull(progress.current)
        assertNull(progress.previous)
    }

    private fun sessionWithSeries(vararg series: ActiveExerciseSet) = ActiveWorkoutSession(
        id = "session",
        workoutGroupId = "workout",
        workoutName = "Treino",
        status = WorkoutSessionStatus.ACTIVE,
        startedAtEpochMillis = 0,
        activeAccumulatedMillis = 0,
        activeAnchorElapsedRealtime = 0,
        activeAnchorBootCount = 1,
        exercises = listOf(
            ActiveSessionExercise(
                id = "exercise",
                name = "Supino",
                notes = null,
                position = 0,
                sets = series.size,
                repetitionsMin = 8,
                repetitionsMax = 12,
                restSeconds = 60,
                isCompleted = series.all(ActiveExerciseSet::isCompleted),
                series = series.toList(),
            ),
        ),
        restTimer = null,
    )
}
