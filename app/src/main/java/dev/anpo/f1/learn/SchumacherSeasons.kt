package dev.anpo.f1.learn

/** One season of Michael Schumacher's career: the points he was credited with, and the same finishes on today's 25-18-15 table. */
data class SeasonPoints(val year: Int, val official: Int, val today: Int)

/**
 * Computed on 2026-11-15 from Jolpica-F1 results. The "today" column is rescore() over that season's finishing
 * positions, and a unit test checks it against the checked-in fixture. 2010 to 2012 already used the modern table.
 */
val schumacherSeasons: List<SeasonPoints> = listOf(
    SeasonPoints(1991, 4, 26), SeasonPoints(1992, 53, 181), SeasonPoints(1993, 52, 160), SeasonPoints(1994, 92, 236),
    SeasonPoints(1995, 102, 268), SeasonPoints(1996, 59, 173), SeasonPoints(1997, 78, 229), SeasonPoints(1998, 86, 242),
    SeasonPoints(1999, 44, 133), SeasonPoints(2000, 108, 286), SeasonPoints(2001, 123, 327), SeasonPoints(2002, 144, 380),
    SeasonPoints(2003, 93, 236), SeasonPoints(2004, 148, 367), SeasonPoints(2005, 62, 153), SeasonPoints(2006, 121, 296),
    SeasonPoints(2010, 72, 72), SeasonPoints(2011, 76, 76), SeasonPoints(2012, 49, 49),
)
