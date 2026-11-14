package dev.anpo.f1.data

import android.content.Context
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.MutableStateFlow

/** Static constructor facts (name/base/power unit/history) not in the standings API. */
data class ConstructorInfo(val full: String, val base: String, val pu: String, val since: Int, val champs: Int)

class Repo(ctx: Context) {
    private val dao = FeedsDb.get(ctx).dao()
    private val limiter = RateLimiter(ctx, "jolpica", capacity = 200, refillPerHour = 200)
    /** Jolpica resolves "current" to the live season server-side; screens read the season off the cached rows. */
    private val current = "current"

    val error = MutableStateFlow<String?>(null)
    val loading = MutableStateFlow(false)

    fun drivers(): Flow<List<DriverStandingEntity>> = dao.drivers()
    fun teams(): Flow<List<ConstructorStandingEntity>> = dao.teams()
    fun races(): Flow<List<RaceEntity>> = dao.races()
    suspend fun driver(id: String) = dao.driver(id)
    suspend fun driverStat(id: String) = dao.driverStat(id)
    fun tokensLeft() = limiter.remaining()

    private val DAY = 86_400_000L
    private val NEWS_TTL = 15 * 60_000L

    /** Current-season standings change only after a race. Daily TTL is plenty and
     *  keeps us at roughly 20 requests a WEEK against a 200/hour ceiling. */
    suspend fun refreshStandings(force: Boolean = false) {
        val age = dao.driversFetchedAt()
        if (!force && age != null && System.currentTimeMillis() - age < DAY) return
        runCatching {
            loading.value = true
            limiter.acquire()
            val d = Net.jolpica.driverStandings(current, 30)
            val now = System.currentTimeMillis()
            d.data.standings?.lists?.firstOrNull()?.let { l ->
                dao.putDrivers(l.drivers.map { s ->
                    DriverStandingEntity(
                        driverId = s.driver.driverId, season = l.season, round = l.round,
                        position = s.position?.toIntOrNull() ?: 99, points = s.points.toIntOrNull() ?: 0,
                        wins = s.wins.toIntOrNull() ?: 0,
                        given = s.driver.givenName, family = s.driver.familyName,
                        code = s.driver.code, number = s.driver.permanentNumber?.toIntOrNull(),
                        team = s.teams.firstOrNull()?.name ?: "-",
                        teamId = s.teams.firstOrNull()?.constructorId ?: "-",
                        fetchedAt = now,
                    )
                })
            }
            limiter.acquire()
            val c = Net.jolpica.constructorStandings(current, 30)
            c.data.standings?.lists?.firstOrNull()?.let { l ->
                dao.putTeams(l.constructors.map { s ->
                    ConstructorStandingEntity(
                        teamId = s.team.constructorId, season = l.season,
                        position = s.position?.toIntOrNull() ?: 99, points = s.points.toIntOrNull() ?: 0,
                        wins = s.wins.toIntOrNull() ?: 0, name = s.team.name, fetchedAt = now,
                    )
                })
            }
            error.value = null
        }.onFailure {
            // Never crash the tab. Stale cache beats an empty screen.
            error.value = it.message ?: "Couldn't reach Jolpica"
        }
        loading.value = false
    }

