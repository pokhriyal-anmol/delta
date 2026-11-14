package dev.anpo.f1.data

import androidx.room.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/* ---------- Jolpica wire format (Ergast-compatible) ---------- */

@Serializable data class JolpicaEnvelope(@SerialName("MRData") val data: MRData)
@Serializable data class MRData(
    val total: String? = null,
    @SerialName("StandingsTable") val standings: StandingsTable? = null,
    @SerialName("RaceTable") val races: RaceTable? = null,
    @SerialName("SeasonTable") val seasonTable: SeasonTable? = null,
)
@Serializable data class SeasonTable(@SerialName("Seasons") val seasons: List<JSeason> = emptyList())
@Serializable data class JSeason(val season: String)
@Serializable data class StandingsTable(val season: String? = null, val round: String? = null,
    @SerialName("StandingsLists") val lists: List<StandingsList> = emptyList())
@Serializable data class StandingsList(val season: String, val round: String,
    @SerialName("DriverStandings") val drivers: List<DriverStanding> = emptyList(),
    @SerialName("ConstructorStandings") val constructors: List<ConstructorStanding> = emptyList())
@Serializable data class DriverStanding(val position: String? = null, val positionText: String? = null,
    val points: String, val wins: String,
    @SerialName("Driver") val driver: JDriver, @SerialName("Constructors") val teams: List<JTeam> = emptyList())
@Serializable data class ConstructorStanding(val position: String? = null, val positionText: String? = null,
    val points: String, val wins: String,
    @SerialName("Constructor") val team: JTeam)
@Serializable data class JDriver(val driverId: String, val permanentNumber: String? = null,
    val code: String? = null, val givenName: String, val familyName: String, val nationality: String? = null)
@Serializable data class JTeam(val constructorId: String, val name: String, val nationality: String? = null)
@Serializable data class RaceTable(val season: String? = null, @SerialName("Races") val races: List<JRace> = emptyList())
@Serializable data class JRace(val season: String, val round: String, val raceName: String,
    val date: String, val time: String? = null, @SerialName("Circuit") val circuit: JCircuit,
    @SerialName("FirstPractice") val fp1: JSession? = null,
    @SerialName("SecondPractice") val fp2: JSession? = null,
    @SerialName("ThirdPractice") val fp3: JSession? = null,
    @SerialName("Qualifying") val quali: JSession? = null,
    @SerialName("SprintQualifying") val sprintQuali: JSession? = null,
    @SerialName("Sprint") val sprint: JSession? = null,
    @SerialName("Results") val results: List<JResult> = emptyList())
@Serializable data class JSession(val date: String, val time: String? = null)
@Serializable data class JResult(val position: String, val positionText: String,
    val points: String, val grid: String, val status: String? = null, val laps: String? = null,
    @SerialName("Driver") val driver: JDriver,
    @SerialName("Constructor") val team: JTeam? = null,
    @SerialName("Time") val time: JResultTime? = null)
/** The winner's total time, or a gap such as "+5.123" for everyone else on the lead lap. */
@Serializable data class JResultTime(val time: String? = null)
@Serializable data class JCircuit(val circuitId: String, val circuitName: String,
    @SerialName("Location") val location: JLocation)
@Serializable data class JLocation(val locality: String? = null, val country: String? = null)

/* ---------- MultiViewer wire format (circuit geometry) ---------- */

@Serializable data class MvCircuit(
    val x: List<Float> = emptyList(), val y: List<Float> = emptyList(), val rotation: Float = 0f,
    val corners: List<MvCorner> = emptyList())
@Serializable data class MvCorner(val number: Int = 0, @SerialName("trackPosition") val pos: MvPos? = null)
@Serializable data class MvPos(val x: Float = 0f, val y: Float = 0f)

/* ---------- Room: feeds.db. Disposable cache, never the source of truth. ---------- */

@Entity(tableName = "driver_standing")
data class DriverStandingEntity(
    @PrimaryKey val driverId: String,
    val season: String, val round: String, val position: Int, val points: Int, val wins: Int,
    val given: String, val family: String, val code: String?, val number: Int?,
    val team: String, val teamId: String,
    val fetchedAt: Long,
)

