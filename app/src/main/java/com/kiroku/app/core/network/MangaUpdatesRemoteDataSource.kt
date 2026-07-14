package com.kiroku.app.core.network

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class MangaUpdatesRemoteDataSource(
    private val api: MangaUpdatesApi,
    private val json: Json,
) {
    suspend fun searchSeries(request: SeriesSearchRequestDto): SeriesSearchResponseDto = execute { api.searchSeries(request) }

    suspend fun getSeries(seriesId: Long): SeriesDetailDto = execute { api.getSeries(seriesId) }

    private suspend fun <T> execute(block: suspend () -> Response<T>): T {
        val response =
            try {
                block()
            } catch (failure: AppFailure) {
                throw failure
            } catch (failure: SocketTimeoutException) {
                throw AppFailure(FailureKind.TIMEOUT, cause = failure)
            } catch (failure: InterruptedIOException) {
                throw AppFailure(FailureKind.TIMEOUT, cause = failure)
            } catch (failure: UnknownHostException) {
                throw AppFailure(FailureKind.OFFLINE, cause = failure)
            } catch (failure: ConnectException) {
                throw AppFailure(FailureKind.OFFLINE, cause = failure)
            } catch (failure: SerializationException) {
                throw AppFailure(FailureKind.SERIALIZATION, cause = failure)
            } catch (failure: IOException) {
                val serializationCause =
                    generateSequence(failure.cause, Throwable::cause)
                        .filterIsInstance<SerializationException>()
                        .firstOrNull()
                if (serializationCause != null) {
                    throw AppFailure(FailureKind.SERIALIZATION, cause = serializationCause)
                }
                throw AppFailure(FailureKind.OFFLINE, cause = failure)
            } catch (failure: Throwable) {
                throw AppFailure(FailureKind.UNKNOWN, cause = failure)
            }

        if (response.isSuccessful) {
            return response.body()
                ?: throw AppFailure(
                    kind = FailureKind.SERIALIZATION,
                    diagnosticReason = "Successful response did not contain a JSON body",
                )
        }

        val error =
            response
                .errorBody()
                ?.string()
                ?.let { body ->
                    runCatching { json.decodeFromString<ApiErrorDto>(body) }.getOrNull()
                }
        val retryAfterSeconds = response.headers()["Retry-After"]?.trim()?.toLongOrNull()
        throw AppFailure(
            kind =
            when (response.code()) {
                400 -> FailureKind.VALIDATION
                401 -> FailureKind.AUTHENTICATION
                403 -> FailureKind.PERMISSION
                404 -> FailureKind.NOT_FOUND
                408 -> FailureKind.TIMEOUT
                429 -> FailureKind.RATE_LIMITED
                in 500..599 -> FailureKind.SERVER
                else -> FailureKind.UNKNOWN
            },
            diagnosticReason = error?.reason,
            retryAfterSeconds = retryAfterSeconds,
        )
    }
}
