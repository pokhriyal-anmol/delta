package dev.anpo.f1.learn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val BANNED = listOf("robust", "seamless", "leverage", "delve", "utilize", "holistic")
private val YEARLY = setOf("points", "penalties", "tyres", "cars-2026")
private const val STAMP = "Rules as of 2026"

/** Every user-facing string in a lesson, blocks included. */
internal fun Lesson.strings(): List<String> = listOf(id, title, source) + facts + blocks.flatMap { b ->
    when (b) {
        is Block.Para -> listOf(b.text)
        is Block.Callout -> listOf(b.text)
        is Block.Rows -> b.items.flatMap { listOf(it.first, it.second) }
        is Block.Table -> b.header + b.rows.flatten()
    }
}

/** Each check returns the problems it found, so a test can prove it fails on bad data. */
internal fun idProblems(l: List<Lesson>) = l.groupBy { it.id }.filter { it.value.size > 1 }.keys.map { "duplicate id $it" }
internal fun numberProblems(l: List<Lesson>) =
    if (l.map { it.number }.sorted() == (1..14).toList()) emptyList() else listOf("numbers are not 1..14")
internal fun sourceProblems(l: List<Lesson>) = l.filter { it.source.isBlank() }.map { "no source on ${it.id}" }
internal fun stampProblems(l: List<Lesson>) = YEARLY.filter { id ->
    l.none { it.id == id && it.blocks.any { b -> b is Block.Callout && b.text.startsWith(STAMP) } }
}.map { "no stamp on $it" }
internal fun factCountProblems(f: List<String>) = if (f.size >= 14) emptyList() else listOf("only ${f.size} facts")
internal fun textProblems(strings: List<String>) = strings.flatMap { s ->
    buildList {
        if ('—' in s || '–' in s) add("dash in: $s")
        BANNED.filter { it in s.lowercase() }.forEach { add("banned $it in: $s") }
    }
}

class LessonsTest {
    private val strings = lessons.flatMap { it.strings() } + funFacts

    @Test fun idsAreUnique() = assertEquals(emptyList<String>(), idProblems(lessons))
    @Test fun numbersAreOneToFourteen() = assertEquals(emptyList<String>(), numberProblems(lessons))
    @Test fun everyLessonHasASource() = assertEquals(emptyList<String>(), sourceProblems(lessons))
    @Test fun yearlyLessonsAreStamped() = assertEquals(emptyList<String>(), stampProblems(lessons))
    @Test fun factBankHasFourteen() = assertEquals(emptyList<String>(), factCountProblems(funFacts))
    @Test fun noDashesOrBannedWords() = assertEquals(emptyList<String>(), textProblems(strings))
    @Test fun lessonsAreInNumberOrder() = assertEquals((1..14).toList(), lessons.map { it.number })

    @Test fun checksFailOnBadData() {
        val a = lessons[0]
        val b = lessons[1]
        assertTrue(idProblems(listOf(a, b.copy(id = a.id))).isNotEmpty())
        assertTrue(numberProblems(lessons.filter { it.number != 7 }).isNotEmpty())
        assertTrue(numberProblems(lessons.map { if (it.number == 7) it.copy(number = 15) else it }).isNotEmpty())
        assertTrue(sourceProblems(listOf(a.copy(source = " "))).isNotEmpty())
        assertTrue(stampProblems(lessons.map { it.copy(blocks = it.blocks.filter { b -> b !is Block.Callout }) }).isNotEmpty())
        assertTrue(factCountProblems(funFacts.drop(1)).isNotEmpty())
        assertTrue(textProblems(listOf("a — b")).isNotEmpty())
        assertTrue(textProblems(listOf("a – b")).isNotEmpty())
        assertTrue(textProblems(listOf("A Robust plan")).isNotEmpty())
        assertTrue(textProblems(listOf("we utilize it")).isNotEmpty())
    }

    @Test fun factOfTheDayCoversTheYear() {
        assertTrue(factOfTheDay(1).isNotBlank())
        assertTrue(factOfTheDay(366).isNotBlank())
        assertTrue((1..365).all { factOfTheDay(it) != factOfTheDay(it + 1) })
        assertNotEquals(factOfTheDay(100), factOfTheDay(101))
    }
}
