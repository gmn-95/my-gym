package com.gmn95.mygym.data.remote

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseDbJsonParserTest {
    @Test
    fun parsesExerciseAndCursor() {
        val page = ExerciseDbJsonParser.parsePage(
            """
            {
              "success": true,
              "meta": { "nextCursor": "next-id" },
              "data": [
                {
                  "exerciseId": "bench-press",
                  "name": "barbell bench press",
                  "gifUrl": "https://static.exercisedb.dev/media/bench-press.gif",
                  "targetMuscles": ["pectorals"],
                  "bodyParts": ["chest"],
                  "equipments": ["barbell"],
                  "secondaryMuscles": ["triceps"],
                  "instructions": ["Lie on a bench"]
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals("next-id", page.nextCursor)
        assertEquals("barbell bench press", page.exercises.single().name)
        assertEquals(listOf("pectorals"), page.exercises.single().targetMuscles)
    }

    @Test
    fun acceptsMissingNextCursor() {
        val page = ExerciseDbJsonParser.parsePage(
            """
            { "success": true, "meta": { "hasNextPage": false }, "data": [] }
            """.trimIndent(),
        )

        assertNull(page.nextCursor)
    }
}
