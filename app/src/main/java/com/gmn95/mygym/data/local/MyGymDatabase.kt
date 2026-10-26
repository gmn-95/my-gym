package com.gmn95.mygym.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AppSettingsEntity::class,
        WorkoutGroupEntity::class,
        CustomExerciseEntity::class,
        WorkoutItemEntity::class,
        WorkoutSessionEntity::class,
        SessionExerciseEntity::class,
        SessionRestTimerEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class MyGymDatabase : RoomDatabase() {
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun workoutGroupDao(): WorkoutGroupDao
    abstract fun customExerciseDao(): CustomExerciseDao
    abstract fun workoutItemDao(): WorkoutItemDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun sessionExerciseDao(): SessionExerciseDao
    abstract fun sessionRestTimerDao(): SessionRestTimerDao

    companion object {
        private const val DATABASE_NAME = "my-gym.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `workout_groups` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `position` INTEGER NOT NULL,
                        `created_at_epoch_millis` INTEGER NOT NULL,
                        `updated_at_epoch_millis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `custom_exercises` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `notes` TEXT,
                        `created_at_epoch_millis` INTEGER NOT NULL,
                        `updated_at_epoch_millis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `workout_items` (
                        `id` TEXT NOT NULL,
                        `workout_group_id` TEXT NOT NULL,
                        `custom_exercise_id` TEXT NOT NULL,
                        `position` INTEGER NOT NULL,
                        `sets` INTEGER NOT NULL,
                        `repetitions_min` INTEGER NOT NULL,
                        `repetitions_max` INTEGER,
                        `rest_seconds` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`workout_group_id`) REFERENCES `workout_groups`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`custom_exercise_id`) REFERENCES `custom_exercises`(`id`)
                            ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_workout_items_workout_group_id` " +
                        "ON `workout_items` (`workout_group_id`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_workout_items_custom_exercise_id` " +
                        "ON `workout_items` (`custom_exercise_id`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_workout_items_workout_group_id_position` " +
                        "ON `workout_items` (`workout_group_id`, `position`)",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `workout_sessions` (
                        `id` TEXT NOT NULL,
                        `workout_group_id` TEXT NOT NULL,
                        `workout_name_snapshot` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `started_at_epoch_millis` INTEGER NOT NULL,
                        `ended_at_epoch_millis` INTEGER,
                        `active_accumulated_millis` INTEGER NOT NULL,
                        `active_anchor_elapsed_realtime` INTEGER,
                        `active_anchor_boot_count` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_workout_sessions_status` " +
                        "ON `workout_sessions` (`status`)",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `session_exercises` (
                        `id` TEXT NOT NULL,
                        `session_id` TEXT NOT NULL,
                        `workout_item_id` TEXT NOT NULL,
                        `exercise_name_snapshot` TEXT NOT NULL,
                        `exercise_notes_snapshot` TEXT,
                        `position` INTEGER NOT NULL,
                        `sets` INTEGER NOT NULL,
                        `repetitions_min` INTEGER NOT NULL,
                        `repetitions_max` INTEGER,
                        `rest_seconds` INTEGER NOT NULL,
                        `is_completed` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`session_id`) REFERENCES `workout_sessions`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_session_exercises_session_id` " +
                        "ON `session_exercises` (`session_id`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_session_exercises_session_id_position` " +
                        "ON `session_exercises` (`session_id`, `position`)",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `session_rest_timers` (
                        `session_id` TEXT NOT NULL,
                        `session_exercise_id` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `duration_millis` INTEGER NOT NULL,
                        `remaining_at_anchor_millis` INTEGER NOT NULL,
                        `anchor_elapsed_realtime` INTEGER,
                        `anchor_boot_count` INTEGER,
                        PRIMARY KEY(`session_id`),
                        FOREIGN KEY(`session_id`) REFERENCES `workout_sessions`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`session_exercise_id`) REFERENCES `session_exercises`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_session_rest_timers_session_exercise_id` " +
                        "ON `session_rest_timers` (`session_exercise_id`)",
                )
            }
        }

        fun create(context: Context): MyGymDatabase =
            Room.databaseBuilder(
                context,
                MyGymDatabase::class.java,
                DATABASE_NAME,
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}
