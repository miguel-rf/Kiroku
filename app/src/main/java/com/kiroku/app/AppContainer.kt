package com.kiroku.app

import android.content.Context
import com.kiroku.app.core.common.SystemClock
import com.kiroku.app.core.database.KirokuDatabase
import com.kiroku.app.core.network.AndroidNetworkMonitor
import com.kiroku.app.core.network.MangaUpdatesRemoteDataSource
import com.kiroku.app.core.network.NetworkMonitor
import com.kiroku.app.core.network.createMangaUpdatesApi
import com.kiroku.app.core.network.createNetworkJson
import com.kiroku.app.core.network.createOkHttpClient
import com.kiroku.app.data.repository.CatalogueRepository
import com.kiroku.app.data.repository.OfflineFirstCatalogueRepository

interface AppContainer {
    val catalogueRepository: CatalogueRepository
    val networkMonitor: NetworkMonitor
}

class DefaultAppContainer(
    context: Context,
) : AppContainer {
    private val applicationContext = context.applicationContext
    private val database: KirokuDatabase by lazy { KirokuDatabase.create(applicationContext) }
    private val json by lazy(::createNetworkJson)
    private val okHttpClient by lazy { createOkHttpClient() }
    private val remoteDataSource: MangaUpdatesRemoteDataSource by lazy {
        MangaUpdatesRemoteDataSource(
            api = createMangaUpdatesApi(okHttpClient, json),
            json = json,
        )
    }

    override val catalogueRepository: CatalogueRepository by lazy {
        OfflineFirstCatalogueRepository(
            database = database,
            remoteDataSource = remoteDataSource,
            clock = SystemClock,
        )
    }

    override val networkMonitor: NetworkMonitor by lazy {
        AndroidNetworkMonitor(applicationContext)
    }
}
