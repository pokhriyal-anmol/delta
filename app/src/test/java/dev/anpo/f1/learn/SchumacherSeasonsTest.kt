package dev.anpo.f1.learn

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class SchumacherSeasonsTest {
    private val career: Map<String, List<String>> by lazy {
        val text = javaClass.getResourceAsStream("/learn/jolpica-schumacher.json")!!.bufferedReader().readText()
        Json.parseToJsonElement(text).jsonObject["schumacher"]!!.jsonObject
            .mapValues { e -> e.value.jsonArray.map { it.jsonPrimitive.content } }
    }

    @Test fun everyBundledSeasonMatchesRescoreOverTheFixture() {
        schumacherSeasons.forEach { assertEquals("${it.year} on today's table", rescore(career.getValue("${it.year}")), it.today) }
    }

    @Test fun theBundledSeasonsAreExactlyTheFixtureSeasons() {
        assertEquals(career.keys.map { it.toInt() }.sorted(), schumacherSeasons.map { it.year })
    }

    @Test fun totalsMatchTheCareerFigures() {
        assertEquals(19, schumacherSeasons.size)
        assertEquals(3890, schumacherSeasons.sumOf { it.today })
        assertEquals(1566, schumacherSeasons.sumOf { it.official })
    }

    @Test fun twentyZeroTwoIsOneFortyFourThenThreeEighty() {
        val s = schumacherSeasons.first { it.year == 2002 }
        assertEquals(144, s.official)
        assertEquals(380, s.today)
    }
}
