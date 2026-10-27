package com.gmn95.mygym.data.backup

import com.gmn95.mygym.data.local.AppSettingsEntity
import com.gmn95.mygym.data.local.CustomExerciseEntity
import com.gmn95.mygym.data.local.SessionExerciseEntity
import com.gmn95.mygym.data.local.SessionExerciseSetEntity
import com.gmn95.mygym.data.local.SessionRestTimerEntity
import com.gmn95.mygym.data.local.WorkoutGroupEntity
import com.gmn95.mygym.data.local.WorkoutFolderEntity
import com.gmn95.mygym.data.local.WorkoutItemEntity
import com.gmn95.mygym.data.local.WorkoutSessionEntity
import org.json.JSONArray
import org.json.JSONObject

data class WorkoutBackup(
    val settings: AppSettingsEntity,
    val folders: List<WorkoutFolderEntity>,
    val groups: List<WorkoutGroupEntity>,
    val exercises: List<CustomExerciseEntity>,
    val items: List<WorkoutItemEntity>,
    val sessions: List<WorkoutSessionEntity>,
    val sessionExercises: List<SessionExerciseEntity>,
    val sessionSets: List<SessionExerciseSetEntity>,
    val restTimers: List<SessionRestTimerEntity>,
)

object WorkoutBackupJson {
    const val CURRENT_SCHEMA_VERSION = 2

    fun encode(backup: WorkoutBackup): String = JSONObject()
        .put("schemaVersion", CURRENT_SCHEMA_VERSION)
        .put("settings", backup.settings.toJson())
        .put("workoutFolders", JSONArray().apply { backup.folders.forEach { put(it.toJson()) } })
        .put("workoutGroups", JSONArray().apply { backup.groups.forEach { put(it.toJson()) } })
        .put("customExercises", JSONArray().apply { backup.exercises.forEach { put(it.toJson()) } })
        .put("workoutItems", JSONArray().apply { backup.items.forEach { put(it.toJson()) } })
        .put("workoutSessions", JSONArray().apply { backup.sessions.forEach { put(it.toJson()) } })
        .put("sessionExercises", JSONArray().apply { backup.sessionExercises.forEach { put(it.toJson()) } })
        .put("sessionExerciseSets", JSONArray().apply { backup.sessionSets.forEach { put(it.toJson()) } })
        .put("restTimers", JSONArray().apply { backup.restTimers.forEach { put(it.toJson()) } })
        .toString(2)

    fun decode(json: String): WorkoutBackup {
        try {
            val root = JSONObject(json)
            val version = root.getInt("schemaVersion")
            require(version in 1..CURRENT_SCHEMA_VERSION) {
                "Versão de backup incompatível"
            }
            val groups = root.getJSONArray("workoutGroups").mapObjects {
                it.toWorkoutGroup(version)
            }
            val backup = WorkoutBackup(
                settings = root.getJSONObject("settings").toSettings(),
                folders = if (version == 1) {
                    groups.map { group ->
                        WorkoutFolderEntity(
                            id = requireNotNull(group.workoutFolderId),
                            name = group.name,
                            position = group.position,
                            createdAtEpochMillis = group.createdAtEpochMillis,
                            updatedAtEpochMillis = group.updatedAtEpochMillis,
                        )
                    }
                } else {
                    root.getJSONArray("workoutFolders").mapObjects { it.toWorkoutFolder() }
                },
                groups = groups,
                exercises = root.getJSONArray("customExercises").mapObjects { it.toCustomExercise() },
                items = root.getJSONArray("workoutItems").mapObjects { it.toWorkoutItem() },
                sessions = root.getJSONArray("workoutSessions").mapObjects { it.toWorkoutSession() },
                sessionExercises = root.getJSONArray("sessionExercises").mapObjects { it.toSessionExercise() },
                sessionSets = root.getJSONArray("sessionExerciseSets").mapObjects { it.toSessionSet() },
                restTimers = root.getJSONArray("restTimers").mapObjects { it.toRestTimer() },
            )
            backup.validate()
            return backup
        } catch (error: Exception) {
            if (error is InvalidWorkoutBackupException) throw error
            throw InvalidWorkoutBackupException(error.message ?: "Arquivo JSON inválido")
        }
    }

