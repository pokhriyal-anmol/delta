package dev.anpo.f1.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anpo.f1.ui.CompactTitleBar
import dev.anpo.f1.ui.F1Dim
import dev.anpo.f1.ui.F1Type
import dev.anpo.f1.ui.LocalPalette
import dev.anpo.f1.ui.ambient

/**
 * The shared lesson page: a poster title, the lesson's hero visual, then its blocks on hairlines,
 * the fun facts and the source line. Nothing here touches the network.
 */
@Composable
fun LessonScreen(id: String) {
    val lesson = remember(id) { lessons.firstOrNull { it.id == id } } ?: return
    val p = LocalPalette.current
    val list = rememberLazyListState()
    val titleGone by remember { derivedStateOf { list.firstVisibleItemIndex > 0 || list.firstVisibleItemScrollOffset > 220 } }
    Box(Modifier.fillMaxSize().background(p.canvas)) {
        LazyColumn(Modifier.fillMaxSize().ambient(p.isDark), state = list, contentPadding = PaddingValues(bottom = 140.dp)) {
            item { LessonHeader(lesson) }
            item { LessonHero(lesson.id) }
            items(lesson.blocks.size) { BlockView(lesson.blocks[it]) }
            items(lesson.facts.size) { FactView(lesson.facts[it]) }
            item { SourceLine(lesson.source) }
        }
        CompactTitleBar(lesson.title, titleGone, Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun LessonHeader(l: Lesson) {
    val p = LocalPalette.current
    Column(Modifier.padding(horizontal = F1Dim.screenPad).padding(top = 60.dp)) {
        Text("P${l.number} · ${l.minutes} min", style = F1Type.body.copy(color = p.sec))
        Spacer(Modifier.height(8.dp))
        Text(l.title, style = F1Type.display.copy(color = p.txt), lineHeight = 44.sp)
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun SourceLine(source: String) {
    val p = LocalPalette.current
    Text("Source: $source", style = F1Type.meta.copy(color = p.sec, fontSize = 11.sp), lineHeight = 16.sp,
        modifier = Modifier.padding(horizontal = F1Dim.screenPad).padding(top = 26.dp))
}
