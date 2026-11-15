package dev.anpo.f1.learn

import dev.anpo.f1.learn.Block.Callout
import dev.anpo.f1.learn.Block.Para
import dev.anpo.f1.learn.Block.Rows
import dev.anpo.f1.learn.Block.Table

private const val FIA_A = "FIA 2026 Section A General Provisions, Issue 02"
private const val FIA_B = "FIA 2026 Section B Sporting Regulations, Issue 07"

/** Lessons 1 to 7: how the sport, a weekend, a race, points and tyres work. */
internal val lessonsBasics: List<Lesson> = listOf(
    Lesson(
        number = 1, id = "sport", title = "The sport in a minute", minutes = 2,
        blocks = listOf(
            Para("Ten or eleven teams each run two cars, and two titles are given out at the end of the season."),
            Para(
                "The Drivers' Championship goes to a person. The Constructors' Championship goes to a team, " +
                    "which adds up the points of both its cars. A team that builds its own car but buys its engine " +
                    "still wins the team title in its own name."
            ),
            Rows(
                listOf(
                    "Races in a season" to "8 to 24",
                    "Cars on the grid" to "24 at most, two per team",
                    "This season in Delta" to "23 circuits, 22 cars",
                )
            ),
            Callout("Why it matters: every other lesson here is about how one of those two titles is won or lost."),
        ),
        facts = listOf("Each team paints the camera housing on one of its two cars fluorescent yellow, so you can tell them apart on screen."),
        source = "$FIA_A, A2.1.2 to A2.1.4 and A2.3.4d. Circuit and car counts from Delta's own data.",
    ),
    Lesson(
        number = 2, id = "weekend", title = "A race weekend", minutes = 2,
        blocks = listOf(
            Para("A normal weekend runs over three days, and only the race and the sprint hand out points."),
            Rows(
                listOf(
                    "Day one" to "Two one hour practice sessions",
                    "Day two" to "A third practice, then qualifying",
                    "Day three" to "The race",
                )
            ),
            Para(
                "A sprint weekend has less practice and more racing. There is one hour of practice, then sprint " +
                    "qualifying and a short sprint race on day two, then qualifying and the grand prix."
            ),
            Callout("Why it matters: practice is where teams learn the car, and qualifying sets the starting order."),
        ),
        facts = listOf("Nobody under 16 is allowed in the pit lane in the 15 minutes before and 5 minutes after practice and qualifying."),
        source = "$FIA_B, B2.1.1, B2.1.2, B2.2, B2.3, B2.4.1, B2.5.1 and B1.6.2e.",
    ),
    Lesson(
        number = 3, id = "qualifying", title = "Qualifying", minutes = 2,
        blocks = listOf(
            Para("Qualifying decides the starting grid with three knockout rounds on one clock. With 22 cars:"),
            Table(
                listOf("Round", "Length", "Who is out"),
                listOf(
                    listOf("Q1", "18 minutes", "Slowest 6, they start 17th to 22nd"),
                    listOf("Q2", "15 minutes", "Slowest 6, they start 11th to 16th"),
                    listOf("Q3", "13 minutes", "None, the fastest lap takes pole"),
                ),
            ),
            Para("There is a 7 minute break between rounds, and lap times reset each round. Sprint qualifying is the same idea in 12, 10 and 8 minutes."),
            Para(
                "The 107 percent rule: if your best Q1 lap is slower than 107 percent of the fastest, you are not " +
                    "classified, unless it rained. The stewards can still let you race."
            ),
        ),
        facts = listOf("At a normal weekend, the tyre for Q3 is fixed in advance as the softest of the three compounds brought to that race."),
        source = "$FIA_B, B2.2.2, B2.4.2, B2.4.3 and B6.1.2b.iii.",
    ),
    Lesson(
        number = 4, id = "race", title = "The race", minutes = 2,
        blocks = listOf(
            Para("A race is the smallest number of whole laps that goes past 305 km. Monaco is the exception at 260 km."),
            Rows(
                listOf(
                    "Time limit" to "2 hours. If it runs out, the leader finishes the lap after",
                    "After a stoppage" to "The clock can stretch to 3 hours in total",
                    "To be classified" to "Finish at least 90 percent of the winner's laps",
                    "Pit lane speed" to "80 km/h all weekend",
                )
            ),
            Para("A sprint is shorter: the fewest laps that go past 100 km, with a one hour limit."),
        ),
        facts = listOf("Monaco is the only race with its own shorter distance written into the rules."),
        source = "$FIA_B, B2.3.2, B2.3.3, B2.5.2, B2.5.3, B2.5.5b and B1.6.3a.",
    ),
    Lesson(
        number = 5, id = "points", title = "Points", minutes = 3,
        blocks = listOf(
            yearStamp(),
            Para("Ten places score and a win is 25. A sprint pays the top eight: 8, 7, 6, 5, 4, 3, 2, 1."),
            Table(
                listOf("Place", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
                listOf(listOf("Points", "25", "18", "15", "12", "10", "8", "6", "4", "2", "1")),
            ),
            Para("If a race is cut short, the points shrink. The table used depends on how much of the distance the leader completed."),
            Table(
                listOf("Leader finished", "Win", "Second", "Third", "Places that score"),
                listOf(
                    listOf("2 laps, under 25%", "6", "4", "3", "5"),
                    listOf("25% to 50%", "13", "10", "8", "9"),
                    listOf("50% to 75%", "19", "14", "12", "10"),
                    listOf("75% or more", "25", "18", "15", "10"),
                ),
            ),
            Para(
                "No points at all unless the leader has done two complete laps without a Safety Car. A sprint " +
                    "scores only if the leader got through half the distance. A tie on points goes to the driver " +
                    "with most wins, then most second places, then thirds, and so on."
            ),
            Para(
                "Schumacher, re-scored. Every race finish in his 308 entries was re-scored with today's table, " +
                    "finishing position only, with disqualifications scoring zero. It is a thought experiment: " +
                    "fewer races, different cars and no sprints would all change how drivers raced."
            ),
            Table(
                listOf("", "Official", "Today's table"),
                listOf(
                    listOf("Career points", "1,566", "3,890"),
                    listOf("Points per entry", "5.1", "12.6"),
                    listOf("Best season", "144 (2002)", "380 (2002)"),
                ),
            ),
            Para(
                "Six of his seven titles survive. 1994 does not: Damon Hill would win it 252 to 236. In 1999, " +
                    "Eddie Irvine would beat Mika Hakkinen 237 to 216, because he retired once all year while " +
                    "Hakkinen retired five times."
            ),
        ),
        facts = listOf("There is no point for the fastest lap. The 2026 table has none."),
        source = "$FIA_A, A2.1.4c, A2.2.1 and A2.2.2. Schumacher figures computed 2026-11-15 from Jolpica-F1 results.",
    ),
    Lesson(
        number = 6, id = "tyres", title = "Tyres", minutes = 2,
        blocks = listOf(
            yearStamp(),
            Para(
                "Every car uses the same tyre maker. Each weekend the maker brings three dry compounds, one " +
                    "intermediate and one full wet, and they must look different on the track."
            ),
            Para(
                "The usual marking is red soft, yellow medium, white hard, green intermediate and blue wet. " +
                    "That colour scheme comes from secondary sources and is still to be checked against the FIA text."
            ),
            Rows(
                listOf(
                    "Soft" to "Grips most and wears fastest",
                    "Medium" to "In between",
                    "Hard" to "Lasts longest and grips least",
                )
            ),
            Para(
                "In a dry race every driver must use at least two different dry compounds, and at least one must " +
                    "be a compound the FIA named as mandatory for that race. Drivers who use intermediates or wets are exempt."
            ),
            Callout("Why it matters: this one rule is why you see pit stops. Nobody can run the whole race on one set."),
        ),
        facts = emptyList(),
        source = "$FIA_B, B6.1.1a and B6.3.6. Colours: secondary sources, to re-check against the FIA text.",
    ),
    Lesson(
        number = 7, id = "pit-stops", title = "Pit stops", minutes = 1,
        blocks = listOf(
            Para("A pit stop swaps tyres, and sometimes a damaged part. Cars are not refuelled, and they come in at no more than 80 km/h."),
            Para(
                "The fastest stop on record is 1.80 seconds, set by McLaren on Lando Norris's car at Qatar 2023. " +
                    "It beat Red Bull's 1.82 from 2019. This record comes from formula1.com and Guinness World Records, " +
                    "and has not been re-checked for anything faster since."
            ),
        ),
        facts = listOf("The pit stop record is 1.80 seconds, McLaren at Qatar 2023."),
        source = "$FIA_B, B1.6.3a. Record: formula1.com and Guinness World Records, to re-check.",
    ),
)