    private fun WorkoutBackup.validate() {
        requireUnique(folders.map { it.id }, "fichas")
        requireUnique(groups.map { it.id }, "fichas")
        requireUnique(exercises.map { it.id }, "exercícios")
        requireUnique(items.map { it.id }, "itens")
        requireUnique(sessions.map { it.id }, "sessões")
        requireUnique(sessionExercises.map { it.id }, "exercícios do histórico")
        requireUnique(sessionSets.map { it.id }, "séries")
        requireUnique(restTimers.map { it.sessionId }, "cronômetros")
        require(folders.all { it.id.isNotBlank() && it.name.isNotBlank() && it.position >= 0 })
        require(groups.all {
            it.id.isNotBlank() && it.name.isNotBlank() && it.position >= 0 &&
                it.workoutFolderId in folders.map(WorkoutFolderEntity::id)
        })
        require(exercises.all { it.id.isNotBlank() && it.name.isNotBlank() })
        require(items.all {
            it.id.isNotBlank() && it.workoutGroupId in groups.map(WorkoutGroupEntity::id) &&
                it.customExerciseId in exercises.map(CustomExerciseEntity::id) &&
                it.position >= 0 && it.sets > 0 && it.repetitionsMin > 0 &&
                (it.repetitionsMax == null || it.repetitionsMax >= it.repetitionsMin) &&
                it.restSeconds >= 0
        })
        require(sessions.all {
            it.id.isNotBlank() && it.workoutNameSnapshot.isNotBlank() &&
                it.status in setOf("ACTIVE", "PAUSED", "COMPLETED", "CANCELLED") &&
                it.activeAccumulatedMillis >= 0
        })
        require(sessionExercises.all {
            it.id.isNotBlank() && it.sessionId in sessions.map(WorkoutSessionEntity::id) &&
                it.position >= 0 && it.sets > 0 && it.repetitionsMin > 0 &&
                (it.repetitionsMax == null || it.repetitionsMax >= it.repetitionsMin) &&
                it.restSeconds >= 0
        })
        require(sessionSets.all {
            it.id.isNotBlank() && it.sessionExerciseId in sessionExercises.map(SessionExerciseEntity::id) &&
                it.position > 0
        })
        require(sessionExercises.all { exercise ->
            sessionSets.count { it.sessionExerciseId == exercise.id } == exercise.sets
        })
        require(restTimers.all {
            it.sessionId in sessions.map(WorkoutSessionEntity::id) &&
                it.sessionExerciseId in sessionExercises.map(SessionExerciseEntity::id) &&
                it.durationMillis >= 0 && it.remainingAtAnchorMillis >= 0 &&
                it.status in setOf("RUNNING", "PAUSED") &&
                sessionExercises.first { exercise -> exercise.id == it.sessionExerciseId }.sessionId == it.sessionId
        })
        require(sessions.count { it.status == "ACTIVE" } <= 1)
        require(settings.themeMode in setOf("SYSTEM", "LIGHT", "DARK"))
        require(settings.languageMode in setOf("SYSTEM", "EN", "PT_BR"))
    }

    private fun requireUnique(values: List<String>, label: String) {
        require(values.none(String::isBlank) && values.distinct().size == values.size) {
            "IDs ausentes ou duplicados em $label"
        }
    }

    private inline fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
        List(length()) { index -> getJSONObject(index).let(transform) }

    private fun JSONObject.requiredString(key: String) = getString(key).also { require(it.isNotBlank()) }
    private fun JSONObject.nullableString(key: String): String? =
        if (isNull(key)) null else getString(key)

    private fun AppSettingsEntity.toJson() = JSONObject()
        .put("themeMode", themeMode).put("languageMode", languageMode)

    private fun JSONObject.toSettings() = AppSettingsEntity(
        themeMode = requiredString("themeMode"),
        languageMode = requiredString("languageMode"),
    )

    private fun WorkoutFolderEntity.toJson() = JSONObject()
        .put("id", id).put("name", name).put("position", position)
        .put("createdAtEpochMillis", createdAtEpochMillis)
        .put("updatedAtEpochMillis", updatedAtEpochMillis)

    private fun JSONObject.toWorkoutFolder() = WorkoutFolderEntity(
        id = requiredString("id"), name = requiredString("name"), position = getInt("position"),
        createdAtEpochMillis = getLong("createdAtEpochMillis"),
        updatedAtEpochMillis = getLong("updatedAtEpochMillis"),
    )

    private fun WorkoutGroupEntity.toJson() = JSONObject()
        .put("id", id).put("name", name).put("position", position)
        .put("workoutFolderId", workoutFolderId ?: JSONObject.NULL)
        .put("createdAtEpochMillis", createdAtEpochMillis)
        .put("updatedAtEpochMillis", updatedAtEpochMillis)

    private fun JSONObject.toWorkoutGroup(version: Int) = WorkoutGroupEntity(
        id = requiredString("id"), name = requiredString("name"), position = getInt("position"),
        workoutFolderId = if (version == 1) requiredString("id") else requiredString("workoutFolderId"),
        createdAtEpochMillis = getLong("createdAtEpochMillis"),
        updatedAtEpochMillis = getLong("updatedAtEpochMillis"),
    )

    private fun CustomExerciseEntity.toJson() = JSONObject()
        .put("id", id).put("name", name).put("notes", notes ?: JSONObject.NULL)
        .put("createdAtEpochMillis", createdAtEpochMillis)
        .put("updatedAtEpochMillis", updatedAtEpochMillis)

    private fun JSONObject.toCustomExercise() = CustomExerciseEntity(
        id = requiredString("id"), name = requiredString("name"), notes = nullableString("notes"),
        createdAtEpochMillis = getLong("createdAtEpochMillis"),
        updatedAtEpochMillis = getLong("updatedAtEpochMillis"),
    )

