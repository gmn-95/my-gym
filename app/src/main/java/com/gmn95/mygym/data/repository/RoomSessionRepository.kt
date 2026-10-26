package com.gmn95.mygym.data.repository

import androidx.room.withTransaction
import com.gmn95.mygym.data.local.MyGymDatabase
import com.gmn95.mygym.data.local.SessionExerciseEntity
import com.gmn95.mygym.data.local.SessionExerciseSetEntity
import com.gmn95.mygym.data.local.SessionRestTimerEntity
import com.gmn95.mygym.data.local.WorkoutSessionEntity
import com.gmn95.mygym.domain.session.SessionClock
import com.gmn95.mygym.domain.session.TimerMath
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class RoomSessionRepository(
    private val database: MyGymDatabase,
    private val clock: SessionClock,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) : SessionRepository {
    private val workoutGroupDao = database.workoutGroupDao()
    private val workoutItemDao = database.workoutItemDao()
    private val sessionDao = database.workoutSessionDao()
    private val sessionExerciseDao = database.sessionExerciseDao()
    private val sessionExerciseSetDao = database.sessionExerciseSetDao()
    private val restTimerDao = database.sessionRestTimerDao()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeActiveSession(): Flow<ActiveWorkoutSession?> =
        sessionDao.observeActive().flatMapLatest { session ->
            if (session == null) {
                flowOf(null)
            } else {
                combine(
                    sessionExerciseDao.observeBySession(session.id),
                    sessionExerciseSetDao.observeBySession(session.id),
                    restTimerDao.observeBySession(session.id),
                ) { exerciseEntities, setEntities, timerEntity ->
                    val exercises = exerciseEntities.map { exercise ->
                        exercise.toModel(
                            setEntities.filter { it.sessionExerciseId == exercise.id },
                        )
                    }
                    ActiveWorkoutSession(
                        id = session.id,
                        workoutGroupId = session.workoutGroupId,
                        workoutName = session.workoutNameSnapshot,
                        status = WorkoutSessionStatus.valueOf(session.status),
                        startedAtEpochMillis = session.startedAtEpochMillis,
                        activeAccumulatedMillis = session.activeAccumulatedMillis,
                        activeAnchorElapsedRealtime = session.activeAnchorElapsedRealtime,
                        activeAnchorBootCount = session.activeAnchorBootCount,
                        exercises = exercises,
                        restTimer = timerEntity?.toModel(exercises),
                    )
                }
            }
        }

    override fun observeActiveSessionSummary(): Flow<ActiveSessionSummary?> =
        sessionDao.observeActive().map { session ->
            session?.let {
                ActiveSessionSummary(
                    id = it.id,
                    workoutName = it.workoutNameSnapshot,
                    status = WorkoutSessionStatus.valueOf(it.status),
                )
            }
        }

    override fun observeCompletedSessions(): Flow<List<CompletedWorkoutSessionSummary>> =
        sessionDao.observeCompletedSummaries().map { rows ->
            rows.map { row ->
                CompletedWorkoutSessionSummary(
                    id = row.session.id,
                    workoutName = row.session.workoutNameSnapshot,
                    endedAtEpochMillis = row.session.endedAtEpochMillis
                        ?: row.session.startedAtEpochMillis,
                    activeDurationMillis = row.session.activeAccumulatedMillis,
                    exerciseCount = row.exerciseCount,
                    completedExerciseCount = row.completedExerciseCount,
                )
            }
        }

    override fun observeCompletedSession(id: String): Flow<CompletedWorkoutSession?> =
        combine(
            sessionDao.observeCompletedById(id),
            sessionExerciseDao.observeBySession(id),
        ) { session, exercises ->
            session?.let {
                CompletedWorkoutSession(
                    id = it.id,
                    workoutName = it.workoutNameSnapshot,
                    endedAtEpochMillis = it.endedAtEpochMillis ?: it.startedAtEpochMillis,
                    activeDurationMillis = it.activeAccumulatedMillis,
                    exercises = exercises.map { exercise ->
                        CompletedSessionExercise(
                            position = exercise.position,
                            name = exercise.exerciseNameSnapshot,
                            notes = exercise.exerciseNotesSnapshot,
                            sets = exercise.sets,
                            repetitionsMin = exercise.repetitionsMin,
                            repetitionsMax = exercise.repetitionsMax,
                            restSeconds = exercise.restSeconds,
                            isCompleted = exercise.isCompleted,
                        )
                    },
                )
            }
        }

    override suspend fun startSession(workoutGroupId: String): String =
        database.withTransaction {
            if (sessionDao.getActive() != null) {
                throw ActiveSessionAlreadyExistsException()
            }
            val workout = workoutGroupDao.getById(workoutGroupId)
                ?: error("Ficha inexistente")
            val items = workoutItemDao.getDetailsByWorkoutGroup(workoutGroupId)
            if (items.isEmpty()) throw WorkoutHasNoExercisesException()

            val sessionId = idFactory()
            sessionDao.insert(
                WorkoutSessionEntity(
                    id = sessionId,
                    workoutGroupId = workout.id,
                    workoutNameSnapshot = workout.name,
                    status = WorkoutSessionStatus.ACTIVE.name,
                    startedAtEpochMillis = clock.epochMillis(),
                    endedAtEpochMillis = null,
                    activeAccumulatedMillis = 0,
                    activeAnchorElapsedRealtime = clock.elapsedRealtimeMillis(),
                    activeAnchorBootCount = clock.bootCount(),
                ),
            )
            val sessionExercises = items.map { row ->
                SessionExerciseEntity(
                    id = idFactory(),
                    sessionId = sessionId,
                    workoutItemId = row.item.id,
                    exerciseNameSnapshot = row.exerciseName,
                    exerciseNotesSnapshot = row.exerciseNotes,
                    position = row.item.position,
                    sets = row.item.sets,
                    repetitionsMin = row.item.repetitionsMin,
                    repetitionsMax = row.item.repetitionsMax,
                    restSeconds = row.item.restSeconds,
                    isCompleted = false,
                )
            }
            sessionExerciseDao.insertAll(sessionExercises)
            sessionExerciseSetDao.insertAll(
                sessionExercises.flatMap { exercise ->
                    (1..exercise.sets).map { position ->
                        SessionExerciseSetEntity(
                            id = idFactory(),
                            sessionExerciseId = exercise.id,
                            position = position,
                            isCompleted = false,
                        )
                    }
                },
            )
            sessionId
        }

    override suspend fun completeActiveAndStartSession(workoutGroupId: String): String =
        database.withTransaction {
            if (workoutItemDao.getDetailsByWorkoutGroup(workoutGroupId).isEmpty()) {
                throw WorkoutHasNoExercisesException()
            }
            finishActiveSession(WorkoutSessionStatus.COMPLETED)
            startSession(workoutGroupId)
        }

    override suspend fun pauseSession() {
        database.withTransaction {
            val session = sessionDao.getActive() ?: return@withTransaction
            if (session.status != WorkoutSessionStatus.ACTIVE.name) return@withTransaction
            sessionDao.update(
                session.copy(
                    status = WorkoutSessionStatus.PAUSED.name,
                    activeAccumulatedMillis = activeDurationAt(session),
                    activeAnchorElapsedRealtime = null,
                    activeAnchorBootCount = null,
                ),
            )
        }
    }

    override suspend fun resumeSession() {
        database.withTransaction {
            val session = sessionDao.getActive() ?: return@withTransaction
            if (session.status != WorkoutSessionStatus.PAUSED.name) return@withTransaction
            sessionDao.update(
                session.copy(
                    status = WorkoutSessionStatus.ACTIVE.name,
                    activeAnchorElapsedRealtime = clock.elapsedRealtimeMillis(),
                    activeAnchorBootCount = clock.bootCount(),
                ),
            )
        }
    }

    override suspend fun setExerciseSetCompleted(
        sessionExerciseId: String,
        sessionExerciseSetId: String,
        isCompleted: Boolean,
    ) {
        database.withTransaction {
            val active = sessionDao.getActive() ?: return@withTransaction
            val exercise = sessionExerciseDao.getById(sessionExerciseId) ?: return@withTransaction
            check(exercise.sessionId == active.id)
            val set = sessionExerciseSetDao.getById(sessionExerciseSetId) ?: return@withTransaction
            check(set.sessionExerciseId == exercise.id)
            sessionExerciseSetDao.update(set.copy(isCompleted = isCompleted))
            val allSeriesCompleted = sessionExerciseSetDao.getByExercise(exercise.id)
                .all(SessionExerciseSetEntity::isCompleted)
            sessionExerciseDao.setCompleted(exercise.id, allSeriesCompleted)
            if (isCompleted && exercise.restSeconds > 0) {
                startRestForExercise(active, exercise)
            }
        }
    }

    override suspend fun setExerciseCompleted(sessionExerciseId: String, isCompleted: Boolean) {
        database.withTransaction {
            val active = sessionDao.getActive() ?: return@withTransaction
            val exercise = sessionExerciseDao.getById(sessionExerciseId) ?: return@withTransaction
            check(exercise.sessionId == active.id)
            sessionExerciseSetDao.setCompletedForExercise(exercise.id, isCompleted)
            sessionExerciseDao.setCompleted(exercise.id, isCompleted)
            if (isCompleted && exercise.restSeconds > 0) {
                startRestForExercise(active, exercise)
            }
        }
    }

    override suspend fun pauseRest() {
        database.withTransaction {
            val active = sessionDao.getActive() ?: return@withTransaction
            val timer = restTimerDao.getBySession(active.id) ?: return@withTransaction
            if (timer.status != RestTimerStatus.RUNNING.name) return@withTransaction
            restTimerDao.upsert(
                timer.copy(
                    status = RestTimerStatus.PAUSED.name,
                    remainingAtAnchorMillis = remainingAt(timer),
                    anchorElapsedRealtime = null,
                    anchorBootCount = null,
                ),
            )
        }
    }

    override suspend fun resumeRest() {
        database.withTransaction {
            val active = sessionDao.getActive() ?: return@withTransaction
            val timer = restTimerDao.getBySession(active.id) ?: return@withTransaction
            if (timer.status != RestTimerStatus.PAUSED.name || timer.remainingAtAnchorMillis <= 0) {
                return@withTransaction
            }
            restTimerDao.upsert(
                timer.copy(
                    status = RestTimerStatus.RUNNING.name,
                    anchorElapsedRealtime = clock.elapsedRealtimeMillis(),
                    anchorBootCount = clock.bootCount(),
                ),
            )
        }
    }

    override suspend fun resetRest() {
        database.withTransaction {
            val active = sessionDao.getActive() ?: return@withTransaction
            val timer = restTimerDao.getBySession(active.id) ?: return@withTransaction
            restTimerDao.upsert(
                timer.copy(
                    status = RestTimerStatus.RUNNING.name,
                    remainingAtAnchorMillis = timer.durationMillis,
                    anchorElapsedRealtime = clock.elapsedRealtimeMillis(),
                    anchorBootCount = clock.bootCount(),
                ),
            )
        }
    }

    override suspend fun endRest() {
        database.withTransaction {
            val active = sessionDao.getActive() ?: return@withTransaction
            restTimerDao.getBySession(active.id)?.let { restTimerDao.delete(it) }
        }
    }

    override suspend fun completeActiveSession() {
        finishActiveSession(WorkoutSessionStatus.COMPLETED)
    }

    override suspend fun cancelActiveSession() {
        finishActiveSession(WorkoutSessionStatus.CANCELLED)
    }

    override suspend fun checkpointActiveTimers() {
        database.withTransaction {
            val session = sessionDao.getActive() ?: return@withTransaction
            if (session.status == WorkoutSessionStatus.ACTIVE.name &&
                session.activeAnchorBootCount == clock.bootCount()
            ) {
                sessionDao.update(
                    session.copy(
                        activeAccumulatedMillis = activeDurationAt(session),
                        activeAnchorElapsedRealtime = clock.elapsedRealtimeMillis(),
                    ),
                )
            }
            val timer = restTimerDao.getBySession(session.id)
            if (timer?.status == RestTimerStatus.RUNNING.name &&
                timer.anchorBootCount == clock.bootCount()
            ) {
                restTimerDao.upsert(
                    timer.copy(
                        remainingAtAnchorMillis = remainingAt(timer),
                        anchorElapsedRealtime = clock.elapsedRealtimeMillis(),
                    ),
                )
            }
        }
    }

    override suspend fun recoverAfterRebootIfNeeded() {
        database.withTransaction {
            val session = sessionDao.getActive() ?: return@withTransaction
            if (session.status == WorkoutSessionStatus.ACTIVE.name &&
                session.activeAnchorBootCount != clock.bootCount()
            ) {
                sessionDao.update(
                    session.copy(
                        status = WorkoutSessionStatus.PAUSED.name,
                        activeAnchorElapsedRealtime = null,
                        activeAnchorBootCount = null,
                    ),
                )
            }
            val timer = restTimerDao.getBySession(session.id)
            if (timer?.status == RestTimerStatus.RUNNING.name &&
                timer.anchorBootCount != clock.bootCount()
            ) {
                restTimerDao.upsert(
                    timer.copy(
                        status = RestTimerStatus.PAUSED.name,
                        anchorElapsedRealtime = null,
                        anchorBootCount = null,
                    ),
                )
            }
        }
    }

    private suspend fun finishActiveSession(status: WorkoutSessionStatus) {
        database.withTransaction {
            val session = sessionDao.getActive() ?: return@withTransaction
            val duration = if (session.status == WorkoutSessionStatus.ACTIVE.name) {
                activeDurationAt(session)
            } else {
                session.activeAccumulatedMillis
            }
            sessionDao.update(
                session.copy(
                    status = status.name,
                    endedAtEpochMillis = clock.epochMillis(),
                    activeAccumulatedMillis = duration,
                    activeAnchorElapsedRealtime = null,
                    activeAnchorBootCount = null,
                ),
            )
            restTimerDao.getBySession(session.id)?.let { restTimerDao.delete(it) }
        }
    }

    private fun activeDurationAt(session: WorkoutSessionEntity): Long =
        TimerMath.activeDurationMillis(
            accumulatedMillis = session.activeAccumulatedMillis,
            anchorElapsedRealtime = session.activeAnchorElapsedRealtime,
            anchorBootCount = session.activeAnchorBootCount,
            nowElapsedRealtime = clock.elapsedRealtimeMillis(),
            currentBootCount = clock.bootCount(),
        )

    private fun remainingAt(timer: SessionRestTimerEntity): Long =
        TimerMath.remainingMillis(
            remainingAtAnchorMillis = timer.remainingAtAnchorMillis,
            anchorElapsedRealtime = timer.anchorElapsedRealtime,
            anchorBootCount = timer.anchorBootCount,
            nowElapsedRealtime = clock.elapsedRealtimeMillis(),
            currentBootCount = clock.bootCount(),
        )

    private fun SessionExerciseEntity.toModel(
        setEntities: List<SessionExerciseSetEntity>,
    ) = ActiveSessionExercise(
        id = id,
        name = exerciseNameSnapshot,
        notes = exerciseNotesSnapshot,
        position = position,
        sets = sets,
        repetitionsMin = repetitionsMin,
        repetitionsMax = repetitionsMax,
        restSeconds = restSeconds,
        isCompleted = isCompleted,
        series = setEntities.map {
            ActiveExerciseSet(
                id = it.id,
                position = it.position,
                isCompleted = it.isCompleted,
            )
        },
    )

    private suspend fun startRestForExercise(
        active: WorkoutSessionEntity,
        exercise: SessionExerciseEntity,
    ) {
        val duration = exercise.restSeconds * 1_000L
        restTimerDao.upsert(
            SessionRestTimerEntity(
                sessionId = active.id,
                sessionExerciseId = exercise.id,
                status = RestTimerStatus.RUNNING.name,
                durationMillis = duration,
                remainingAtAnchorMillis = duration,
                anchorElapsedRealtime = clock.elapsedRealtimeMillis(),
                anchorBootCount = clock.bootCount(),
            ),
        )
    }

    private fun SessionRestTimerEntity.toModel(
        exercises: List<ActiveSessionExercise>,
    ): ActiveRestTimer? {
        val exerciseName = exercises.firstOrNull { it.id == sessionExerciseId }?.name ?: return null
        return ActiveRestTimer(
            sessionExerciseId = sessionExerciseId,
            exerciseName = exerciseName,
            status = RestTimerStatus.valueOf(status),
            durationMillis = durationMillis,
            remainingAtAnchorMillis = remainingAtAnchorMillis,
            anchorElapsedRealtime = anchorElapsedRealtime,
            anchorBootCount = anchorBootCount,
        )
    }
}