    /** Per-driver podiums / poles / DNFs, derived from the whole season's results in
     *  one request. Standings change only after a race, so a daily TTL is plenty. */
    suspend fun refreshResults(force: Boolean = false) {
        val age = dao.statsFetchedAt()
        if (!force && age != null && System.currentTimeMillis() - age < DAY) return
        runCatching {
            // Jolpica caps limit at 100, so page through with offset until we have `total`.
            val races = mutableListOf<JRace>()
            var offset = 0; var total = Int.MAX_VALUE
            while (offset < total) {
                limiter.acquire()
                val page = Net.jolpica.seasonResults(current, 100, offset)
                total = page.data.total?.toIntOrNull() ?: 0
                val pageRaces = page.data.races?.races.orEmpty()
                if (pageRaces.isEmpty()) break
                races += pageRaces
                offset += 100
            }
            val now = System.currentTimeMillis()
            val agg = HashMap<String, IntArray>()            // driverId -> [podiums, poles, dnfs]
            val perRound = HashMap<String, HashMap<Int, Int>>() // driverId -> round -> points
            races.forEach { race ->
                val rnd = race.round.toIntOrNull()
                race.results.forEach { res ->
                    val id = res.driver.driverId
                    val a = agg.getOrPut(id) { IntArray(3) }
                    val finish = res.positionText.toIntOrNull()   // numeric = classified finish
                    if (finish != null && finish <= 3) a[0]++      // podium
                    if (res.grid.toIntOrNull() == 1) a[1]++        // pole (started P1)
                    if (finish == null) a[2]++                     // DNF (R/D/W/... not a number)
                    if (rnd != null) perRound.getOrPut(id) { HashMap() }[rnd] = res.points.toIntOrNull() ?: 0
                }
            }
            dao.putRaceResults(races.flatMap { resultRows(it) })
            val maxRound = races.mapNotNull { it.round.toIntOrNull() }.maxOrNull() ?: 0
            if (agg.isNotEmpty()) dao.putStats(agg.map { (id, a) ->
                val rounds = perRound[id]
                val csv = (1..maxRound).joinToString(",") { (rounds?.get(it) ?: 0).toString() }
                DriverSeasonStat(id, races.first().season, podiums = a[0], poles = a[1], dnfs = a[2],
                    roundPoints = csv, fetchedAt = now)
            })
            error.value = null
        }.onFailure { error.value = it.message ?: "Couldn't reach Jolpica" }
    }

    /** The calendar is fixed at season start - fetch once, keep for a week. */
    suspend fun refreshRaces(force: Boolean = false) {
        val age = dao.racesFetchedAt()
        if (!force && age != null && System.currentTimeMillis() - age < 7 * DAY) return
        runCatching {
            limiter.acquire()
            val r = Net.jolpica.races(current, 30)
            val now = System.currentTimeMillis()
            dao.putRaces(r.data.races?.races.orEmpty().map { x ->
                RaceEntity(
                    id = "${x.season}-${x.round}", season = x.season, round = x.round.toIntOrNull() ?: 0,
                    name = x.raceName, date = x.date, time = x.time,
                    circuitId = x.circuit.circuitId, circuitName = x.circuit.circuitName,
                    locality = x.circuit.location.locality, country = x.circuit.location.country,
                    fp1 = x.fp1.millis(), fp2 = x.fp2.millis(), fp3 = x.fp3.millis(),
                    quali = x.quali.millis(), sprintQuali = x.sprintQuali.millis(), sprint = x.sprint.millis(),
                    fetchedAt = now,
                )
            })
        }.onFailure { error.value = it.message }
    }

    /** team_colour is not in Jolpica - it's an OpenF1 field. Since OpenF1 is
     *  unreachable during live sessions, these are pinned locally. 11 teams,
     *  changes once a year: a constant is more reliable than a request. */
    /** Pinned constructor facts (like teamColour) - names/base/PU/titles the API omits. */
    fun constructorInfo(teamId: String): ConstructorInfo = when (teamId) {
        "mercedes" -> ConstructorInfo("Mercedes-AMG Petronas", "Brackley, UK", "Mercedes", 1954, 8)
        "ferrari" -> ConstructorInfo("Scuderia Ferrari", "Maranello, Italy", "Ferrari", 1950, 16)
        "mclaren" -> ConstructorInfo("McLaren F1 Team", "Woking, UK", "Mercedes", 1966, 9)
        "red_bull" -> ConstructorInfo("Oracle Red Bull Racing", "Milton Keynes, UK", "Red Bull Ford", 2005, 6)
        "aston_martin" -> ConstructorInfo("Aston Martin Aramco", "Silverstone, UK", "Honda", 2021, 0)
        "alpine" -> ConstructorInfo("BWT Alpine F1 Team", "Enstone, UK", "Mercedes", 1986, 2)
        "williams" -> ConstructorInfo("Williams Racing", "Grove, UK", "Mercedes", 1977, 9)
        "rb" -> ConstructorInfo("Visa Cash App RB", "Faenza, Italy", "Red Bull Ford", 2020, 0)
        "haas" -> ConstructorInfo("MoneyGram Haas F1", "Kannapolis, USA", "Ferrari", 2016, 0)
        "audi" -> ConstructorInfo("Audi F1 Team", "Hinwil, Switzerland", "Audi", 2026, 0)
        "cadillac" -> ConstructorInfo("Cadillac F1 Team", "USA", "Ferrari", 2026, 0)
        else -> ConstructorInfo(teamId, "—", "—", 0, 0)
    }

