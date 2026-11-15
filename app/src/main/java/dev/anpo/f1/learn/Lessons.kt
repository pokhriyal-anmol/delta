package dev.anpo.f1.learn

import dev.anpo.f1.learn.Block.Callout
import dev.anpo.f1.learn.Block.Para
import dev.anpo.f1.learn.Block.Table

/** Lessons 12 to 14: the penalty records, the 2026 cars and the glossary. */
private val lessonsLast: List<Lesson> = listOf(
    Lesson(
        number = 12, id = "hall-of-shame", title = "Hall of shame", minutes = 3,
        blocks = listOf(
            Para("Penalty records. Each figure names its source and should be re-checked every year."),
            Table(
                listOf("Record", "Holder", "Source"),
                listOf(
                    listOf("Most penalties in one race", "5, Esteban Ocon, Austria 2023", "Wikipedia records"),
                    listOf("Most grid places lost by one driver in one race", "80, Gabriel Bortoleto, United States GP 2026", "Wikipedia, PlanetF1"),
                    listOf("Most grid places lost by the whole field", "168 at Monza 2015", "Autosport, Jalopnik"),
                    listOf("Most disqualifications in a career", "11, Stefan Bellof, all in 1984", "Jolpica, counted 2026-11-15"),
                ),
            ),
            Para(
                "Bortoleto's 80 came in two steps. On Friday Audi fitted a fourth control electronics unit and " +
                    "energy store (the limit is three) for 20 places. On Saturday new parts each cost 5 or 10 places, " +
                    "60 more. Anything over 15 means starting last whatever the number says."
            ),
            Para(
                "Bellof's 11 is not 11 offences. After Detroit 1984, officials found lead balls in the water tank on " +
                    "Martin Brundle's Tyrrell, and the team was thrown out of the rest of the championship. Jolpica " +
                    "lists all 22 of Tyrrell's 1984 results as disqualified, 11 of them Bellof's."
            ),
            Para(
                "The sport has had 153 disqualifications in total (Jolpica, 1952 to 2025, counted 2026-11-15). " +
                    "1984 had 22. 2025 had 6, the most since 1989."
            ),
            Para(
                "Schumacher, 1994: at Silverstone he overtook on the formation lap, ignored a stop-and-go and then " +
                    "a black flag, and was disqualified later, with a $500,000 team fine and a two race ban. At Spa " +
                    "he won on the road but his skid block was worn past the 1 mm limit. In 1997 the FIA threw him " +
                    "out of the whole championship after the Jerez collision with Villeneuve, which Wikipedia calls unprecedented."
            ),
        ),
        facts = listOf("Stefan Bellof has the most disqualifications ever, 11, all in one season."),
        source = "Wikipedia, PlanetF1, Autosport, Jalopnik and Jolpica-F1 (counted 2026-11-15). Secondary sources.",
    ),
    Lesson(
        number = 13, id = "cars-2026", title = "The 2026 cars", minutes = 2,
        blocks = listOf(
            yearStamp(),
            Para("2026 is the biggest rule change in years."),
            Table(
                listOf("Change", "What it means"),
                listOf(
                    listOf("Smaller and lighter", "Wheelbase 200 mm shorter, minimum weight down from 800 to 768 kg, narrower tyres"),
                    listOf("Moving wings", "Closed in corners for downforce, open on straights to cut drag. The old DRS flap is gone"),
                    listOf("Stronger hybrid motor", "No MGU-H, and the MGU-K goes from 120 kW to 350 kW, so about half the power is electric"),
                    listOf("Overtake mode", "A car within one second at a marked point gets a little extra electrical power for the next lap"),
                    listOf("Fuel", "An advanced sustainable fuel"),
                ),
            ),
            Callout(
                "Some power limits changed after the first rounds of 2026 (the Miami refinements), so this is the " +
                    "lesson most likely to age. The figures come from formula1.com and one secondary source for the motor power."
            ),
        ),
        facts = emptyList(),
        source = "formula1.com explainer on the 2026 regulations. Motor power: secondary source, to re-check.",
    ),
    Lesson(
        number = 14, id = "glossary", title = "Glossary", minutes = 2,
        blocks = listOf(
            Table(
                listOf("Term", "Plain words"),
                listOf(
                    listOf("Box", "The pit stop, from box, box on the radio"),
                    listOf("Pole", "First on the grid"),
                    listOf("Undercut", "Pit earlier so fresh tyres win you the place"),
                    listOf("Overcut", "Stay out longer and pit later"),
                    listOf("Dirty air", "The turbulence behind a car that makes the next car lose grip"),
                    listOf("Lapped", "A car the leader has caught up with from behind"),
                    listOf("DNF", "Did not finish"),
                    listOf("DSQ", "Disqualified"),
                    listOf("Formation lap", "The slow lap before the start"),
                    listOf("Parc ferme", "The sealed area where cars are held after qualifying and the race"),
                    listOf("Delta", "The gap in time to another driver or to a target"),
                    listOf("Lock-up", "A wheel stops turning under braking and the tyre flat-spots"),
                    listOf("Interval", "The gap to the car directly ahead"),
                    listOf("Stewards", "The officials who decide penalties"),
                ),
            ),
            Callout("Undercut, overcut, dirty air, lock-up and delta are standard terms but have not been checked against a primary source."),
        ),
        facts = emptyList(),
        source = "Common F1 usage. Five terms still to re-check against a primary source.",
    ),
)

/** All 14 lessons in order, P1 to P14. */
val lessons: List<Lesson> = lessonsBasics + lessonsRules + lessonsLast

/** The "Did you know" bank. */
val funFacts: List<String> = listOf(
    "Each team paints one car's camera housing fluorescent yellow.",
    "Monaco's race is 260 km, every other is 305.",
    "The pit lane speed limit is 80 km/h all weekend.",
    "Nobody under 16 is allowed in the pit lane around practice and qualifying.",
    "At a normal weekend, Q3 is run on the softest of the three tyres.",
    "A tie on points goes to whoever has more wins.",
    "There is no point for the fastest lap.",
    "A race that stops early pays less, down to 6 points for the winner.",
    "The pit stop record is 1.80 seconds, McLaren, Qatar 2023.",
    "Schumacher would have 3,890 points on today's table, not 1,566.",
    "Eddie Irvine would beat Hakkinen for the 1999 title on today's table.",
    "Bellof has the most disqualifications ever, 11, all in one season.",
    "Bortoleto lost 80 grid places at Austin 2026.",
    "During a 5 second penalty stop, the mechanics may not touch the car.",
)

/** One fact per day of the year, no stored state. Day 366 and day 1 differ. */
fun factOfTheDay(dayOfYear: Int): String = funFacts[Math.floorMod(dayOfYear - 1, funFacts.size)]
