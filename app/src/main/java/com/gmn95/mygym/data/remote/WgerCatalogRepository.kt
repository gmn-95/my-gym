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
    val imageUrl: String?,
    val videoUrl: String?,
    val mediaAttribution: String?,
)

data class CatalogPage(
    val exercises: List<CatalogExercise>,
    val nextPageUrl: String?,
)

interface ExerciseCatalogRepository {
    suspend fun search(name: String, nextPageUrl: String? = null): CatalogPage
}

class ExerciseCatalogException(
    val reason: Reason,
    cause: Throwable? = null,
) : IOException(cause) {
    enum class Reason { RATE_LIMITED, UNAVAILABLE, INVALID_RESPONSE }
}

/**
 * Consulta somente o catálogo público da wger. Dados remotos não são persistidos: ao escolher
 * um resultado, apenas o nome entra no treino local.
 */
class WgerCatalogRepository : ExerciseCatalogRepository {
    override suspend fun search(name: String, nextPageUrl: String?): CatalogPage = withContext(Dispatchers.IO) {
        val endpoint = nextPageUrl ?: "$EXERCISE_INFO_URL?language__code=pt&name__search=${name.urlEncoded()}&limit=20"
        if (!endpoint.startsWith(EXERCISE_INFO_URL)) {
            throw ExerciseCatalogException(ExerciseCatalogException.Reason.INVALID_RESPONSE)
        }

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/json")
        }
        try {
            when (val status = connection.responseCode) {
                in 200..299 -> connection.inputStream.bufferedReader().use { reader ->
                    WgerJsonParser.parsePage(reader.readText())
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

    private companion object {
        const val EXERCISE_INFO_URL = "https://wger.de/api/v2/exerciseinfo/"
    }
}

internal object WgerJsonParser {
    private const val PORTUGUESE_LANGUAGE_ID = 7

    fun parsePage(json: String): CatalogPage {
        val root = JSONObject(json)
        val exercises = root.getJSONArray("results").mapNotNullObjects(::parsePortugueseExercise)
        return CatalogPage(
            exercises = exercises,
            nextPageUrl = root.optString("next").takeIf(String::isNotBlank),
        )
    }

    private fun parsePortugueseExercise(item: JSONObject): CatalogExercise? {
        val translation = item.optJSONArray("translations")
            ?.firstObject { it.optInt("language") == PORTUGUESE_LANGUAGE_ID }
            ?: return null
        val name = translation.optString("name").trim().takeIf(String::isNotBlank) ?: return null
        return CatalogExercise(
            id = item.getInt("id").toString(),
            name = name,
            imageUrl = item.optJSONArray("images")?.mainMediaUrl("image"),
            videoUrl = item.optJSONArray("videos")?.mainMediaUrl("video"),
            mediaAttribution = item.mediaAttribution(),
        )
    }

    private fun JSONArray.mainMediaUrl(key: String): String? {
        val media = firstObject { it.optBoolean("is_main") } ?: firstObject { true } ?: return null
        return media.optString(key).trim().takeIf(String::isNotBlank)
    }

    private fun JSONObject.mediaAttribution(): String? {
        val license = optJSONObject("license")?.optString("full_name")
            ?: optString("license")
        val author = optString("license_author")
        return listOf(license, author)
            .map(String::trim)
            .filter(String::isNotBlank)
            .joinToString(" — ")
            .takeIf(String::isNotBlank)
    }

    private fun JSONArray.firstObject(predicate: (JSONObject) -> Boolean): JSONObject? =
        (0 until length()).asSequence()
            .map { getJSONObject(it) }
            .firstOrNull(predicate)

    private inline fun <T : Any> JSONArray.mapNotNullObjects(transform: (JSONObject) -> T?): List<T> =
        (0 until length()).mapNotNull { index -> transform(getJSONObject(index)) }
}

private fun String.urlEncoded(): String = URLEncoder.encode(this, Charsets.UTF_8.name())