    fun teamColour(teamId: String): String = when (teamId) {
        "mclaren" -> "F47600"; "red_bull" -> "4781D7"; "ferrari" -> "ED1131"
        "mercedes" -> "00D7B6"; "aston_martin" -> "229971"; "alpine" -> "00A1E8"
        "williams" -> "1868DB"; "rb" -> "6C98FF"; "haas" -> "9C9FA2"
        "audi" -> "F50537"; "cadillac" -> "909090"; else -> "888888"
    }

    /** F1's CDN path is not derivable from driverId - the folder letter and the
     *  code disagree (Antonelli lives under K/ but is coded ANDANT01). Pinned. */
    fun headshotKey(driverId: String): String? = HEADSHOTS[driverId]

    /** Jolpica teamId -> the folder name F1's CDN uses for that team's car image. Measured 2026-11-14:
     *  all eleven return a transparent image. */
    fun carFolder(teamId: String): String? = when (teamId) {
        "mercedes", "ferrari", "mclaren", "alpine", "williams", "haas", "audi", "cadillac" -> teamId
        "red_bull" -> "redbullracing"; "aston_martin" -> "astonmartin"; "rb" -> "racingbulls"
        else -> null
    }

    /** Ergast circuitId -> MultiViewer circuit key, or null when MultiViewer has no outline
     *  (the screen then says so instead of drawing something wrong). */
    fun circuitKey(circuitId: String): Int? = CIRCUIT_KEYS[circuitId]

    suspend fun circuit(circuitId: String) = dao.circuit(circuitId)

    /** Geometry is immutable - fetch once, keep forever. Decimated to every 2nd point
     *  (harder loses La Source and the Bus Stop chicane). Rotation stored, applied at draw.
     *  The length comes from the full outline, before decimation. Eau Rouge -> Raidillon is
     *  derived from corners 2..5, which is only true at Spa, so every other circuit gets none. */
    suspend fun refreshCircuit(circuitId: String, year: Int) {
        if (dao.circuit(circuitId) != null) return
        val key = circuitKey(circuitId) ?: return
        runCatching {
            val c = Net.multiviewer.circuit(key, year)
            val xs = c.x.filterIndexed { i, _ -> i % 2 == 0 }
            val ys = c.y.filterIndexed { i, _ -> i % 2 == 0 }
            fun nearest(pos: MvPos): Int = xs.indices.minByOrNull { i ->
                val dx = xs[i] - pos.x; val dy = ys[i] - pos.y; dx * dx + dy * dy
            } ?: 0
            fun cornerPos(n: Int) = c.corners.firstOrNull { it.number == n }?.pos
            val a = cornerPos(2)?.let { nearest(it) } ?: 0
            val b = cornerPos(5)?.let { nearest(it) } ?: 0
            val spa = circuitId == "spa"
            dao.putCircuit(CircuitGeometryEntity(
                circuitId, c.rotation,
                xs = xs.joinToString(",") { it.toString() },
                ys = ys.joinToString(",") { it.toString() },
                corners = c.corners.size,
                eauStart = if (spa) minOf(a, b) else 0, eauEnd = if (spa) maxOf(a, b) else 0,
                lengthKm = trackLengthKm(c.x, c.y),
                fetchedAt = System.currentTimeMillis()))
        }.onFailure { error.value = it.message ?: "Couldn't reach MultiViewer" }
    }

