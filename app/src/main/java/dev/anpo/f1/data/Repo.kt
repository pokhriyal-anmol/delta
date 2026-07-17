package dev.anpo.f1.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Static per-circuit facts the geometry API doesn't carry. */
data class CircuitFacts(val lengthKm: String, val laps: Int, val topSpeed: String, val elevation: String)

/** Static constructor facts (name/base/power unit/history) not in the standings API. */
data class ConstructorInfo(val full: String, val base: String, val pu: String, val since: Int, val champs: Int)

class Repo(ctx: Context) {
    private val dao = FeedsDb.get(ctx).dao()
    private val limiter = RateLimiter(ctx, "jolpica", capacity = 200, refillPerHour = 200)
    val season = "2026"

    val error = MutableStateFlow<String?>(null)
    val loading = MutableStateFlow(false)

    fun drivers(): Flow<List<DriverStandingEntity>> = dao.drivers(season)
    fun teams(): Flow<List<ConstructorStandingEntity>> = dao.teams(season)
    fun races(): Flow<List<RaceEntity>> = dao.races(season)
    suspend fun driver(id: String) = dao.driver(id)
    suspend fun driverStat(id: String) = dao.driverStat(id)
    fun tokensLeft() = limiter.remaining()

    private val DAY = 86_400_000L

    /** Current-season standings change only after a race. Daily TTL is plenty and
     *  keeps us at roughly 20 requests a WEEK against a 200/hour ceiling. */
    suspend fun refreshStandings(force: Boolean = false) {
        val age = dao.driversFetchedAt(season)
        if (!force && age != null && System.currentTimeMillis() - age < DAY) return
        runCatching {
            loading.value = true
            limiter.acquire()
            val d = Net.jolpica.driverStandings(season, 30)
            val now = System.currentTimeMillis()
            d.data.standings?.lists?.firstOrNull()?.let { l ->
                dao.putDrivers(l.drivers.map { s ->
                    DriverStandingEntity(
                        driverId = s.driver.driverId, season = l.season, round = l.round,
                        position = s.position.toIntOrNull() ?: 99, points = s.points.toIntOrNull() ?: 0,
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
            val c = Net.jolpica.constructorStandings(season, 30)
            c.data.standings?.lists?.firstOrNull()?.let { l ->
                dao.putTeams(l.constructors.map { s ->
                    ConstructorStandingEntity(
                        teamId = s.team.constructorId, season = l.season,
                        position = s.position.toIntOrNull() ?: 99, points = s.points.toIntOrNull() ?: 0,
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
        val age = dao.statsFetchedAt(season)
        if (!force && age != null && System.currentTimeMillis() - age < DAY) return
        runCatching {
            // Jolpica caps limit at 100, so page through with offset until we have `total`.
            val races = mutableListOf<JRace>()
            var offset = 0; var total = Int.MAX_VALUE
            while (offset < total) {
                limiter.acquire()
                val page = Net.jolpica.seasonResults(season, 100, offset)
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
            val maxRound = races.mapNotNull { it.round.toIntOrNull() }.maxOrNull() ?: 0
            if (agg.isNotEmpty()) dao.putStats(agg.map { (id, a) ->
                val rounds = perRound[id]
                val csv = (1..maxRound).joinToString(",") { (rounds?.get(it) ?: 0).toString() }
                DriverSeasonStat(id, season, podiums = a[0], poles = a[1], dnfs = a[2],
                    roundPoints = csv, fetchedAt = now)
            })
            error.value = null
        }.onFailure { error.value = it.message ?: "Couldn't reach Jolpica" }
    }

    /** The calendar is fixed at season start - fetch once, keep for a week. */
    suspend fun refreshRaces(force: Boolean = false) {
        val age = dao.racesFetchedAt(season)
        if (!force && age != null && System.currentTimeMillis() - age < 7 * DAY) return
        runCatching {
            limiter.acquire()
            val r = Net.jolpica.races(season, 30)
            val now = System.currentTimeMillis()
            dao.putRaces(r.data.races?.races.orEmpty().map { x ->
                RaceEntity(
                    id = "${x.season}-${x.round}", season = x.season, round = x.round.toIntOrNull() ?: 0,
                    name = x.raceName, date = x.date, time = x.time,
                    circuitId = x.circuit.circuitId, circuitName = x.circuit.circuitName,
                    locality = x.circuit.location.locality, country = x.circuit.location.country,
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

    /** Ergast circuitId -> MultiViewer circuit key. Spa=7 is confirmed; the rest of
     *  the 2026 calendar still needs verifying against the MV circuits list, so an
     *  unknown id returns null and the circuit hero is simply skipped (never wrong). */
    fun circuitKey(circuitId: String): Int? = CIRCUIT_KEYS[circuitId]

    suspend fun circuit(circuitId: String) = dao.circuit(circuitId)

    /** Static circuit facts not carried by the geometry endpoint (length/laps/top
     *  speed/elevation). Turns come from the corner count instead. Spa only for now,
     *  same pinned-data pattern as teamColour/headshotKey. */
    fun circuitFacts(circuitId: String): CircuitFacts? = when (circuitId) {
        "spa" -> CircuitFacts(lengthKm = "7.004", laps = 44, topSpeed = "359.7", elevation = "102.2")
        else -> null
    }

    /** Geometry is immutable - fetch once, keep forever. Decimated to every 2nd point
     *  (harder loses La Source and the Bus Stop chicane). Rotation stored, applied at draw.
     *  Eau Rouge -> Raidillon is derived from corners 2..5, not magic point indices. */
    suspend fun refreshCircuit(circuitId: String) {
        if (dao.circuit(circuitId) != null) return
        val key = circuitKey(circuitId) ?: return
        runCatching {
            val c = Net.multiviewer.circuit(key)
            val xs = c.x.filterIndexed { i, _ -> i % 2 == 0 }
            val ys = c.y.filterIndexed { i, _ -> i % 2 == 0 }
            fun nearest(pos: MvPos): Int = xs.indices.minByOrNull { i ->
                val dx = xs[i] - pos.x; val dy = ys[i] - pos.y; dx * dx + dy * dy
            } ?: 0
            fun cornerPos(n: Int) = c.corners.firstOrNull { it.number == n }?.pos
            val a = cornerPos(2)?.let { nearest(it) } ?: 0
            val b = cornerPos(5)?.let { nearest(it) } ?: 0
            dao.putCircuit(CircuitGeometryEntity(
                circuitId, c.rotation,
                xs = xs.joinToString(",") { it.toString() },
                ys = ys.joinToString(",") { it.toString() },
                corners = c.corners.size,
                eauStart = minOf(a, b), eauEnd = maxOf(a, b),
                fetchedAt = System.currentTimeMillis()))
        }.onFailure { error.value = it.message ?: "Couldn't reach MultiViewer" }
    }

    companion object {
        /** MultiViewer circuit keys. Spa=7 is verified; extend as the other 2026
         *  circuitIds are confirmed (same endpoint, swap the key). */
        private val CIRCUIT_KEYS = mapOf("spa" to 7)

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
