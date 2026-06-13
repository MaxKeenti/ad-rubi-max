package com.example.bachewatch.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoBoundsTest {

    @Test
    fun `contains accepts points inside the viewport`() {
        val bounds = GeoBounds(swLat = 19.20, swLng = -99.30, neLat = 19.55, neLng = -98.95)

        assertTrue(bounds.contains(lat = 19.40, lng = -99.10))
    }

    @Test
    fun `contains includes boundary latitude and longitude equality`() {
        val bounds = GeoBounds(swLat = 19.20, swLng = -99.30, neLat = 19.55, neLng = -98.95)

        assertTrue(bounds.contains(lat = 19.20, lng = -99.30))
        assertTrue(bounds.contains(lat = 19.55, lng = -98.95))
    }

    @Test
    fun `contains rejects points outside the viewport`() {
        val bounds = GeoBounds(swLat = 19.20, swLng = -99.30, neLat = 19.55, neLng = -98.95)

        assertFalse(bounds.contains(lat = 19.56, lng = -99.10))
        assertFalse(bounds.contains(lat = 19.40, lng = -99.31))
    }

    @Test
    fun `contains documents that antimeridian crossing is not handled`() {
        val crossingAntimeridian = GeoBounds(swLat = -10.0, swLng = 170.0, neLat = 10.0, neLng = -170.0)

        assertFalse(crossingAntimeridian.contains(lat = 0.0, lng = 179.0))
        assertFalse(crossingAntimeridian.contains(lat = 0.0, lng = -179.0))
    }
}
