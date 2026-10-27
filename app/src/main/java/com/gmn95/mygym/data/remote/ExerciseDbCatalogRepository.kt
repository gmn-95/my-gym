package com.gmn95.mygym.data.remote

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class CatalogExercise(
    val id: String,
    val name: String,
    val gifUrl: String?,
    val targetMuscles: List<String>,
    val bodyParts: List<String>,
    val equipment: List<String>,
    val instructions: List<String>,
)

data class CatalogPage(
    val exercises: List<CatalogExercise>,
    val nextCursor: String?,
)

interface ExerciseCatalogRepository {
    suspend fun search(name: String, after: String? = null): CatalogPage
}

class ExerciseCatalogException(
    val reason: Reason,
    cause: Throwable? = null,
) : IOException(cause) {
    enum class Reason { RATE_LIMITED, UNAVAILABLE, INVALID_RESPONSE }
}

class ExerciseDbCatalogRepository : ExerciseCatalogRepository {
    override suspend fun search(name: String, after: String?): CatalogPage = withContext(Dispatchers.IO) {
        val query = buildList {
            add("name=${name.urlEncoded()}")
            add("limit=20")
            after?.let { add("after=${it.urlEncoded()}") }
        }.joinToString("&")
        val connection = (URL("https://oss.exercisedb.dev/api/v1/exercises?$query").openConnection()
            as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/json")
        }
        try {
            when (val status = connection.responseCode) {
                in 200..299 -> connection.inputStream.bufferedReader().use { reader ->
                    ExerciseDbJsonParser.parsePage(reader.readText())
                }
                429 -> throw ExerciseCatalogException(ExerciseCatalogException.Reason.RATE_LIMITED)
                else -> throw ExerciseCatalogException(
                    ExerciseCatalogException.Reason.UNAVAILABLE,
                    IOException("Resposta HTTP $status"),
                )
            }
        } catch (error: ExerciseCatalogException) {
            throw error
        } catch (error: IOException) {
            throw ExerciseCatalogException(ExerciseCatalogException.Reason.UNAVAILABLE, error)
        } catch (error: Exception) {
            throw ExerciseCatalogException(ExerciseCatalogException.Reason.INVALID_RESPONSE, error)
        } finally {
            connection.disconnect()
        }
    }
}

internal object ExerciseDbJsonParser {
    fun parsePage(json: String): CatalogPage {
        val root = JSONObject(json)
        if (!root.optBoolean("success")) {
            throw ExerciseCatalogException(ExerciseCatalogException.Reason.INVALID_RESPONSE)
        }
        val exercises = root.getJSONArray("data").mapObjects(::parseExercise)
        val meta = root.optJSONObject("meta")
        return CatalogPage(
            exercises = exercises,
            nextCursor = meta?.optString("nextCursor")?.takeIf(String::isNotBlank),
        )
    }

    private fun parseExercise(item: JSONObject): CatalogExercise = CatalogExercise(
        id = item.getString("exerciseId"),
        name = item.getString("name"),
        gifUrl = item.gifUrl(),
        targetMuscles = item.getJSONArray("targetMuscles").stringList(),
        bodyParts = item.getJSONArray("bodyParts").stringList(),
        equipment = item.getJSONArray("equipments").stringList(),
        instructions = item.getJSONArray("instructions").stringList(),
    )

    private fun JSONObject.gifUrl(): String? {
        val value = opt("gifUrl")
        return when {
            value is String -> value.takeIf(String::isNotBlank)
            value is JSONObject -> value.optString("180p").takeIf(String::isNotBlank)
            else -> null
        }
    }

    private fun JSONArray.stringList(): List<String> = List(length()) { getString(it) }

    private inline fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
        List(length()) { index -> transform(getJSONObject(index)) }
}

private fun String.urlEncoded(): String = URLEncoder.encode(this, Charsets.UTF_8.name())
