package com.kiroku.app.core.database

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json

class DatabaseConverters {
    private val json =
        Json {
            ignoreUnknownKeys = false
            explicitNulls = false
        }

    @TypeConverter
    fun stringListToJson(value: List<String>): String = json.encodeToString(value)

    @TypeConverter
    fun jsonToStringList(value: String): List<String> = json.decodeFromString(value)
}
