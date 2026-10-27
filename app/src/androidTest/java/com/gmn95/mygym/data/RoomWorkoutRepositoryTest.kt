package com.gmn95.mygym.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gmn95.mygym.data.local.MyGymDatabase
import com.gmn95.mygym.data.local.WorkoutSessionEntity
import com.gmn95.mygym.data.backup.WorkoutBackupRepository
import com.gmn95.mygym.data.repository.RoomWorkoutRepository
import com.gmn95.mygym.data.repository.AppSettingsRepository
import com.gmn95.mygym.data.repository.WorkoutSessionStatus
import com.gmn95.mygym.domain.WorkoutItemConfiguration
import com.gmn95.mygym.domain.session.SessionClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomWorkoutRepositoryTest {
    private lateinit var database: MyGymDatabase
    private lateinit var repository: RoomWorkoutRepository
    private var nextId = 0
    private var currentTime = 1_000L

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MyGymDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomWorkoutRepository(
            database = database,
            idFactory = { "id-${nextId++}" },
            currentTimeMillis = { currentTime++ },
        )
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun createsDuplicatesReordersAndDeletesWorkoutsWithoutSharingItems() = runBlocking {
        val workoutId = repository.createWorkout("A — Superiores")
        repository.createAndAddExercise(workoutId, "Supino", "Com barra")
        repository.createAndAddExercise(
            workoutId,
            "Remada",
            null,
            WorkoutItemConfiguration(4, 8, 12, 90),
        )

        val original = repository.observeWorkout(workoutId).first()!!
        assertEquals(listOf("Supino", "Remada"), original.exercises.map { it.name })

        repository.moveExercise(workoutId, original.exercises.last().id, -1)
        val reordered = repository.observeWorkout(workoutId).first()!!
        assertEquals(listOf("Remada", "Supino"), reordered.exercises.map { it.name })

        val duplicateId = repository.duplicateWorkout(workoutId, "A — Superiores (cópia)")!!
        val duplicate = repository.observeWorkout(duplicateId).first()!!
        assertEquals(2, duplicate.exercises.size)
        assertTrue(
            duplicate.exercises.map { it.id }.toSet()
                .intersect(reordered.exercises.map { it.id }.toSet())
                .isEmpty(),
        )

        repository.removeExercise(workoutId, reordered.exercises.first().id)
        assertEquals(1, repository.observeWorkout(workoutId).first()!!.exercises.size)
        assertEquals(2, repository.observeWorkout(duplicateId).first()!!.exercises.size)

        repository.deleteWorkout(workoutId)
        assertEquals(listOf(duplicateId), repository.observeWorkouts().first().map { it.id })
    }

    @Test
    fun folderGroupsMultipleIndependentWorkouts() = runBlocking {
        val folderId = repository.createFolder("Rotina")
        val upperBodyId = repository.createWorkout(folderId, "A — Superiores")
        val lowerBodyId = repository.createWorkout(folderId, "B — Inferiores")

        repository.createAndAddExercise(upperBodyId, "Supino", null)
        repository.createAndAddExercise(lowerBodyId, "Agachamento", null)

        val folder = repository.observeFolder(folderId).first()!!
        assertEquals("Rotina", folder.name)
        assertEquals(listOf("A — Superiores", "B — Inferiores"), folder.workouts.map { it.name })
        assertEquals(listOf("Supino"), repository.observeWorkout(upperBodyId).first()!!.exercises.map { it.name })
        assertEquals(listOf("Agachamento"), repository.observeWorkout(lowerBodyId).first()!!.exercises.map { it.name })
    }

    @Test
    fun workoutSummaryUsesOnlyCompletedSessionsForAverageDuration() = runBlocking {
        val workoutId = repository.createWorkout("A — Superiores")
        database.workoutSessionDao().insert(
            WorkoutSessionEntity(
                id = "completed-one",
                workoutGroupId = workoutId,
                workoutNameSnapshot = "A — Superiores",
                status = WorkoutSessionStatus.COMPLETED.name,
                startedAtEpochMillis = 1_000L,
                endedAtEpochMillis = 61_000L,
                activeAccumulatedMillis = 60_000L,
                activeAnchorElapsedRealtime = null,
                activeAnchorBootCount = null,
            ),
        )
        database.workoutSessionDao().insert(
            WorkoutSessionEntity(
                id = "completed-two",
                workoutGroupId = workoutId,
                workoutNameSnapshot = "A — Superiores",
                status = WorkoutSessionStatus.COMPLETED.name,
                startedAtEpochMillis = 100_000L,
                endedAtEpochMillis = 220_000L,
                activeAccumulatedMillis = 120_000L,
                activeAnchorElapsedRealtime = null,
                activeAnchorBootCount = null,
            ),
        )
        database.workoutSessionDao().insert(
            WorkoutSessionEntity(
                id = "cancelled",
                workoutGroupId = workoutId,
                workoutNameSnapshot = "A — Superiores",
                status = WorkoutSessionStatus.CANCELLED.name,
                startedAtEpochMillis = 300_000L,
                endedAtEpochMillis = 600_000L,
                activeAccumulatedMillis = 300_000L,
                activeAnchorElapsedRealtime = null,
                activeAnchorBootCount = null,
            ),
        )

        val summary = repository.observeWorkouts().first().single()

        assertEquals(90_000L, summary.averageDurationMillis)
    }

    @Test
    fun backupRoundTripRestoresWorkoutAndInvalidBackupDoesNotReplaceData() = runBlocking {
        val workoutId = repository.createWorkout("Ficha para backup")
        repository.createAndAddExercise(workoutId, "Supino", "Pegada média")
        val backupRepository = WorkoutBackupRepository(database, TestSessionClock)

        val json = backupRepository.createJson()
        repository.deleteWorkout(workoutId)
        backupRepository.restore(json)

        val restored = repository.observeWorkouts().first().single()
        assertEquals("Ficha para backup", restored.name)
        assertEquals(
            listOf("Supino"),
            repository.observeWorkout(restored.id).first()!!.exercises.map { it.name },
        )

        assertTrue(runCatching { backupRepository.restore("{invalid") }.isFailure)
        assertEquals(listOf(restored.id), repository.observeWorkouts().first().map { it.id })
    }

    @Test
    fun appSettingsPersistThemeAndLanguageTogether() = runBlocking {
        val settingsRepository = AppSettingsRepository(database.appSettingsDao())

        settingsRepository.setThemeMode("DARK")
        settingsRepository.setLanguageMode("PT_BR")

        val settings = database.appSettingsDao().get()!!
        assertEquals("DARK", settings.themeMode)
        assertEquals("PT_BR", settings.languageMode)
    }

    private object TestSessionClock : SessionClock {
        override fun epochMillis() = 10_000L
        override fun elapsedRealtimeMillis() = 500L
        override fun bootCount() = 1
    }
}
