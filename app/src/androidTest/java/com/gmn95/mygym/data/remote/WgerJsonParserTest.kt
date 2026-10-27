package com.gmn95.mygym.data.remote

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WgerJsonParserTest {
    @Test
    fun keepsOnlyPortugueseTranslationAndParsesMedia() {
        val page = WgerJsonParser.parsePage(
            """
            {
              "next": "https://wger.de/api/v2/exerciseinfo/?offset=20",
              "results": [
                {
                  "id": 42,
                  "translations": [
                    { "language": 2, "name": "Bench press" },
                    { "language": 7, "name": "Supino reto" }
                  ],
                  "images": [
                    { "is_main": true, "image": "https://example.com/supino.jpg" }
                  ],
                  "videos": [
                    { "is_main": true, "video": "https://example.com/supino.mp4" }
                  ],
                  "license": { "full_name": "CC BY-SA 4.0" },
                  "license_author": "Autor"
                },
                {
                  "id": 43,
                  "translations": [
                    { "language": 2, "name": "English only" }
                  ],
                  "images": [],
                  "videos": []
                }
              ]
            }
            """.trimIndent(),
        )

        val exercise = page.exercises.single()
        assertEquals("42", exercise.id)
        assertEquals("Supino reto", exercise.name)
        assertEquals("https://example.com/supino.jpg", exercise.imageUrl)
        assertEquals("https://example.com/supino.mp4", exercise.videoUrl)
        assertEquals("CC BY-SA 4.0 — Autor", exercise.mediaAttribution)
        assertEquals("https://wger.de/api/v2/exerciseinfo/?offset=20", page.nextPageUrl)
    }

    @Test
    fun acceptsLastPageWithoutNextUrl() {
        val page = WgerJsonParser.parsePage("""{ "next": null, "results": [] }""")

        assertNull(page.nextPageUrl)
    }
}
