package com.kiroku.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        SeriesSummaryEntity::class,
        SeriesDetailEntity::class,
        AlternativeTitleEntity::class,
        CategoryEntity::class,
        ContributorEntity::class,
        PublisherEntity::class,
        SearchQueryEntity::class,
        SearchResultEntity::class,
        SearchRemoteKeyEntity::class,
        RecentSearchEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
abstract class KirokuDatabase : RoomDatabase() {
    abstract fun seriesDao(): SeriesDao

    abstract fun searchDao(): SearchDao

    companion object {
        fun create(context: Context): KirokuDatabase = Room
            .databaseBuilder(
                context = context.applicationContext,
                klass = KirokuDatabase::class.java,
                name = "kiroku.db",
            ).build()
    }
}
