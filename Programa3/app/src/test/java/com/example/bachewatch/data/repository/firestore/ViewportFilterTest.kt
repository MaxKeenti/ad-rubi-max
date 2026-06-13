package com.example.bachewatch.data.repository.firestore

import com.example.bachewatch.data.model.GeoBounds
import com.example.bachewatch.data.model.Reporte
import com.google.firebase.Timestamp
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Test

class ViewportFilterTest {

    private val bounds = GeoBounds(swLat = 19.20, swLng = -99.30, neLat = 19.55, neLng = -98.95)

    @Test
    fun `filtrarViewport keeps only reports inside the viewport`() {
        val inside = reporte(id = "inside", lat = 19.40, lng = -99.10)
        val northOutside = reporte(id = "north", lat = 19.56, lng = -99.10)
        val westOutside = reporte(id = "west", lat = 19.40, lng = -99.31)

        val result = filtrarViewport(listOf(inside, northOutside, westOutside), bounds)

        assertEquals(listOf("inside"), result.map { it.id })
    }

    @Test
    fun `filtrarViewport includes boundary matches`() {
        val sw = reporte(id = "sw", lat = 19.20, lng = -99.30)
        val ne = reporte(id = "ne", lat = 19.55, lng = -98.95)

        val result = filtrarViewport(listOf(sw, ne), bounds)

        assertEquals(listOf("sw", "ne"), result.map { it.id })
    }

    @Test
    fun `filtrarViewport drops geohash false positives outside the viewport`() {
        val realHit = reporte(id = "real", lat = 19.35, lng = -99.10, geohash = "9g3")
        val falsePositive = reporte(id = "false-positive", lat = 19.19, lng = -99.10, geohash = "9g3")

        val result = filtrarViewport(listOf(realHit, falsePositive), bounds)

        assertEquals(listOf("real"), result.map { it.id })
    }

    @Test
    fun `filtrarViewport dedupes overlapping geohash query results`() {
        val first = reporte(id = "same", lat = 19.40, lng = -99.10, descripcion = "first")
        val duplicate = reporte(id = "same", lat = 19.41, lng = -99.11, descripcion = "duplicate")

        val result = filtrarViewport(listOf(first, duplicate), bounds)

        assertEquals(listOf(first), result)
    }

    @Test
    fun `filtrarViewport excludes soft deleted reports`() {
        val active = reporte(id = "active", lat = 19.40, lng = -99.10)
        val deleted = reporte(
            id = "deleted",
            lat = 19.40,
            lng = -99.10,
            deletedAt = Timestamp(Date(0L)),
        )

        val result = filtrarViewport(listOf(active, deleted), bounds)

        assertEquals(listOf("active"), result.map { it.id })
    }

    private fun reporte(
        id: String,
        lat: Double,
        lng: Double,
        geohash: String = "",
        descripcion: String? = null,
        deletedAt: Timestamp? = null,
    ): Reporte = Reporte(
        id = id,
        lat = lat,
        lng = lng,
        geohash = geohash,
        descripcion = descripcion,
        deletedAt = deletedAt,
    )
}
