package com.gmn95.mygym.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gmn95.mygym.data.local.MyGymDatabase
import com.gmn95.mygym.data.repository.RoomWorkoutRepository
import com.gmn95.mygym.domain.WorkoutItemConfiguration
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
}
