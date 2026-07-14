package com.kiroku.app

import androidx.window.core.layout.WindowSizeClass
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KirokuAppTest {
    @Test
    fun layoutSwitchesAtStableMediumWidthBreakpoint() {
        assertFalse(WindowSizeClass(599, 800).isListDetailLayout())
        assertTrue(WindowSizeClass(600, 800).isListDetailLayout())
        assertTrue(WindowSizeClass(840, 800).isListDetailLayout())
    }

    @Test
    fun seriesRoute_roundTripsItsLongIdentifier() {
        val route = SeriesRoute(seriesId = 55_099_564_912L)

        val encoded = Json.encodeToString(SeriesRoute.serializer(), route)
        val restored = Json.decodeFromString(SeriesRoute.serializer(), encoded)

        assertEquals(route, restored)
    }
}
