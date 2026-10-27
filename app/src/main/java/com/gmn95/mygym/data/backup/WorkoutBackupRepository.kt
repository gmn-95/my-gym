package com.gmn95.mygym.data.backup

import androidx.room.withTransaction
import com.gmn95.mygym.data.local.MyGymDatabase
import com.gmn95.mygym.domain.session.SessionClock
import com.gmn95.mygym.domain.session.TimerMath

data class WorkoutBackupSummary(
    val workoutCount: Int,
    val customExerciseCount: Int,
    val completedSessionCount: Int,
)

class WorkoutBackupRepository(
    private val database: MyGymDatabase,
    private val clock: SessionClock,
) {
    suspend fun createJson(): String = database.withTransaction {
        val bootCount = clock.bootCount()
        val nowElapsedRealtime = clock.elapsedRealtimeMillis()
        val sessions = database.workoutSessionDao().getAll().map { session ->
            if (session.status != "ACTIVE" || session.activeAnchorBootCount != bootCount) session
            else session.copy(
                status = "PAUSED",
                activeAccumulatedMillis = TimerMath.activeDurationMillis(
                    accumulatedMillis = session.activeAccumulatedMillis,
                    anchorElapsedRealtime = session.activeAnchorElapsedRealtime,
                    anchorBootCount = session.activeAnchorBootCount,
                    nowElapsedRealtime = nowElapsedRealtime,
                    currentBootCount = bootCount,
                ),
                activeAnchorElapsedRealtime = null,
                activeAnchorBootCount = null,
            )
        }
        val timers = database.sessionRestTimerDao().getAll().map { timer ->
            if (timer.status != "RUNNING" || timer.anchorBootCount != bootCount) timer
            else timer.copy(
                status = "PAUSED",
                remainingAtAnchorMillis = TimerMath.remainingMillis(
                    remainingAtAnchorMillis = timer.remainingAtAnchorMillis,
                    anchorElapsedRealtime = timer.anchorElapsedRealtime,
                    anchorBootCount = timer.anchorBootCount,
                    nowElapsedRealtime = nowElapsedRealtime,
                    currentBootCount = bootCount,
                ),
                anchorElapsedRealtime = null,
                anchorBootCount = null,
            )
        }
        WorkoutBackupJson.encode(
            WorkoutBackup(
                settings = database.appSettingsDao().get()
                    ?: com.gmn95.mygym.data.local.AppSettingsEntity(),
                groups = database.workoutGroupDao().getAll(),
                exercises = database.customExerciseDao().getAll(),
                items = database.workoutItemDao().getAll(),
                sessions = sessions,
                sessionExercises = database.sessionExerciseDao().getAll(),
                sessionSets = database.sessionExerciseSetDao().getAll(),
                restTimers = timers,
            ),
        )
    }

    fun validate(json: String): WorkoutBackupSummary {
        val backup = WorkoutBackupJson.decode(json)
        return WorkoutBackupSummary(
            workoutCount = backup.groups.size,
            customExerciseCount = backup.exercises.size,
            completedSessionCount = backup.sessions.count { it.status == "COMPLETED" },
        )
    }

    suspend fun restore(json: String) {
        val backup = WorkoutBackupJson.decode(json)
        val sessions = backup.sessions.map { session ->
            if (session.status == "ACTIVE") {
                session.copy(
                    status = "PAUSED",
                    activeAnchorElapsedRealtime = null,
                    activeAnchorBootCount = null,
                )
            } else {
                session
            }
        }
        val timers = backup.restTimers.map { timer ->
            if (timer.status == "RUNNING") {
                timer.copy(
                    status = "PAUSED",
                    anchorElapsedRealtime = null,
                    anchorBootCount = null,
                )
            } else {
                timer
            }
        }

        database.withTransaction {
            database.sessionRestTimerDao().deleteAll()
            database.sessionExerciseSetDao().deleteAll()
            database.sessionExerciseDao().deleteAll()
            database.workoutSessionDao().deleteAll()
            database.workoutItemDao().deleteAll()
            database.workoutGroupDao().deleteAll()
            database.customExerciseDao().deleteAll()

            database.appSettingsDao().upsert(backup.settings)
            database.workoutGroupDao().insertAll(backup.groups)
            database.customExerciseDao().insertAll(backup.exercises)
            database.workoutItemDao().insertAll(backup.items)
            database.workoutSessionDao().insertAll(sessions)
            database.sessionExerciseDao().insertAll(backup.sessionExercises)
            database.sessionExerciseSetDao().insertAll(backup.sessionSets)
            timers.forEach { database.sessionRestTimerDao().upsert(it) }
        }
    }
}
