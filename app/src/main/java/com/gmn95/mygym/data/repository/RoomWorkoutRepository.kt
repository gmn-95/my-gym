package com.gmn95.mygym.data.repository

import androidx.room.withTransaction
import com.gmn95.mygym.data.local.CustomExerciseEntity
import com.gmn95.mygym.data.local.MyGymDatabase
import com.gmn95.mygym.data.local.WorkoutFolderEntity
import com.gmn95.mygym.data.local.WorkoutGroupEntity
import com.gmn95.mygym.data.local.WorkoutItemDetailsRow
import com.gmn95.mygym.data.local.WorkoutItemEntity
import com.gmn95.mygym.domain.WorkoutInputValidator
import com.gmn95.mygym.domain.WorkoutItemConfiguration
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RoomWorkoutRepository(
    private val database: MyGymDatabase,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : WorkoutRepository {
    private val workoutFolderDao = database.workoutFolderDao()
    private val workoutGroupDao = database.workoutGroupDao()
    private val workoutItemDao = database.workoutItemDao()
    private val customExerciseDao = database.customExerciseDao()

    override fun observeFolders(): Flow<List<WorkoutFolderSummary>> =
        workoutFolderDao.observeSummaries().map { rows ->
            rows.map { row ->
                WorkoutFolderSummary(
                    id = row.folder.id,
                    name = row.folder.name,
                    workoutCount = row.workoutCount,
                )
            }
        }

    /** Visão plana mantida para testes de persistência já existentes. */
    fun observeWorkouts(): Flow<List<WorkoutSummary>> =
        workoutGroupDao.observeAllSummaries().map { rows -> rows.map { it.toModel() } }

    override fun observeFolder(id: String): Flow<WorkoutFolder?> =
        combine(
            workoutFolderDao.observeById(id),
            workoutGroupDao.observeSummaries(id),
        ) { folder, rows ->
            folder?.let {
                WorkoutFolder(
                    id = it.id,
                    name = it.name,
                    workouts = rows.map { row -> row.toModel() },
                )
            }
        }

    override fun observeWorkout(id: String): Flow<Workout?> =
        combine(
            workoutGroupDao.observeById(id),
            workoutItemDao.observeDetails(id),
        ) { group, itemRows ->
            group?.let {
                Workout(
                    id = it.id,
                    name = it.name,
                    exercises = itemRows.map { it.toModel() },
                )
            }
        }

    override fun observeCustomExercises(): Flow<List<CustomExercise>> =
        customExerciseDao.observeAll().map { exercises ->
            exercises.map {
                CustomExercise(
                    id = it.id,
                    name = it.name,
                    notes = it.notes,
                )
            }
        }

    override suspend fun createFolder(name: String): String {
        require(WorkoutInputValidator.isValidName(name))
        val id = idFactory()
        val now = currentTimeMillis()
        database.withTransaction {
            workoutFolderDao.insert(
                WorkoutFolderEntity(
                    id = id,
                    name = name.trim(),
                    position = workoutFolderDao.nextPosition(),
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                ),
            )
        }
        return id
    }

    override suspend fun renameFolder(id: String, name: String) {
        require(WorkoutInputValidator.isValidName(name))
        workoutFolderDao.rename(id, name.trim(), currentTimeMillis())
    }

    override suspend fun deleteFolder(id: String) {
        database.withTransaction {
            val folder = workoutFolderDao.getById(id) ?: return@withTransaction
            workoutGroupDao.getAll()
                .filter { it.workoutFolderId == id }
                .forEach { group -> workoutGroupDao.delete(group) }
            workoutFolderDao.delete(folder)
            workoutFolderDao.getAll().forEachIndexed { index, current ->
                if (current.position != index) workoutFolderDao.updatePosition(current.id, index)
            }
        }
    }

    override suspend fun createWorkout(folderId: String, name: String): String {
        require(WorkoutInputValidator.isValidName(name))
        val id = idFactory()
        val now = currentTimeMillis()
        database.withTransaction {
            checkNotNull(workoutFolderDao.getById(folderId))
            workoutGroupDao.insert(
                WorkoutGroupEntity(
                    id = id,
                    workoutFolderId = folderId,
                    name = name.trim(),
                    position = workoutGroupDao.nextPosition(folderId),
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                ),
            )
        }
        return id
    }

    override suspend fun renameWorkout(id: String, name: String) {
        require(WorkoutInputValidator.isValidName(name))
        workoutGroupDao.rename(id, name.trim(), currentTimeMillis())
    }

    override suspend fun duplicateWorkout(id: String, duplicateName: String): String? =
        database.withTransaction {
            val source = workoutGroupDao.getById(id) ?: return@withTransaction null
            val sourceItems = workoutItemDao.getByWorkoutGroup(id)
            val now = currentTimeMillis()
            val newId = idFactory()
            workoutGroupDao.insert(
                source.copy(
                    id = newId,
                    name = duplicateName.trim(),
                    position = workoutGroupDao.nextPosition(
                        source.workoutFolderId ?: error("Treino sem ficha"),
                    ),
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                ),
            )
            workoutItemDao.insertAll(
                sourceItems.map { item ->
                    item.copy(
                        id = idFactory(),
                        workoutGroupId = newId,
                    )
                },
            )
            newId
        }

    override suspend fun deleteWorkout(id: String) {
        database.withTransaction {
            workoutGroupDao.getById(id)?.let { group ->
                workoutGroupDao.delete(group)
                workoutGroupDao.getAll()
                    .filter { it.workoutFolderId == group.workoutFolderId }
                    .forEachIndexed { index, sibling ->
                        if (sibling.position != index) {
                            workoutGroupDao.updatePosition(sibling.id, index)
                        }
                    }
                }
        }
    }

    /** Compatibilidade dos testes de persistência: cria uma ficha contendo um treino. */
    suspend fun createWorkout(name: String): String {
        val folderId = createFolder(name)
        return createWorkout(folderId, name)
    }

    override suspend fun createAndAddExercise(
        workoutId: String,
        name: String,
        notes: String?,
        configuration: WorkoutItemConfiguration,
    ) {
        require(WorkoutInputValidator.isValidName(name))
        require(WorkoutInputValidator.isValidConfiguration(configuration))
        database.withTransaction {
            checkNotNull(workoutGroupDao.getById(workoutId))
            val exerciseId = idFactory()
            val now = currentTimeMillis()
            customExerciseDao.insert(
                CustomExerciseEntity(
                    id = exerciseId,
                    name = name.trim(),
                    notes = notes?.trim()?.takeIf(String::isNotEmpty),
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                ),
            )
            insertWorkoutItem(workoutId, exerciseId, configuration)
            workoutGroupDao.touch(workoutId, now)
        }
    }

    override suspend fun addExistingExercise(
        workoutId: String,
        customExerciseId: String,
        configuration: WorkoutItemConfiguration,
    ) {
        require(WorkoutInputValidator.isValidConfiguration(configuration))
        database.withTransaction {
            checkNotNull(workoutGroupDao.getById(workoutId))
            checkNotNull(customExerciseDao.getById(customExerciseId))
            insertWorkoutItem(workoutId, customExerciseId, configuration)
            workoutGroupDao.touch(workoutId, currentTimeMillis())
        }
    }

    override suspend fun updateExerciseConfiguration(
        workoutId: String,
        itemId: String,
        configuration: WorkoutItemConfiguration,
    ) {
        require(WorkoutInputValidator.isValidConfiguration(configuration))
        database.withTransaction {
            val item = workoutItemDao.getById(itemId) ?: return@withTransaction
            check(item.workoutGroupId == workoutId)
            workoutItemDao.update(
                item.copy(
                    sets = configuration.sets,
                    repetitionsMin = configuration.repetitionsMin,
                    repetitionsMax = configuration.repetitionsMax,
                    restSeconds = configuration.restSeconds,
                ),
            )
            workoutGroupDao.touch(workoutId, currentTimeMillis())
        }
    }

    override suspend fun removeExercise(workoutId: String, itemId: String) {
        database.withTransaction {
            val item = workoutItemDao.getById(itemId) ?: return@withTransaction
            check(item.workoutGroupId == workoutId)
            workoutItemDao.delete(item)
            normalizeItemPositions(workoutId)
            workoutGroupDao.touch(workoutId, currentTimeMillis())
        }
    }

    override suspend fun moveExercise(workoutId: String, itemId: String, offset: Int) {
        require(offset == -1 || offset == 1)
        database.withTransaction {
            val items = workoutItemDao.getByWorkoutGroup(workoutId)
            val currentIndex = items.indexOfFirst { it.id == itemId }
            if (currentIndex == -1) return@withTransaction
            val targetIndex = currentIndex + offset
            if (targetIndex !in items.indices) return@withTransaction

            workoutItemDao.updatePosition(items[currentIndex].id, targetIndex)
            workoutItemDao.updatePosition(items[targetIndex].id, currentIndex)
            workoutGroupDao.touch(workoutId, currentTimeMillis())
        }
    }

    private suspend fun insertWorkoutItem(
        workoutId: String,
        customExerciseId: String,
        configuration: WorkoutItemConfiguration,
    ) {
        workoutItemDao.insert(
            WorkoutItemEntity(
                id = idFactory(),
                workoutGroupId = workoutId,
                customExerciseId = customExerciseId,
                position = workoutItemDao.nextPosition(workoutId),
                sets = configuration.sets,
                repetitionsMin = configuration.repetitionsMin,
                repetitionsMax = configuration.repetitionsMax,
                restSeconds = configuration.restSeconds,
            ),
        )
    }

    private suspend fun normalizeItemPositions(workoutId: String) {
        workoutItemDao.getByWorkoutGroup(workoutId).forEachIndexed { index, item ->
            if (item.position != index) {
                workoutItemDao.updatePosition(item.id, index)
            }
        }
    }

    private fun WorkoutItemDetailsRow.toModel() = WorkoutExercise(
        id = item.id,
        customExerciseId = item.customExerciseId,
        name = exerciseName,
        notes = exerciseNotes,
        position = item.position,
        configuration = WorkoutItemConfiguration(
            sets = item.sets,
            repetitionsMin = item.repetitionsMin,
            repetitionsMax = item.repetitionsMax,
            restSeconds = item.restSeconds,
        ),
    )

    private fun com.gmn95.mygym.data.local.WorkoutGroupSummaryRow.toModel() = WorkoutSummary(
        id = group.id,
        name = group.name,
        exerciseCount = exerciseCount,
        averageDurationMillis = averageDurationMillis,
    )
}
