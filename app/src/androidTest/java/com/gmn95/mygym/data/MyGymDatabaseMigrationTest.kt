package com.gmn95.mygym.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gmn95.mygym.data.local.MyGymDatabase
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MyGymDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MyGymDatabase::class.java,
    )

    @Test
    @Throws(IOException::class)
    fun migrateFrom1To2PreservesSettingsAndCreatesWorkoutTables() {
        helper.createDatabase(TEST_DATABASE, 1).apply {
            execSQL(
                """
                INSERT INTO app_settings (id, theme_mode, language_mode)
                VALUES (0, 'DARK', 'PT_BR')
                """.trimIndent(),
            )
            close()
        }

        val database = helper.runMigrationsAndValidate(
            TEST_DATABASE,
            2,
            true,
            MyGymDatabase.MIGRATION_1_2,
        )

        database.query("SELECT theme_mode, language_mode FROM app_settings WHERE id = 0").use {
            it.moveToFirst()
            assertEquals("DARK", it.getString(0))
            assertEquals("PT_BR", it.getString(1))
        }

        database.query(
            """
            SELECT name FROM sqlite_master
            WHERE type = 'table'
              AND name IN ('workout_groups', 'custom_exercises', 'workout_items')
            ORDER BY name
            """.trimIndent(),
        ).use {
            assertEquals(3, it.count)
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrateFrom2To3PreservesWorkoutsAndCreatesSessionTables() {
        helper.createDatabase(TEST_DATABASE_2, 2).apply {
            execSQL(
                """
                INSERT INTO workout_groups (
                    id, name, position, created_at_epoch_millis, updated_at_epoch_millis
                ) VALUES ('workout-1', 'Treino A', 0, 1000, 1000)
                """.trimIndent(),
            )
            close()
        }

        val database = helper.runMigrationsAndValidate(
            TEST_DATABASE_2,
            3,
            true,
            MyGymDatabase.MIGRATION_2_3,
        )

        database.query("SELECT name FROM workout_groups WHERE id = 'workout-1'").use {
            it.moveToFirst()
            assertEquals("Treino A", it.getString(0))
        }
        database.query(
            """
            SELECT name FROM sqlite_master
            WHERE type = 'table'
              AND name IN ('workout_sessions', 'session_exercises', 'session_rest_timers')
            ORDER BY name
            """.trimIndent(),
        ).use {
            assertEquals(3, it.count)
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrateFrom3To4CreatesSeriesForExistingSessionSnapshots() {
        helper.createDatabase(TEST_DATABASE_3, 3).apply {
            execSQL(
                """
                INSERT INTO workout_sessions (
                    id, workout_group_id, workout_name_snapshot, status,
                    started_at_epoch_millis, active_accumulated_millis
                ) VALUES ('session-1', 'workout-1', 'Treino A', 'COMPLETED', 1000, 1000)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO session_exercises (
                    id, session_id, workout_item_id, exercise_name_snapshot,
                    position, sets, repetitions_min, rest_seconds, is_completed
                ) VALUES ('exercise-1', 'session-1', 'item-1', 'Supino', 0, 3, 10, 60, 1)
                """.trimIndent(),
            )
            close()
        }

        val database = helper.runMigrationsAndValidate(
            TEST_DATABASE_3,
            4,
            true,
            MyGymDatabase.MIGRATION_3_4,
        )

        database.query(
            """
            SELECT position, is_completed
            FROM session_exercise_sets
            WHERE session_exercise_id = 'exercise-1'
            ORDER BY position
            """.trimIndent(),
        ).use {
            assertEquals(3, it.count)
            while (it.moveToNext()) {
                assertEquals(1, it.getInt(1))
            }
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrateFrom4To5PlacesExistingTrainingsInTheirOwnFolders() {
        helper.createDatabase(TEST_DATABASE_4, 4).apply {
            execSQL(
                """
                INSERT INTO workout_groups (
                    id, name, position, created_at_epoch_millis, updated_at_epoch_millis
                ) VALUES ('training-a', 'A — Superiores', 0, 1000, 2000)
                """.trimIndent(),
            )
            close()
        }

        val database = helper.runMigrationsAndValidate(
            TEST_DATABASE_4,
            5,
            true,
            MyGymDatabase.MIGRATION_4_5,
        )

        database.query(
            "SELECT name, workout_folder_id FROM workout_groups WHERE id = 'training-a'",
        ).use {
            it.moveToFirst()
            assertEquals("A — Superiores", it.getString(0))
            assertEquals("training-a", it.getString(1))
        }
        database.query("SELECT name FROM workout_folders WHERE id = 'training-a'").use {
            it.moveToFirst()
            assertEquals("A — Superiores", it.getString(0))
        }
    }

    private companion object {
        const val TEST_DATABASE = "migration-test"
        const val TEST_DATABASE_2 = "migration-test-2"
        const val TEST_DATABASE_3 = "migration-test-3"
        const val TEST_DATABASE_4 = "migration-test-4"
    }
}
