package dev.anpo.f1.data

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

const val USER_AGENT = "Delta/${dev.anpo.f1.BuildConfig.VERSION_NAME} (+https://github.com/pokhriyal-anmol/delta)"

/** Seconds from a Retry-After header, as milliseconds, capped so a bad value cannot stall a request. A date form or junk gives null. */
fun retryAfterMs(header: String?, capMs: Long = 10_000L): Long? =
    header?.trim()?.toLongOrNull()?.takeIf { it >= 0 }?.let { (it * 1000L).coerceAtMost(capMs) }

interface JolpicaApi {
    @GET("ergast/f1/{season}/driverstandings/")
    suspend fun driverStandings(@Path("season") season: String, @Query("limit") limit: Int = 30): JolpicaEnvelope

    @GET("ergast/f1/{season}/constructorstandings/")
    suspend fun constructorStandings(@Path("season") season: String, @Query("limit") limit: Int = 30): JolpicaEnvelope

    @GET("ergast/f1/{season}/races/")
    suspend fun races(@Path("season") season: String, @Query("limit") limit: Int = 30): JolpicaEnvelope

    @GET("ergast/f1/{season}/{round}/results/")
    suspend fun results(@Path("season") season: String, @Path("round") round: Int): JolpicaEnvelope

    /** Season results, paginated. Jolpica CAPS limit at 100 regardless of what you ask
     *  for, so a full season (~200 rows) is 2 requests via offset. Cached daily, this
     *  derives podiums, poles, DNFs and points-per-round for every driver. */
    /** Every winner at a circuit, oldest first. Each carries the laps the winner completed. */
    @GET("ergast/f1/circuits/{id}/results/1/")
    suspend fun circuitWinners(@Path("id") circuitId: String, @Query("limit") limit: Int = 100): JolpicaEnvelope

    /** Total number of results rows at one finishing position (read `total`, limit 1). scope is "drivers" or "constructors". */
    @GET("ergast/f1/{scope}/{id}/results/{pos}/")
    suspend fun careerResults(@Path("scope") scope: String, @Path("id") id: String, @Path("pos") pos: Int,
        @Query("limit") limit: Int = 1): JolpicaEnvelope

    @GET("ergast/f1/{scope}/{id}/qualifying/1/")
    suspend fun careerPoles(@Path("scope") scope: String, @Path("id") id: String, @Query("limit") limit: Int = 1): JolpicaEnvelope

    @GET("ergast/f1/{scope}/{id}/seasons/")
    suspend fun careerSeasons(@Path("scope") scope: String, @Path("id") id: String, @Query("limit") limit: Int = 100): JolpicaEnvelope

    @GET("ergast/f1/{season}/results/")
    suspend fun seasonResults(@Path("season") season: String,
        @Query("limit") limit: Int = 100, @Query("offset") offset: Int = 0): JolpicaEnvelope
}

interface MultiViewerApi {
    /** Circuit outline for a given MultiViewer circuit key and year. Free, no auth,
     *  real geometry for all 22 tracks (Spa = key 7). ~1005 points + rotation. */
    @GET("api/v1/circuits/{key}/{year}")
    suspend fun circuit(@Path("key") key: Int, @Path("year") year: Int): MvCircuit
}

object Net {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    /** Jolpica requires a custom User-Agent, and a 429 means wait: honour Retry-After once, then let the error through. */
    private val polite = Interceptor { chain ->
        val request = chain.request().newBuilder().header("User-Agent", USER_AGENT).build()
        var response = chain.proceed(request)
        if (response.code == 429) retryAfterMs(response.header("Retry-After"))?.let { wait ->
            response.close(); Thread.sleep(wait); response = chain.proceed(request)
        }
        response
    }

    /** No logging interceptor in release - it would print URLs to logcat where any
     *  app with READ_LOGS could read them. Debug only, and even then keep it BASIC. */
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor(polite)
        .build()

    /** One feed as text, refusing anything larger than MAX_FEED_BYTES. Cleartext is blocked app-wide, so a redirect to http fails. */
    suspend fun feed(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { r ->
            check(r.isSuccessful) { "HTTP ${r.code}" }
            val source = r.body!!.source()
            source.request(MAX_FEED_BYTES + 1L)
            check(source.buffer.size <= MAX_FEED_BYTES) { "feed larger than $MAX_FEED_BYTES bytes" }
            source.readUtf8()
        }
    }

    val jolpica: JolpicaApi = Retrofit.Builder()
        .baseUrl("https://api.jolpi.ca/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(JolpicaApi::class.java)

    /** Not rate-limited like Jolpica, and unrelated to OpenF1 - MultiViewer serves
     *  static circuit geometry, so it stays up during live sessions. */
    val multiviewer: MultiViewerApi = Retrofit.Builder()
        .baseUrl("https://api.multiviewer.app/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(MultiViewerApi::class.java)

    /** F1's own CDN. headshot_url from OpenF1 points here; we build it ourselves
     *  so we don't need OpenF1 at all - which matters, because OpenF1's free tier
     *  goes fully dark during live sessions (see SECURITY.md / architecture doc). */
    fun headshot(key: String) = "https://media.formula1.com/content/dam/fom-website/drivers/$key.png"

    /** The 2026 side-on car, transparent, from the same CDN. Pinned by team folder (see Repo.carFolder).
     *  The season is in the path, so next year this needs one edit; until then a 404 just shows no car. */
    fun car(folder: String) =
        "https://media.formula1.com/image/upload/c_lfill,w_1200/q_auto/v1740000001/common/f1/2026/$folder/2026${folder}carright.webp"
}
