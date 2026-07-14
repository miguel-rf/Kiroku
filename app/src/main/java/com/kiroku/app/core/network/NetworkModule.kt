package com.kiroku.app.core.network

import com.kiroku.app.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

const val MANGA_UPDATES_BASE_URL = "https://api.mangaupdates.com/v1/"

data class NetworkTimeouts(
    val connectMillis: Long = 10_000L,
    val readMillis: Long = 20_000L,
    val writeMillis: Long = 20_000L,
    val callMillis: Long = 30_000L,
) {
    init {
        require(connectMillis > 0L)
        require(readMillis > 0L)
        require(writeMillis > 0L)
        require(callMillis > 0L)
    }
}

fun createNetworkJson(): Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
    isLenient = false
    coerceInputValues = false
}

fun createOkHttpClient(
    isDebug: Boolean = BuildConfig.DEBUG,
    timeouts: NetworkTimeouts = NetworkTimeouts(),
): OkHttpClient {
    val builder =
        OkHttpClient
            .Builder()
            .connectTimeout(timeouts.connectMillis, TimeUnit.MILLISECONDS)
            .readTimeout(timeouts.readMillis, TimeUnit.MILLISECONDS)
            .writeTimeout(timeouts.writeMillis, TimeUnit.MILLISECONDS)
            .callTimeout(timeouts.callMillis, TimeUnit.MILLISECONDS)
            .addInterceptor(
                Interceptor { chain ->
                    val userAgent =
                        "Kiroku/" +
                            BuildConfig.VERSION_NAME +
                            " (unofficial MangaUpdates client)"
                    val request =
                        chain
                            .request()
                            .newBuilder()
                            .header(name = "User-Agent", value = userAgent)
                            .header(name = "Accept", value = "application/json")
                            .build()
                    chain.proceed(request)
                },
            )

    if (isDebug) {
        builder.addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
                redactHeader("Authorization")
                redactHeader("Cookie")
                redactHeader("Set-Cookie")
            },
        )
    }
    return builder.build()
}

fun createMangaUpdatesApi(
    client: OkHttpClient,
    json: Json,
    baseUrl: String = MANGA_UPDATES_BASE_URL,
): MangaUpdatesApi = Retrofit
    .Builder()
    .baseUrl(baseUrl)
    .client(client)
    .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
    .build()
    .create(MangaUpdatesApi::class.java)
