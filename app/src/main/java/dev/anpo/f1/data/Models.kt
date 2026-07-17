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
)
@Serializable data class StandingsTable(val season: String? = null, val round: String? = null,
    @SerialName("StandingsLists") val lists: List<StandingsList> = emptyList())
@Serializable data class StandingsList(val season: String, val round: String,
    @SerialName("DriverStandings") val drivers: List<DriverStanding> = emptyList(),
    @SerialName("ConstructorStandings") val constructors: List<ConstructorStanding> = emptyList())
@Serializable data class DriverStanding(val position: String, val points: String, val wins: String,
    @SerialName("Driver") val driver: JDriver, @SerialName("Constructors") val teams: List<JTeam> = emptyList())
@Serializable data class ConstructorStanding(val position: String, val points: String, val wins: String,
    @SerialName("Constructor") val team: JTeam)
@Serializable data class JDriver(val driverId: String, val permanentNumber: String? = null,
    val code: String? = null, val givenName: String, val familyName: String, val nationality: String? = null)
@Serializable data class JTeam(val constructorId: String, val name: String, val nationality: String? = null)
@Serializable data class RaceTable(val season: String? = null, @SerialName("Races") val races: List<JRace> = emptyList())
@Serializable data class JRace(val season: String, val round: String, val raceName: String,
    val date: String, val time: String? = null, @SerialName("Circuit") val circuit: JCircuit)
@Serializable data class JCircuit(val circuitId: String, val circuitName: String,
    @SerialName("Location") val location: JLocation)
@Serializable data class JLocation(val locality: String? = null, val country: String? = null)

/* ---------- Room: feeds.db. Disposable cache, never the source of truth. ---------- */

@Entity(tableName = "driver_standing")
data class DriverStandingEntity(
    @PrimaryKey val driverId: String,
    val season: String, val round: String, val position: Int, val points: Int, val wins: Int,
    val given: String, val family: String, val code: String?, val number: Int?,
    val team: String, val teamId: String,
    val fetchedAt: Long,
)

@Entity(tableName = "constructor_standing")
data class ConstructorStandingEntity(
    @PrimaryKey val teamId: String,
    val season: String, val position: Int, val points: Int, val wins: Int, val name: String,
    val fetchedAt: Long,
)

@Entity(tableName = "race")
data class RaceEntity(
    @PrimaryKey val id: String,               // "2026-10"
    val season: String, val round: Int, val name: String,
    val date: String, val time: String?,
    val circuitId: String, val circuitName: String, val locality: String?, val country: String?,
    val fetchedAt: Long,
) {
    /** Historical F1 data is immutable - a 2019 result will never change.
     *  Only the current season needs a TTL. This is what keeps us inside
     *  Jolpica's 200 req/hour. */
    val isImmutable: Boolean get() = season.toIntOrNull()?.let { it < CURRENT_SEASON } ?: false
    companion object { const val CURRENT_SEASON = 2026 }
}