    fun archive(season: String, kind: Int): Flow<List<ArchiveStandingEntity>> = dao.archive(season, kind)

    /**
     * Final standings for a past season, one request per table, kept forever. Both tables are fetched
     * together so the toggle never waits. There is no constructors' table before 1958.
     */
    private fun NewsEntity.toItem() = NewsItem(source, title, snippet, imageUrl, publishedAt, url, rank)

    /** Press headlines merged across the eight sources, newest first. */
    fun news(): Flow<List<NewsItem>> = dao.news().map { rows ->
        mergeNews(rows.filter { !it.source.startsWith(REDDIT_PREFIX) }.map { it.toItem() })
    }.flowOn(Dispatchers.Default)

    /** One subreddit in Reddit's own hot order. */
    fun reddit(sub: String): Flow<List<NewsItem>> = dao.news().map { rows ->
        rows.filter { it.source == REDDIT_PREFIX + sub }.map { it.toItem() }.sortedBy { it.rank }
    }.flowOn(Dispatchers.Default)

    private val newsPrefs = ctx.getSharedPreferences("news", Context.MODE_PRIVATE)
    fun redditChoice(): String = newsPrefs.getString("subreddit", null)?.takeIf { it in SUBREDDITS } ?: SUBREDDITS.first()
    fun setRedditChoice(sub: String) { if (sub in SUBREDDITS) newsPrefs.edit().putString("subreddit", sub).apply() }
    /** Set when Reddit refused or failed the last request, so the tab can say so and keep showing the old copy. */
    val redditProblem = MutableStateFlow<String?>(null)

    /** Reddit allows about one request a minute without an account, so this fetches only the chosen subreddit,
     *  and not at all while its copy is under 15 minutes old. */
    suspend fun refreshReddit(sub: String, force: Boolean = false) {
        if (sub !in SUBREDDITS) return
        val age = dao.newsFetchedAt(REDDIT_PREFIX + sub)
        if (!force && age != null && System.currentTimeMillis() - age < NEWS_TTL) return
        newsLoading.value = true
        runCatching {
            val posts = parseReddit(sub, Net.feed(redditUrl(sub)))
            val now = System.currentTimeMillis()
            dao.putNews(posts.map { NewsEntity(it.url, it.source, it.title, it.snippet, it.imageUrl, it.publishedAt, it.rank, now) })
            dao.dropOldNews(REDDIT_PREFIX + sub, now)
            redditProblem.value = null
        }.onFailure { redditProblem.value = if (it.message?.contains("429") == true) "Reddit asked for a pause. Try again in a minute." else "Couldn't reach Reddit." }
        newsLoading.value = false
    }
    val newsLoading = MutableStateFlow(false)
    /** Sources that failed on the last refresh, so the tab can say so quietly and still show the rest. */
    val newsFailed = MutableStateFlow<List<String>>(emptyList())

    /** Fetched only when the News tab opens (the app does nothing in the background), and a source newer than
     *  15 minutes is left alone. Each source fails on its own and keeps its old rows. */
    suspend fun refreshNews(force: Boolean = false) {
        newsLoading.value = true
        val failed = mutableListOf<String>()
        coroutineScope {
            FEEDS.map { feed ->
                async {
                    val age = dao.newsFetchedAt(feed.name)
                    if (!force && age != null && System.currentTimeMillis() - age < NEWS_TTL) return@async
                    runCatching {
                        val items = parseFeed(feed, Net.feed(feed.url))
                        val now = System.currentTimeMillis()
                        dao.putNews(items.map { NewsEntity(it.url, it.source, it.title, it.snippet, it.imageUrl, it.publishedAt, it.rank, now) })
                        dao.dropOldNews(feed.name, now)
                    }.onFailure { synchronized(failed) { failed += feed.name } }
                }
            }.awaitAll()
        }
        newsFailed.value = failed.sorted()
        newsLoading.value = false
    }

