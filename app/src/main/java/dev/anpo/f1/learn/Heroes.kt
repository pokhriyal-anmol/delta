package dev.anpo.f1.learn

import androidx.compose.runtime.Composable
import dev.anpo.f1.learn.heroes.FlagsHero
import dev.anpo.f1.learn.heroes.LicenceHero
import dev.anpo.f1.learn.heroes.PenaltiesHero
import dev.anpo.f1.learn.heroes.PointsHero
import dev.anpo.f1.learn.heroes.QualifyingHero
import dev.anpo.f1.learn.heroes.ShameHero
import dev.anpo.f1.learn.heroes.TyresHero

/** The hero visual for a lesson. A lesson without one opens straight into its text. */
@Composable
internal fun LessonHero(id: String) {
    when (id) {
        "qualifying" -> QualifyingHero()
        "flags" -> FlagsHero()
        "tyres" -> TyresHero()
        "points" -> PointsHero()
        "penalties" -> PenaltiesHero()
        "bans-licence" -> LicenceHero()
        "hall-of-shame" -> ShameHero()
        else -> Unit
    }
}
