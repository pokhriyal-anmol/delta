package dev.anpo.f1.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anpo.f1.ui.F1Dim
import dev.anpo.f1.ui.F1Type
import dev.anpo.f1.ui.LocalPalette
import dev.anpo.f1.ui.pressable

/** The Home row that opens Learn F1: a hairline, a poster line and an accent chevron. */
@Composable
fun LearnEntry(onClick: () -> Unit) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth().padding(horizontal = F1Dim.screenPad).padding(top = 34.dp)) {
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(p.divider))
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).pressable(role = Role.Button, onClickLabel = "Open Learn F1") { onClick() }
            .padding(top = 18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("New to F1?", style = F1Type.meta.copy(color = p.sec))
                Spacer(Modifier.height(6.dp))
                Text("Learn F1", style = F1Type.section.copy(color = p.txt))
                Spacer(Modifier.height(6.dp))
                Text("${lessons.size} short lessons on points, flags, tyres and penalties.", style = F1Type.meta.copy(color = p.sec, fontSize = 11.sp))
            }
            Text("›", style = F1Type.section.copy(color = p.accent))
        }
    }
}
