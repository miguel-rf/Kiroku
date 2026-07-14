package com.kiroku.app.core.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KirokuDatabaseDeviceTest {
    @Test
    fun versionOneSchema_hasExpectedTablesAndPersistsAcrossReopen() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "kiroku-device-validation.db"
        context.deleteDatabase(databaseName)
        var database: KirokuDatabase? = null

        try {
            database =
                Room.databaseBuilder(context, KirokuDatabase::class.java, databaseName).build()
            val writableDatabase = database.openHelper.writableDatabase

            val actualTables =
                writableDatabase
                    .query(
                        """
                        SELECT name
                        FROM sqlite_master
                        WHERE type = 'table'
                          AND name NOT LIKE 'android_%'
                          AND name NOT LIKE 'room_%'
                          AND name NOT LIKE 'sqlite_%'
                        ORDER BY name
                        """.trimIndent(),
                    ).use { cursor ->
                        buildSet {
                            while (cursor.moveToNext()) add(cursor.getString(0))
                        }
                    }
            assertEquals(EXPECTED_TABLES, actualTables)

            val userVersion =
                writableDatabase.query("PRAGMA user_version").use { cursor ->
                    check(cursor.moveToFirst())
                    cursor.getInt(0)
                }
            assertEquals(1, userVersion)

            val identityHash =
                writableDatabase
                    .query("SELECT identity_hash FROM room_master_table WHERE id = 42")
                    .use { cursor ->
                        check(cursor.moveToFirst())
                        cursor.getString(0)
                    }
            assertEquals(EXPECTED_IDENTITY_HASH, identityHash)

            runBlocking {
                database.searchDao().insertRecentSearch(
                    RecentSearchEntity(
                        normalizedQuery = "one piece",
                        displayQuery = "One Piece",
                        searchedAtEpochMillis = 1L,
                    ),
                )
            }
            database.close()

            database =
                Room.databaseBuilder(context, KirokuDatabase::class.java, databaseName).build()
            val restoredSearches = runBlocking {
                database.searchDao().observeRecentSearches(limit = 8).first()
            }
            assertEquals(listOf("One Piece"), restoredSearches.map { it.displayQuery })
        } finally {
            database?.close()
            context.deleteDatabase(databaseName)
        }
    }

    private companion object {
        const val EXPECTED_IDENTITY_HASH = "0c7bbedd114167b238c5c1d2aad91db8"
        val EXPECTED_TABLES =
            setOf(
                "series_summaries",
                "series_details",
                "series_alternative_titles",
                "series_categories",
                "series_contributors",
                "series_publishers",
                "search_queries",
                "search_results",
                "search_remote_keys",
                "recent_searches",
            )
    }
}
