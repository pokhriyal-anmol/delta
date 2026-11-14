package dev.anpo.f1.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao interface F1Dao {
    // "Current" means the newest season held in the cache, so the app rolls over by itself when
    // Jolpica's `current` alias moves on. Old seasons stay in the table and are simply not read.
    @Query("SELECT * FROM driver_standing WHERE season = (SELECT MAX(season) FROM driver_standing) ORDER BY position") fun drivers(): Flow<List<DriverStandingEntity>>
    @Query("SELECT * FROM constructor_standing WHERE season = (SELECT MAX(season) FROM constructor_standing) ORDER BY position") fun teams(): Flow<List<ConstructorStandingEntity>>
    @Query("SELECT * FROM race WHERE season = (SELECT MAX(season) FROM race) ORDER BY round") fun races(): Flow<List<RaceEntity>>
    @Query("SELECT * FROM driver_standing WHERE driverId = :id ORDER BY season DESC LIMIT 1") suspend fun driver(id: String): DriverStandingEntity?
    @Query("SELECT * FROM driver_season_stat WHERE driverId = :id ORDER BY season DESC LIMIT 1") suspend fun driverStat(id: String): DriverSeasonStat?
    @Query("SELECT * FROM circuit_geometry WHERE circuitId = :id LIMIT 1") suspend fun circuit(id: String): CircuitGeometryEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putCircuit(x: CircuitGeometryEntity)
    @Query("SELECT * FROM archive_standing WHERE season = :s AND kind = :k ORDER BY position") fun archive(s: String, k: Int): Flow<List<ArchiveStandingEntity>>
    @Query("SELECT COUNT(*) FROM archive_standing WHERE season = :s AND kind = :k") suspend fun archiveCount(s: String, k: Int): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putArchive(x: List<ArchiveStandingEntity>)
    @Query("SELECT * FROM circuit_fact WHERE circuitId = :id LIMIT 1") suspend fun circuitFact(id: String): CircuitFactEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putCircuitFact(x: CircuitFactEntity)
    @Query("SELECT * FROM race_result WHERE season = :s AND round = :r ORDER BY position, grid") fun raceResult(s: String, r: Int): Flow<List<RaceResultEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putRaceResults(x: List<RaceResultEntity>)
    @Query("SELECT * FROM race_result WHERE season = (SELECT MAX(season) FROM race_result) ORDER BY round") fun seasonResults(): Flow<List<RaceResultEntity>>
    @Query("SELECT * FROM career WHERE `key` = :key LIMIT 1") fun career(key: String): Flow<CareerEntity?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putCareer(x: CareerEntity)
    @Query("SELECT * FROM archive_standing WHERE kind = :kind AND entityId = :id ORDER BY season DESC") fun history(kind: Int, id: String): Flow<List<ArchiveStandingEntity>>
    @Query("SELECT MAX(season) FROM race") suspend fun currentSeason(): String?
    @Query("SELECT * FROM circuit_winner WHERE circuitId = :id ORDER BY season DESC") fun winners(id: String): Flow<List<CircuitWinnerEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putWinners(x: List<CircuitWinnerEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putDrivers(x: List<DriverStandingEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putTeams(x: List<ConstructorStandingEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putRaces(x: List<RaceEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putStats(x: List<DriverSeasonStat>)
    @Query("SELECT MIN(fetchedAt) FROM driver_standing WHERE season = (SELECT MAX(season) FROM driver_standing)") suspend fun driversFetchedAt(): Long?
    @Query("SELECT MIN(fetchedAt) FROM race WHERE season = (SELECT MAX(season) FROM race)") suspend fun racesFetchedAt(): Long?
    @Query("SELECT MIN(fetchedAt) FROM driver_season_stat WHERE season = (SELECT MAX(season) FROM driver_season_stat)") suspend fun statsFetchedAt(): Long?
}

@Database(
    entities = [DriverStandingEntity::class, ConstructorStandingEntity::class, RaceEntity::class,
        DriverSeasonStat::class, CircuitGeometryEntity::class, CircuitFactEntity::class, ArchiveStandingEntity::class,
        RaceResultEntity::class, CircuitWinnerEntity::class, CareerEntity::class],
    version = 9, exportSchema = false
)
abstract class FeedsDb : RoomDatabase() {
    abstract fun dao(): F1Dao
    companion object {
        @Volatile private var I: FeedsDb? = null
        /** feeds.db - deliberately separate from any future tasks.db.
         *  This one is a cache: delete it any time, it refetches. tasks.db will be
         *  the sacred one. Different backup rules, different lifecycles, never mixed. */
        fun get(ctx: Context): FeedsDb = I ?: synchronized(this) {
            I ?: Room.databaseBuilder(ctx.applicationContext, FeedsDb::class.java, "feeds.db")
                .fallbackToDestructiveMigration()   // safe: it's a cache
                .build().also { I = it }
        }
    }
}
