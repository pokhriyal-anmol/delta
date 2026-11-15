package dev.anpo.f1.learn

import dev.anpo.f1.learn.Block.Callout
import dev.anpo.f1.learn.Block.Para
import dev.anpo.f1.learn.Block.Rows
import dev.anpo.f1.learn.Block.Table

private const val FIA_B = "FIA 2026 Section B Sporting Regulations, Issue 07"

/** Lessons 8 to 11: flags, the Safety Car, penalties, bans and the licence. */
internal val lessonsRules: List<Lesson> = listOf(
    Lesson(
        number = 8, id = "flags", title = "Flags", minutes = 2,
        blocks = listOf(
            Para("Marshals wave flags, and the same signals light up on panels on the car and around the track."),
            Table(
                listOf("Flag", "Meaning"),
                listOf(
                    listOf("Green", "Hazard cleared, racing as normal"),
                    listOf("Yellow", "Danger. Slow down, no overtaking. Two waved means be ready to stop"),
                    listOf("Yellow with red stripes", "Slippery surface ahead"),
                    listOf("Blue", "A faster car is behind you. Let it past"),
                    listOf("White", "A slow vehicle on track"),
                    listOf("Red", "Session stopped, cars return to the pits"),
                    listOf("Black and white", "A warning for unsporting driving"),
                    listOf("Black", "Driver disqualified"),
                    listOf("Chequered", "The session is over"),
                ),
            ),
            Callout(
                "These meanings come from secondary sources (Red Bull, Silverstone and Planet F1, which agree). " +
                    "The official text is in the FIA International Sporting Code, Appendix H, and is still to be checked."
            ),
        ),
        facts = emptyList(),
        source = "Red Bull, Silverstone and Planet F1, to re-check against FIA International Sporting Code Appendix H. Light panels: $FIA_B, B5.12.1.",
    ),
    Lesson(
        number = 9, id = "safety-car", title = "Safety car and red flags", minutes = 2,
        blocks = listOf(
            Para(
                "A Safety Car comes out when people on or near the track are in immediate danger, but the race " +
                    "does not need to stop. Everyone lines up behind it and bunches up."
            ),
            Para(
                "A Virtual Safety Car is the lighter version. Drivers must slow to a set pace without a car " +
                    "leading them, used when double yellow flags are out and marshals may be in danger. Panels show VSC."
            ),
            Para("A red flag stops the race. Cars return to the pit lane and the clock keeps counting towards the three hour cap."),
            Callout("Why it matters: a Safety Car resets the gaps, so a race that looked won can open up again. It is also when teams gamble on a cheap pit stop."),
        ),
        facts = emptyList(),
        source = "$FIA_B, B2.5.3b, B5.12 and B5.13.",
    ),
    Lesson(
        number = 10, id = "penalties", title = "Penalties", minutes = 3,
        blocks = listOf(
            yearStamp(),
            Para("The stewards decide, and only if a driver was wholly or mostly to blame. During a race the menu is:"),
            Table(
                listOf("Penalty", "What happens"),
                listOf(
                    listOf("Reprimand", "A warning. Five of them in a season means a 10 place grid drop"),
                    listOf("5 second", "Stop in the box for 5 seconds. Nobody may touch the car"),
                    listOf("10 second", "Same, for 10 seconds"),
                    listOf("Drive-through", "Drive down the pit lane without stopping"),
                    listOf("Stop and go", "Stop in the box for 10 seconds"),
                    listOf("Grid drop", "Lose places at the next start"),
                    listOf("Disqualification", "Removed from the results"),
                    listOf("Suspension", "Miss the next event"),
                ),
            ),
            Para(
                "If a drive-through or stop-and-go lands in the last three laps, it turns into extra time instead: " +
                    "20 seconds and 30 seconds. After the flag, a 5 second penalty is simply added to the finishing time."
            ),
            Rows(
                listOf(
                    "Before 2014" to "Penalties were rare, and the usual one was a ten second stop-and-go (RaceFans)",
                    "1994" to "A five second stop-and-go already existed. Schumacher got one at Silverstone (Wikipedia)",
                    "2014" to "The five second time penalty arrived (RaceFans)",
                    "2015" to "The ten second time penalty arrived (RaceFans)",
                )
            ),
        ),
        facts = listOf("During a 5 second stop, the car sits still and the mechanics may not touch it."),
        source = "$FIA_B, B1.9.3, B1.9.5, B1.9.6b to B1.9.6d. History: RaceFans and Wikipedia, secondary sources.",
    ),
    Lesson(
        number = 11, id = "bans-licence", title = "Race bans and your licence", minutes = 3,
        blocks = listOf(
            Para(
                "Every driver needs a Super Licence, the FIA's permission to race in F1. It was introduced in 1984 " +
                    "to keep unqualified drivers out, and rebuilt in 2015 after Max Verstappen arrived at 17."
            ),
            Para(
                "The same licence carries penalty points, since 2014. The licence is about what you earned, penalty points are about " +
                    "what you did. Each point drops off 12 months after it was given, and after a ban 12 points are removed."
            ),
            Para(
                "Other routes: the stewards can suspend a driver from the next event directly. Five reprimands in " +
                    "one season mean a 10 place grid drop. Lose more than 15 places of unserved grid penalties in " +
                    "12 months and you start behind everyone."
            ),
        ),
        facts = listOf("Only one driver has ever been banned through penalty points: Kevin Magnussen."),
        source = "$FIA_B, B1.9.5f, B1.9.5j and B2.5.4b.iv. Licence and bans: Wikipedia, Autosport and RacingNews365, secondary sources.",
    ),
)
