package com.kiroku.app.core.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface MangaUpdatesApi {
    @POST("series/search")
    suspend fun searchSeries(
        @Body request: SeriesSearchRequestDto,
    ): Response<SeriesSearchResponseDto>

    @GET("series/{id}")
    suspend fun getSeries(
        @Path("id") seriesId: Long,
    ): Response<SeriesDetailDto>
}
