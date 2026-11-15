package dev.anpo.f1.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anpo.f1.ui.F1Dim
import dev.anpo.f1.ui.F1Type
import dev.anpo.f1.ui.LocalPalette

private const val WHY = "Why it matters:"
private const val STAMP = "Rules as of"
private const val STACK_NARROW = 1.3f
private const val STACK_WIDE = 1.15f

/** One block on a hairline. Blocks are separated by lines and space, never boxed into cards. */
@Composable
internal fun BlockView(b: Block) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth().padding(horizontal = F1Dim.screenPad).padding(top = 18.dp)) {
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(p.divider))
        Spacer(Modifier.height(14.dp))
        when (b) {
            is Block.Para -> Text(b.text, style = F1Type.body.copy(color = p.txt, fontWeight = FontWeight.Normal), lineHeight = 22.sp)
            is Block.Rows -> RowsView(b.items)
            is Block.Table -> TableView(b)
            is Block.Callout -> CalloutView(b.text)
        }
    }
}

@Composable
private fun RowsView(items: List<Pair<String, String>>) {
    val p = LocalPalette.current
    items.forEachIndexed { i, (label, value) ->
        if (i > 0) Spacer(Modifier.fillMaxWidth().height(0.5.dp).background(p.divider))
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.Top) {
            Text(label, style = F1Type.body.copy(color = p.sec, fontSize = 13.sp, fontWeight = FontWeight.Normal), modifier = Modifier.weight(0.9f))
            Text(value, style = F1Type.body.copy(color = p.txt, fontSize = 13.sp), textAlign = TextAlign.End, modifier = Modifier.weight(1.1f))
        }
    }
}

@Composable
private fun TableView(t: Block.Table) {
    val scale = LocalDensity.current.fontScale
    val wide = t.header.size > 5
    // Columns stop fitting once text is enlarged, and words would break mid-word, so each row stacks instead.
    if (scale >= (if (wide) STACK_WIDE else STACK_NARROW)) StackedTable(t) else ColumnTable(t, wide)
}

@Composable
private fun ColumnTable(t: Block.Table, wide: Boolean) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        t.header.forEachIndexed { c, h -> Cell(h, c, wide, header = true) }
    }
    t.rows.forEach { row ->
        Spacer(Modifier.fillMaxWidth().height(0.5.dp).background(p.divider))
        Row(Modifier.fillMaxWidth().padding(vertical = 9.dp)) { row.forEachIndexed { c, v -> Cell(v, c, wide, header = false) } }
    }
}

/** One row becomes a short block: its first cell as a title, then one "column: value" line per other cell. */
@Composable
private fun StackedTable(t: Block.Table) {
    val p = LocalPalette.current
    val place = t.header.firstOrNull() == "Place"
    t.rows.forEachIndexed { r, row ->
        if (r > 0) Spacer(Modifier.fillMaxWidth().height(0.5.dp).background(p.divider))
        Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
            if (row.first().isNotBlank()) Text(row.first(), style = F1Type.body.copy(color = p.txt, fontWeight = FontWeight.Bold), lineHeight = 22.sp)
            row.drop(1).forEachIndexed { i, v ->
                val head = t.header.getOrElse(i + 1) { "" }.let { if (place) "Place $it" else it }
                Text(if (head.isBlank()) v else "$head: $v", style = F1Type.body.copy(color = p.sec, fontWeight = FontWeight.Normal), lineHeight = 22.sp)
            }
        }
    }
}

/** The first column is wider, so labels wrap less than the numbers beside them. */
@Composable
private fun RowScope.Cell(text: String, col: Int, wide: Boolean, header: Boolean) {
    val p = LocalPalette.current
    val w = if (col == 0) (if (wide) 1.7f else 1.5f) else 1f
    val style = if (header) F1Type.label.copy(color = p.sec, fontSize = 11.sp)
    else F1Type.body.copy(color = if (col == 0) p.sec else p.txt, fontSize = 13.sp, fontWeight = if (col == 0) FontWeight.Normal else FontWeight.Medium)
    Text(text, style = style, modifier = Modifier.weight(w).padding(end = 6.dp))
}

/** "Why it matters" gets an accent line, the season stamp becomes a small pill, anything else is a quiet note. */
@Composable
private fun CalloutView(text: String) {
    val p = LocalPalette.current
    if (text.startsWith(STAMP)) {
        val head = text.substringBefore(".")
        Text(head, style = F1Type.label.copy(color = p.accent, fontSize = 11.sp),
            modifier = Modifier.clip(RoundedCornerShape(50)).background(p.chip).padding(horizontal = 12.dp, vertical = 6.dp))
        Spacer(Modifier.height(8.dp))
        Text(text.substringAfter(". ", "").ifBlank { head }, style = F1Type.meta.copy(color = p.sec, fontSize = 11.sp), lineHeight = 16.sp)
        return
    }
    val why = text.startsWith(WHY)
    Row(Modifier.height(IntrinsicSize.Min)) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(if (why) p.accent else p.sec.copy(alpha = 0.4f)))
        Column(Modifier.padding(start = 12.dp)) {
            if (why) {
                Text("WHY IT MATTERS", style = F1Type.label.copy(color = p.accent, fontSize = 11.sp))
                Spacer(Modifier.height(4.dp))
            }
            Text(if (why) text.removePrefix(WHY).trim().replaceFirstChar { it.uppercase() } else text,
                style = F1Type.body.copy(color = if (why) p.txt else p.sec, fontWeight = FontWeight.Normal), lineHeight = 22.sp)
        }
    }
}

@Composable
internal fun FactView(fact: String) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth().padding(horizontal = F1Dim.screenPad).padding(top = 22.dp)) {
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(p.divider))
        Spacer(Modifier.height(14.dp))
        Text("DID YOU KNOW", style = F1Type.label.copy(color = p.accent, fontSize = 11.sp))
        Spacer(Modifier.height(6.dp))
        Text(fact, style = F1Type.stat.copy(color = p.txt), lineHeight = 25.sp)
    }
}