    private fun WorkoutItemEntity.toJson() = JSONObject()
        .put("id", id).put("workoutGroupId", workoutGroupId)
        .put("customExerciseId", customExerciseId).put("position", position).put("sets", sets)
        .put("repetitionsMin", repetitionsMin)
        .put("repetitionsMax", repetitionsMax ?: JSONObject.NULL).put("restSeconds", restSeconds)

    private fun JSONObject.toWorkoutItem() = WorkoutItemEntity(
        id = requiredString("id"), workoutGroupId = requiredString("workoutGroupId"),
        customExerciseId = requiredString("customExerciseId"), position = getInt("position"),
        sets = getInt("sets"), repetitionsMin = getInt("repetitionsMin"),
        repetitionsMax = if (isNull("repetitionsMax")) null else getInt("repetitionsMax"),
        restSeconds = getInt("restSeconds"),
    )

    private fun WorkoutSessionEntity.toJson() = JSONObject()
        .put("id", id).put("workoutGroupId", workoutGroupId)
        .put("workoutNameSnapshot", workoutNameSnapshot).put("status", status)
        .put("startedAtEpochMillis", startedAtEpochMillis)
        .put("endedAtEpochMillis", endedAtEpochMillis ?: JSONObject.NULL)
        .put("activeAccumulatedMillis", activeAccumulatedMillis)
        .put("activeAnchorElapsedRealtime", activeAnchorElapsedRealtime ?: JSONObject.NULL)
        .put("activeAnchorBootCount", activeAnchorBootCount ?: JSONObject.NULL)

    private fun JSONObject.toWorkoutSession() = WorkoutSessionEntity(
        id = requiredString("id"), workoutGroupId = requiredString("workoutGroupId"),
        workoutNameSnapshot = requiredString("workoutNameSnapshot"), status = requiredString("status"),
        startedAtEpochMillis = getLong("startedAtEpochMillis"),
        endedAtEpochMillis = if (isNull("endedAtEpochMillis")) null else getLong("endedAtEpochMillis"),
        activeAccumulatedMillis = getLong("activeAccumulatedMillis"),
        activeAnchorElapsedRealtime = if (isNull("activeAnchorElapsedRealtime")) null else getLong("activeAnchorElapsedRealtime"),
        activeAnchorBootCount = if (isNull("activeAnchorBootCount")) null else getInt("activeAnchorBootCount"),
    )

    private fun SessionExerciseEntity.toJson() = JSONObject()
        .put("id", id).put("sessionId", sessionId).put("workoutItemId", workoutItemId)
        .put("exerciseNameSnapshot", exerciseNameSnapshot)
        .put("exerciseNotesSnapshot", exerciseNotesSnapshot ?: JSONObject.NULL)
        .put("position", position).put("sets", sets).put("repetitionsMin", repetitionsMin)
        .put("repetitionsMax", repetitionsMax ?: JSONObject.NULL).put("restSeconds", restSeconds)
        .put("isCompleted", isCompleted)

    private fun JSONObject.toSessionExercise() = SessionExerciseEntity(
        id = requiredString("id"), sessionId = requiredString("sessionId"),
        workoutItemId = requiredString("workoutItemId"),
        exerciseNameSnapshot = requiredString("exerciseNameSnapshot"),
        exerciseNotesSnapshot = nullableString("exerciseNotesSnapshot"), position = getInt("position"),
        sets = getInt("sets"), repetitionsMin = getInt("repetitionsMin"),
        repetitionsMax = if (isNull("repetitionsMax")) null else getInt("repetitionsMax"),
        restSeconds = getInt("restSeconds"), isCompleted = getBoolean("isCompleted"),
    )

    private fun SessionExerciseSetEntity.toJson() = JSONObject()
        .put("id", id).put("sessionExerciseId", sessionExerciseId)
        .put("position", position).put("isCompleted", isCompleted)

    private fun JSONObject.toSessionSet() = SessionExerciseSetEntity(
        id = requiredString("id"), sessionExerciseId = requiredString("sessionExerciseId"),
        position = getInt("position"), isCompleted = getBoolean("isCompleted"),
    )

    private fun SessionRestTimerEntity.toJson() = JSONObject()
        .put("sessionId", sessionId).put("sessionExerciseId", sessionExerciseId)
        .put("status", status).put("durationMillis", durationMillis)
        .put("remainingAtAnchorMillis", remainingAtAnchorMillis)
        .put("anchorElapsedRealtime", anchorElapsedRealtime ?: JSONObject.NULL)
        .put("anchorBootCount", anchorBootCount ?: JSONObject.NULL)

    private fun JSONObject.toRestTimer() = SessionRestTimerEntity(
        sessionId = requiredString("sessionId"), sessionExerciseId = requiredString("sessionExerciseId"),
        status = requiredString("status"), durationMillis = getLong("durationMillis"),
        remainingAtAnchorMillis = getLong("remainingAtAnchorMillis"),
        anchorElapsedRealtime = if (isNull("anchorElapsedRealtime")) null else getLong("anchorElapsedRealtime"),
        anchorBootCount = if (isNull("anchorBootCount")) null else getInt("anchorBootCount"),
    )
}

class InvalidWorkoutBackupException(message: String) : IllegalArgumentException(message)