    fun seasonResults(): Flow<List<RaceResultEntity>> = dao.seasonResults()
    fun career(kind: Int, id: String): Flow<CareerEntity?> = dao.career(careerKey(kind, id))
    fun history(kind: Int, id: String): Flow<List<ArchiveStandingEntity>> = dao.history(kind, id)

    /** Totals for one driver (five requests) or team (four). Refreshed weekly while they are active. */
    suspend fun refreshCareer(kind: Int, id: String) {
        val key = careerKey(kind, id)
        val old = dao.career(key).firstOrNull()
        if (old != null && System.currentTimeMillis() - old.fetchedAt < 7 * DAY) return
        val scope = careerScope(kind)
        runCatching {
            suspend fun total(call: suspend () -> JolpicaEnvelope): Int { limiter.acquire(); return call().data.total?.toIntOrNull() ?: 0 }
            val wins = total { Net.jolpica.careerResults(scope, id, 1) }
            val second = total { Net.jolpica.careerResults(scope, id, 2) }
            val third = total { Net.jolpica.careerResults(scope, id, 3) }
            // Qualifying results start in 1994 (Ferrari shows 126 against its real 250 or so), so only drivers get poles.
            val poles = if (kind == KIND_DRIVER) total { Net.jolpica.careerPoles(scope, id) } else 0
            limiter.acquire()
            val years = Net.jolpica.careerSeasons(scope, id).data.seasonTable?.seasons.orEmpty().joinToString(",") { it.season }
            dao.putCareer(CareerEntity(key, wins, wins + second + third, poles, years, System.currentTimeMillis()))
            error.value = null
        }.onFailure { error.value = it.message ?: "Couldn't reach Jolpica" }
    }

    /** Fetches the final tables this career is missing, one request each, then keeps them for good. */
    suspend fun refreshHistory(kind: Int, id: String) {
        val career = careerSeasons(dao.career(careerKey(kind, id)).firstOrNull())
        val have = dao.history(kind, id).firstOrNull().orEmpty().map { it.season.toInt() }.toSet()
        val currentYear = dao.currentSeason()?.toIntOrNull()
        runCatching {
            // Rows are kept per season, so "have" must mean the season is saved, not that this entity is in it.
            missingSeasons(kind, career, have, currentYear).filter { dao.archiveCount(it.toString(), kind) == 0 }.forEach { fetchTable(it.toString(), kind) }
            error.value = null
        }.onFailure { error.value = it.message ?: "Couldn't reach Jolpica" }
    }

    private suspend fun fetchTable(season: String, kind: Int) {
        limiter.acquire()
        val now = System.currentTimeMillis()
        if (kind == KIND_DRIVER) {
            Net.jolpica.driverStandings(season, 100).data.standings?.lists?.firstOrNull()?.let { dao.putArchive(driverArchive(it, now)) }
        } else {
            Net.jolpica.constructorStandings(season, 100).data.standings?.lists?.firstOrNull()?.let { dao.putArchive(teamArchive(it, now)) }
        }
    }

    suspend fun refreshArchive(season: String) {
        val year = season.toIntOrNull() ?: return
        runCatching {
            if (dao.archiveCount(season, KIND_DRIVER) == 0) {
                limiter.acquire()
                val list = Net.jolpica.driverStandings(season, 100).data.standings?.lists?.firstOrNull()
                if (list != null) dao.putArchive(driverArchive(list, System.currentTimeMillis()))
            }
            if (year >= FIRST_CONSTRUCTORS_SEASON && dao.archiveCount(season, KIND_TEAM) == 0) {
                limiter.acquire()
                val list = Net.jolpica.constructorStandings(season, 100).data.standings?.lists?.firstOrNull()
                if (list != null) dao.putArchive(teamArchive(list, System.currentTimeMillis()))
            }
            error.value = null
        }.onFailure { error.value = it.message ?: "Couldn't reach Jolpica" }
    }

