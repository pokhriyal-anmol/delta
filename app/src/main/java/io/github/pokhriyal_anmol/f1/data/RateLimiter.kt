package io.github.pokhriyal_anmol.f1.data

import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Tokens after time passes. A clock that moved backwards (a manual change, a bad network time sync)
 * counts as no time passing. Treating it as negative time once froze every request for as long as
 * the clock had been set back.
 */
internal fun refilled(tokens: Double, lastMs: Long, nowMs: Long, capacity: Int, perHour: Int): Double =
    (tokens + (nowMs - lastMs).coerceAtLeast(0L) / 3_600_000.0 * perHour).coerceAtMost(capacity.toDouble())

/**
 * Jolpica allows 200 requests/hour unauthenticated. That is NOT generous, and it
 * will bite during development long before it bites in production - hot-reloading
 * a screen that fetches on composition burns through it in minutes.
 *
 * Token bucket, persisted across process death (an in-memory limiter resets every
 * time the app is killed, which is exactly when you'd blow the quota).
 */
class RateLimiter(
    ctx: Context,
    private val name: String,
    private val capacity: Int = 200,
    private val refillPerHour: Int = 200,
) {
    private val prefs = ctx.getSharedPreferences("ratelimit_$name", Context.MODE_PRIVATE)
    private val mutex = Mutex()

    private var tokens: Double
        get() = prefs.getFloat("tokens", capacity.toFloat()).toDouble()
        set(v) = prefs.edit().putFloat("tokens", v.toFloat()).apply()
    private var last: Long
        get() = prefs.getLong("last", System.currentTimeMillis())
        set(v) = prefs.edit().putLong("last", v).apply()

    /** Suspends until a token is available. Never throws; back-pressure, not failure. */
    suspend fun acquire() = mutex.withLock {
        while (true) {
            val now = System.currentTimeMillis()
            val available = refilled(tokens, last, now, capacity, refillPerHour)
            if (available >= 1.0) { tokens = available - 1.0; last = now; return@withLock }
            // Wait for the next token to drip in.
            val needH = (1.0 - available) / refillPerHour
            delay((needH * 3_600_000).toLong().coerceAtLeast(250L))
        }
    }

    fun remaining(): Int = refilled(tokens, last, System.currentTimeMillis(), capacity, refillPerHour).toInt()
}
