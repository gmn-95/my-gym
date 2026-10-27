package com.gmn95.mygym.data.session

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gmn95.mygym.data.local.MyGymDatabase
import com.gmn95.mygym.data.repository.RestTimerStatus
import com.gmn95.mygym.data.repository.RoomSessionRepository
import com.gmn95.mygym.data.repository.RoomWorkoutRepository
import com.gmn95.mygym.data.repository.WorkoutSessionStatus
import com.gmn95.mygym.domain.WorkoutItemConfiguration
import com.gmn95.mygym.domain.session.SessionClock
import com.gmn95.mygym.domain.session.TimerMath
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomSessionRepositoryTest {
    private lateinit var database: MyGymDatabase
    private lateinit var workoutRepository: RoomWorkoutRepository
    private lateinit var sessionRepository: RoomSessionRepository
    private val clock = FakeSessionClock()
    private var nextId = 0

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MyGymDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val idFactory = { "id-${nextId++}" }
        workoutRepository = RoomWorkoutRepository(
            database = database,
            idFactory = idFactory,
            currentTimeMillis = clock::epochMillis,
        )
        sessionRepository = RoomSessionRepository(
            database = database,
            clock = clock,
            idFactory = idFactory,
        )
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun sessionUsesSnapshotAndKeepsActiveTimeWhenWorkoutIsDeleted() = runBlocking {
        val workoutId = workoutRepository.createWorkout("A — Superiores")
        workoutRepository.createAndAddExercise(workoutId, "Supino", "Com barra")
        val sessionId = sessionRepository.startSession(workoutId)

        clock.advance(5_000)
        sessionRepository.pauseSession()
        workoutRepository.renameWorkout(workoutId, "Nome alterado")
        workoutRepository.deleteWorkout(workoutId)

        val active = sessionRepository.observeActiveSession().first()!!
        assertEquals("A — Superiores", active.workoutName)
        assertEquals("Supino", active.exercises.single().name)
        assertEquals(5_000, active.activeAccumulatedMillis)

        val firstSeries = active.exercises.single().series.first()
        sessionRepository.setExerciseSetCompleted(
            sessionExerciseId = active.exercises.single().id,
            sessionExerciseSetId = firstSeries.id,
            isCompleted = true,
        )
        assertEquals(
            RestTimerStatus.RUNNING,
            sessionRepository.observeActiveSession().first()!!.restTimer!!.status,
        )
        assertEquals(
            false,
            sessionRepository.observeActiveSession().first()!!.exercises.single().isCompleted,
        )
        active.exercises.single().series.drop(1).forEach { set ->
            sessionRepository.setExerciseSetCompleted(
                sessionExerciseId = active.exercises.single().id,
                sessionExerciseSetId = set.id,
                isCompleted = true,
            )
        }
        assertEquals(
            true,
            sessionRepository.observeActiveSession().first()!!.exercises.single().isCompleted,
        )
        sessionRepository.resumeSession()
        clock.advance(2_000)
        sessionRepository.completeActiveSession()

        assertNull(sessionRepository.observeActiveSession().first())
        val stored = database.workoutSessionDao().getById(sessionId)!!
        assertEquals(WorkoutSessionStatus.COMPLETED.name, stored.status)
        assertEquals(7_000, stored.activeAccumulatedMillis)
        val history = sessionRepository.observeCompletedSessions().first()
        assertEquals(1, history.size)
        assertEquals("A — Superiores", history.single().workoutName)
        assertEquals(1, history.single().completedExerciseCount)
        val details = sessionRepository.observeCompletedSession(sessionId).first()!!
        assertEquals("A — Superiores", details.workoutName)
        assertEquals(listOf("Supino"), details.exercises.map { it.name })
        assertEquals(true, details.exercises.single().isCompleted)
    }

    @Test
    fun deletesOnlyCompletedSessionAndItsSnapshots() = runBlocking {
        val workoutId = workoutRepository.createWorkout("Histórico removível")
        workoutRepository.createAndAddExercise(workoutId, "Supino", null)
        val completedSessionId = sessionRepository.startSession(workoutId)
        sessionRepository.completeActiveSession()
        val completedExerciseIds = database.sessionExerciseDao().getAll()
            .filter { it.sessionId == completedSessionId }
            .map { it.id }

        val activeSessionId = sessionRepository.startSession(workoutId)
        sessionRepository.deleteCompletedSession(completedSessionId)

        assertNull(database.workoutSessionDao().getById(completedSessionId))
        assertTrue(
            database.sessionExerciseDao().getAll()
                .none { it.sessionId == completedSessionId },
        )
        assertTrue(
            database.sessionExerciseSetDao().getAll()
                .none { it.sessionExerciseId in completedExerciseIds },
        )
        assertEquals(activeSessionId, sessionRepository.observeActiveSession().first()!!.id)
        assertTrue(sessionRepository.observeCompletedSessions().first().isEmpty())
    }

    @Test
    fun clearsCompletedHistoryWithoutAffectingActiveSession() = runBlocking {
        val workoutId = workoutRepository.createWorkout("Histórico completo")
        workoutRepository.createAndAddExercise(workoutId, "Remada", null)
        val firstCompletedId = sessionRepository.startSession(workoutId)
        sessionRepository.completeActiveSession()
        val secondCompletedId = sessionRepository.startSession(workoutId)
        sessionRepository.completeActiveSession()
        val completedIds = setOf(firstCompletedId, secondCompletedId)
        val completedExerciseIds = database.sessionExerciseDao().getAll()
            .filter { it.sessionId in completedIds }
            .map { it.id }
        val activeSessionId = sessionRepository.startSession(workoutId)

        sessionRepository.clearCompletedSessions()

        assertTrue(completedIds.all { database.workoutSessionDao().getById(it) == null })
        assertTrue(
            database.sessionExerciseDao().getAll()
                .none { it.sessionId in completedIds },
        )
        assertTrue(
            database.sessionExerciseSetDao().getAll()
                .none { it.sessionExerciseId in completedExerciseIds },
        )
        assertEquals(activeSessionId, sessionRepository.observeActiveSession().first()!!.id)
        assertTrue(sessionRepository.observeCompletedSessions().first().isEmpty())
    }

    @Test
    fun restTimerPausesResumesAndRecoversPausedAfterReboot() = runBlocking {
        val workoutId = workoutRepository.createWorkout("B — Inferiores")
        workoutRepository.createAndAddExercise(workoutId, "Agachamento", null)
        sessionRepository.startSession(workoutId)
        val exercise = sessionRepository.observeActiveSession().first()!!.exercises.single()
        val firstSeries = exercise.series.first()

        sessionRepository.setExerciseSetCompleted(
            sessionExerciseId = exercise.id,
            sessionExerciseSetId = firstSeries.id,
            isCompleted = true,
        )
        clock.advance(10_000)
        sessionRepository.pauseRest()
        var timer = sessionRepository.observeActiveSession().first()!!.restTimer!!
        assertEquals(RestTimerStatus.PAUSED, timer.status)
        assertEquals(50_000, timer.remainingAtAnchorMillis)

        sessionRepository.resumeRest()
        clock.advance(5_000)
        sessionRepository.checkpointActiveTimers()
        clock.reboot()
        sessionRepository.recoverAfterRebootIfNeeded()

        val active = sessionRepository.observeActiveSession().first()!!
        assertEquals(WorkoutSessionStatus.PAUSED, active.status)
        timer = active.restTimer!!
        assertEquals(RestTimerStatus.PAUSED, timer.status)
        assertEquals(
            45_000,
            TimerMath.remainingMillis(
                timer.remainingAtAnchorMillis,
                timer.anchorElapsedRealtime,
                timer.anchorBootCount,
                clock.elapsedRealtimeMillis(),
                clock.bootCount(),
            ),
        )
    }

    @Test
    fun adjustsRemainingRestWithoutChangingExerciseConfiguration() = runBlocking {
        val workoutId = workoutRepository.createWorkout("Descanso ajustável")
        workoutRepository.createAndAddExercise(workoutId, "Leg press", null)
        sessionRepository.startSession(workoutId)
        val exercise = sessionRepository.observeActiveSession().first()!!.exercises.single()

        sessionRepository.setExerciseSetCompleted(exercise.id, exercise.series.first().id, true)
        sessionRepository.adjustRestBy(10_000)

        var timer = sessionRepository.observeActiveSession().first()!!.restTimer!!
        assertEquals(70_000, timer.remainingAtAnchorMillis)
        sessionRepository.adjustRestBy(-80_000)
        timer = sessionRepository.observeActiveSession().first()!!.restTimer!!
        assertEquals(0, timer.remainingAtAnchorMillis)
        assertEquals(60, sessionRepository.observeActiveSession().first()!!.exercises.single().restSeconds)
    }

    @Test
    fun updatesOnlyActiveSnapshotAndKeepsCompletedSeries() = runBlocking {
        val workoutId = workoutRepository.createWorkout("Treino ajustável")
        workoutRepository.createAndAddExercise(workoutId, "Puxada", null)
        sessionRepository.startSession(workoutId)
        var exercise = sessionRepository.observeActiveSession().first()!!.exercises.single()
        sessionRepository.setExerciseSetCompleted(exercise.id, exercise.series[0].id, true)

        sessionRepository.updateActiveExerciseConfiguration(
            exercise.id,
            WorkoutItemConfiguration(4, 6, 8, 90),
        )

        exercise = sessionRepository.observeActiveSession().first()!!.exercises.single()
        assertEquals(4, exercise.sets)
        assertEquals(4, exercise.series.size)
        assertEquals(6, exercise.repetitionsMin)
        assertEquals(8, exercise.repetitionsMax)
        assertEquals(90, exercise.restSeconds)
        assertTrue(exercise.series.first().isCompleted)

        sessionRepository.setExerciseSetCompleted(exercise.id, exercise.series[1].id, true)
        assertTrue(
            runCatching {
                sessionRepository.updateActiveExerciseConfiguration(
                    exercise.id,
                    WorkoutItemConfiguration(1, 6, 8, 90),
                )
            }.isFailure,
        )

        sessionRepository.updateActiveExerciseConfiguration(
            exercise.id,
            WorkoutItemConfiguration(2, 6, 8, 90),
        )
        exercise = sessionRepository.observeActiveSession().first()!!.exercises.single()
        assertEquals(2, exercise.sets)
        assertEquals(2, exercise.series.size)
        assertTrue(exercise.series.all { it.isCompleted })
    }

    @Test
    fun completedSeriesDoesNotCreateTimerWhenExerciseHasNoRest() = runBlocking {
        val workoutId = workoutRepository.createWorkout("C — Sem descanso")
        workoutRepository.createAndAddExercise(
            workoutId = workoutId,
            name = "Prancha",
            notes = null,
            configuration = WorkoutItemConfiguration(
                sets = 1,
                repetitionsMin = 1,
                repetitionsMax = null,
                restSeconds = 0,
            ),
        )
        sessionRepository.startSession(workoutId)
        val exercise = sessionRepository.observeActiveSession().first()!!.exercises.single()

        sessionRepository.setExerciseSetCompleted(
            sessionExerciseId = exercise.id,
            sessionExerciseSetId = exercise.series.single().id,
            isCompleted = true,
        )

        val active = sessionRepository.observeActiveSession().first()!!
        assertNull(active.restTimer)
        assertEquals(true, active.exercises.single().isCompleted)
    }

    @Test
    fun completingExerciseUpdatesAllItsSeriesAndCanBeUndone() = runBlocking {
        val workoutId = workoutRepository.createWorkout("D — Conclusão rápida")
        workoutRepository.createAndAddExercise(workoutId, "Remada", null)
        sessionRepository.startSession(workoutId)
        val exercise = sessionRepository.observeActiveSession().first()!!.exercises.single()

        sessionRepository.setExerciseCompleted(exercise.id, true)

        var activeExercise = sessionRepository.observeActiveSession().first()!!.exercises.single()
        assertEquals(true, activeExercise.isCompleted)
        assertEquals(true, activeExercise.series.all { it.isCompleted })
        assertEquals(
            RestTimerStatus.RUNNING,
            sessionRepository.observeActiveSession().first()!!.restTimer!!.status,
        )

        sessionRepository.setExerciseCompleted(exercise.id, false)

        activeExercise = sessionRepository.observeActiveSession().first()!!.exercises.single()
        assertEquals(false, activeExercise.isCompleted)
        assertEquals(false, activeExercise.series.any { it.isCompleted })
    }

    private class FakeSessionClock : SessionClock {
        private var epoch = 1_000_000L
        private var elapsed = 10_000L
        private var boot = 1

        override fun epochMillis(): Long = epoch

        override fun elapsedRealtimeMillis(): Long = elapsed

        override fun bootCount(): Int = boot

        fun advance(millis: Long) {
            epoch += millis
            elapsed += millis
        }

        fun reboot() {
            epoch += 30_000
            elapsed = 100
            boot += 1
        }
    }
}
