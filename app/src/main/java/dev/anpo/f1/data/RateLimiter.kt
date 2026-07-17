package dev.anpo.f1.data

import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
            val elapsedH = (now - last) / 3_600_000.0
            val refilled = (tokens + elapsedH * refillPerHour).coerceAtMost(capacity.toDouble())
            if (refilled >= 1.0) { tokens = refilled - 1.0; last = now; return@withLock }
            // Wait for the next token to drip in.
            val needH = (1.0 - refilled) / refillPerHour
            delay((needH * 3_600_000).toLong().coerceAtLeast(250L))
        }
    }

    fun remaining(): Int {
        val elapsedH = (System.currentTimeMillis() - last) / 3_600_000.0
        return (tokens + elapsedH * refillPerHour).coerceAtMost(capacity.toDouble()).toInt()
    }
}