    fun raceResult(season: String, round: Int): Flow<List<RaceResultEntity>> = dao.raceResult(season, round)
    fun winners(circuitId: String): Flow<List<CircuitWinnerEntity>> = dao.winners(circuitId)

    /** Scheduled laps and past winners, from one request. Kept forever once known; a circuit with no
     *  history yet (0) is retried weekly. */
    suspend fun refreshCircuitHistory(circuitId: String) {
        val known = dao.circuitFact(circuitId)
        if (known != null && (known.laps > 0 || System.currentTimeMillis() - known.fetchedAt < 7 * DAY)) return
        runCatching {
            limiter.acquire()
            val winners = Net.jolpica.circuitWinners(circuitId).data.races?.races.orEmpty()
            dao.putWinners(winnerRows(circuitId, winners))
            dao.putCircuitFact(CircuitFactEntity(circuitId, scheduledLaps(winners) ?: 0, System.currentTimeMillis()))
        }.onFailure { error.value = it.message ?: "Couldn't reach Jolpica" }
    }

    /** Null when unknown, so the screen can leave the tile out instead of showing a wrong number. */
    suspend fun circuitLaps(circuitId: String): Int? = dao.circuitFact(circuitId)?.laps?.takeIf { it > 0 }

    companion object {
        /** Ergast circuitId -> MultiViewer key. Measured 2026-11-14: each of these returns a 2026
         *  outline. Madring and Sepang have none, so they are absent and the screen says so. */
        private val CIRCUIT_KEYS = mapOf(
            "albert_park" to 10, "shanghai" to 49, "suzuka" to 46, "miami" to 151, "villeneuve" to 23,
            "monaco" to 22, "catalunya" to 15, "red_bull_ring" to 19, "silverstone" to 2, "spa" to 7,
            "hungaroring" to 4, "zandvoort" to 55, "monza" to 39, "baku" to 144, "marina_bay" to 61,
            "americas" to 9, "rodriguez" to 65, "interlagos" to 14, "vegas" to 152, "losail" to 150,
            "yas_marina" to 70,
        )

        private val HEADSHOTS = mapOf(
            "antonelli" to "K/ANDANT01_Kimi_Antonelli/andant01",
            "russell" to "G/GEORUS01_George_Russell/georus01",
            "hamilton" to "L/LEWHAM01_Lewis_Hamilton/lewham01",
            "leclerc" to "C/CHALEC01_Charles_Leclerc/chalec01",
            "norris" to "L/LANNOR01_Lando_Norris/lannor01",
            "piastri" to "O/OSCPIA01_Oscar_Piastri/oscpia01",
            "max_verstappen" to "M/MAXVER01_Max_Verstappen/maxver01",
            "hadjar" to "I/ISAHAD01_Isack_Hadjar/isahad01",
            "gasly" to "P/PIEGAS01_Pierre_Gasly/piegas01",
            "lawson" to "L/LIALAW01_Liam_Lawson/lialaw01",
            "arvid_lindblad" to "A/ARVLIN01_Arvid_Lindblad/arvlin01",
            "bearman" to "O/OLIBEA01_Oliver_Bearman/olibea01",
            "colapinto" to "F/FRACOL01_Franco_Colapinto/fracol01",
            "bortoleto" to "G/GABBOR01_Gabriel_Bortoleto/gabbor01",
            "sainz" to "C/CARSAI01_Carlos_Sainz/carsai01",
            "albon" to "A/ALEALB01_Alexander_Albon/alealb01",
            "ocon" to "E/ESTOCO01_Esteban_Ocon/estoco01",
            "alonso" to "F/FERALO01_Fernando_Alonso/feralo01",
            "hulkenberg" to "N/NICHUL01_Nico_Hulkenberg/nichul01",
            "bottas" to "V/VALBOT01_Valtteri_Bottas/valbot01",
            "perez" to "S/SERPER01_Sergio_Perez/serper01",
            "stroll" to "L/LANSTR01_Lance_Stroll/lanstr01",
        )
    }
}

private fun JSession?.millis(): Long? = this?.let { utcMillis(it.date, it.time) }
