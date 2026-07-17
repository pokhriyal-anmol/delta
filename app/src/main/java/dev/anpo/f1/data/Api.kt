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

    /** F1's own CDN. headshot_url from OpenF1 points here; we build it ourselves
     *  so we don't need OpenF1 at all - which matters, because OpenF1's free tier
     *  goes fully dark during live sessions (see SECURITY.md / architecture doc). */
    fun headshot(key: String) = "https://media.formula1.com/content/dam/fom-website/drivers/$key.png"
}
