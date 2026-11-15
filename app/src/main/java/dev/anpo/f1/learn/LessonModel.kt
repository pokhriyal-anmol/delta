package dev.anpo.f1.learn

/** One bundled lesson. Pure data: no network, nothing stored. */
data class Lesson(
    val number: Int,
    val id: String,
    val title: String,
    val minutes: Int,
    val blocks: List<Block>,
    val facts: List<String>,
    val source: String,
)

sealed interface Block {
    data class Para(val text: String) : Block
    /** Label and value pairs, shown as hairline-divided rows. */
    data class Rows(val items: List<Pair<String, String>>) : Block
    data class Table(val header: List<String>, val rows: List<List<String>>) : Block
    data class Callout(val text: String) : Block
}

/** The stamp on lessons whose content changes every season. */
fun yearStamp(): Block = Block.Callout("Rules as of 2026. This lesson is bundled text and changes when the rules do.")
