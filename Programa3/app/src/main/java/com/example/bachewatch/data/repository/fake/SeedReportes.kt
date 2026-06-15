package com.example.bachewatch.data.repository.fake

import com.example.bachewatch.data.auth.FakeSesionAnonima
import com.example.bachewatch.data.model.Reporte
import com.example.bachewatch.data.model.Severidad
import com.example.bachewatch.data.model.TipoIncidencia
import com.google.firebase.Timestamp
import java.util.Date

/**
 * CDMX demo potholes — doubles as emulator demo data for Melanie
 * (work-division §1). Ages are staggered so relative dates and the
 * 24 h delete window are visible: seed-01 is own & <24 h (Eliminar
 * shows), seed-04 is own & >24 h (Eliminar hidden).
 */
internal fun seedReportes(): List<Reporte> {
    val ahora = System.currentTimeMillis()
    fun hace(horas: Double) = Timestamp(Date(ahora - (horas * 3_600_000).toLong()))
    fun reporte(
        n: Int,
        lat: Double,
        lng: Double,
        severidad: Severidad?,
        descripcion: String?,
        horas: Double,
        confirmaciones: Long,
        propio: Boolean = false,
        precision: Double = 12.0,
        tipo: TipoIncidencia = TipoIncidencia.BACHE,
    ): Reporte {
        val id = "seed-%02d".format(n)
        return Reporte(
            id = id,
            tipo = tipo,
            lat = lat,
            lng = lng,
            geohash = "", // fakes filter by lat/lng; the real impl writes one (task 06)
            accuracyMeters = precision,
            severidad = severidad,
            descripcion = descripcion,
            fotoPath = "reportes/$id.jpg",
            fotoUrl = "https://picsum.photos/seed/bache$n/800/600",
            createdBy = if (propio) FakeSesionAnonima.FAKE_UID else "uid-vecino-$n",
            confirmCount = confirmaciones,
            serverWrittenAt = hace(horas),
        )
    }

    return listOf(
        reporte(1, 19.3962, -99.0907, Severidad.SEVERO,
            "Hundimiento grande frente a UPIICSA, carril derecho", 3.0, 4, propio = true, precision = 9.0),
        reporte(2, 19.3989, -99.0850, Severidad.MODERADO,
            "Bache en Av. Canal de Apatlaco, junto al tope", 7.0, 2),
        reporte(3, 19.4337, -99.1400, Severidad.LEVE,
            null, 1.0, 0, precision = 28.0),
        reporte(4, 19.4194, -99.1620, Severidad.MODERADO,
            "Álvaro Obregón esquina Orizaba", 26.0, 5, propio = true),
        reporte(5, 19.3556, -99.1626, Severidad.SEVERO,
            "Cráter en Av. Universidad, ya se llevó dos rines", 12.0, 7, precision = 6.0),
        reporte(6, 19.3960, -99.1530, null,
            "Coladera sin tapa en Eje 5 Sur", 5.0, 1, tipo = TipoIncidencia.OTRO),
        reporte(7, 19.4180, -99.1430, Severidad.MODERADO,
            null, 48.0, 3),
        reporte(8, 19.3570, -99.0550, Severidad.SEVERO,
            "Ermita Iztapalapa, carril de baja velocidad", 30.0, 6, precision = 18.0),
        reporte(9, 19.4255, -99.1130, Severidad.LEVE,
            "Pequeño pero crece con cada lluvia", 0.5, 0),
        reporte(10, 19.3690, -99.1410, null,
            null, 90.0, 2, precision = 22.0),
        reporte(11, 19.3890, -99.0680, Severidad.MODERADO,
            "Agrícola Oriental, junto a la escuela", 16.0, 1),
        reporte(12, 19.3850, -99.1620, Severidad.LEVE,
            "Del Valle, bache de banqueta a banqueta", 60.0, 0),
    )
}
