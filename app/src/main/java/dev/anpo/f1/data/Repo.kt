package dev.anpo.f1.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

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
    fun teamColour(teamId: String): String = when (teamId) {
        "mclaren" -> "F47600"; "red_bull" -> "4781D7"; "ferrari" -> "ED1131"
        "mercedes" -> "00D7B6"; "aston_martin" -> "229971"; "alpine" -> "00A1E8"
        "williams" -> "1868DB"; "rb" -> "6C98FF"; "haas" -> "9C9FA2"
        "audi" -> "F50537"; "cadillac" -> "909090"; else -> "888888"
    }

    /** F1's CDN path is not derivable from driverId - the folder letter and the
     *  code disagree (Antonelli lives under K/ but is coded ANDANT01). Pinned. */
    fun headshotKey(driverId: String): String? = HEADSHOTS[driverId]

    companion object {
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