/** Derived per-driver season aggregates, computed from /results (which the standings
 *  endpoint doesn't carry). Poles are grid-P1 counts - a fair proxy without a second
 *  call to /qualifying; DNF = a non-numeric classification (positionText "R"/"D"/...). */
@Entity(tableName = "driver_season_stat")
data class DriverSeasonStat(
    @PrimaryKey val driverId: String,
    val season: String, val podiums: Int, val poles: Int, val dnfs: Int,
    /** Points scored each round, in order, as CSV ("0,25,18,..."). Drives the bar chart. */
    val roundPoints: String,
    val fetchedAt: Long,
)

@Entity(tableName = "constructor_standing")
data class ConstructorStandingEntity(
    @PrimaryKey val teamId: String,
    val season: String, val position: Int, val points: Int, val wins: Int, val name: String,
    val fetchedAt: Long,
)

/** Circuit outline from MultiViewer, decimated to every 2nd point and stored as CSV.
 *  Track geometry is immutable, so this is fetched once and kept forever (no TTL).
 *  Rotation is applied and the path normalised to the canvas at render time. */
@Entity(tableName = "circuit_geometry")
data class CircuitGeometryEntity(
    @PrimaryKey val circuitId: String,
    val rotation: Float, val xs: String, val ys: String,
    /** Turns = corner count. eauStart/eauEnd are decimated-point indices for the
     *  Eau Rouge -> Raidillon stretch, derived from corners 2..5 at fetch time. */
    val corners: Int, val eauStart: Int, val eauEnd: Int,
    /** Derived from the full outline, see trackLengthKm. */
    val lengthKm: Double,
    val fetchedAt: Long,
)

/**
 * Final standings of a past season, kept forever (a 1988 table will never change). The current season
 * lives in driver_standing and constructor_standing instead, which refresh daily.
 * kind is KIND_DRIVER or KIND_TEAM. title is a driver's surname or a team's name, subtitle a driver's team.
 */
@Entity(tableName = "archive_standing", primaryKeys = ["season", "kind", "entityId"])
data class ArchiveStandingEntity(
    val season: String, val kind: Int, val entityId: String,
    val position: Int, val points: Double, val wins: Int,
    val title: String, val subtitle: String, val teamId: String,
    val fetchedAt: Long,
)

/** One finisher in one race of the current season. position is always numeric (Jolpica numbers retirements too); positionText is the label: a number, or R, D, W. */
@Entity(tableName = "race_result", primaryKeys = ["season", "round", "driverId"])
data class RaceResultEntity(
    val season: String, val round: Int, val driverId: String,
    val position: Int, val positionText: String,
    val driverName: String, val code: String?, val teamId: String, val teamName: String,
    val grid: Int, val laps: Int, val status: String, val time: String?, val points: Double,
)

/**
 * A driver's or team's career totals. key is "d:" or "t:" plus the id. seasons is the
 * comma-separated list of years they took part in. Wins, podiums and poles are counts of results rows, so a
 * team's podiums count each car.
 */
@Entity(tableName = "career")
data class CareerEntity(
    @PrimaryKey val key: String, val wins: Int, val podiums: Int, val poles: Int,
    val seasons: String, val fetchedAt: Long,
)

/** Who won each past race at a circuit. Kept once fetched, since history does not change. */
@Entity(tableName = "circuit_winner", primaryKeys = ["circuitId", "season"])
data class CircuitWinnerEntity(
    val circuitId: String, val season: String, val round: Int, val raceName: String,
    val driverId: String, val driverName: String, val teamName: String, val laps: Int,
)

/** Scheduled lap count for a circuit, from past winners. 0 means "none known" (a new circuit). */
@Entity(tableName = "circuit_fact")
data class CircuitFactEntity(@PrimaryKey val circuitId: String, val laps: Int, val fetchedAt: Long)

@Entity(tableName = "race")
data class RaceEntity(
    @PrimaryKey val id: String,               // "2026-10"
    val season: String, val round: Int, val name: String,
    val date: String, val time: String?,
    val circuitId: String, val circuitName: String, val locality: String?, val country: String?,
    /** Session starts as UTC epoch millis. Null when the calendar does not list that session. */
    val fp1: Long?, val fp2: Long?, val fp3: Long?, val quali: Long?, val sprintQuali: Long?, val sprint: Long?,
    val fetchedAt: Long,
)
