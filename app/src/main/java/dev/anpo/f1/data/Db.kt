package dev.anpo.f1.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao interface F1Dao {
    @Query("SELECT * FROM driver_standing WHERE season = :s ORDER BY position") fun drivers(s: String): Flow<List<DriverStandingEntity>>
    @Query("SELECT * FROM constructor_standing WHERE season = :s ORDER BY position") fun teams(s: String): Flow<List<ConstructorStandingEntity>>
    @Query("SELECT * FROM race WHERE season = :s ORDER BY round") fun races(s: String): Flow<List<RaceEntity>>
    @Query("SELECT * FROM driver_standing WHERE driverId = :id LIMIT 1") suspend fun driver(id: String): DriverStandingEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putDrivers(x: List<DriverStandingEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putTeams(x: List<ConstructorStandingEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putRaces(x: List<RaceEntity>)
    @Query("SELECT MIN(fetchedAt) FROM driver_standing WHERE season = :s") suspend fun driversFetchedAt(s: String): Long?
    @Query("SELECT MIN(fetchedAt) FROM race WHERE season = :s") suspend fun racesFetchedAt(s: String): Long?
}

@Database(
    entities = [DriverStandingEntity::class, ConstructorStandingEntity::class, RaceEntity::class],
    version = 1, exportSchema = false
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
