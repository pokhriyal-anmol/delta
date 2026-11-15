package dev.anpo.f1.learn

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RescoreTest {
    /** Jolpica results fetched once on 2026-11-15 (see the fixture's "source"). Test resource only. */
    private val fixture: JsonObject by lazy {
        Json.parseToJsonElement(javaClass.getResourceAsStream("/learn/jolpica-schumacher.json")!!.bufferedReader().readText()).jsonObject
    }
    private fun texts(a: JsonArray) = a.map { it.jsonPrimitive.content }
    private val career: Map<String, List<String>> by lazy {
        fixture["schumacher"]!!.jsonObject.mapValues { texts(it.value.jsonArray) }
    }
    private fun list(key: String) = texts(fixture[key]!!.jsonArray)
    private val all get() = career.values.flatten()

    @Test fun fixtureIsComplete() {
        assertEquals("2026-11-15", fixture["fetched"]!!.jsonPrimitive.content)
        assertTrue(fixture["source"]!!.jsonPrimitive.content.contains("jolpi.ca"))
        assertEquals(308, all.size)
        assertEquals(91, all.count { it == "1" })
        assertEquals(155, all.count { it in listOf("1", "2", "3") })
    }

    @Test fun careerOnTodaysTable() = assertEquals(3890, rescore(all))

    @Test fun seasonsOnTodaysTable() {
        assertEquals(380, rescore(career.getValue("2002")))
        assertEquals(236, rescore(career.getValue("1994")))
        assertEquals(252, rescore(list("hill1994")))
        assertEquals(237, rescore(list("irvine1999")))
        assertEquals(216, rescore(list("hakkinen1999")))
    }

    @Test fun nonNumericAndBeyondTenScoreZero() {
        assertEquals(0, rescore(listOf("D", "R", "W", "11", "20")))
        assertEquals(25 + 1, rescore(listOf("1", "R", "10", "11", "D")))
    }
}
