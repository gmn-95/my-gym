package com.gmn95.mygym.feature.catalog

import com.gmn95.mygym.data.remote.CatalogExercise
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseCatalogPaginationTest {
    @Test
    fun appendsOnlyNewExercisesWhenPagesOverlap() {
        val current = listOf(exercise("1", "Supino"), exercise("2", "Agachamento"))
        val incoming = listOf(exercise("2", "Agachamento"), exercise("3", "Remada"))

        val merged = mergeCatalogExercises(current, incoming, append = true)

        assertEquals(listOf("1", "2", "3"), merged.map(CatalogExercise::id))
    }

    @Test
    fun replacesPreviousResultsForANewSearch() {
        val previous = listOf(exercise("1", "Supino"))
        val incoming = listOf(exercise("2", "Remada"), exercise("2", "Remada repetida"))

        val merged = mergeCatalogExercises(previous, incoming, append = false)

        assertEquals(listOf("2"), merged.map(CatalogExercise::id))
        assertEquals("Remada", merged.single().name)
    }

    private fun exercise(id: String, name: String) = CatalogExercise(
        id = id,
        name = name,
        imageUrl = null,
        videoUrl = null,
        mediaAttribution = null,
    )
}
