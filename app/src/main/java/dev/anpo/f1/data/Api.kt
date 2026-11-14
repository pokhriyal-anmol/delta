package dev.anpo.f1.data

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

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

    /** No logging interceptor in release - it would print URLs to logcat where any
     *  app with READ_LOGS could read them. Debug only, and even then keep it BASIC. */
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

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
}
