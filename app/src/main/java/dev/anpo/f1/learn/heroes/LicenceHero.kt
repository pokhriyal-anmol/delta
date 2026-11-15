package dev.anpo.f1.learn.heroes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.anpo.f1.ui.F1Type
import dev.anpo.f1.ui.LocalPalette

private const val ENTRY_POINTS = 40
private const val BAN_POINTS = 12

private val BANS = listOf(
    "1994" to "Schumacher. Two races, Italy and Portugal, after Silverstone.",
    "2012" to "Grosjean. One race, Italy, after the first lap crash at Spa.",
    "2024" to "Magnussen. One race, Azerbaijan. The only ban from penalty points.",
)

/** The licence as an object: 40 points to get one, 12 dots to lose a race. No FIA or F1 mark, only a plain label. */
@Composable
fun LicenceHero() {
    val p = LocalPalette.current
    HeroFrame {
        Column(Modifier.fillMaxWidth().background(p.card, RoundedCornerShape(20.dp)).border(1.dp, p.sec.copy(alpha = 0.3f), RoundedCornerShape(20.dp)).padding(18.dp)
            .semantics(mergeDescendants = true) { contentDescription = "Super licence. $ENTRY_POINTS points from junior racing over three seasons to get one. $BAN_POINTS penalty points in 12 months means missing the next race." }) {
            HeroLabel("Super licence")
            Spacer(Modifier.height(6.dp))
            Text("$ENTRY_POINTS", style = F1Type.hero.copy(color = p.txt, fontSize = 96.sp), lineHeight = 100.sp)
            Text("points from junior racing over three seasons to get in. Plus being 18 and passing the FIA's checks.", style = F1Type.meta.copy(color = p.sec), lineHeight = 17.sp)
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(p.divider))
            Spacer(Modifier.height(14.dp))
            Text("$BAN_POINTS penalty points in 12 months and you miss the next race.", style = F1Type.stat.copy(color = p.txt), lineHeight = 25.sp)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(BAN_POINTS) { i ->
                    Box(Modifier.size(15.dp).then(if (i == BAN_POINTS - 1) Modifier.background(p.accent, CircleShape) else Modifier.border(1.5.dp, p.sec.copy(alpha = 0.6f), CircleShape)))
                }
            }
            Text("Each point expires after 12 months.", style = F1Type.meta.copy(color = p.sec, fontSize = 11.sp), modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.height(16.dp))
        BANS.forEach { (year, text) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.Top) {
                Text(year, style = F1Type.stat.copy(color = p.accent), modifier = Modifier.width(58.dp))
                Text(text, style = F1Type.body.copy(color = p.txt, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), lineHeight = 20.sp)
            }
        }
    }
}
